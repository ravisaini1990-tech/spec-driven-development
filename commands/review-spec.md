# Command: Review Specification File

Use this prompt to review any spec file for completeness and correctness.

---

## Prompt Template

```
Review the following specification document for the support ticket system. Check for:

1. **Completeness**
   - Are all happy paths described?
   - Are all error/edge cases covered?
   - Are all enums/states listed explicitly?

2. **Ambiguity**
   - Are there any phrases like "etc.", "and so on", "similar" that leave implementation unclear?
   - Is every field's optionality (required vs optional) stated?
   - Are data types specified for all fields?

3. **Missing edge cases**
   - What happens on concurrent updates to the same ticket?
   - What if the OpenAI API is down? Is there a fallback?
   - What if the same ticket is ingested twice (duplicate document)?
   - What if assignee or category contains special characters?

4. **Conflicting requirements**
   - Does any statement contradict another in the same or related spec?
   - Do the API contract and state machine spec agree on valid transitions?

5. **Testability**
   - Can each requirement be verified by a specific test?
   - Are acceptance criteria measurable (e.g. "≤ 500 ms" vs "fast")?

Spec document:
<paste spec here>
```
