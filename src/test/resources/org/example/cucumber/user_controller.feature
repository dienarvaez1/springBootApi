Feature: User controller
  Unit-level behaviour of UserController, with the repository mocked out.

  Background:
    Given the repository contains the following users:
      | id | firstName | lastName |
      | 1  | Charles   | Darwin   |
      | 2  | Ada       | Lovelace |

  Scenario: Listing users returns all of them
    When I request all users
    Then I receive 2 users

  Scenario: Fetching an existing user
    When I request the user with id 1
    Then the returned user is "Charles" "Darwin"

  Scenario: Fetching a missing user is a 404
    When I request the user with id 99
    Then the request fails with status 404

  Scenario: Creating a user saves only the supplied names
    When I create a user "Grace" "Hopper"
    Then the response status is 201
    And the repository saved a new user "Grace" "Hopper"

  Scenario: Updating an existing user changes its names
    When I update user 2 to "Ada" "King"
    Then the returned user is "Ada" "King"
    And the repository saved user 2

  Scenario: Updating a missing user is a 404
    When I update user 99 to "Nobody" "Here"
    Then the request fails with status 404
    And nothing was saved

  Scenario: Deleting an existing user
    When I delete user 1
    Then the response status is 204
    And user 1 was deleted from the repository

  Scenario: Deleting a missing user is a 404
    When I delete user 99
    Then the request fails with status 404

  Scenario Outline: Blank names are rejected by validation
    When I validate a user request "<firstName>" "<lastName>"
    Then there is a validation error on "<field>"

    Examples:
      | firstName | lastName | field     |
      |           | Hopper   | firstName |
      | Grace     |          | lastName  |
