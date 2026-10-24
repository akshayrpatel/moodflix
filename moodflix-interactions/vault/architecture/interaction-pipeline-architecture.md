# MoodFlix — Interaction Pipeline Architecture

> Architectural specification for the event-driven user interaction and dynamic taste-vector update pipeline.
> Services involved: `moodflix-gateway`, `moodflix-interactions`, Apache Kafka, Redis, PostgreSQL (`moodflix_dev`).

---

## 1. High-Level Architecture Flow

```
[ User Action: Like / Dislike / Skip ]
                 │
                 ▼ (HTTP POST /profile/interactions)
        ┌───────────────────────────────────┐
        │        moodflix-gateway           │
        │  • Validates JWT                  │
        │  • Attaches Correlation ID        │
        │  • Deterministic Key Generation   │
        │  • Publishes to Kafka             │
        │  • Returns 202 Accepted (<5ms)    │
        └─────────────────┬─────────────────┘
                          │
                          ▼ (Partition Key: userId)
        ┌───────────────────────────────────┐
        │      Apache Kafka Broker          │
        │  Topic: user-interactions-topic   │
        │  Partitions: 3                    │
        └─────────────────┬─────────────────┘
                          │
                          ▼ (Consumer Group: moodflix-interactions-group)
        ┌───────────────────────────────────┐
        │      moodflix-interactions        │
        │  (VectorWorker Consumer)          │
        │                                   │
        │  1. Claim in Redis (SETNX 30s)    │
        │  2. Fetch Vectors (User & Movie)  │
        │  3. Vector Math + L2 Normalization│
        │  4. Save Postgres (OptimisticLock)│
        │  5. Confirm in Redis (24h TTL)    │
        │  6. Evict Gateway Cache           │
        │  7. Commit Kafka Offset           │
        └──────────────┬─────────────┬──────┘
                       │             │
                       ▼             ▼
             ┌──────────────┐   ┌──────────────┐
             │  PostgreSQL  │   │    Redis     │
             │ users table  │   │ Idempotency  │
             │ (pgvector)   │   │ Vector Cache │
             └──────────────┘   └──────────────┘
```

---

## 2. Component Responsibilities

| Component | Technology | Primary Role |
| :--- | :--- | :--- |
| **Gateway (Producer)** | Java 21, Spring Boot 4, WebFlux | Authenticates incoming clicks, constructs `InteractionEvent`, hashes deterministic idempotency key, publishes to Kafka with key=`userId`, immediately returns `202 Accepted` to keep Netty threads unblocked. |
| **Message Broker** | Apache Kafka (KRaft mode) | Asynchronous buffer and decoupling pipe. Partitions events by `userId` to guarantee strict chronological per-user ordering. |
| **Worker (Consumer)** | Java 21, Spring Boot 4, Spring Kafka, JPA | Consumes events, enforces distributed idempotency, computes weighted vector adjustments, updates PostgreSQL with optimistic locking, and invalidates Redis cache. |
| **Cache & Gate** | Redis 7 | 1) **Idempotency Gate:** Atomic `SETNX` preventing duplicate calculation.<br>2) **Cache Eviction:** Deletes `vector:taste:{userId}` so the Gateway fetches the updated vector on the next user query. |
| **Durable Store** | PostgreSQL 18 (`moodflix_dev`) | Source-of-truth storage for `users.taste_vector` (384-dim vector) with `@Version` concurrency control. |

---

## 3. Data Contracts & Event Schema

### Kafka Message Contract: `InteractionEvent`
* **Topic:** `user-interactions-topic`
* **Message Key:** `userId` (UUID string) — guarantees all interactions for a user route to the same partition and single consumer thread.
* **Payload:**
```json
{
  "eventId": "SHA256(userId:movieId:action)",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "movieId": "550",
  "interactionType": "LIKE",
  "weight": 0.1,
  "timestamp": "2026-10-26T20:00:00Z",
  "correlationId": "corr-abc-123"
}
```

---

## 4. Redis Key Registry & Lifecycles

| Redis Key Pattern | Type | Value | TTL | Managed By | Lifecycle |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `idempotency:event:{eventId}` | String | `"PROCESSING"` $\to$ `"PROCESSED"` | Phase 1: `30s`<br>Phase 2: `24h` | `moodflix-interactions` | Claimed before math, updated on DB success. Never manually deleted unless DB write fails. |
| `vector:taste:{userId}` | String (JSON float[]) | 384-dim vector | `1 hour` | `moodflix-gateway` / `interactions` | Set by Gateway on search; evicted by `interactions` immediately after updating PostgreSQL. |

---

## 5. The Two-Phase "Claim-Mutate-Confirm" Execution Order

```
[Kafka Event Arrives]
        │
        ▼
Step 1: CLAIM IN REDIS (Atomic SETNX)
        Key: idempotency:event:{eventId}
        Value: "PROCESSING"
        TTL: 30 seconds
        ├─ If FALSE (Key exists) ──> Duplicate or in-flight. Log, ACK Kafka offset, and return.
        └─ If TRUE (Key acquired) ─> Proceed to Step 2.
        │
        ▼
Step 2: FETCH & VECTOR ARITHMETIC
        • Fetch User Vector from Postgres: users.taste_vector
        • Fetch Movie Vector: movie embedding (384-dim)
        • Calculate Shift: V_temp = V_user + (weight * V_movie)
        • L2 Normalization: V_new = V_temp / ||V_temp||_2
        │
        ▼
Step 3: PERSIST WITH OPTIMISTIC LOCKING
        • user.setTasteVector(V_new)
        • userRepository.save(user)  --> checks WHERE id = ? AND version = ?
        │
        ├── [FAILURE / EXCEPTION?]
        │    ├── Catch Block: Delete Redis key idempotency:event:{eventId}
        │    └── Re-throw Exception --> Triggers Spring Kafka retry / backoff
        │
        ▼
Step 4: CONFIRM & INVALIDATE
        • Update Redis idempotency key to "PROCESSED" with 24-hour TTL
        • Delete Redis Gateway cache: vector:taste:{userId}
        │
        ▼
Step 5: ACKNOWLEDGE KAFKA OFFSET
        • Offset committed to Kafka (At-least-once safely achieved).
```
