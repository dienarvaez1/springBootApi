package org.example.cucumber;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.example.controller.UserController;
import org.example.controller.UserRequest;
import org.example.controller.UserResponse;
import org.example.entities.User;
import org.example.repo.UserRepo;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserControllerSteps {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private UserRepo userRepo;
    private UserController controller;
    private final List<User> storedUsers = new ArrayList<>();

    private Object result;
    private ResponseStatusException failure;
    private Set<ConstraintViolation<UserRequest>> violations;

    @Before
    public void setUp() {
        userRepo = mock(UserRepo.class);
        controller = new UserController(userRepo);
        UserRepoStubs.backWith(userRepo, storedUsers);
    }

    @Given("the repository contains the following users:")
    public void theRepositoryContains(DataTable table) {
        storedUsers.addAll(UserRepoStubs.usersFrom(table));
    }

    @When("I request all users")
    public void iRequestAllUsers() {
        call(() -> controller.getUsers());
    }

    @When("I request the user with id {int}")
    public void iRequestTheUserWithId(int id) {
        call(() -> controller.getUser(id));
    }

    @When("I create a user {string} {string}")
    public void iCreateAUser(String firstName, String lastName) {
        call(() -> controller.createUser(new UserRequest(firstName, lastName)));
    }

    @When("I update user {int} to {string} {string}")
    public void iUpdateUser(int id, String firstName, String lastName) {
        call(() -> controller.updateUser(id, new UserRequest(firstName, lastName)));
    }

    @When("I delete user {int}")
    public void iDeleteUser(int id) {
        call(() -> controller.deleteUser(id));
    }

    @When("I validate a user request {string} {string}")
    public void iValidateAUserRequest(String firstName, String lastName) {
        violations = VALIDATOR.validate(new UserRequest(firstName, lastName));
    }

    @Then("I receive {int} users")
    public void iReceiveUsers(int count) {
        assertThat((List<?>) result).hasSize(count);
    }

    @Then("the returned user is {string} {string}")
    public void theReturnedUserIs(String firstName, String lastName) {
        UserResponse user = result instanceof ResponseEntity<?> response ? (UserResponse) response.getBody() : (UserResponse) result;
        assertThat(user.firstName()).isEqualTo(firstName);
        assertThat(user.lastName()).isEqualTo(lastName);
    }

    @Then("the request fails with status {int}")
    public void theRequestFailsWithStatus(int status) {
        assertThat(failure).as("expected the call to fail").isNotNull();
        assertThat(failure.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(failure).as("unexpected failure").isNull();
        assertThat(((ResponseEntity<?>) result).getStatusCode().value()).isEqualTo(status);
    }

    @And("the repository saved a new user {string} {string}")
    public void theRepositorySavedANewUser(String firstName, String lastName) {
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getFirstName()).isEqualTo(firstName);
        assertThat(saved.getValue().getLastName()).isEqualTo(lastName);
    }

    @And("the repository saved user {int}")
    public void theRepositorySavedUser(int id) {
        verify(userRepo).save(argThat(u -> u.getId() == id));
    }

    @And("nothing was saved")
    public void nothingWasSaved() {
        verify(userRepo, never()).save(any());
    }

    @And("user {int} was deleted from the repository")
    public void userWasDeleted(int id) {
        verify(userRepo).delete(argThat(u -> u.getId() == id));
    }

    @Then("there is a validation error on {string}")
    public void thereIsAValidationErrorOn(String field) {
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsOnly(field);
    }

    private void call(Supplier<Object> action) {
        try {
            result = action.get();
        } catch (ResponseStatusException e) {
            failure = e;
        }
    }
}
