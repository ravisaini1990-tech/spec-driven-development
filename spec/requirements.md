# Support Ticket System — Functional & Non-Functional Requirements

## 1. Functional Requirements

### 1.1 Ticket Management

| ID | Requirement |
|----|-------------|
| FR-01 | Users can create a support ticket with title, description, priority, assignee, and category. |
| FR-02 | Users can view a paginated/filtered list of tickets by status and/or full-text search. |
| FR-03 | Users can view a single ticket with all its fields and comments. |
| FR-04 | Users can update ticket fields (title, description, priority, assignee, category) at any time. |
| FR-05 | Users can transition ticket status following the defined state machine (see §1.2). |
| FR-06 | Users can add comments to any ticket (author + body). |

### 1.2 State Machine

Valid transitions:

```
OPEN        → IN_PROGRESS
OPEN        → CANCELLED
IN_PROGRESS → RESOLVED
IN_PROGRESS → CANCELLED
RESOLVED    → CLOSED
```

All other transitions (e.g. CLOSED→OPEN, RESOLVED→OPEN, CANCELLED→anything) are invalid and must be rejected with HTTP 409.

### 1.3 RAG Question-Answering

| ID | Requirement |
|----|-------------|
| RAG-01 | Users can submit a natural-language question via POST /api/ai/ask. |
| RAG-02 | The system retrieves the top-K most semantically similar ticket chunks (default K=5, threshold=0.7). |
| RAG-03 | The LLM generates an answer grounded exclusively in the retrieved ticket context. |
| RAG-04 | The response includes the list of ticket IDs that were used as context. |
| RAG-05 | If no tickets exceed the similarity threshold, the system returns noRelevantTickets=true and a fixed "no relevant tickets found" message without calling the LLM. |
| RAG-06 | The system prompt instructs the LLM to say "I don't know" when the answer is not in the context (grounding constraint). |
| RAG-07 | Tickets are ingested/re-ingested into the vector store whenever they are created or updated. |

## 2. Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-01 | API response time for CRUD operations ≤ 500 ms (p95) under normal load. |
| NFR-02 | AI answer endpoint latency is best-effort; UI shows a loading indicator. |
| NFR-03 | No secrets (API keys, DB passwords) hardcoded in source; use environment variables. |
| NFR-04 | All API inputs are validated; invalid requests return structured 400 error bodies. |
| NFR-05 | CORS is configured to allow the frontend origin (http://localhost:5173). |
| NFR-06 | Database schema is managed via Flyway migrations; no hibernate DDL auto-create in production. |
| NFR-07 | The system targets Java 21 and Spring Boot 3.3.x LTS. |
| NFR-08 | Frontend is a single-page application built with React 18 + Vite 5. |
