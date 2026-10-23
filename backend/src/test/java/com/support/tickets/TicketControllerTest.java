package com.support.tickets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.tickets.domain.Priority;
import com.support.tickets.domain.Ticket;
import com.support.tickets.domain.TicketStatus;
import com.support.tickets.exception.InvalidStateTransitionException;
import com.support.tickets.exception.TicketNotFoundException;
import com.support.tickets.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TicketController — Integration Tests")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    private Ticket sampleTicket;

    @BeforeEach
    void setUp() {
        sampleTicket = new Ticket();
        sampleTicket.setId(1L);
        sampleTicket.setTitle("Login page broken on Safari");
        sampleTicket.setDescription("Users on Safari 17 cannot log in.");
        sampleTicket.setStatus(TicketStatus.OPEN);
        sampleTicket.setPriority(Priority.HIGH);
        sampleTicket.setAssignee("alice");
        sampleTicket.setCategory("authentication");
        // Note: createdAt/updatedAt will be null in mock — acceptable for tests
    }

    // =========================================================
    // POST /api/tickets — Create
    // =========================================================

    @Test
    @DisplayName("POST /api/tickets — valid request returns 201")
    void createTicket_validRequest_returns201() throws Exception {
        when(ticketService.createTicket(any())).thenReturn(sampleTicket);

        Map<String, Object> body = Map.of(
                "title", "Login page broken on Safari",
                "description", "Users on Safari 17 cannot log in.",
                "priority", "HIGH",
                "assignee", "alice",
                "category", "authentication"
        );

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Login page broken on Safari"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /api/tickets — missing title returns 400")
    void createTicket_missingTitle_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "description", "Some description"
        );

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    @DisplayName("POST /api/tickets — missing description returns 400")
    void createTicket_missingDescription_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Some title"
        );

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    @DisplayName("POST /api/tickets — blank title returns 400")
    void createTicket_blankTitle_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "   ",
                "description", "Some description"
        );

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    // =========================================================
    // GET /api/tickets/{id}
    // =========================================================

    @Test
    @DisplayName("GET /api/tickets/{id} — found returns 200")
    void getTicket_found_returns200() throws Exception {
        when(ticketService.getTicket(1L)).thenReturn(sampleTicket);

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Login page broken on Safari"));
    }

    @Test
    @DisplayName("GET /api/tickets/{id} — not found returns 404")
    void getTicket_notFound_returns404() throws Exception {
        when(ticketService.getTicket(9999L)).thenThrow(new TicketNotFoundException(9999L));

        mockMvc.perform(get("/api/tickets/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ticket not found: 9999"));
    }

    // =========================================================
    // GET /api/tickets — List
    // =========================================================

    @Test
    @DisplayName("GET /api/tickets — returns list of tickets")
    void listTickets_returns200WithList() throws Exception {
        when(ticketService.listTickets(null, null)).thenReturn(List.of(sampleTicket));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // =========================================================
    // PATCH /api/tickets/{id}/status — Status Transition
    // =========================================================

    @Test
    @DisplayName("PATCH /api/tickets/{id}/status — valid transition returns 200")
    void transitionStatus_validTransition_returns200() throws Exception {
        Ticket inProgressTicket = new Ticket();
        inProgressTicket.setId(1L);
        inProgressTicket.setTitle("Login page broken on Safari");
        inProgressTicket.setDescription("Users on Safari 17 cannot log in.");
        inProgressTicket.setStatus(TicketStatus.IN_PROGRESS);
        inProgressTicket.setPriority(Priority.HIGH);

        when(ticketService.transitionStatus(eq(1L), eq(TicketStatus.IN_PROGRESS)))
                .thenReturn(inProgressTicket);

        Map<String, String> body = Map.of("status", "IN_PROGRESS");

        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PATCH /api/tickets/{id}/status — invalid transition returns 409")
    void transitionStatus_invalidTransition_returns409() throws Exception {
        when(ticketService.transitionStatus(eq(1L), eq(TicketStatus.RESOLVED)))
                .thenThrow(new InvalidStateTransitionException(TicketStatus.CLOSED, TicketStatus.RESOLVED));

        Map<String, String> body = Map.of("status", "RESOLVED");

        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("PATCH /api/tickets/{id}/status — missing status returns 400")
    void transitionStatus_missingStatus_returns400() throws Exception {
        Map<String, String> body = Map.of();

        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // POST /api/tickets/{id}/comments — Add Comment
    // =========================================================

    @Test
    @DisplayName("POST /api/tickets/{id}/comments — valid request returns 201")
    void addComment_validRequest_returns201() throws Exception {
        com.support.tickets.domain.Comment comment = new com.support.tickets.domain.Comment();
        comment.setId(1L);
        comment.setTicket(sampleTicket);
        comment.setAuthor("bob");
        comment.setBody("Looking into this now.");

        when(ticketService.addComment(eq(1L), any())).thenReturn(comment);

        Map<String, String> body = Map.of(
                "author", "bob",
                "body", "Looking into this now."
        );

        mockMvc.perform(post("/api/tickets/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("bob"));
    }
}
