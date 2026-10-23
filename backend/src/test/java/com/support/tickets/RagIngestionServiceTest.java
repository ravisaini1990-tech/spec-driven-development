package com.support.tickets;

import com.support.tickets.domain.Comment;
import com.support.tickets.domain.Priority;
import com.support.tickets.domain.Ticket;
import com.support.tickets.domain.TicketStatus;
import com.support.tickets.repository.CommentRepository;
import com.support.tickets.service.RagIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagIngestionService — Ingestion & Retrieval")
class RagIngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private CommentRepository commentRepository;

    private RagIngestionService ragIngestionService;

    @BeforeEach
    void setUp() {
        ragIngestionService = new RagIngestionService(vectorStore, commentRepository);
        ReflectionTestUtils.setField(ragIngestionService, "topK", 5);
        ReflectionTestUtils.setField(ragIngestionService, "similarityThreshold", 0.7);
    }

    @Test
    @DisplayName("ingest() deletes any existing vector document for the ticket before adding the new one")
    void ingest_deletesExistingDocumentBeforeAdding() {
        Ticket ticket = sampleTicket();
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        ragIngestionService.ingest(ticket);

        verify(vectorStore).delete(any(Filter.Expression.class));
        verify(vectorStore).add(anyList());
    }

    @Test
    @DisplayName("ingest() includes ticket comments in the document text so resolution history is searchable")
    void ingest_includesCommentsInDocumentText() {
        Ticket ticket = sampleTicket();
        Comment comment = new Comment();
        comment.setAuthor("agent-1");
        comment.setBody("Root cause was an expired card token; reissued and resolved.");
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(comment));

        ragIngestionService.ingest(ticket);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        String content = captor.getValue().get(0).getFormattedContent();
        assertThat(content).contains("Root cause was an expired card token; reissued and resolved.");
        assertThat(content).contains("agent-1");
    }

    @Test
    @DisplayName("ingest() attaches ticketId, status, priority, assignee, category as metadata")
    void ingest_attachesMetadata() {
        Ticket ticket = sampleTicket();
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        ragIngestionService.ingest(ticket);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        Document doc = captor.getValue().get(0);
        assertThat(doc.getMetadata())
                .containsEntry("ticketId", "1")
                .containsEntry("status", "OPEN")
                .containsEntry("priority", "HIGH")
                .containsEntry("assignee", "alice")
                .containsEntry("category", "payments");
    }

    @Test
    @DisplayName("ingest() with null ticket ID is a no-op and does not touch the vector store")
    void ingest_nullTicketId_isNoOp() {
        Ticket ticket = new Ticket();
        ticket.setId(null);

        ragIngestionService.ingest(ticket);

        verifyNoInteractions(vectorStore);
    }

    @Test
    @DisplayName("search() builds a SearchRequest using the configured topK and similarity threshold")
    void search_usesConfiguredTopKAndThreshold() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        ragIngestionService.search("payment failures");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        assertThat(captor.getValue().getTopK()).isEqualTo(5);
        assertThat(captor.getValue().getSimilarityThreshold()).isEqualTo(0.7);
    }

    private Ticket sampleTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTitle("Payment fails at checkout");
        ticket.setDescription("Card is declined for all US customers.");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(Priority.HIGH);
        ticket.setAssignee("alice");
        ticket.setCategory("payments");
        return ticket;
    }
}
