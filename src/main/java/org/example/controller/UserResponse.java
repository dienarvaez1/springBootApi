package org.example.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.entities.User;

import java.time.Instant;

/**
 * A user as returned by the API. Kept separate from the {@link User} entity so the
 * database model can change without changing the API.
 */
@Schema(description = "A stored user")
public record UserResponse(
        @Schema(description = "Server-assigned id", example = "1",
                accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
        Integer id,
        @Schema(example = "Grace", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,
        @Schema(example = "Hopper", requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,
        @Schema(description = "When the user was created (UTC)", example = "2026-09-30T05:39:19.444Z",
                accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,
        @Schema(description = "When the user was last changed (UTC)", example = "2026-09-30T05:39:19.444Z",
                accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt
) {

    static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
