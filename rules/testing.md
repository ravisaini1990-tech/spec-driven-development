# Testing Steering Rules

## Framework

- **JUnit 5** (Jupiter) — use `@Test`, `@BeforeEach`, `@ParameterizedTest`.
- **Mockito** — use `@ExtendWith(MockitoExtension.class)` for unit tests.
- **MockMvc** — for controller integration tests via `@SpringBootTest` + `@AutoConfigureMockMvc`.
- **AssertJ** — prefer `assertThat(...)` over JUnit 5 assertions for readability.

## Test Slices

| Layer | Annotation |
|-------|-----------|
| Repository | `@DataJpaTest` |
| Service (unit) | `@ExtendWith(MockitoExtension.class)` |
| Controller (integration) | `@SpringBootTest` + `@AutoConfigureMockMvc` |

## StateMachineService — Required Coverage

Test every cell of the 5×5 transition matrix:

```java
// Pattern for valid transitions:
@Test void openToInProgress_valid() {
    assertDoesNotThrow(() -> stateMachineService.validateTransition(OPEN, IN_PROGRESS));
}

// Pattern for invalid transitions:
@Test void closedToOpen_invalid() {
    assertThrows(InvalidStateTransitionException.class,
        () -> stateMachineService.validateTransition(CLOSED, OPEN));
}
```

Cover:
- All 5 valid transitions → expect no exception
- Minimum 10 invalid transitions → expect `InvalidStateTransitionException`
- Include terminal state cases (CLOSED→anything, CANCELLED→anything)

## Controller Tests

- Use `MockMvc` with `.perform(post(...).content(...).contentType(APPLICATION_JSON))`.
- Assert status, response JSON content type, key response fields.
- Use `@MockBean` for `RagIngestionService` and AI services to avoid real OpenAI calls.

## Test Naming Convention

`{methodUnderTest}_{scenario}_{expectedOutcome}()`

Example: `createTicket_missingTitle_returns400()`

## What NOT to Test

- Spring Boot auto-configuration (trust the framework).
- Lombok-generated methods.
- Simple getters/setters.
