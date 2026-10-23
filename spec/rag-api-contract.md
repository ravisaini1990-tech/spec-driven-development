# RAG API Contract

## POST /api/ai/ask

### Request

```
POST /api/ai/ask
Content-Type: application/json
```

```json
{
  "question": "string (required, not blank)"
}
```

### Processing Steps

1. Embed `question` using text-embedding-3-small.
2. Query PGVector with `top-k=5`, `similarity-threshold=0.7`.
3. If result set is empty → short-circuit response (no LLM call).
4. Build context string from retrieved documents.
5. Call OpenAI chat (GPT-4o-mini) with system + user prompt.
6. Extract ticket IDs from document metadata.
7. Return structured response.

### System Prompt (sent to LLM)

```
You are a support ticket assistant. Answer ONLY based on the provided ticket context below.
If the answer is not present in the context, say "I don't know based on the available tickets."
Always cite the ticket IDs you used in your answer (e.g. "Ticket #1", "Ticket #3").
Do not invent information not present in the context.
```

### User Prompt Template

```
Context:
<formatted ticket documents>

Question: <user question>
```

### Response (tickets found)

```json
{
  "answer": "string — LLM-generated answer citing ticket IDs",
  "ticket_ids": ["1", "3", "7"],
  "no_relevant_tickets": false
}
```

### Response (no relevant tickets)

```json
{
  "answer": "No relevant tickets found for your query.",
  "ticket_ids": [],
  "no_relevant_tickets": true
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | `question` is blank or missing |
| 500 | OpenAI API unreachable or error |

### Citation Fields

- `ticket_ids`: list of string IDs extracted from the `ticketId` metadata of retrieved documents. These are the tickets whose embeddings were in the top-K results, regardless of whether the LLM used all of them in its answer.
- `no_relevant_tickets`: boolean flag — `true` only when the vector similarity search returned zero results above threshold.
