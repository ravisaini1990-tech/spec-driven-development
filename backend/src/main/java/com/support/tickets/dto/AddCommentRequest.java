package com.support.tickets.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
        @NotBlank(message = "Author must not be blank")
        String author,

        @NotBlank(message = "Body must not be blank")
        String body
) {}
