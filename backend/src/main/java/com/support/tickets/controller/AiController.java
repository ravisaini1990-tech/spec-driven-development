package com.support.tickets.controller;

import com.support.tickets.dto.AiAnswerResponse;
import com.support.tickets.dto.AskRequest;
import com.support.tickets.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/ask")
    public ResponseEntity<AiAnswerResponse> ask(@Valid @RequestBody AskRequest request) {
        log.info("AI ask request received: {}", request.question());
        AiAnswerResponse response = aiAssistantService.ask(request.question());
        return ResponseEntity.ok(response);
    }
}
