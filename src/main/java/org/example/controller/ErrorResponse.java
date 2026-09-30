package org.example.controller;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Documentation-only schema for the error body Spring Boot returns on 400/401/404/415.
 * The body itself is produced by Spring Boot's error handling, not by this class.
 */
@Schema(name = "ErrorResponse", description = "Standard Spring Boot error body. It does not say which field was invalid.")
public record ErrorResponse(
        @Schema(description = "When the error occurred", example = "2026-09-30T05:39:19.444+00:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime timestamp,
        @Schema(description = "HTTP status code", example = "404", requiredMode = Schema.RequiredMode.REQUIRED)
        int status,
        @Schema(description = "HTTP status reason", example = "Not Found", requiredMode = Schema.RequiredMode.REQUIRED)
        String error,
        @Schema(description = "Request path", example = "/api/users/99", requiredMode = Schema.RequiredMode.REQUIRED)
        String path
) {
}
