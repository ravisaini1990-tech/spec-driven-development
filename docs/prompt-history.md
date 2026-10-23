# Prompt History

Record of significant prompts used during development, AI responses, and mistakes found.

---

## Entry Template

```
## [DATE] — [COMPONENT]

### Prompt
<what was asked of the AI>

### AI Response Summary
<what the AI generated / proposed>

### Mistakes Found
<bugs, wrong API usage, missing validation, etc.>

### Fix Applied
<what was changed and why>
```

---

## 2024-01-15 — Initial Project Setup

### Prompt
Generate a Spring Boot 3.x + Spring AI project skeleton with PGVector integration.

### AI Response Summary
Generated pom.xml with Spring AI dependencies and basic application.yml.

### Mistakes Found
- Used `spring-ai-openai` without BOM — version conflicts with Spring Boot managed deps.
- Set `dimensions: 768` (wrong for text-embedding-3-small which needs 1536).

### Fix Applied
- Added `spring-ai-bom` to `<dependencyManagement>`.
- Corrected dimensions to 1536.

---

## 2024-01-15 — RagIngestionService

### Prompt
Implement RagIngestionService that deletes and re-inserts a ticket's vector document.

### AI Response Summary
Generated service using `vectorStore.delete(List.of(documentId))` — attempted to delete by Spring AI document UUID.

### Mistakes Found
- Wrong deletion approach: we don't store the Spring AI UUID — we store our own ticketId in metadata. Deletion must use metadata filter, not document UUID.
- Used field injection `@Autowired` instead of constructor injection.

### Fix Applied
- Switched to `FilterExpressionBuilder` with `eq("ticketId", ...)` for deletion.
- Converted to `@RequiredArgsConstructor` + final fields.

---

## 2026-09-28 — Repo Review & Gap Remediation

### Prompt
"Please review the code with the requirement documents" followed by "Please implement all
features which are mentioned into the assignment and please prepare the prompt history very
good."

### AI Response Summary
Ran a full agent-based audit of the repo against the assignment PDF: rules/, commands/, spec/,
prompt history, backend Java sources, tests, and frontend. Found the codebase already
substantially implemented (CRUD, state machine, RAG ask-flow, frontend) rather than empty, so
work was scoped to closing the specific gaps found, not a rebuild.

### Mistakes Found
1. **Hardcoded OpenAI API key in `application.yml` (uncommitted, working-tree only).** A prior
   local edit had replaced `api-key: ${OPENAI_API_KEY:}` with a live-looking key baked in as the
   fallback default. This was never committed to git history, but was sitting in plaintext on
   disk. Caught before any commit could include it.
2. **RAG knowledge base went stale on new comments.** `TicketService.addComment()` never called
   `ragIngestionService.ingest(ticket)`, and `Ticket.toDocumentText()` didn't include comments at
   all — directly contradicting the assignment's requirement to convert "description, comments,
   resolution notes" into searchable knowledge documents and to re-ingest on ticket update.
3. **`@SpringBootTest`-based tests only passed by accident.** `TicketControllerTest` (pre-existing)
   silently required a real `OPENAI_API_KEY` environment variable and a reachable Postgres+pgvector
   instance to even boot the Spring context, because Spring AI's OpenAI autoconfiguration calls
   `Assert.hasText()` on the API key unconditionally — even though the test fully mocks
   `TicketService`/`AiAssistantService` and never actually calls the model. Running `./gradlew test`
   on a clean checkout without `OPENAI_API_KEY` set failed with `IllegalArgumentException: OpenAI
   API key must be set`, not a test assertion failure — meaning CI or a fresh clone would have
   reported the whole suite as broken. No test-scoped configuration existed to isolate this.
4. **Repo hygiene:** `frontend/node_modules/` (2245+ files) and `.idea/` were committed to git;
   no `.gitignore` existed anywhere in the repo.
5. **`spec/test-strategy.md` was already wrong before this session** — it claimed "use H2
   in-memory DB... if PGVector unavailable," but the project has no H2 dependency and the Flyway
   migration uses Postgres-specific DDL (`CREATE EXTENSION vector`), so that fallback was never
   actually usable.

### Fix Applied
- Reverted `application.yml` to `${OPENAI_API_KEY:}`; advised rotating the exposed key at OpenAI
  since it had already been pasted into a chat transcript.
- Added `Comment`-aware `Ticket.toDocumentText(List<Comment>)`, wired `RagIngestionService` to
  fetch comments via `CommentRepository` before building the document, and added
  `ragIngestionService.ingest(ticket)` to the end of `TicketService.addComment()`.
- Added `src/test/resources/application.yml` with
  `api-key: ${OPENAI_API_KEY:test-dummy-key-not-a-real-credential}` so the full config loads
  without a real secret while still respecting a real env var if one is set.
- Added `.gitignore` (root) and ran `git rm -r --cached` on `frontend/node_modules` and `.idea`
  (uncommitted at time of writing — user has not yet been asked to confirm the commit).
- Added missing test coverage called for by `spec/test-strategy.md` but never implemented:
  `AiAssistantServiceTest`, `AiControllerTest`, `RagIngestionServiceTest`, `TicketServiceTest`.
  Full suite: 57 tests, 0 failures, run via `./gradlew test`.
- Added `@Valid` + `@Size(min=1)` constraints to `UpdateTicketRequest`/`TicketController.updateTicket`,
  which previously accepted an unvalidated `@RequestBody`.
- Updated `spec/rag-ingestion.md`, `spec/api-contract.md`, and `spec/test-strategy.md` to match
  the corrected implementation and to document the H2/API-key findings above.
