package com.support.tickets.service;

import com.support.tickets.dto.AiAnswerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handles RAG-based question answering over the support ticket vector store.
 */
@Slf4j
@Service
public class AiAssistantService {

    private static final String SYSTEM_PROMPT =
            "You are a support ticket assistant. Answer ONLY based on the provided ticket context below. " +
            "If the answer is not present in the context, say \"I don't know based on the available tickets.\" " +
            "Always cite the ticket IDs you used in your answer (e.g. \"Ticket #1\", \"Ticket #3\"). " +
            "Do not invent information not present in the context.";

    private static final String NO_TICKETS_ANSWER = "No relevant tickets found for your query.";

    private final RagIngestionService ragIngestionService;
    private final ChatModel chatModel;

    public AiAssistantService(RagIngestionService ragIngestionService, ChatModel chatModel) {
        this.ragIngestionService = ragIngestionService;
        this.chatModel = chatModel;
    }

    /**
     * Answers a question using RAG over the ticket vector store.
     *
     * @param question the user's natural language question
     * @return an answer with ticket citations
     */
    public AiAnswerResponse ask(String question) {
        log.info("Processing AI question: {}", question);

        // Step 1: Retrieve relevant ticket documents
        List<Document> relevantDocs = ragIngestionService.search(question);

        // Step 2: If no relevant documents, short-circuit without calling LLM
        if (relevantDocs.isEmpty()) {
            log.info("No relevant tickets found for question");
            return new AiAnswerResponse(NO_TICKETS_ANSWER, List.of(), true);
        }

        // Step 3: Extract ticket IDs from metadata
        List<String> ticketIds = relevantDocs.stream()
                .map(doc -> (String) doc.getMetadata().get("ticketId"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Step 4: Build context string from retrieved documents
        String context = relevantDocs.stream()
                .map(doc -> {
                    String id = (String) doc.getMetadata().get("ticketId");
                    return "Ticket #" + id + ":\n" + doc.getFormattedContent();
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        // Step 5: Build prompt and call LLM
        String userPromptText = "Context:\n" + context + "\n\nQuestion: " + question;

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(userPromptText)
        ));

        String answer;
        try {
            answer = chatModel.call(prompt).getResult().getOutput().getText();
            log.info("AI answer generated using {} ticket(s)", ticketIds.size());
        } catch (Exception e) {
            log.error("Failed to generate AI answer", e);
            throw new RuntimeException("Failed to generate AI answer: " + e.getMessage(), e);
        }

        return new AiAnswerResponse(answer, ticketIds, false);
    }
}
