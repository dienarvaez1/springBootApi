Feature: User API over HTTP
  Every /api user endpoint exercised through Spring MVC: security, JSON binding,
  validation and status codes. The repository is mocked.

  Background:
    Given the API has these stored users:
      | id | firstName | lastName |
      | 1  | Charles   | Darwin   |
      | 2  | Ada       | Lovelace |

  # --- Security (all endpoints) ---

  Scenario Outline: <method> <path> requires credentials
    When an anonymous client sends <method> "<path>" with body '<body>'
    Then the API responds with status 401
    And the response asks for HTTP Basic credentials

    Examples:
      | method | path         | body                                 |
      | GET    | /api/users   |                                      |
      | GET    | /api/users/1 |                                      |
      | POST   | /api/users   | {"firstName":"Grace","lastName":"H"} |
      | PUT    | /api/users/1 | {"firstName":"Grace","lastName":"H"} |
      | DELETE | /api/users/1 |                                      |

  Scenario Outline: <method> <path> rejects a wrong password
    When a client with a wrong password sends <method> "<path>" with body '<body>'
    Then the API responds with status 401
    And the repository was not touched

    Examples:
      | method | path         | body                                 |
      | GET    | /api/users   |                                      |
      | GET    | /api/users/1 |                                      |
      | POST   | /api/users   | {"firstName":"Grace","lastName":"H"} |
      | PUT    | /api/users/1 | {"firstName":"Grace","lastName":"H"} |
      | DELETE | /api/users/1 |                                      |

  # --- GET /api/users ---

  Scenario: Listing users returns every user as JSON
    When an authenticated client sends GET "/api/users" with body ''
    Then the API responds with status 200
    And the response is JSON
    And the response is a list of 2 users
    And every user in the response has exactly the fields "id,firstName,lastName,createdAt,updatedAt"
    And the repository was asked for users sorted by "id" descending

  Scenario: Listing users when there are none
    Given the API has no stored users
    When an authenticated client sends GET "/api/users" with body ''
    Then the API responds with status 200
    And the response is a list of 0 users

  # --- GET /api/users/{id} ---

  Scenario: Fetching an existing user
    When an authenticated client sends GET "/api/users/2" with body ''
    Then the API responds with status 200
    And the response is JSON
    And the response user has id 2, first name "Ada" and last name "Lovelace"

  Scenario: Fetching a missing user
    When an authenticated client sends GET "/api/users/99" with body ''
    Then the API responds with status 404

  Scenario: Fetching with a non-numeric id
    When an authenticated client sends GET "/api/users/abc" with body ''
    Then the API responds with status 400
    And the repository was not touched

  # --- POST /api/users ---

  Scenario: Creating a user
    When an authenticated client sends POST "/api/users" with body '{"firstName":"Grace","lastName":"Hopper"}'
    Then the API responds with status 201
    And the response is JSON
    And the response user has id 3, first name "Grace" and last name "Hopper"
    And the repository saved a user with no id, first name "Grace" and last name "Hopper"

  Scenario: Client-supplied id and timestamps are ignored on create
    When an authenticated client sends POST "/api/users" with body '{"id":1,"firstName":"Grace","lastName":"Hopper","createdAt":"2000-01-01T00:00:00Z"}'
    Then the API responds with status 201
    And the repository saved a user with no id, first name "Grace" and last name "Hopper"
    And the saved user has no timestamps set

  Scenario Outline: Creating a user with an invalid body is rejected
    When an authenticated client sends POST "/api/users" with body '<body>'
    Then the API responds with status 400
    And nothing was saved to the repository

    Examples:
      | body                                  |
      | {"firstName":"","lastName":"Hopper"}  |
      | {"firstName":"Grace","lastName":"  "} |
      | {"lastName":"Hopper"}                 |
      | {"firstName":"Grace"}                 |
      | {}                                    |
      | {bad json                             |

  Scenario: Creating a user with a name longer than 255 characters is rejected
    When an authenticated client creates a user with a first name of 256 characters
    Then the API responds with status 400
    And nothing was saved to the repository

  Scenario: Creating a user with a non-JSON content type is rejected
    When an authenticated client sends POST "/api/users" as plain text
    Then the API responds with status 415
    And nothing was saved to the repository

  # --- PUT /api/users/{id} ---

  Scenario: Updating an existing user
    When an authenticated client sends PUT "/api/users/2" with body '{"firstName":"Ada","lastName":"King"}'
    Then the API responds with status 200
    And the response is JSON
    And the response user has id 2, first name "Ada" and last name "King"
    And the repository saved user 2 with first name "Ada" and last name "King"

  Scenario: Client-supplied id in the body cannot redirect an update
    When an authenticated client sends PUT "/api/users/2" with body '{"id":1,"firstName":"Ada","lastName":"King"}'
    Then the API responds with status 200
    And the repository saved user 2 with first name "Ada" and last name "King"

  Scenario: Updating a missing user
    When an authenticated client sends PUT "/api/users/99" with body '{"firstName":"No","lastName":"One"}'
    Then the API responds with status 404
    And nothing was saved to the repository

  Scenario Outline: Updating a user with an invalid body is rejected
    When an authenticated client sends PUT "/api/users/2" with body '<body>'
    Then the API responds with status 400
    And nothing was saved to the repository

    Examples:
      | body                               |
      | {"firstName":"","lastName":"King"} |
      | {"firstName":"Ada"}                |
      | {bad json                          |

  # --- DELETE /api/users/{id} ---

  Scenario: Deleting an existing user
    When an authenticated client sends DELETE "/api/users/1" with body ''
    Then the API responds with status 204
    And the response body is empty
    And the repository deleted user 1

  Scenario: Deleting a missing user
    When an authenticated client sends DELETE "/api/users/99" with body ''
    Then the API responds with status 404
    And nothing was deleted from the repository

  # --- Cross-site request forgery (why CSRF protection can stay disabled, see SecurityConfig) ---

  Scenario Outline: Cross-site requests cannot change data: form-style bodies are rejected
    # A browser with cached Basic credentials can send these content types cross-site without a preflight
    When an authenticated client sends POST "<path>" with content type "<contentType>" and body '<body>'
    Then the API responds with status 415
    And nothing was saved to the repository

    Examples:
      | path       | contentType                       | body                             |
      | /api/users | application/x-www-form-urlencoded | firstName=Evil&lastName=Site     |
      | /api/users | multipart/form-data               | firstName=Evil                   |
      | /api/users | text/plain                        | {"firstName":"E","lastName":"S"} |

  Scenario Outline: Cross-site requests cannot change data: CORS preflights are not approved
    When a page on "https://evil.example" asks to send <method> "<path>" with headers "<headers>"
    Then the response does not allow cross-origin access

    Examples:
      | method | path         | headers      |
      | POST   | /api/users   | content-type |
      | PUT    | /api/users/1 | content-type |
      | DELETE | /api/users/1 |              |
