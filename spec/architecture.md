# Architecture Decision Record

## System Overview

```
┌─────────────────┐        HTTP/JSON        ┌──────────────────────────────────────┐
│  React Frontend │ ──────────────────────► │           Spring Boot 3.x            │
│  (Vite, port    │                         │           (port 8080)                │
│   5173)         │                         │                                      │
└─────────────────┘                         │  TicketController  AiController      │
                                            │  TicketService     AiAssistantService│
                                            │  StateMachineService                 │
                                            │  RagIngestionService                 │
                                            └──────────┬───────────────────────────┘
                                                       │
                          ┌────────────────────────────┼──────────────────────┐
                          │                            │                      │
                   ┌──────▼──────┐            ┌────────▼───────┐    ┌────────▼────────┐
                   │ PostgreSQL  │            │  PGVector ext  │    │  OpenAI API     │
                   │  tickets    │            │  vector store  │    │  - embeddings   │
                   │  comments   │            │  (same DB)     │    │  - chat/GPT-4o  │
                   └─────────────┘            └────────────────┘    └─────────────────┘
```

## Decision: PostgreSQL + PGVector (single database)

**Chosen:** PostgreSQL 16 with the `pgvector` extension, managed by a single datasource.

**Alternatives considered:**
- Pinecone / Weaviate (dedicated vector DB): rejected — adds operational complexity (second service, second auth, second failure point) for no benefit at this scale. PGVector handles millions of vectors with HNSW indexing at sub-millisecond recall.
- Redis Vector Search: rejected — ephemeral by default; poor fit for durable ticket data.
- ChromaDB: rejected — Python-native; no official Spring AI autoconfiguration.

**Why PGVector wins here:**
1. Single DB simplifies deployment (one connection pool, one Flyway migration set, one backup target).
2. Transactional consistency: ticket row and its vector embedding are in the same ACID transaction scope.
3. Spring AI ships a `PgVectorStore` autoconfigured bean — minimal boilerplate.
4. HNSW index delivers approximate nearest-neighbour search at O(log n); at support-ticket scale (< 1M documents) latency is < 5 ms.

## Decision: OpenAI text-embedding-3-small (cloud, 1536 dims)

**Chosen:** `text-embedding-3-small` via OpenAI API.

**Alternatives considered:**

| Model | Dims | Cost ($/1M tokens) | Latency | Quality (MTEB) |
|-------|------|--------------------|---------|----------------|
| text-embedding-3-large | 3072 | $0.13 | ~200 ms | Highest |
| **text-embedding-3-small** | **1536** | **$0.02** | **~80 ms** | **High** |
| text-embedding-ada-002 | 1536 | $0.10 | ~80 ms | Medium |
| all-MiniLM-L6 (local) | 384 | free | ~5 ms local | Lower |

**Why text-embedding-3-small:**
- **Cost:** 6.5× cheaper than ada-002, 6.5× cheaper than large. At 1000 tickets × ~300 tokens avg = $0.006 for full re-ingestion.
- **Quality:** Outperforms ada-002 on MTEB benchmark despite lower price. Adequate for support-ticket semantic search (no domain-specific jargon that would require fine-tuning).
- **Latency:** ~80 ms per embed call is acceptable; ingestion is async on ticket save, not on the critical read path.
- **Dimensions:** 1536 fits PGVector's HNSW without excessive memory overhead.
- **Local models** (MiniLM): rejected because they require bundling model weights, add cold-start latency, and quality is noticeably lower for diverse technical text.

## Decision: Chunking Strategy — One Document Per Ticket (field-group)

**Chosen:** Each ticket is ingested as a single document whose content combines all key fields (title + description + status + priority + assignee + category).

**Alternatives considered:**
- Fixed-size chunking (e.g. 512 tokens with overlap): rejected — ticket descriptions are short (typically < 200 tokens). Fixed chunking would split mid-sentence and destroy sentence-level semantics with no benefit.
- Semantic/sentence splitting: rejected — same reason. The "paragraph" is the entire ticket; splitting further loses cross-field context (e.g. the description refers to the title).
- One chunk per field: rejected — a query about "login bug assigned to Alice" needs title + assignee context together; splitting by field forces the retriever to reconstruct what was artificially separated.

**Why one-document-per-ticket:**
- Ticket data is already short and self-contained — each ticket is its own semantic unit.
- Metadata (ticketId, status, priority, category, assignee) is stored alongside the vector for post-retrieval filtering and citation.
- On update, the old document is deleted by ticketId metadata filter and a new one is inserted — simple and correct.

## LLM: GPT-4o-mini via OpenAI Chat API

Used only in AiAssistantService for generating grounded answers from retrieved context. System prompt enforces grounding: "Answer ONLY from the provided ticket context."
