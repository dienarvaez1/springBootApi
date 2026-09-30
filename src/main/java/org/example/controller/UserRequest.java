package org.example.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Client-supplied fields for creating or updating a user. The id and timestamps are
 * intentionally absent so clients cannot overwrite them.
 */
@Schema(description = "Names for a new or updated user. Any other fields in the body, such as id, are ignored.")
public record UserRequest(
        @Schema(description = "First name; must contain a non-whitespace character",
                example = "Grace", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 1, max = 255) String firstName,
        @Schema(description = "Last name; must contain a non-whitespace character",
                example = "Hopper", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 1, max = 255) String lastName
) {
}
