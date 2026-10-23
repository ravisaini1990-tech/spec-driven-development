# Test Strategy

## Testing Pyramid

```
         /\
        /  \         E2E (manual / Playwright — out of scope v1)
       /────\
      /      \       Integration: @SpringBootTest + MockMvc
     /────────\
    /          \     Unit: JUnit 5 + Mockito (StateMachineService, pure logic)
   /────────────\
```

## Unit Tests

### StateMachineServiceTest

Exhaustively cover all 5×5 transition matrix entries:

- **Valid transitions (5):** OPEN→IN_PROGRESS, OPEN→CANCELLED, IN_PROGRESS→RESOLVED, IN_PROGRESS→CANCELLED, RESOLVED→CLOSED → assert no exception thrown.
- **Invalid transitions (all others):** assert `InvalidStateTransitionException` is thrown.

Total: 5 valid + ~15 meaningful invalid = ~20 test methods.

### AiAssistantServiceTest (implemented)

Pure Mockito unit test (`RagIngestionService` and `ChatModel` mocked, no Spring context):

- Empty retrieval → asserts `noRelevantTickets=true`, empty `ticketIds`, no LLM call (`verifyNoInteractions(chatModel)`).
- Non-empty retrieval → asserts LLM called exactly once, ticket IDs are read from document **metadata** (not parsed from LLM output text).
- Duplicate ticket IDs across retrieved chunks are de-duplicated in the citation list.
- LLM failure propagates as a `RuntimeException` rather than returning a fabricated answer.

### RagIngestionServiceTest (implemented)

Pure Mockito unit test (`VectorStore` and `CommentRepository` mocked):

- `ingest()` deletes any existing vector document for the ticket before adding the new one.
- `ingest()` includes the ticket's comments in the document content (resolution history is searchable).
- `ingest()` attaches `ticketId`/`status`/`priority`/`assignee`/`category` metadata.
- `ingest()` with a null ticket ID is a no-op.
- `search()` builds a `SearchRequest` using the configured `top-k` / `similarity-threshold`.

### TicketServiceTest (implemented)

Pure Mockito unit test covering logic the mocked-controller test can't reach directly: default status/priority on create, partial-patch semantics on update (blank fields ignored), all four `listTickets` branches (search only / status only / both / neither), state-transition delegation, and — critically — that `addComment` re-ingests the ticket so the RAG knowledge base doesn't go stale when a comment is added.

## Integration Tests

### TicketControllerTest (@SpringBootTest + MockMvc, TicketService mocked)

| Test | Endpoint | Expected |
|------|----------|---------|
| createTicket_valid | POST /api/tickets | 201 + body |
| createTicket_missingTitle | POST /api/tickets | 400 + errors.title |
| createTicket_missingDesc | POST /api/tickets | 400 + errors.description |
| getTicket_found | GET /api/tickets/{id} | 200 |
| getTicket_notFound | GET /api/tickets/9999 | 404 |
| transitionStatus_valid | PATCH /api/tickets/{id}/status | 200 |
| transitionStatus_invalid | PATCH /api/tickets/{id}/status | 409 |
| addComment_valid | POST /api/tickets/{id}/comments | 201 |

### AiControllerTest (@SpringBootTest + MockMvc, AiAssistantService mocked) (implemented)

Covers: grounded answer with citations (200 + `ticket_ids` + `answer`), honest no-match response (`no_relevant_tickets=true`, not a fabricated answer), blank question (400), missing question field (400).

### RagIngestionIT (manual / future — still not implemented)

Verify against a real PGVector instance that after creating a ticket, `vectorStore.similaritySearch("ticket title keywords")` returns the ticket. Not automated because it requires a live embedding-model call; left as a manual verification step for now.

## Test Configuration

**Correction (found during implementation):** this project has no H2 dependency, and the Flyway
migration uses Postgres-specific DDL (`CREATE EXTENSION vector`, etc.), so "H2 in-memory DB if
PGVector unavailable" as originally written here does not work — `@SpringBootTest` requires a
real reachable Postgres+pgvector instance (e.g. the `docker-compose` service), matching what
`TicketControllerTest` already assumed. Testcontainers would be the correct fix for portability,
but is out of scope for now — the working assumption is a local `pgvector/pgvector` container on
`localhost:5432` with `postgres`/`postgres` credentials.

Separately, `@SpringBootTest`-based tests failed to start at all before `src/test/resources/application.yml`
was added, because Spring AI's OpenAI autoconfiguration hard-requires `spring.ai.openai.api-key`
to be non-blank even when the `ChatModel`/`VectorStore` beans are never actually invoked (fully
mocked services in `AiControllerTest`). The fix supplies a placeholder key
(`${OPENAI_API_KEY:test-dummy-key-not-a-real-credential}`) scoped to the test classpath only, so
the suite is runnable without a real API key while still allowing a real key to be honored via env
var if present. See `docs/prompt-history.md` for the full writeup — this was a mistake AI-generated
tests had baked in silently (the original `TicketControllerTest` only ever passed by accident, on a
machine that happened to have `OPENAI_API_KEY` set in the shell).

- Mock `ChatModel` and `VectorStore` in unit tests (pure Mockito, no Spring context needed for AiAssistantService/RagIngestionService tests).
- Use `@MockBean` for `TicketService`/`AiAssistantService` in controller tests.

## Coverage Target

- StateMachineService: 100% branch coverage (achieved — 21 test methods covering all 25 matrix cells).
- TicketService: ≥ 80% line coverage (achieved via `TicketServiceTest`).
- AiAssistantService / RagIngestionService: grounding, citation, no-match, and re-ingestion behavior covered.
- Controllers: all happy paths + key error paths, including `/api/ai/ask`.
