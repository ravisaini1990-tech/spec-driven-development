# RAG / Vector Store Steering Rules

## Spring AI API Usage

- Use `VectorStore` (interface) — never depend on `PgVectorStore` directly. This allows swapping implementations.
- Use `EmbeddingModel` interface — not `OpenAiEmbeddingModel` directly.
- Inject both via constructor injection.

## Document Creation

```java
// Correct: use Map.of() for immutable metadata
Document doc = new Document(
    ticket.toDocumentText(),
    Map.of(
        "ticketId", String.valueOf(ticket.getId()),
        "status",   ticket.getStatus().name(),
        "priority", ticket.getPriority().name(),
        "assignee", ticket.getAssignee() != null ? ticket.getAssignee() : "",
        "category", ticket.getCategory() != null ? ticket.getCategory() : ""
    )
);
vectorStore.add(List.of(doc));
```

## Deletion Before Re-ingestion

Always delete the existing document before re-inserting to avoid duplicates:

```java
// Spring AI PgVectorStore supports FilterExpressionBuilder
FilterExpressionBuilder b = new FilterExpressionBuilder();
vectorStore.delete(b.eq("ticketId", String.valueOf(ticket.getId())).build());
```

## Similarity Search

```java
SearchRequest request = SearchRequest.builder()
    .query(question)
    .topK(topK)
    .similarityThreshold(similarityThreshold)
    .build();
List<Document> results = vectorStore.similaritySearch(request);
```

## Configuration

Both `topK` and `similarityThreshold` must be injected via `@Value`:

```java
@Value("${rag.retrieval.top-k:5}")
private int topK;

@Value("${rag.retrieval.similarity-threshold:0.7}")
private double similarityThreshold;
```

Never hardcode these values.

## Context Building

Build context for the LLM from retrieved documents:

```java
String context = results.stream()
    .map(doc -> "Ticket #" + doc.getMetadata().get("ticketId") + ":\n" + doc.getFormattedContent())
    .collect(Collectors.joining("\n\n---\n\n"));
```

## Ticket ID Extraction

Extract ticketIds from metadata (not from LLM response text):

```java
List<String> ticketIds = results.stream()
    .map(doc -> (String) doc.getMetadata().get("ticketId"))
    .filter(Objects::nonNull)
    .distinct()
    .toList();
```

## PGVector Configuration

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true   # auto-creates vector_store table
        dimensions: 1536          # must match embedding model output
        distance-type: COSINE_DISTANCE
        index-type: HNSW
```

Do not set `dimensions` lower than the embedding model's output — this causes data corruption.
