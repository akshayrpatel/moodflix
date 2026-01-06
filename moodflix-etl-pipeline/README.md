# MoodFlix Ingestion Pipeline

The ingestion pipeline is MoodFlix's content backbone. It fetches movie and TV metadata from TMDB, enriches each item with LLM-generated moods and analysis, and writes the result to PostgreSQL and ChromaDB so downstream services can query it semantically.

## Pipeline Components

### FetcherWorker

Producer thread. Fetches TMDB pages, normalizes each item into a `ContentItem`, and pushes batched `ContentPackage`s onto a Redis queue. Maintains a checkpoint in Postgres so a restart resumes from the last successfully fetched page. Retries with exponential backoff on TMDB failures and observes queue-size backpressure to avoid overwhelming the writer.

### WriterWorker

Consumer thread. Pulls packages from Redis, runs LLM enrichment for mood and analysis, and writes to Postgres first, then ChromaDB. Cooldown intervals between batches prevent overload of the LLM and the vector DB. Failures roll back the Postgres transaction so partial writes never leak downstream.

### RedisBufferQueue

Redis-backed queue with batch push and pop semantics. Decouples fetcher and writer throughput, letting the producer work ahead during LLM-bound stretches and pause when the queue fills up.

### PostgresService

Stores raw and normalized content, maintains the fetch checkpoint, and acts as the source of truth for the system. If something exists in ChromaDB, it must exist here first.

### ChromaService

Stores content embeddings for semantic search. Writes happen only after Postgres confirms, keeping the two stores consistent.

### MoodExtractor

Uses the LLM to tag each item with mood labels and analysis. The output is validated against a schema before storage.

### Pipeline Orchestrator

Clears the Redis queue on startup for a fresh run, launches the worker threads, monitors their lifecycle, and handles graceful shutdown via threading events.

## Flow Overview

1. FetcherWorker reads the last checkpoint from Postgres, pulls a TMDB page, normalizes items, and pushes a batch to Redis with retry and backoff on errors.
2. WriterWorker pops from Redis, enriches each item with LLM-generated moods and analysis, persists to Postgres, and writes embeddings to ChromaDB.
3. The orchestrator coordinates startup, lifecycle monitoring, and clean shutdown across both workers.

## Engineering Decisions

- **Postgres is the source of truth** — the consistency rule is strict: if a row exists in ChromaDB, it exists in Postgres. The reverse is allowed temporarily (Postgres-only rows), but never the other way around. This constrains failure modes to ones that can be recovered by replaying enrichment.
- **Threaded producer-consumer over async** — the pipeline is I/O-heavy but the LLM calls are blocking; threads with a Redis buffer give the simplest working model with clean backpressure semantics.
- **Checkpointing in Postgres, not on disk** — a restart reads the last page number from the same DB that holds the data, so there's no drift between checkpoint state and actual ingestion state.
- **Transactions + rollback on write failure** — rather than dead-letter queues or reconciliation jobs, a failed Chroma write rolls back the Postgres transaction. Simpler to reason about; the item gets reprocessed on the next run.

## Technologies

- Python 3.13, uv
- Redis for queueing and backpressure
- PostgreSQL for relational storage and checkpoints
- ChromaDB for embeddings
- SentenceTransformers for vector generation
- Ollama (local LLM via LangChain) for mood enrichment
- TMDB API for source content

## Key Learnings

- Enriching content with LLM-generated mood and analysis for semantic search.
- Producer-consumer pipelines with Redis buffering and backpressure handling.
- Checkpointing and transactional consistency across two stores with different guarantees.
- Retry, exponential backoff, and graceful shutdown in long-running threaded workers.
