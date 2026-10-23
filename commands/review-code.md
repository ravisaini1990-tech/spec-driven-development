# Command: Review AI-Generated Java Code

Use this prompt when reviewing any AI-generated Java code in this project.

---

## Prompt Template

```
Review the following Java code for the support ticket system. Check specifically for:

1. **Null safety**
   - Are all nullable fields (assignee, category) null-checked before use?
   - Are Optional return values from repository handled correctly (orElseThrow vs get)?

2. **Transaction boundaries**
   - Is @Transactional on the service layer (not controller, not repository)?
   - Are read-only queries annotated with @Transactional(readOnly = true)?
   - Is the transaction open when lazy-loaded associations are accessed?

3. **Missing validation**
   - Are all @RequestBody params annotated with @Valid?
   - Are jakarta constraint annotations present on DTO records?
   - Is the GlobalExceptionHandler catching MethodArgumentNotValidException?

4. **Hardcoded values**
   - Are top-K and similarity threshold read from @Value / application.yml?
   - Is the OpenAI API key NEVER hardcoded?
   - Are port numbers or URLs not hardcoded?

5. **Spring AI API correctness**
   - Is VectorStore used (interface), not PgVectorStore?
   - Is SearchRequest built with the builder pattern?
   - Is the deletion filter using FilterExpressionBuilder correctly?
   - Is the Document created with Map metadata (not mutable HashMap that Spring AI may reject)?

6. **PGVector configuration mistakes**
   - Does dimensions in application.yml match 1536 (text-embedding-3-small)?
   - Is initialize-schema: true set for first run?

7. **Constructor injection**
   - No @Autowired field injection anywhere?
   - All dependencies declared as final fields?

8. **Exception handling**
   - Does TicketNotFoundException return 404?
   - Does InvalidStateTransitionException return 409?
   - Is there a catch-all handler returning 500?

Code to review:
<paste code here>
```
