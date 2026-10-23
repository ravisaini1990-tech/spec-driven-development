# Command: Generate JUnit Tests

Use this prompt to generate comprehensive JUnit 5 tests for a given class.

---

## Prompt Template

```
Generate JUnit 5 tests for the following Java class. The project uses:
- JUnit 5 (Jupiter)
- Mockito (@ExtendWith(MockitoExtension.class) for unit tests)
- AssertJ for assertions
- Spring Boot Test (@SpringBootTest + @AutoConfigureMockMvc) for integration tests

Requirements for the test suite:

1. **Happy path tests**
   - Cover the main success scenario for each public method.
   - Verify the return value, not just that no exception is thrown.

2. **Edge case tests**
   - Null inputs where the method accepts nullable parameters.
   - Empty strings or empty collections.
   - Boundary values (e.g. exactly at the similarity threshold).

3. **Failure path tests**
   - Each scenario where an exception should be thrown.
   - Verify the exact exception type and message.
   - For state machine: every invalid transition must have its own test method.

4. **Naming convention**
   Use: {methodName}_{scenario}_{expectedBehavior}()
   Example: validateTransition_closedToOpen_throwsInvalidStateTransitionException()

5. **Mock setup**
   - Mock all external dependencies (repositories, VectorStore, OpenAiChatModel).
   - Do not mock the class under test.
   - Use @InjectMocks for the class under test.

6. **Avoid**
   - Testing Spring Boot auto-configuration.
   - Testing Lombok-generated code.
   - Multiple assertions on unrelated behaviors in one test method.

Class to test:
<paste class here>
```
