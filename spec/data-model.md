# Data Model

## Entity: Ticket

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGSERIAL | PK | Auto-generated |
| title | VARCHAR(255) | NOT NULL | Short summary |
| description | TEXT | NOT NULL | Full problem description |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'OPEN' | Enum: OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED |
| priority | VARCHAR(50) | NOT NULL, DEFAULT 'MEDIUM' | Enum: LOW, MEDIUM, HIGH, CRITICAL |
| assignee | VARCHAR(255) | NULLABLE | Person or team handle |
| category | VARCHAR(255) | NULLABLE | e.g. "billing", "authentication" |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Immutable after creation |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Updated on every save |

## Entity: Comment

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGSERIAL | PK | Auto-generated |
| ticket_id | BIGINT | FK → tickets.id, ON DELETE CASCADE | Parent ticket |
| author | VARCHAR(255) | NOT NULL | Display name of commenter |
| body | TEXT | NOT NULL | Comment text |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Immutable |

## Indexes

- `idx_tickets_status` on `tickets(status)` — supports filter queries
- `idx_comments_ticket_id` on `comments(ticket_id)` — supports comment listing by ticket

## Vector Store (PGVector managed table)

Spring AI's PgVectorStore creates a `vector_store` table automatically (controlled by `initialize-schema: true`):

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| content | TEXT | Ticket text representation |
| metadata | JSONB | ticketId, status, priority, assignee, category |
| embedding | vector(1536) | text-embedding-3-small output |

## Relationships

```
tickets (1) ──────< comments (N)
tickets (1) ──────  vector_store (1)   [soft relationship via metadata.ticketId]
```
