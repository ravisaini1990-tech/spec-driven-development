# REST API Contract

Base URL: `http://localhost:8080`

All request/response bodies are `application/json`. JSON field names use `snake_case` (Jackson configured globally).

---

## Tickets

### POST /api/tickets — Create ticket

**Request:**
```json
{
  "title": "Login page broken on Safari",
  "description": "Users on Safari 17 cannot log in — the submit button is unresponsive.",
  "priority": "HIGH",
  "assignee": "alice",
  "category": "authentication"
}
```
Fields: `title` (required), `description` (required), `priority` (optional, default MEDIUM), `assignee` (optional), `category` (optional).

**Response 201:**
```json
{
  "id": 1,
  "title": "Login page broken on Safari",
  "description": "Users on Safari 17 cannot log in — the submit button is unresponsive.",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": "alice",
  "category": "authentication",
  "created_at": "2024-01-15T10:00:00",
  "updated_at": "2024-01-15T10:00:00"
}
```

**Response 400** (validation failure):
```json
{
  "errors": {
    "title": "must not be blank",
    "description": "must not be blank"
  }
}
```

---

### GET /api/tickets — List tickets

Query params: `search` (string, optional), `status` (enum string, optional)

**Response 200:**
```json
[
  { "id": 1, "title": "...", "status": "OPEN", "priority": "HIGH", "assignee": "alice", "category": "authentication", "created_at": "...", "updated_at": "..." }
]
```

---

### GET /api/tickets/{id} — Get ticket

**Response 200:** Full ticket object (same shape as POST response).

**Response 404:**
```json
{ "error": "Ticket not found: 999" }
```

---

### PATCH /api/tickets/{id} — Update ticket fields

**Request** (all fields optional):
```json
{
  "title": "Updated title",
  "priority": "CRITICAL",
  "assignee": "bob"
}
```
**Response 200:** Updated ticket object.

---

### PATCH /api/tickets/{id}/status — Transition status

**Request:**
```json
{ "status": "IN_PROGRESS" }
```

**Response 200:** Updated ticket object.

**Response 409** (invalid transition):
```json
{ "error": "Invalid state transition from CLOSED to OPEN" }
```

---

### POST /api/tickets/{id}/comments — Add comment

**Request:**
```json
{
  "author": "bob",
  "body": "Reproduced on Safari 17.2. Will investigate."
}
```

**Response 201:**
```json
{
  "id": 5,
  "ticket_id": 1,
  "author": "bob",
  "body": "Reproduced on Safari 17.2. Will investigate.",
  "created_at": "2024-01-15T10:05:00"
}
```

Adding a comment re-ingests the ticket into the vector store (see [rag-ingestion.md](./rag-ingestion.md)) so the comment becomes part of the searchable knowledge base immediately.

---

### GET /api/tickets/{id}/comments — List comments

**Response 200:**
```json
[
  {
    "id": 5,
    "ticket_id": 1,
    "author": "bob",
    "body": "Reproduced on Safari 17.2. Will investigate.",
    "created_at": "2024-01-15T10:05:00"
  }
]
```

**Response 404** (ticket not found):
```json
{ "error": "Ticket not found: 999" }
```

---

## AI Assistant

### POST /api/ai/ask — Ask a question

**Request:**
```json
{ "question": "Which tickets are related to authentication issues?" }
```

**Response 200 (tickets found):**
```json
{
  "answer": "Ticket #1 (Login page broken on Safari) is about an authentication issue where Safari 17 users cannot submit the login form.",
  "ticket_ids": ["1", "3"],
  "no_relevant_tickets": false
}
```

**Response 200 (no relevant tickets):**
```json
{
  "answer": "No relevant tickets found for your query.",
  "ticket_ids": [],
  "no_relevant_tickets": true
}
```
