# AI-Powered Support Ticket Management System

A support ticket tracker with a strictly enforced status state machine and a
retrieval-augmented (RAG) question-answering assistant that answers only from real
ticket data, cites the tickets it used, and says so honestly when nothing relevant
is found.

Built with Java 21, Spring Boot, Spring AI, PostgreSQL + PGVector, and a React (Vite)
frontend, following a spec-driven development workflow (requirement → specification →
plan → implementation → testing → review → fix).

## Features

- Create, list, view, and update tickets (title, description, priority, assignee, category).
- Add comments to a ticket; keyword search and status filtering across tickets.
- A backend-enforced ticket status state machine — invalid transitions are rejected with `409`.
- `POST /api/ai/ask` — ask natural-language questions over ticket history. Answers are
  grounded strictly in retrieved tickets, cite the specific ticket ID(s) used, and
  explicitly say "no relevant tickets found" instead of fabricating an answer.
- Ticket data (including comments) is re-embedded into the vector store whenever a
  ticket is created, updated, transitioned, or commented on, so the knowledge base
  never goes stale.

## Architecture at a Glance

```
React (Vite) frontend  ─────────►  Spring Boot REST API  ─────────►  PostgreSQL
                                          │                       (tickets, comments)
                                          │
                                          ▼
                                  Spring AI (OpenAI)
                                          │
                              ┌───────────┴───────────┐
                              ▼                       ▼
                    text-embedding-3-small        gpt-4o-mini
                              │
                              ▼
                       PGVector store
                    (one document per ticket,
                     re-ingested on every change)
```

See [spec/architecture.md](spec/architecture.md) for the full reasoning behind these
choices (embedding model, chunking strategy, vector store), and
[spec/rag-ingestion.md](spec/rag-ingestion.md) / [spec/rag-api-contract.md](spec/rag-api-contract.md)
for the ingestion and `/api/ai/ask` request/response contracts in detail.

## Prerequisites

- Java 21
- Node.js 18+ (tested with Node 20) and npm
- Docker (for PostgreSQL + PGVector), or a local Postgres 15+ with the `vector` extension available
- An OpenAI API key (for embeddings + chat completions)

## Running Locally

### 1. Start PostgreSQL with PGVector

```bash
docker run -d --name support-pg \
  -e POSTGRES_DB=support_tickets \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg17
```

Flyway migrations (`backend/src/main/resources/db/migration/`) create the `tickets`,
`comments`, and `vector_store` tables automatically on first boot — no manual schema
setup needed.

### 2. Configure your OpenAI API key

```bash
export OPENAI_API_KEY=sk-...   # never commit this — see Security note below
```

### 3. Run the backend

```bash
cd backend
./gradlew bootRun
```

The API starts on `http://localhost:8080`.

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server starts on `http://localhost:5173` and proxies `/api/*` to the backend.

## Running Tests

**Backend** (requires the Postgres container above to be running — tests boot the
full Spring context against it, with a placeholder OpenAI key so no real API key is
needed just to run the suite):

```bash
cd backend
./gradlew test
```

57 tests: ticket CRUD/state-machine/service logic, RAG ingestion and retrieval, and
AI-assistant grounding/citation/no-match behavior (all with `VectorStore`/`ChatModel`
mocked — no real OpenAI calls are made during the test run).

**Frontend:**

```bash
cd frontend
npm test
```

16 component tests covering the ticket detail status-transition dropdown, ticket
creation validation, and AI assistant grounding/citation/error states.

See [spec/test-strategy.md](spec/test-strategy.md) for the full test strategy,
including why a real `RagIngestionIT`/`RAGEvaluationIT` against a live embedding
model is deliberately left as a manual/future step rather than part of the default
automated run.

## Project Structure

```
backend/    Spring Boot API (Java 21, Spring AI, PGVector, Flyway)
frontend/   React + Vite UI
spec/       Specifications written before implementation (requirements, architecture,
            data model, API contract, state machine, RAG ingestion/contract,
            evaluation strategy, UI flow, test strategy)
rules/      Steering docs for AI-assisted development (Spring Boot conventions,
            testing conventions, API standards, RAG/vector-store conventions)
commands/   Reusable review/generation prompts (review-code, review-spec,
            generate-tests, review-rag-output — the hallucination/grounding check)
docs/prompt-history.md   Record of significant prompts, AI responses, and the
                         mistakes they introduced (with fixes) during development
```

## Configuration

Key runtime settings (`backend/src/main/resources/application.yml`), all overridable
via environment variables:

| Setting | Default | Purpose |
|---|---|---|
| `spring.ai.openai.api-key` | `${OPENAI_API_KEY:}` | OpenAI credential — must be set for real use |
| `spring.ai.openai.embedding.options.model` | `text-embedding-3-small` | Embedding model (1536 dims) |
| `spring.ai.openai.chat.options.model` | `gpt-4o-mini` | Chat model for answer generation |
| `rag.retrieval.top-k` | `5` | Number of documents retrieved per question |
| `rag.retrieval.similarity-threshold` | `0.7` | Minimum similarity score to consider a document relevant |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/support_tickets` | Database connection |

## Security Note

Never commit a real OpenAI API key. `application.yml` uses
`${OPENAI_API_KEY:}` (empty fallback) specifically so the app fails closed rather than
silently embedding a credential in source control — always supply the key via
environment variable.

## Status Machine

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
OPEN → CANCELLED
IN_PROGRESS → CANCELLED
```

`CLOSED` and `CANCELLED` are terminal. All other transitions (e.g. `CLOSED → OPEN`,
`RESOLVED → OPEN`) are rejected by the backend with `409 Conflict`. Full transition
table and rationale: [spec/state-machine.md](spec/state-machine.md).
