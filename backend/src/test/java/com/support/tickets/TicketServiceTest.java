package com.support.tickets;

import com.support.tickets.domain.Comment;
import com.support.tickets.domain.Priority;
import com.support.tickets.domain.Ticket;
import com.support.tickets.domain.TicketStatus;
import com.support.tickets.dto.AddCommentRequest;
import com.support.tickets.dto.CreateTicketRequest;
import com.support.tickets.dto.UpdateTicketRequest;
import com.support.tickets.exception.TicketNotFoundException;
import com.support.tickets.repository.CommentRepository;
import com.support.tickets.repository.TicketRepository;
import com.support.tickets.service.RagIngestionService;
import com.support.tickets.service.StateMachineService;
import com.support.tickets.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService — Create/Update/Search/Comment")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private StateMachineService stateMachineService;
    @Mock
    private RagIngestionService ragIngestionService;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, commentRepository, stateMachineService, ragIngestionService);
    }

    @Test
    @DisplayName("createTicket() defaults status to OPEN and re-ingests into the vector store")
    void createTicket_defaultsStatusAndIngests() {
        CreateTicketRequest request = new CreateTicketRequest("Title", "Desc", Priority.HIGH, "alice", "payments");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        Ticket saved = ticketService.createTicket(request);

        assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
        verify(ragIngestionService).ingest(saved);
    }

    @Test
    @DisplayName("createTicket() defaults priority to MEDIUM when not provided")
    void createTicket_defaultsPriorityWhenNull() {
        CreateTicketRequest request = new CreateTicketRequest("Title", "Desc", null, null, null);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket saved = ticketService.createTicket(request);

        assertThat(saved.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("updateTicket() ignores blank title/description but applies other fields, then re-ingests")
    void updateTicket_ignoresBlankFieldsAppliesOthers() {
        Ticket existing = new Ticket();
        existing.setId(1L);
        existing.setTitle("Old title");
        existing.setDescription("Old description");
        existing.setAssignee("alice");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTicketRequest request = new UpdateTicketRequest("   ", null, null, "bob", null);
        Ticket updated = ticketService.updateTicket(1L, request);

        assertThat(updated.getTitle()).isEqualTo("Old title");
        assertThat(updated.getAssignee()).isEqualTo("bob");
        verify(ragIngestionService).ingest(updated);
    }

    @Test
    @DisplayName("updateTicket() throws TicketNotFoundException for unknown ticket id")
    void updateTicket_unknownId_throwsNotFound() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
                () -> ticketService.updateTicket(999L, new UpdateTicketRequest(null, null, null, null, null)));
    }

    @Test
    @DisplayName("listTickets() with only a status filter delegates to findByStatus")
    void listTickets_statusOnly_usesFindByStatus() {
        when(ticketRepository.findByStatus(TicketStatus.OPEN)).thenReturn(List.of(new Ticket()));

        List<Ticket> result = ticketService.listTickets(null, TicketStatus.OPEN);

        assertThat(result).hasSize(1);
        verify(ticketRepository).findByStatus(TicketStatus.OPEN);
        verifyNoMoreInteractions(ticketRepository);
    }

    @Test
    @DisplayName("listTickets() with only a search keyword delegates to searchByTitleOrDescription")
    void listTickets_searchOnly_usesSearch() {
        when(ticketRepository.searchByTitleOrDescription("payment")).thenReturn(List.of(new Ticket()));

        List<Ticket> result = ticketService.listTickets("payment", null);

        assertThat(result).hasSize(1);
        verify(ticketRepository).searchByTitleOrDescription("payment");
    }

    @Test
    @DisplayName("listTickets() with both search and status combines both filters")
    void listTickets_searchAndStatus_usesCombinedQuery() {
        when(ticketRepository.searchByTitleOrDescriptionAndStatus("payment", TicketStatus.OPEN))
                .thenReturn(List.of(new Ticket()));

        List<Ticket> result = ticketService.listTickets("payment", TicketStatus.OPEN);

        assertThat(result).hasSize(1);
        verify(ticketRepository).searchByTitleOrDescriptionAndStatus("payment", TicketStatus.OPEN);
    }

    @Test
    @DisplayName("listTickets() with neither filter returns all tickets")
    void listTickets_noFilters_returnsAll() {
        when(ticketRepository.findAll()).thenReturn(List.of(new Ticket(), new Ticket()));

        List<Ticket> result = ticketService.listTickets(null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("transitionStatus() validates via StateMachineService before saving and re-ingests")
    void transitionStatus_validatesAndIngests() {
        Ticket existing = new Ticket();
        existing.setId(1L);
        existing.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = ticketService.transitionStatus(1L, TicketStatus.IN_PROGRESS);

        verify(stateMachineService).validateTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        assertThat(result.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(ragIngestionService).ingest(result);
    }

    @Test
    @DisplayName("addComment() persists the comment and re-ingests the ticket so the knowledge base stays fresh")
    void addComment_persistsAndReIngestsTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        AddCommentRequest request = new AddCommentRequest("bob", "Investigating now.");
        ticketService.addComment(1L, request);

        verify(commentRepository).save(any(Comment.class));
        verify(ragIngestionService).ingest(ticket);
    }

    @Test
    @DisplayName("addComment() throws TicketNotFoundException for unknown ticket id")
    void addComment_unknownTicket_throwsNotFound() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
                () -> ticketService.addComment(999L, new AddCommentRequest("bob", "text")));
    }
}
