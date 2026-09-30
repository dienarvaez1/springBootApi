package org.example.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.example.entities.User;
import org.example.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UserApiE2ESteps {

    private static final List<String> FIRST_NAMES = List.of(
            "Ada", "Grace", "Alan", "Linus", "Margaret", "Dennis", "Barbara", "Ken", "Frances", "Edsger",
            "Hedy", "Tim", "Radia", "José", "Zoë", "Björn", "Chloé", "Anaïs", "Siobhán", "Małgorzata");
    private static final List<String> LAST_NAMES = List.of(
            "Lovelace", "Hopper", "Turing", "Torvalds", "Hamilton", "Ritchie", "Liskov", "Thompson", "Allen",
            "Dijkstra", "Lamarr", "Berners-Lee", "Perlman", "O'Neil", "García", "Müller", "Nguyễn", "Smith-Jones");
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZáéíóúñüßøåæ李王张 '-";

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    private Random random;

    /** Users created by this scenario, keyed by the id the API returned, in creation order. */
    private final Map<Integer, CreatedUser> added = new LinkedHashMap<>();
    private final List<HttpResponse<String>> addResponses = new ArrayList<>();
    private final List<HttpResponse<String>> renameResponses = new ArrayList<>();
    private final List<HttpResponse<String>> deleteResponses = new ArrayList<>();
    private final Set<Integer> deletedIds = new HashSet<>();
    private HttpResponse<String> response;

    private static final class CreatedUser {
        final int id;
        String firstName;
        String lastName;
        final OffsetDateTime createdAt;
        final OffsetDateTime updatedAt;

        CreatedUser(int id, String firstName, String lastName, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    @Before
    public void seedRandom(Scenario scenario) {
        String fixed = System.getProperty("e2e.seed");
        long seed = fixed != null ? Long.parseLong(fixed) : System.nanoTime();
        random = new Random(seed);
        scenario.log("Random seed: " + seed + "  (replay with -De2e.seed=" + seed + ")");
    }

    /** Lets steps say "the 2nd added user". */
    @ParameterType("(\\d+)(?:st|nd|rd|th)")
    public Integer ordinal(String number) {
        return Integer.valueOf(number);
    }

    // --- Setup ---

    @Given("the user table is empty")
    public void theUserTableIsEmpty() {
        // Guard: this wipes user_table, so never run against a database that isn't a dedicated e2e one
        assertThat(datasourceUrl).as("E2E tests delete all users; database name must end in _e2e")
                .matches(".*/[^/?]+_e2e(\\?.*)?");
        userRepo.deleteAll();
    }

    // --- Actions ---

    @When("I add {int} random users")
    public void iAddRandomUsers(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            addUser(randomFirstName(), randomLastName(), null);
        }
    }

    @When("I add a user with first name of {int} random characters and last name {string}")
    public void iAddAUserWithLongFirstName(int length, String lastName) throws Exception {
        addUser(randomString(length), lastName, null);
    }

    @And("I add a random user whose request body claims the {ordinal} added user's id")
    public void iAddAUserClaimingAnExistingId(int position) throws Exception {
        addUser(randomFirstName(), randomLastName(), nthAdded(position).id);
    }

    @And("I rename each added user to new random names")
    public void iRenameEachAddedUser() throws Exception {
        for (CreatedUser user : added.values()) {
            String firstName = randomFirstName() + "-renamed";
            String lastName = randomLastName() + "-renamed";
            HttpResponse<String> put = send("PUT", "/api/updateUser/" + user.id, json(firstName, lastName, null), true);
            renameResponses.add(put);
            if (put.statusCode() == 200) {
                user.firstName = firstName;
                user.lastName = lastName;
            }
        }
    }

    @When("I delete each added user")
    public void iDeleteEachAddedUser() throws Exception {
        for (CreatedUser user : added.values()) {
            deleteResponses.add(send("DELETE", "/api/deleteUser/" + user.id, null, true));
            deletedIds.add(user.id);
        }
    }

    @And("I delete the {ordinal} added user")
    public void iDeleteTheNthAddedUser(int position) throws Exception {
        CreatedUser user = nthAdded(position);
        HttpResponse<String> delete = send("DELETE", "/api/deleteUser/" + user.id, null, true);
        assertThat(delete.statusCode()).isEqualTo(204);
        deletedIds.add(user.id);
    }

    @When("I send {word} {string} with body {string}")
    public void iSend(String method, String path, String body) throws Exception {
        response = send(method, path, body.isEmpty() ? null : body, true);
    }

    @When("an anonymous client sends {word} {string} with body {string}")
    public void anAnonymousClientSends(String method, String path, String body) throws Exception {
        response = send(method, path, body.isEmpty() ? null : body, false);
    }

    // --- Assertions ---

    @Then("every add responded 201 with a distinct server-assigned id")
    public void everyAddResponded201() {
        assertThat(addResponses).isNotEmpty().allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(201));
        assertThat(added).hasSameSizeAs(addResponses);
    }

    @And("each added user can be retrieved by its id with the names it was created with")
    @And("each added user can be retrieved by its id with its new names")
    public void eachAddedUserCanBeRetrieved() throws Exception {
        for (CreatedUser user : added.values()) {
            assertRetrievable(user);
        }
    }

    @And("each added user has server-set created_at and updated_at timestamps")
    public void eachAddedUserHasTimestamps() {
        OffsetDateTime testStart = OffsetDateTime.now().minusMinutes(5);
        for (CreatedUser user : added.values()) {
            assertThat(user.createdAt).isAfter(testStart);
            assertThat(user.updatedAt).isAfterOrEqualTo(user.createdAt);
        }
    }

    @And("every added user is stored in the database")
    public void everyAddedUserIsStored() {
        for (CreatedUser user : added.values()) {
            User row = userRepo.findById(user.id).orElseThrow(() -> new AssertionError("No row for id " + user.id));
            assertThat(row.getFirstName()).isEqualTo(user.firstName);
            assertThat(row.getLastName()).isEqualTo(user.lastName);
        }
    }

    @Then("listing all users returns exactly the added users")
    public void listingReturnsExactlyTheAddedUsers() throws Exception {
        assertListContainsExactly(added.values().stream().toList());
    }

    @And("listing all users returns exactly the remaining added users")
    public void listingReturnsTheRemainingUsers() throws Exception {
        assertListContainsExactly(remaining());
    }

    @And("listing all users returns no users")
    public void listingReturnsNoUsers() throws Exception {
        assertListContainsExactly(List.of());
    }

    @And("the listed users are ordered by id, newest first")
    public void theListedUsersAreOrderedNewestFirst() throws Exception {
        List<Integer> ids = new ArrayList<>();
        list().forEach(u -> ids.add(u.get("id").asInt()));
        assertThat(ids).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Then("every rename responded 200 with the new names")
    public void everyRenameResponded200() throws Exception {
        assertThat(renameResponses).hasSameSizeAs(added.values());
        List<CreatedUser> users = added.values().stream().toList();
        for (int i = 0; i < users.size(); i++) {
            HttpResponse<String> put = renameResponses.get(i);
            assertThat(put.statusCode()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(put.body());
            assertThat(body.get("id").asInt()).isEqualTo(users.get(i).id);
            assertThat(body.get("firstName").asText()).isEqualTo(users.get(i).firstName);
            assertThat(body.get("lastName").asText()).isEqualTo(users.get(i).lastName);
        }
    }

    @And("each renamed user's updated_at moved forward while created_at stayed the same")
    public void updatedAtMovedForward() throws Exception {
        for (CreatedUser user : added.values()) {
            JsonNode current = get(user.id);
            assertThat(timestamp(current, "created_at")).isEqualTo(user.createdAt);
            assertThat(timestamp(current, "updated_at")).isAfter(user.updatedAt);
        }
    }

    @Then("every delete responded 204")
    public void everyDeleteResponded204() {
        assertThat(deleteResponses).hasSameSizeAs(added.values())
                .allSatisfy(r -> {
                    assertThat(r.statusCode()).isEqualTo(204);
                    assertThat(r.body()).isEmpty();
                });
    }

    @And("each added user is gone")
    public void eachAddedUserIsGone() throws Exception {
        for (CreatedUser user : added.values()) {
            assertGone(user);
        }
    }

    @Then("the {ordinal} added user is gone")
    public void theNthAddedUserIsGone(int position) throws Exception {
        assertGone(nthAdded(position));
    }

    @And("the other added users can still be retrieved unchanged")
    public void theOtherUsersAreUnchanged() throws Exception {
        for (CreatedUser user : remaining()) {
            assertRetrievable(user);
        }
    }

    @Then("the API responds with status {int}")
    public void theApiRespondsWithStatus(int status) {
        assertThat(response.statusCode()).isEqualTo(status);
    }

    @And("the database still holds only the added users")
    public void theDatabaseHoldsOnlyTheAddedUsers() {
        assertThat(userRepo.findAll()).extracting(User::getId)
                .containsExactlyInAnyOrderElementsOf(added.keySet());
    }

    // --- Helpers ---

    private void addUser(String firstName, String lastName, Integer claimedId) throws Exception {
        HttpResponse<String> post = send("POST", "/api/addUser", json(firstName, lastName, claimedId), true);
        addResponses.add(post);
        if (post.statusCode() == 201) {
            JsonNode body = objectMapper.readTree(post.body());
            int id = body.get("id").asInt();
            assertThat(added).as("server returned an id that was already in use").doesNotContainKey(id);
            added.put(id, new CreatedUser(id, firstName, lastName,
                    timestamp(body, "created_at"), timestamp(body, "updated_at")));
        }
    }

    private void assertRetrievable(CreatedUser user) throws Exception {
        HttpResponse<String> get = send("GET", "/api/getUser/" + user.id, null, true);
        assertThat(get.statusCode()).as("GET /api/getUser/%d", user.id).isEqualTo(200);
        JsonNode body = objectMapper.readTree(get.body());
        assertThat(body.get("id").asInt()).isEqualTo(user.id);
        assertThat(body.get("firstName").asText()).isEqualTo(user.firstName);
        assertThat(body.get("lastName").asText()).isEqualTo(user.lastName);
    }

    private void assertGone(CreatedUser user) throws Exception {
        assertThat(send("GET", "/api/getUser/" + user.id, null, true).statusCode()).isEqualTo(404);
        assertThat(userRepo.findById(user.id)).isEmpty();
    }

    private void assertListContainsExactly(List<CreatedUser> expected) throws Exception {
        Map<Integer, String> listed = new LinkedHashMap<>();
        for (JsonNode u : list()) {
            listed.put(u.get("id").asInt(), u.get("firstName").asText() + "|" + u.get("lastName").asText());
        }
        Map<Integer, String> wanted = new LinkedHashMap<>();
        expected.forEach(u -> wanted.put(u.id, u.firstName + "|" + u.lastName));
        assertThat(listed).containsExactlyInAnyOrderEntriesOf(wanted);
    }

    private JsonNode list() throws Exception {
        HttpResponse<String> get = send("GET", "/api/getUsers", null, true);
        assertThat(get.statusCode()).isEqualTo(200);
        return objectMapper.readTree(get.body());
    }

    private JsonNode get(int id) throws Exception {
        HttpResponse<String> get = send("GET", "/api/getUser/" + id, null, true);
        assertThat(get.statusCode()).isEqualTo(200);
        return objectMapper.readTree(get.body());
    }

    private List<CreatedUser> remaining() {
        return added.values().stream().filter(u -> !deletedIds.contains(u.id)).toList();
    }

    private CreatedUser nthAdded(int position) {
        return added.values().stream().skip(position - 1L).findFirst()
                .orElseThrow(() -> new AssertionError("Only " + added.size() + " users were added"));
    }

    private HttpResponse<String> send(String method, String path, String body, boolean authenticated) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (authenticated) {
            String credentials = E2ESpringConfiguration.USERNAME + ":" + E2ESpringConfiguration.PASSWORD;
            request.header("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String json(String firstName, String lastName, Integer id) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (id != null) {
            body.put("id", id);
        }
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        return objectMapper.writeValueAsString(body);
    }

    private static OffsetDateTime timestamp(JsonNode user, String field) {
        return OffsetDateTime.parse(user.get(field).asText());
    }

    private String randomFirstName() {
        return FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
    }

    private String randomLastName() {
        return LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
    }

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        sb.append(RANDOM_CHARS.charAt(random.nextInt(52)));  // start with a letter, never blank
        while (sb.length() < length) {
            sb.append(RANDOM_CHARS.charAt(random.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }
}
