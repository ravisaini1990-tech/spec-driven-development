# Java / Spring Boot Steering Rules

These rules apply to all AI-generated Java code in this project.

## Package Structure

```
com.support.tickets
├── config/       — Spring configuration classes
├── controller/   — @RestController classes
├── domain/       — JPA entities and enums
├── dto/          — Request/response records
├── exception/    — Custom exceptions + GlobalExceptionHandler
├── repository/   — Spring Data JPA interfaces
└── service/      — Business logic services
```

## Entity Rules

- All entities use `@Entity` and `@Table(name = "...")`.
- IDs use `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- Use `@CreationTimestamp` and `@UpdateTimestamp` from Hibernate for timestamp fields.
- No `@JsonIgnore` on entities — use DTOs for response shaping instead (but for this project, entities are returned directly for simplicity).

## DTO Rules

- All DTOs are Java 21 **records** (not classes).
- Use `jakarta.validation.constraints` annotations: `@NotBlank`, `@NotNull`, `@Size`.
- Controller methods must annotate record parameters with `@Valid`.

## Dependency Injection

- **Constructor injection only.** Never use `@Autowired` field injection.
- Use `@RequiredArgsConstructor` (Lombok) on `@Service` and `@RestController` classes where all fields are `final`.

## Transaction Rules

- `@Transactional` belongs on the **service layer**, not controllers or repositories.
- Read-only operations should use `@Transactional(readOnly = true)`.
- Avoid `@Transactional` on individual repository methods unless strictly necessary.

## Validation

- All incoming request bodies annotated with `@Valid` in controller methods.
- Validation errors are handled in `GlobalExceptionHandler` → return 400 with field error map.

## Database Migrations

- Schema is managed exclusively by **Flyway**. Never use `spring.jpa.hibernate.ddl-auto=create` or `update` in non-test profiles.
- Migration files go in `src/main/resources/db/migration/` with naming `V{n}__{description}.sql`.

## Naming Conventions

- Entity: `Ticket`, `Comment` (singular noun).
- Repository: `TicketRepository extends JpaRepository<Ticket, Long>`.
- Service: `TicketService`, `RagIngestionService`.
- Controller: `TicketController`, `AiController`.
- DTO: `CreateTicketRequest`, `AiAnswerResponse` (intent + Request/Response suffix).

## Logging

- Use SLF4J `@Slf4j` (Lombok). Log at DEBUG for routine operations, WARN/ERROR for exceptions.
- Never log the OpenAI API key or any credential.
