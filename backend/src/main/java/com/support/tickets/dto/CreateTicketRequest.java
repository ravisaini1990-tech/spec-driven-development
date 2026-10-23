package com.support.tickets.dto;

import com.support.tickets.domain.Priority;
import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank(message = "Title must not be blank")
        String title,

        @NotBlank(message = "Description must not be blank")
        String description,

        Priority priority,

        String assignee,

        String category
) {}
