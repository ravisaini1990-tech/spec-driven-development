package com.support.tickets.service;

import com.support.tickets.domain.Comment;
import com.support.tickets.domain.Ticket;
import com.support.tickets.repository.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Handles ingestion of tickets into the PGVector store for RAG retrieval.
 *
 * Strategy: one document per ticket (field-group chunking), including all of the
 * ticket's comments so the knowledge base captures resolution history, not just
 * the original description.
 * Re-ingestion: delete existing document by ticketId metadata, then insert new.
 */
@Slf4j
@Service
public class RagIngestionService {

    private final VectorStore vectorStore;
    private final CommentRepository commentRepository;

    @Value("${rag.retrieval.top-k:5}")
    private int topK;

    @Value("${rag.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    public RagIngestionService(VectorStore vectorStore, CommentRepository commentRepository) {
        this.vectorStore = vectorStore;
        this.commentRepository = commentRepository;
    }

    /**
     * Deletes any existing vector document for this ticket, then inserts a fresh one.
     *
     * @param ticket the ticket to ingest
     */
    public void ingest(Ticket ticket) {
        if (ticket.getId() == null) {
            log.warn("Attempted to ingest ticket with null ID — skipping");
            return;
        }

        String ticketIdStr = String.valueOf(ticket.getId());

        // Delete existing document(s) for this ticket to avoid duplicates
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            vectorStore.delete(b.eq("ticketId", ticketIdStr).build());
            log.debug("Deleted existing vector document(s) for ticketId={}", ticketIdStr);
        } catch (Exception e) {
            log.warn("Failed to delete existing vector document for ticketId={}: {}", ticketIdStr, e.getMessage());
        }

        // Build new document
        Map<String, Object> metadata = Map.of(
                "ticketId", ticketIdStr,
                "status",   ticket.getStatus() != null ? ticket.getStatus().name() : "OPEN",
                "priority", ticket.getPriority() != null ? ticket.getPriority().name() : "MEDIUM",
                "assignee", ticket.getAssignee() != null ? ticket.getAssignee() : "",
                "category", ticket.getCategory() != null ? ticket.getCategory() : ""
        );

        List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
        Document document = new Document(ticket.toDocumentText(comments), metadata);

        try {
            vectorStore.add(List.of(document));
            log.info("Ingested ticket ticketId={} into vector store", ticketIdStr);
        } catch (Exception e) {
            log.error("Failed to ingest ticket ticketId={} into vector store: {}", ticketIdStr, e.getMessage(), e);
        }
    }

    /**
     * Searches for similar documents given a query string.
     *
     * @param query the natural language question
     * @return list of matching documents
     */
    public List<Document> search(String query) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
        return vectorStore.similaritySearch(request);
    }
}
