package com.support.tickets.service;

import com.support.tickets.domain.Comment;
import com.support.tickets.domain.Ticket;
import com.support.tickets.domain.TicketStatus;
import com.support.tickets.dto.AddCommentRequest;
import com.support.tickets.dto.CreateTicketRequest;
import com.support.tickets.dto.UpdateTicketRequest;
import com.support.tickets.exception.TicketNotFoundException;
import com.support.tickets.repository.CommentRepository;
import com.support.tickets.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final StateMachineService stateMachineService;
    private final RagIngestionService ragIngestionService;

    public TicketService(TicketRepository ticketRepository,
                         CommentRepository commentRepository,
                         StateMachineService stateMachineService,
                         RagIngestionService ragIngestionService) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.stateMachineService = stateMachineService;
        this.ragIngestionService = ragIngestionService;
    }

    @Transactional
    public Ticket createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(request.priority() != null ? request.priority() : com.support.tickets.domain.Priority.MEDIUM);
        ticket.setAssignee(request.assignee());
        ticket.setCategory(request.category());

        Ticket saved = ticketRepository.save(ticket);
        log.info("Created ticket id={}", saved.getId());

        ragIngestionService.ingest(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Ticket> listTickets(String search, TicketStatus status) {
        if (search != null && !search.isBlank() && status != null) {
            return ticketRepository.searchByTitleOrDescriptionAndStatus(search.trim(), status);
        } else if (search != null && !search.isBlank()) {
            return ticketRepository.searchByTitleOrDescription(search.trim());
        } else if (status != null) {
            return ticketRepository.findByStatus(status);
        } else {
            return ticketRepository.findAll();
        }
    }

    @Transactional(readOnly = true)
    public Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional
    public Ticket updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (request.title() != null && !request.title().isBlank()) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null && !request.description().isBlank()) {
            ticket.setDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignee() != null) {
            ticket.setAssignee(request.assignee());
        }
        if (request.category() != null) {
            ticket.setCategory(request.category());
        }

        Ticket saved = ticketRepository.save(ticket);
        log.info("Updated ticket id={}", saved.getId());

        ragIngestionService.ingest(saved);
        return saved;
    }

    @Transactional
    public Ticket transitionStatus(Long id, TicketStatus newStatus) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        stateMachineService.validateTransition(ticket.getStatus(), newStatus);

        ticket.setStatus(newStatus);
        Ticket saved = ticketRepository.save(ticket);
        log.info("Transitioned ticket id={} to status={}", saved.getId(), newStatus);

        ragIngestionService.ingest(saved);
        return saved;
    }

    @Transactional
    public Comment addComment(Long ticketId, AddCommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(request.author());
        comment.setBody(request.body());

        Comment saved = commentRepository.save(comment);
        log.info("Added comment id={} to ticket id={}", saved.getId(), ticketId);

        ragIngestionService.ingest(ticket);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Comment> getComments(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }
}
