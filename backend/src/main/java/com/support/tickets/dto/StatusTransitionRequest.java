package com.support.tickets.dto;

import com.support.tickets.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(
        @NotNull(message = "Status must not be null")
        TicketStatus status
) {}
