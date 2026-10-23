package com.support.tickets.dto;

import java.util.List;

public record AiAnswerResponse(
        String answer,
        List<String> ticketIds,
        boolean noRelevantTickets
) {}
