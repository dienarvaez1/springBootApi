package org.example.controller;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Documentation-only schema for the error body Spring Boot returns on 400/401/404.
 * The body itself is produced by Spring Boot's error handling, not by this class.
 */
@Schema(name = "ErrorResponse", description = "Standard Spring Boot error body")
public record ErrorResponse(
        @Schema(example = "2026-09-30T05:39:19.444+00:00") OffsetDateTime timestamp,
        @Schema(example = "404") int status,
        @Schema(example = "Not Found") String error,
        @Schema(example = "/api/getUser/99") String path
) {
}
