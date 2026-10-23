# RAG Evaluation Strategy

## Goals

1. **Grounding:** Every claim in the answer traces to a cited ticket.
2. **No hallucination:** The LLM does not introduce facts not present in the retrieved context.
3. **Honest no-match:** When no tickets are relevant, the system returns the fixed no-match response (not a hallucinated answer).
4. **Recall:** The correct tickets are retrieved for domain-specific queries.

## Test Dataset

Seed the database with a known set of tickets before evaluation:

| Ticket ID | Title | Category | Assignee |
|-----------|-------|----------|---------|
| 1 | Login broken on Safari | authentication | alice |
| 2 | Payment timeout on checkout | billing | bob |
| 3 | Password reset email not delivered | authentication | alice |
| 4 | Invoice PDF download fails | billing | carol |
| 5 | Unrelated — internal tooling test | tooling | dave |

## Test Cases

### TC-01: On-topic retrieval (expected tickets: 1, 3)
- Question: "What authentication issues are open?"
- Expected: answer mentions tickets 1 and/or 3, ticketIds contains "1" and/or "3"
- Pass criteria: ticketIds ⊆ {1, 3} and answer does not mention tickets 2, 4, 5

### TC-02: Category-specific (expected tickets: 2, 4)
- Question: "Any billing related problems?"
- Expected: answer mentions tickets 2 and/or 4

### TC-03: Assignee filter (expected: 1, 3)
- Question: "What is alice working on?"
- Expected: ticketIds ⊆ {1, 3}

### TC-04: No relevant tickets
- Question: "What is the weather like today?"
- Expected: noRelevantTickets=true, answer = "No relevant tickets found for your query."
- Pass criteria: noRelevantTickets is true; LLM was NOT called

### TC-05: Grounding check
- Question: "Is ticket 2 related to authentication?"
- Expected: answer says NO or says it is about billing, does not hallucinate an authentication link

## Evaluation Approach

### Automated

```java
// RAGEvaluationIT.java
@Test void testNoHallucinationWhenNoContext() {
    AiAnswerResponse resp = askAi("What is the weather like today?");
    assertTrue(resp.noRelevantTickets());
    assertEquals("No relevant tickets found for your query.", resp.answer());
}

@Test void testAuthenticationTicketsRetrieved() {
    AiAnswerResponse resp = askAi("What authentication issues are open?");
    assertFalse(resp.noRelevantTickets());
    assertTrue(resp.ticketIds().stream().anyMatch(id -> id.equals("1") || id.equals("3")));
}
```

### Manual / LLM-as-Judge

For grounding checks, use a secondary LLM call:

**Prompt:**
```
Given this context:
<retrieved ticket documents>

And this answer:
<ai answer>

Does the answer contain any claims NOT supported by the context? 
Reply with YES/NO and list unsupported claims if YES.
```

A grounding score of ≥ 95% (over 20 test questions) is the target.

## Hallucination Red Flags to Check

- Answer mentions a ticket ID not in `ticketIds`
- Answer states a status (e.g. CLOSED) when the ticket context says OPEN
- Answer invents a resolution or workaround not present in the description
- Answer answers a no-context question with fabricated ticket data
