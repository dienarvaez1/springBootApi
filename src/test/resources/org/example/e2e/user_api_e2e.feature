Feature: User API end to end
  The running application over real HTTP, backed by a real Postgres database.
  Users are generated randomly; each scenario logs its seed so a failure can be replayed
  with: ./mvnw verify -De2e.seed=<seed>

  Background:
    Given the user table is empty

  Scenario: Random users can each be retrieved by the id the API returned
    When I add 10 random users
    Then every add responded 201 with a distinct server-assigned id
    And each added user can be retrieved by its id with the names it was created with
    And each added user has server-set createdAt and updatedAt timestamps
    And every added user is stored in the database

  Scenario: Listing returns exactly the added users, newest first
    When I add 8 random users
    Then listing all users returns exactly the added users
    And the listed users are ordered by id, newest first

  Scenario: Full lifecycle of random users
    When I add 5 random users
    And I rename each added user to new random names
    Then every rename responded 200 with the new names
    And each added user can be retrieved by its id with its new names
    And each renamed user's updatedAt moved forward while createdAt stayed the same
    When I delete each added user
    Then every delete responded 204
    And each added user is gone
    And listing all users returns no users

  Scenario: Deleting one user leaves the others untouched
    When I add 4 random users
    And I delete the 2nd added user
    Then the 2nd added user is gone
    And the other added users can still be retrieved unchanged
    And listing all users returns exactly the remaining added users

  Scenario: Names at the 255-character limit and with non-ASCII characters round-trip intact
    When I add a user with first name of 255 random characters and last name "Ñúñez-O'Brien 李"
    Then every add responded 201 with a distinct server-assigned id
    And each added user can be retrieved by its id with the names it was created with

  Scenario: A client-supplied id cannot overwrite an existing user
    When I add 1 random users
    And I add a random user whose request body claims the 1st added user's id
    Then every add responded 201 with a distinct server-assigned id
    And each added user can be retrieved by its id with the names it was created with

  Scenario Outline: Invalid input is rejected and nothing is stored
    Given I add 2 random users
    When I send POST "/api/users" with body '<body>'
    Then the API responds with status 400
    And the database still holds only the added users

    Examples:
      | body                              |
      | {"firstName":"","lastName":"Doe"} |
      | {"firstName":"Jane"}              |
      | {bad json                         |

  Scenario Outline: Ids that do not exist return 404 on every endpoint
    Given I add 2 random users
    When I send <method> "<path>" with body '<body>'
    Then the API responds with status 404
    And the database still holds only the added users

    Examples:
      | method | path              | body                                |
      | GET    | /api/users/999999 |                                     |
      | PUT    | /api/users/999999 | {"firstName":"No","lastName":"One"} |
      | DELETE | /api/users/999999 |                                     |

  Scenario Outline: Every endpoint requires credentials
    Given I add 1 random users
    When an anonymous client sends <method> "<path>" with body '<body>'
    Then the API responds with status 401
    And the database still holds only the added users

    Examples:
      | method | path         | body                                |
      | GET    | /api/users   |                                     |
      | GET    | /api/users/1 |                                     |
      | POST   | /api/users   | {"firstName":"Anon","lastName":"Y"} |
      | PUT    | /api/users/1 | {"firstName":"Anon","lastName":"Y"} |
      | DELETE | /api/users/1 |                                     |
