# MoodFlix Gateway

Spring Cloud Gateway that sits between the MoodFlix frontend and the downstream services. Every request the browser makes terminates here first: authentication, risk checks, and taste-vector injection all happen before anything reaches the AI engine.

## Tech Stack

- Java 21 / Spring Boot 4 / Spring Cloud 2025
- Spring Cloud Gateway (WebFlux) — fully reactive, non-blocking
- Spring Security Reactive + JJWT for stateless JWT auth
- R2DBC + PostgreSQL with pgvector for user storage and 384-dim taste vectors
- Redis for session state, rate limits, and vector cache
- Project Reactor (`Mono`/`Flux`) end-to-end

## Responsibilities

### Auth

Register and login endpoints issue a JWT signed with HS256. The frontend stores it in an httpOnly cookie — the browser never sees the token directly. `JwtAuthFilter` validates the JWT on every non-public request and attaches an `AuthenticatedPrincipal` to the reactive context.

### Taste Vector Injection

After authentication, a global filter looks up the user's taste vector (cached in Redis, backed by Postgres) and injects it as a header before forwarding to the AI engine. The downstream service never queries user data itself — the gateway is the single source of identity.

### Onboarding

A 4-step mood quiz from the UI lands at `/onboarding/complete`. The gateway builds a seed taste vector from the mood selections and persists it to the user's row. This vector shapes every search and discover call the user makes afterwards.

### Risk & Rate Limiting

Three reactive filters run on every request: an adaptive rate limiter keyed on session, a contextual risk scorer, and a threat-pattern detector. Each can short-circuit the chain with a 429 or 403 before the request ever reaches a downstream service.

## Security

The gateway is the only component that holds the JWT secret and the only one with write access to the users table. Downstream services trust the gateway's injected headers — they do not re-validate tokens. This keeps the AI engine stateless and means a compromised downstream service cannot mint credentials.

- **Stateless JWT** — no server-side session; horizontal scaling is free.
- **BCrypt password hashing** — adaptive work factor, upgradeable without invalidating existing hashes.
- **Uniform error messages** — wrong username and wrong password return identical responses to prevent user enumeration.
- **CORS locked to the frontend origin** — `localhost:3000` in dev; configured per environment.

## Engineering Decisions

- **Reactive all the way down** — blocking I/O in a gateway is a self-inflicted bottleneck. JPA calls are bridged via `Schedulers.boundedElastic()`; everything else stays on the event loop.
- **Feature-first packages** (`auth/`, `onboarding/`, `profile/`, `risk/`) over layer-first (`controller/`, `service/`, `repository/`) — code that changes together lives together.
- **Gateway owns identity, not the AI engine** — the engine is a pure compute service. All user state, auth, and access control are centralized here.
- **Redis for hot paths, Postgres for truth** — taste vectors and sessions read from Redis on the request path; Postgres is consulted only on cache miss or write.
