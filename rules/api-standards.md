# API Standards Steering Rules

## REST Conventions

- Resource URLs use plural nouns: `/api/tickets`, not `/api/ticket`.
- Use HTTP methods semantically: GET (read), POST (create), PATCH (partial update), DELETE (remove).
- Sub-resources: `/api/tickets/{id}/comments`, `/api/tickets/{id}/status`.
- AI endpoints are grouped under `/api/ai/`.

## JSON Naming

- All JSON fields use **snake_case** (e.g. `created_at`, `ticket_id`, `no_relevant_tickets`).
- Configure globally in Spring via:
  ```yaml
  spring:
    jackson:
      property-naming-strategy: SNAKE_CASE
  ```
  Or via `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` on DTO classes.

## HTTP Status Codes

| Situation | Code |
|-----------|------|
| Created successfully | 201 |
| Retrieved / updated successfully | 200 |
| Request body validation failure | 400 |
| Resource not found | 404 |
| Invalid state transition | 409 |
| Internal / upstream error | 500 |

## Error Response Format

All error responses return a JSON object:

```json
// Single error:
{ "error": "Ticket not found: 42" }

// Validation errors:
{ "errors": { "title": "must not be blank", "description": "must not be blank" } }
```

## CORS

Allow origin `http://localhost:5173` globally via `@CrossOrigin` on controllers or a `WebMvcConfigurer` bean.

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS");
    }
}
```

## Pagination

Not required in v1. List endpoints return all results. Add pagination when ticket count exceeds 1000.

## Versioning

No versioning prefix in v1 (just `/api/`). Add `/api/v2/` when breaking changes are introduced.
