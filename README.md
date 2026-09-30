# springBootApi

[![CI](https://github.com/dienarvaez1/springBootApi/actions/workflows/ci.yml/badge.svg)](https://github.com/dienarvaez1/springBootApi/actions/workflows/ci.yml)

A sample Spring Boot REST API for managing users, backed by PostgreSQL and protected with HTTP Basic auth.

## Requirements

- JDK 21 or newer (the build targets Java 21)
- PostgreSQL on `localhost:5432` with a `springboot_app` database
- Docker, only for the optional local SonarQube

## Setup

Copy `.env.example` to `.env` and fill it in. `.env` is git-ignored and read by the app at startup.

| Variable       | Used for                               |
|----------------|----------------------------------------|
| `DB_USERNAME`  | Postgres user                          |
| `DB_PASSWORD`  | Postgres password (may be empty)       |
| `API_PASSWORD` | Password for the API user `api_user`   |
| `SONAR_TOKEN`  | Token for the local SonarQube analysis |

## Running

```sh
./mvnw spring-boot:run
```

The app listens on http://localhost:8088. `spring-boot:run` activates the `dev` profile
(`application-dev.yaml`): Hibernate creates and updates the schema, and SQL and security debug
logging are on. Without that profile, Hibernate only validates that the schema matches.

## API

The full, always-current spec is generated from the code (springdoc) and served by the running app:

- Swagger UI: http://localhost:8088/api/swagger-ui
- OpenAPI 3.1 JSON: http://localhost:8088/api/api-docs

Both need the same credentials as the API; the browser asks for them.

### Authentication

Every endpoint requires HTTP Basic auth as `api_user` with the password from `API_PASSWORD`.
Only `/actuator/health` is public. Missing or wrong credentials get `401`.

### Endpoints

| Method   | Path              | Description                  | Success | Errors             |
|----------|-------------------|------------------------------|---------|--------------------|
| `GET`    | `/api/users`      | List all users, newest first | 200     | 401                |
| `GET`    | `/api/users/{id}` | Get a user                   | 200     | 400, 401, 404      |
| `POST`   | `/api/users`      | Create a user                | 201     | 400, 401, 415      |
| `PUT`    | `/api/users/{id}` | Replace a user's names       | 200     | 400, 401, 404, 415 |
| `DELETE` | `/api/users/{id}` | Delete a user                | 204     | 400, 401, 404      |

- `400`: the id is not a number, a name is blank, missing or over 255 characters, or the JSON is malformed
- `404`: no user has that id
- `415`: the body is not `application/json`

### Request body

`POST` and `PUT` take JSON with both names. Each must contain a non-whitespace character and be
at most 255 characters. Any other fields, such as `id` or `createdAt`, are ignored.

```json
{"firstName": "Grace", "lastName": "Hopper"}
```

### Responses

A user:

```json
{
  "id": 1,
  "firstName": "Grace",
  "lastName": "Hopper",
  "createdAt": "2026-09-30T05:39:19.444Z",
  "updatedAt": "2026-09-30T05:39:19.444Z"
}
```

`id`, `createdAt` and `updatedAt` are set by the server; timestamps are UTC (ISO 8601).
`GET /api/users` returns an array of these, and `DELETE` returns an empty body.

Errors use Spring Boot's standard body, which does not say which field was invalid:

```json
{"timestamp": "2026-09-30T05:39:19.444+00:00", "status": 404, "error": "Not Found", "path": "/api/users/99"}
```

### Example

```sh
set -a; source .env; set +a
curl -u "api_user:$API_PASSWORD" -H 'Content-Type: application/json' \
     -d '{"firstName":"Grace","lastName":"Hopper"}' http://localhost:8088/api/users
curl -u "api_user:$API_PASSWORD" http://localhost:8088/api/users/1
```

## Tests

| Command               | What runs                                                                              |
|-----------------------|----------------------------------------------------------------------------------------|
| `./mvnw test`         | Cucumber scenarios against the web layer with a mocked repository. No database needed. |
| `./mvnw verify`       | The above, plus end-to-end Cucumber scenarios against a real Postgres (see below).     |
| `./mvnw gatling:test` | Read-only load test against an already running app (see below).                        |

**End-to-end tests** start the whole app on a random port and use a separate
`springboot_app_e2e` database, which they empty before each scenario. The database name must end
in `_e2e`. Override the connection with `E2E_DB_URL`, `E2E_DB_USERNAME` and `E2E_DB_PASSWORD`.
Replay a randomized run with `-De2e.seed=<seed>` (each scenario logs its seed).

**Load tests** need the app running and the API password in the environment:

```sh
./mvnw spring-boot:run
set -a; source .env; set +a; ./mvnw gatling:test
```

Tune with `USERS` (default 200), `RAMP_SECONDS` (default 30) and `BASE_URL`.

GitHub Actions runs `./mvnw verify` on every push to `main` and every pull request, with a
Postgres service container for the end-to-end suite (`.github/workflows/ci.yml`). The reports
are attached to each run as the `test-reports` artifact.

Reports: Cucumber in `target/site/cucumber-*.html`, coverage in `target/site/jacoco/`,
Gatling in `target/gatling/`.

## Code quality

Run SonarQube locally with Docker:

```sh
docker compose -f docker-compose.sonar.yml up -d     # UI at http://localhost:9000
set -a; source .env; set +a
./mvnw verify sonar:sonar -Dsonar.token=$SONAR_TOKEN
```
