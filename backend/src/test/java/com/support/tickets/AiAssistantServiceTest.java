package com.support.tickets;

import com.support.tickets.dto.AiAnswerResponse;
import com.support.tickets.service.AiAssistantService;
import com.support.tickets.service.RagIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAssistantService — RAG Grounding & Citation")
class AiAssistantServiceTest {

    @Mock
    private RagIngestionService ragIngestionService;

    @Mock
    private ChatModel chatModel;

    private AiAssistantService aiAssistantService;

    @BeforeEach
    void setUp() {
        aiAssistantService = new AiAssistantService(ragIngestionService, chatModel);
    }

    @Test
    @DisplayName("No relevant documents found — returns honest no-match response without calling the LLM")
    void ask_noRelevantDocuments_returnsNoMatchWithoutCallingLlm() {
        when(ragIngestionService.search("Have we seen payment failures before?"))
                .thenReturn(List.of());

        AiAnswerResponse response = aiAssistantService.ask("Have we seen payment failures before?");

        assertThat(response.noRelevantTickets()).isTrue();
        assertThat(response.ticketIds()).isEmpty();
        assertThat(response.answer()).isEqualTo("No relevant tickets found for your query.");
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("Relevant documents found — calls LLM once and cites ticket IDs from metadata")
    void ask_relevantDocumentsFound_callsLlmAndCitesTicketIds() {
        Document doc1 = new Document("Title: Payment fails\nDescription: Card declined",
                Map.of("ticketId", "101", "status", "RESOLVED"));
        Document doc2 = new Document("Title: Payment timeout\nDescription: Gateway timeout",
                Map.of("ticketId", "102", "status", "CLOSED"));

        when(ragIngestionService.search("What caused previous payment failures?"))
                .thenReturn(List.of(doc1, doc2));
        when(chatModel.call(any(Prompt.class))).thenReturn(fakeChatResponse(
                "Payment failures were caused by declined cards (Ticket #101) and gateway timeouts (Ticket #102)."));

        AiAnswerResponse response = aiAssistantService.ask("What caused previous payment failures?");

        assertThat(response.noRelevantTickets()).isFalse();
        assertThat(response.ticketIds()).containsExactlyInAnyOrder("101", "102");
        assertThat(response.answer()).contains("Ticket #101", "Ticket #102");
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Ticket IDs cited come from retrieved document metadata, not parsed from LLM text")
    void ask_ticketIdsAreExtractedFromMetadataNotLlmOutput() {
        Document doc = new Document("Title: Shipment stuck\nDescription: Tracking not updating",
                Map.of("ticketId", "55", "status", "OPEN"));

        when(ragIngestionService.search(any())).thenReturn(List.of(doc));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(fakeChatResponse("The shipment issue is a known tracking delay."));

        AiAnswerResponse response = aiAssistantService.ask("Any shipment tracking issues?");

        assertThat(response.ticketIds()).containsExactly("55");
    }

    @Test
    @DisplayName("Duplicate ticket IDs across retrieved chunks are de-duplicated in citations")
    void ask_duplicateTicketIdsAreDeduplicated() {
        Document doc1 = new Document("chunk A", Map.of("ticketId", "7"));
        Document doc2 = new Document("chunk B", Map.of("ticketId", "7"));

        when(ragIngestionService.search(any())).thenReturn(List.of(doc1, doc2));
        when(chatModel.call(any(Prompt.class))).thenReturn(fakeChatResponse("Answer referencing Ticket #7."));

        AiAnswerResponse response = aiAssistantService.ask("question");

        assertThat(response.ticketIds()).containsExactly("7");
    }

    @Test
    @DisplayName("LLM failure surfaces as a RuntimeException rather than a fabricated answer")
    void ask_llmThrows_propagatesAsRuntimeException() {
        Document doc = new Document("chunk", Map.of("ticketId", "9"));
        when(ragIngestionService.search(any())).thenReturn(List.of(doc));
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("upstream timeout"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> aiAssistantService.ask("question"));
    }

    private ChatResponse fakeChatResponse(String text) {
        Generation generation = new Generation(new org.springframework.ai.chat.messages.AssistantMessage(text));
        return new ChatResponse(List.of(generation), ChatResponseMetadata.builder().build());
    }
}
