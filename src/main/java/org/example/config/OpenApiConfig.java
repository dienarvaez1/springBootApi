package org.example.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documents the API's HTTP Basic auth in the OpenAPI spec, so every operation lists it and
 * Swagger UI offers an "Authorize" button.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "User API", version = "1.0",
                description = "Create, read, update and delete users. Every endpoint requires HTTP Basic auth."),
        security = @SecurityRequirement(name = "basicAuth")
)
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public class OpenApiConfig {
}
