package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /*
     * CSRF protection is disabled deliberately (Sonar java:S4502). This is safe because:
     *  - The API is stateless: no session or auth cookie exists for a forged request to ride on.
     *  - Even with browser-cached Basic credentials, a cross-site page can only send "simple"
     *    requests (GET/POST with form or text bodies). Every state-changing endpoint needs an
     *    application/json body or PUT/DELETE, both of which require a CORS preflight, and no
     *    CORS origins are allowed. Form/text bodies are rejected with 415.
     * Both conditions are covered by user_api.feature ("Cross-site requests cannot change data").
     * Revisit this if cookie-based login, CORS origins or form-encoded endpoints are ever added.
     */
    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
