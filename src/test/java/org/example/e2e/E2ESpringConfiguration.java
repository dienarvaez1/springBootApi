package org.example.e2e;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Runs the real application (security, filters, JPA, Hibernate DDL) on a random port against
 * the local Postgres at localhost:5432, in a dedicated springboot_app_e2e database.
 * Override with E2E_DB_URL / E2E_DB_USERNAME / E2E_DB_PASSWORD; the local .env file is not read.
 * The scenarios delete every user before they run, so the database name must end in "_e2e"
 * (checked in UserApiE2ESteps) to keep them away from real data.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=",
        "spring.datasource.url=${E2E_DB_URL:jdbc:postgresql://localhost:5432/springboot_app_e2e}",
        "spring.datasource.username=${E2E_DB_USERNAME:${user.name}}",
        "spring.datasource.password=${E2E_DB_PASSWORD:}",
        "spring.security.user.name=" + E2ESpringConfiguration.USERNAME,
        "spring.security.user.password=" + E2ESpringConfiguration.PASSWORD,
        "spring.jpa.show-sql=false",
        "logging.level.org.springframework.security=warn"
})
public class E2ESpringConfiguration {

    static final String USERNAME = "e2e_user";
    static final String PASSWORD = "e2e_password";
}
