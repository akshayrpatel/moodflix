# MoodFlix AI Engine

FastAPI service that powers MoodFlix's semantic search and personalized discovery. Every user query lands here after passing through the gateway: the engine classifies intent, extracts moods, builds metadata filters, and queries ChromaDB to return results that match how the user feels, not just what they typed.

## Tech Stack

- Python 3.13 / FastAPI / Pydantic v2
- ChromaDB for vector storage with SentenceTransformer embeddings
- LangChain orchestrating multiple LLM providers — Ollama (Gemma3) as the primary, with Mistral, OpenRouter, and Groq as fallbacks
- HuggingFace Transformers for zero-shot intent classification
- `sentence-transformers` for mood extraction and query embeddings
- uv for dependency management

## Responsibilities

### Semantic Search

Every query runs through a pipeline: intent classification (regex fast path with a zero-shot ML fallback), mood extraction via cosine similarity against 12 mood embeddings, and LLM-powered filter construction when the query is factual. The final query blends the user's mood labels into the search text and hits ChromaDB. If a filtered query returns nothing, the engine retries without filters so a result is always returned.

### Personalized Discovery

Authenticated requests arrive with a taste vector in the `X-User-Vector` header (injected by the gateway). The engine blends this vector with the query embedding — 70% query, 30% taste — so the same search returns different results for different users. The discover endpoint builds four personalized rows directly from the taste vector with pure vector similarity and metadata filtering, no LLM calls involved.

### Onboarding

The onboarding endpoint accepts the UI's 4-step mood quiz selections and computes a 384-dim seed taste vector via weighted embedding averaging across the chosen moods. This vector is returned to the gateway for persistence.

### LLM Failover

The `LLMService` wraps multiple providers behind a single interface. If Ollama is unavailable or rate-limited, calls fall through to the next provider in the chain. Prompts use LangChain templates with `JsonOutputParser` / `PydanticOutputParser` so every structured output is validated before use.

## Engineering Decisions

- **Service registry over DI framework** — a plain `ServiceRegistry` singleton, initialized once at app startup via FastAPI's lifespan. Lighter than a full IoC container and makes test wiring trivial.
- **Lazy ChromaDB connection** — the client initializes on first query, not at startup, so dev restarts stay fast.
- **Stateless by design** — no sessions, no user tables. All identity flows in as headers from the gateway. This keeps the engine horizontally scalable and lets us redeploy without auth impact.
- **Fallback-first pipeline** — each stage (filter, embed, query) has a defined failure path. Users get *some* result even when the LLM or the filter builder fails.
- **Zero-LLM discovery** — discover rows are built from taste-vector similarity + metadata filters. Keeps recommendation latency low and variable cost at zero once the user is onboarded.
