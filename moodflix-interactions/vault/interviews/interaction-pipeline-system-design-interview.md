# MoodFlix — Interaction Pipeline: System Design & Interview Defense

> Comprehensive interview guide and deep-dive technical defense notes for senior/staff engineering loops.
> Mapped specifically to Mastercard interview rounds: Veroon (Architecture), Ashutosh (Behavioral/Ownership), and the Bar Raiser (Technical Depth).

---

## 1. Interview Persona Mapping

### Round 1: Veroon (Architecture & Concurrency)
* **Core Topics:** Event-driven architecture, distributed stream partitioning, ordering guarantees, idempotency, backpressure.
* **Key Visual:** Draw the Gateway $\to$ Kafka $\to$ Worker $\to$ Redis/Postgres pipeline.
* **What Veroon is testing:** Can you explain how you prevent race conditions, avoid blocking I/O, and handle out-of-order event delivery?

### Round 2: Ashutosh (Behavioral & Engineering Ownership)
* **Core Topics:** Problem identification, taking initiative, architectural trade-offs, system health metrics.
* **The Pitch:** *"During my audit of the gateway, I identified that computing 384-dimensional vector shifts and database updates synchronously on HTTP requests risked starving Netty event loops under high concurrency. I took ownership of redesigning the interaction pipeline into an asynchronous, reactive event-driven system with Kafka, decoupling ingestion from heavy computation."*

### Round 3: Bar Raiser (Deep Failure Modes & Trade-offs)
* **Core Topics:** Exact failure recovery, split-brain rebalances, Two-Phase Claim-Mutate-Confirm idempotency, Eventual Consistency vs. Strict Consistency.
* **What the Bar Raiser is testing:** Do you understand the boundary conditions when Redis or PostgreSQL fails midway through a transaction?

---

## 2. The 4 Deep-Dive Failure Modes & Exact Answers

### Q1: "How do you handle duplicate events? What if a user rapidly taps 'Like'?"
> **The Problem:** A mobile client on a jittery network retries an HTTP request, or a user double-taps "Like". If the Gateway assigns random UUIDs, 2 different events arrive for the same user interaction. If processed twice, vector math is cumulative ($+0.1 + 0.1 = +0.2$), distorting user recommendations through **vector drift**.
>
> **The Solution (Deterministic Business Key):**
> 1. The Gateway constructs a deterministic idempotency key derived from the business identity:
>    $$\text{eventId} = \text{SHA256}(\text{userId} + \text{":"} + \text{movieId} + \text{":"} + \text{action})$$
> 2. Even if 5 requests arrive at the Gateway within seconds, they all produce the exact same `eventId`.
> 3. The worker consumer uses Redis `SETNX idempotency:event:{eventId}`. The first event acquires the lock and processes; the remaining 4 are dropped immediately.

---

### Q2: "What if the consumer crashes midway through processing an event?"
> **The Problem:** If we acknowledge/commit the Kafka offset before processing (at-most-once), a consumer crash causes message loss. If we acknowledge after processing (at-least-once), a crash right after DB save causes Kafka to re-deliver the event.
>
> **The Solution (At-Least-Once Delivery + Idempotent Consumer):**
> 1. We commit offsets **after** the pipeline executes successfully.
> 2. If the consumer crashes after saving to Postgres but before committing the Kafka offset, Kafka re-delivers the event to another consumer pod.
> 3. The new consumer checks Redis: `idempotency:event:{eventId}` is already marked `"PROCESSED"`.
> 4. The duplicate event is safely skipped, and the Kafka offset is committed.
> 5. **Formula:** *At-least-once transport + Idempotent consumer = Effectively-once processing.*

---

### Q3: "What if Redis is down or times out when the consumer reads an event?"
> **The Problem:** If the idempotency gate is unreachable, should the worker proceed blindly to update PostgreSQL?
>
> **The Solution (Fail-Fast & DLQ):**
> 1. **Never bypass the gate:** If Redis fails, the consumer must fail fast to avoid processing duplicate events without protection.
> 2. The consumer throws a transient exception (`RedisConnectionException`).
> 3. Spring Kafka intercepts the error and executes **exponential backoff retries** (e.g. 1s, 2s, 4s).
> 4. If Redis remains unreachable after retries are exhausted, the message is routed to the **Dead Letter Queue (DLQ)** topic (`user-interactions-dlq`) for inspection. Zero data loss.

---

### Q4: "What if saving to PostgreSQL fails after claiming the event in Redis?"
> **The Problem:** If the consumer sets `eventId` in Redis, but PostgreSQL write fails (e.g. connection pool exhausted or optimistic lock failure), will future Kafka retries be blocked by Redis thinking it was already processed?
>
> **The Solution (Two-Phase Claim-Mutate-Confirm & Compensating Deletion):**
> 1. **Phase 1 (Claim):** Worker sets `SETNX idempotency:event:{eventId} "PROCESSING" EX 30` (30-second lease).
> 2. **Phase 2 (Mutate):** If PostgreSQL throws an exception:
>    * The `catch` block executes a **compensating transaction**: it deletes the Redis key `idempotency:event:{eventId}`.
>    * It re-throws the exception to let Spring Kafka retry.
> 3. **Phase 3 (Confirm):** Only when PostgreSQL save succeeds does the worker update the Redis key to `"PROCESSED"` with a 24-hour TTL.
> 4. **Handling OptimisticLockException:** If another service updated the user row concurrently, JPA detects version mismatch. The worker catches `OptimisticLockingFailureException`, re-fetches the latest user vector, re-applies the $+0.1$ shift to the fresh vector, and re-saves.

---

### Q5: "How do you prevent race conditions if a user is active on two devices?"
> **The Solution (Partitioning by User ID):**
> 1. The Gateway sets the Kafka message **key** to `userId`.
> 2. Kafka's default partitioner hashes `hash(userId) % num_partitions`. All events for a specific user are guaranteed to land on the **exact same partition**.
> 3. In Kafka's consumer group protocol, a single partition is assigned to **exactly one consumer thread at a time**.
> 4. Therefore, events for a user are processed strictly in sequential, single-file order—eliminating consumer race conditions.

---

## 3. Key Architectural Trade-Offs

### Trade-off 1: Immediate Stream Update vs. Micro-Batching
* **Immediate Stream Update (Our Choice):**
  * *Pros:* User feedback loop is instantaneous ($<100\text{ms}$). User likes a sci-fi movie, immediately searches, and recommendations reflect the new taste.
  * *Cons:* Higher database write IOPS under heavy swipe bursts.
* **Micro-Batching (The Scaling Evolution):**
  * *Concept:* Buffer events in Redis or via `batchListener = true` for 5 seconds; aggregate 10 likes into a single vector shift and 1 DB write.
  * *Trade-off:* Saves 80% DB writes, but recommendations lag 5–10 seconds behind user actions.

### Trade-off 2: Eventual Consistency vs. Strict ACID Consistency
* **Our Pipeline:** Uses **Eventual Consistency**.
* **Rationale:** Strong ACID distributed transactions (like 2-Phase Commit across Gateway, Kafka, Redis, and Postgres) introduce latency ($>200\text{ms}$) and catastrophic cascading failures if any single node is slow. Eventual consistency provides sub-5ms ingestion response times, absorbing spikes gracefully.

---

## 4. Non-Functional Requirements (NFR) Cheat Sheet

| NFR | Architectural Implementation |
| :--- | :--- |
| **Resilience** | Decoupled Kafka buffer, non-blocking retries with exponential backoff, Dead Letter Queue (DLQ). |
| **Consistency** | Two-Phase Idempotency Gate (Redis SETNX) + Optimistic Concurrency Control (`@Version`). |
| **Concurrency & Ordering** | Kafka topic partitioned by `userId`, serializing events per user to a single thread. |
| **High Throughput** | Gateway offloads math to Kafka; returns HTTP 202 Accepted in $<5\text{ms}$. |
| **Observability** | `correlationId` propagated from Gateway to Kafka header to consumer logs. Actuator consumer lag metrics exposed. |
