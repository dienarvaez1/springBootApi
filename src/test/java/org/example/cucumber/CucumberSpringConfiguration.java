package org.example.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.example.config.SecurityConfig;
import org.example.controller.UserController;
import org.example.repo.UserRepo;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Spring context shared by all scenarios: only the web layer (UserController, the real
 * SecurityConfig and request filters) with the repository mocked, so no database is needed.
 * Credentials are fixed here so the tests don't depend on a local .env file.
 */
@CucumberContextConfiguration
@WebMvcTest(controllers = UserController.class, properties = {
        "spring.config.import=",
        "spring.security.user.name=" + CucumberSpringConfiguration.USERNAME,
        "spring.security.user.password=" + CucumberSpringConfiguration.PASSWORD,
        "logging.level.org.springframework.security=warn"
})
@Import(SecurityConfig.class)
public class CucumberSpringConfiguration {

    static final String USERNAME = "test_user";
    static final String PASSWORD = "test_password";

    @MockitoBean
    private UserRepo userRepo;
}
