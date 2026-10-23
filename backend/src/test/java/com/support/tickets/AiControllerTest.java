package com.support.tickets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.tickets.dto.AiAnswerResponse;
import com.support.tickets.service.AiAssistantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AiController — POST /api/ai/ask")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiAssistantService aiAssistantService;

    @Test
    @DisplayName("Grounded answer with citations returns 200")
    void ask_groundedAnswer_returns200WithCitations() throws Exception {
        when(aiAssistantService.ask(eq("What caused previous payment failures?")))
                .thenReturn(new AiAnswerResponse(
                        "Payment failures were caused by declined cards (Ticket #101).",
                        List.of("101"),
                        false));

        Map<String, String> body = Map.of("question", "What caused previous payment failures?");

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Payment failures were caused by declined cards (Ticket #101)."))
                .andExpect(jsonPath("$.ticket_ids[0]").value("101"))
                .andExpect(jsonPath("$.no_relevant_tickets").value(false));
    }

    @Test
    @DisplayName("No relevant tickets found returns honest no-match response, not a fabricated answer")
    void ask_noRelevantTickets_returnsHonestNoMatch() throws Exception {
        when(aiAssistantService.ask(eq("Do we support quantum teleportation refunds?")))
                .thenReturn(new AiAnswerResponse(
                        "No relevant tickets found for your query.",
                        List.of(),
                        true));

        Map<String, String> body = Map.of("question", "Do we support quantum teleportation refunds?");

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.no_relevant_tickets").value(true))
                .andExpect(jsonPath("$.ticket_ids").isEmpty());
    }

    @Test
    @DisplayName("Blank question returns 400")
    void ask_blankQuestion_returns400() throws Exception {
        Map<String, String> body = Map.of("question", "   ");

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Missing question field returns 400")
    void ask_missingQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
