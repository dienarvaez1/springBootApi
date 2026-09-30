package org.example.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.example.entities.User;
import org.example.repo.UserRepo;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

public class UserApiSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<User> storedUsers = new ArrayList<>();
    private MvcResult response;

    @Given("the API has these stored users:")
    public void theApiHasTheseStoredUsers(DataTable table) {
        storedUsers.addAll(UserRepoStubs.usersFrom(table));
        UserRepoStubs.backWith(userRepo, storedUsers);
    }

    @Given("the API has no stored users")
    public void theApiHasNoStoredUsers() {
        storedUsers.clear();
    }

    @When("an anonymous client sends {word} {string} with body {string}")
    public void anAnonymousClientSends(String method, String path, String body) throws Exception {
        perform(method, path, body, null);
    }

    @When("a client with a wrong password sends {word} {string} with body {string}")
    public void aClientWithAWrongPasswordSends(String method, String path, String body) throws Exception {
        perform(method, path, body, "not-the-password");
    }

    @When("an authenticated client sends {word} {string} with body {string}")
    public void anAuthenticatedClientSends(String method, String path, String body) throws Exception {
        perform(method, path, body, CucumberSpringConfiguration.PASSWORD);
    }

    @When("an authenticated client creates a user with a first name of {int} characters")
    public void createsAUserWithALongFirstName(int length) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("firstName", "a".repeat(length), "lastName", "Hopper"));
        perform("POST", "/api/users", body, CucumberSpringConfiguration.PASSWORD);
    }

    @When("an authenticated client sends POST {string} as plain text")
    public void sendsPostAsPlainText(String path) throws Exception {
        sendsPostWithContentType(path, MediaType.TEXT_PLAIN_VALUE, "firstName=Grace");
    }

    @When("an authenticated client sends POST {string} with content type {string} and body {string}")
    public void sendsPostWithContentType(String path, String contentType, String body) throws Exception {
        response = mockMvc.perform(request(HttpMethod.POST, path)
                        .with(httpBasic(CucumberSpringConfiguration.USERNAME, CucumberSpringConfiguration.PASSWORD))
                        .contentType(contentType)
                        .content(body))
                .andReturn();
    }

    @When("a page on {string} asks to send {word} {string} with headers {string}")
    public void aCrossOriginPreflight(String origin, String method, String path, String headers) throws Exception {
        MockHttpServletRequestBuilder builder = request(HttpMethod.OPTIONS, path)
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method);
        if (!headers.isEmpty()) {
            builder.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, headers);
        }
        response = mockMvc.perform(builder).andReturn();
    }

    @Then("the API responds with status {int}")
    public void theApiRespondsWithStatus(int status) {
        assertThat(response.getResponse().getStatus()).isEqualTo(status);
    }

    @And("the response asks for HTTP Basic credentials")
    public void theResponseAsksForBasicCredentials() {
        assertThat(response.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Basic");
    }

    @And("the response is JSON")
    public void theResponseIsJson() {
        assertThat(MediaType.parseMediaType(response.getResponse().getContentType()))
                .satisfies(type -> assertThat(type.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue());
    }

    @And("the response is a list of {int} users")
    public void theResponseIsAListOfUsers(int count) throws Exception {
        JsonNode body = body();
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(count);
    }

    @And("every user in the response has exactly the fields {string}")
    public void everyUserHasExactlyTheFields(String fields) throws Exception {
        Set<String> expected = new TreeSet<>(List.of(fields.split(",")));
        for (JsonNode user : body()) {
            Set<String> actual = new TreeSet<>();
            user.fieldNames().forEachRemaining(actual::add);
            assertThat(actual).isEqualTo(expected);
        }
    }

    @And("the response user has id {int}, first name {string} and last name {string}")
    public void theResponseUserHas(int id, String firstName, String lastName) throws Exception {
        JsonNode user = body();
        assertThat(user.get("id").asInt()).isEqualTo(id);
        assertThat(user.get("firstName").asText()).isEqualTo(firstName);
        assertThat(user.get("lastName").asText()).isEqualTo(lastName);
    }

    @Then("the response does not allow cross-origin access")
    public void theResponseDoesNotAllowCrossOriginAccess() {
        assertThat(response.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(response.getResponse().getStatus()).isNotEqualTo(200);
    }

    @And("the response body is empty")
    public void theResponseBodyIsEmpty() throws Exception {
        assertThat(response.getResponse().getContentAsString()).isEmpty();
    }

    @And("the repository was asked for users sorted by {string} descending")
    public void theRepositoryWasAskedForSortedUsers(String property) {
        verify(userRepo).findAll(Sort.by(Sort.Direction.DESC, property));
    }

    @And("the repository saved a user with no id, first name {string} and last name {string}")
    public void theRepositorySavedANewUser(String firstName, String lastName) {
        User saved = capturedSave();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getFirstName()).isEqualTo(firstName);
        assertThat(saved.getLastName()).isEqualTo(lastName);
    }

    @And("the saved user has no timestamps set")
    public void theSavedUserHasNoTimestamps() {
        User saved = capturedSave();
        assertThat(saved.getCreatedAt()).isNull();
        assertThat(saved.getUpdatedAt()).isNull();
    }

    @And("the repository saved user {int} with first name {string} and last name {string}")
    public void theRepositorySavedUser(int id, String firstName, String lastName) {
        User saved = capturedSave();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getFirstName()).isEqualTo(firstName);
        assertThat(saved.getLastName()).isEqualTo(lastName);
    }

    @And("the repository deleted user {int}")
    public void theRepositoryDeletedUser(int id) {
        verify(userRepo).delete(argThat(u -> u.getId() == id));
    }

    @And("nothing was saved to the repository")
    public void nothingWasSaved() {
        verify(userRepo, never()).save(any());
    }

    @And("nothing was deleted from the repository")
    public void nothingWasDeleted() {
        verify(userRepo, never()).delete(any());
    }

    @And("the repository was not touched")
    public void theRepositoryWasNotTouched() {
        verifyNoInteractions(userRepo);
    }

    private void perform(String method, String path, String body, String password) throws Exception {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path);
        if (password != null) {
            builder.with(httpBasic(CucumberSpringConfiguration.USERNAME, password));
        }
        if (!body.isEmpty()) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        response = mockMvc.perform(builder).andReturn();
    }

    private JsonNode body() throws Exception {
        return objectMapper.readTree(response.getResponse().getContentAsString());
    }

    private User capturedSave() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        return captor.getValue();
    }
}
