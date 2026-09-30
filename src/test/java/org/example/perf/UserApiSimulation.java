package org.example.perf;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Read-only load test for the user API. Runs against an already running app:
 * <pre>
 *   ./mvnw spring-boot:run
 *   set -a; source .env; set +a; ./mvnw gatling:test
 * </pre>
 * Settings come from environment variables: API_PASSWORD (required), API_USER (default api_user),
 * BASE_URL (default http://localhost:8088), USERS (default 200) and RAMP_SECONDS (default 30).
 * The write endpoints are left out on purpose, because this app has only one database.
 */
public class UserApiSimulation extends Simulation {

    private static final String BASE_URL = env("BASE_URL", "http://localhost:8088");
    private static final String API_USER = env("API_USER", "api_user");
    private static final String API_PASSWORD = env("API_PASSWORD", null);
    private static final int USERS = Integer.parseInt(env("USERS", "200"));
    private static final int RAMP_SECONDS = Integer.parseInt(env("RAMP_SECONDS", "30"));

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .basicAuth(API_USER, API_PASSWORD);

    // Lists the users, then fetches the first one by id when the list is not empty
    private final ScenarioBuilder browseUsers = scenario("Browse users")
            .exec(http("getUsers")
                    .get("/api/getUsers")
                    .check(status().is(200))
                    .check(jmesPath("[0].id").optional().saveAs("userId")))
            .pause(1)
            .doIf(session -> session.contains("userId")).then(
                    exec(http("getUser")
                            .get("/api/getUser/#{userId}")
                            .check(status().is(200))));

    {
        if (API_PASSWORD == null || API_PASSWORD.isBlank()) {
            throw new IllegalStateException("Set API_PASSWORD to the app's API password (see .env)");
        }
        setUp(browseUsers.injectOpen(rampUsers(USERS).during(RAMP_SECONDS)))
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().percentile(95.0).lt(300),
                        global().failedRequests().percent().lt(1.0));
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
