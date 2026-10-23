package com.support.tickets.controller;

import com.support.tickets.domain.Comment;
import com.support.tickets.domain.Ticket;
import com.support.tickets.domain.TicketStatus;
import com.support.tickets.dto.AddCommentRequest;
import com.support.tickets.dto.CreateTicketRequest;
import com.support.tickets.dto.StatusTransitionRequest;
import com.support.tickets.dto.UpdateTicketRequest;
import com.support.tickets.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        Ticket ticket = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> listTickets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status) {
        List<Ticket> tickets = ticketService.listTickets(search, status);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id) {
        Ticket ticket = ticketService.getTicket(id);
        return ResponseEntity.ok(ticket);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {
        Ticket ticket = ticketService.updateTicket(id, request);
        return ResponseEntity.ok(ticket);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Ticket> transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusTransitionRequest request) {
        Ticket ticket = ticketService.transitionStatus(id, request.status());
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request) {
        Comment comment = ticketService.addComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long id) {
        List<Comment> comments = ticketService.getComments(id);
        return ResponseEntity.ok(comments);
    }
}
