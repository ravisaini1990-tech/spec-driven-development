# Command: Review RAG Answer Quality

Use this prompt to evaluate whether a RAG answer is grounded and hallucination-free.

---

## Prompt Template

```
You are evaluating a RAG (Retrieval-Augmented Generation) answer from a support ticket system.
The system retrieves relevant ticket documents from a vector store and generates an answer.

Evaluate the answer below against the retrieved context for the following properties:

1. **Grounding**
   - Is every factual claim in the answer directly supported by at least one of the retrieved tickets?
   - Are all cited ticket IDs (e.g. "Ticket #3") actually present in the retrieved context?
   - Flag any claim that cannot be traced to a specific ticket.

2. **Hallucination**
   - Does the answer introduce any information NOT present in the retrieved context?
   - Examples of hallucination: inventing a resolution, stating a different assignee, adding a root cause not described in the ticket.
   - List each hallucinated claim with the text of the hallucination.

3. **Honest no-match handling**
   - If the retrieved context is empty (no tickets above threshold), did the system return the fixed message "No relevant tickets found for your query." without calling the LLM?
   - If context is empty but the system still generated an answer — this is a critical failure.

4. **Completeness**
   - Did the answer address all parts of the user's question?
   - If it missed part of the question, was it because the context didn't have the information (acceptable) or because the LLM ignored relevant context (not acceptable)?

5. **Citation accuracy**
   - Are the ticket IDs in the `ticket_ids` field the same as the tickets retrieved?
   - Does the answer narrative match the ticket IDs cited?

---

Retrieved context:
<paste document chunks here>

User question:
<paste question here>

System answer:
<paste answer here>

Ticket IDs returned:
<paste ticketIds array here>

No relevant tickets flag:
<true/false>

---

Provide:
- PASS/FAIL for each of the 5 properties above.
- A list of specific issues found.
- An overall verdict: PASS (safe to ship) or FAIL (requires fix).
```
