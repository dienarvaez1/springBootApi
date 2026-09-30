package org.example.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Client-supplied fields for creating or updating a user. The id and timestamps are
 * intentionally absent so clients cannot overwrite them.
 */
public record UserRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName
) {
}
