package com.support.tickets.dto;

import com.support.tickets.domain.Priority;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(min = 1, message = "title must not be blank when provided")
        String title,
        @Size(min = 1, message = "description must not be blank when provided")
        String description,
        Priority priority,
        String assignee,
        String category
) {}
