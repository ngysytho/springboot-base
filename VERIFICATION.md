# Current refactor status — 3 November 2026

**Not executed:** Maven compilation, tests, packaging, Docker commands, application startup and smoke tests. The user explicitly requested source changes without running checks.

The existing `target/` JAR/classes and Surefire reports, if present, are pre-refactor artifacts and must not be treated as verification of current source. No current pass count or compilation-success claim is made. `REFACTOR_REPORT.md` describes the changes and commands to run later.

---

## Historical report (before this refactor)

# Initialization verification

Verified on 3 November 2026 (Asia/Ho_Chi_Minh).

## Build and tests

- Runtime: Azul Java **21.0.13**, Maven Wrapper **3.9.11**.
- Spring Boot **4.1.2**, springdoc **3.2.0** resolved from Maven Central.
- `./mvnw clean verify` succeeded during implementation; final `./mvnw -B verify` succeeded after the membership-port cleanup and missing-audience validation fix.
- **35 tests, zero failures, zero errors, zero skipped.**

| Suite | Tests |
|---|---:|
| BaseServiceTest | 5 |
| CoreQueryTest | 17 |
| JwtValidationTest | 1 |
| SecurityHttpTest | 2 |
| SecurityUnitTest | 2 |
| LessonIntegrationTest | 8 |

The JWT test checks real RSA signatures, wrong and missing audiences, issuer and expiration. PostgreSQL integration tests ran with Testcontainers against `postgres:18` (resolved server version 18.6), using actual Flyway migrations and Hibernate schema validation. They cover CRUD/publish, every filter operator, pagination, membership permission checks, cross-tenant reads and mutations, and optimistic locking through both HTTP version checks and independent EntityManagers.

Test XML/text reports are under `target/surefire-reports/`. The executable artifact is `target/mpl-one-0.0.1-SNAPSHOT.jar`.

## Local environment smoke test

- `docker compose --env-file .env.example --profile api config --quiet`: passed.
- Started separate application and Keycloak PostgreSQL services plus **Keycloak 26.8.1**, using isolated Compose project `mpl-one-verification`.
- Realm import completed successfully and the OIDC discovery endpoint returned HTTP 200.
- Started the packaged Java 21 application with the local profile against Compose PostgreSQL.
- `python3 scripts/smoke_local.py`: **passed** real authorization code + PKCE S256, token exchange, JWT verification, `/me`, Lesson creation/update/publishing/filtering/deletion. No tokens were printed or persisted.
- `/actuator/health`: **UP**.
- `/v3/api-docs`: HTTP 200, four API paths, bearer and Keycloak authorization-code security schemes.
- The smoke test removed its own Lesson. The temporary API process and isolated verification containers/volumes were removed after verification. Docker Desktop remains available.

## Scope and limitations

- The application Dockerfile is supplied but its image was not built during this verification; the packaged executable JAR was run directly with Compose infrastructure.
- Java 21 was downloaded into a temporary directory for this session. Install/configure a durable Java 21 JDK for host development, or use the Docker application profile.
- AI, device pairing/authorization, interactive classroom transports, workflow execution, and organization administration are deliberate interfaces for future implementation, as requested. They do not return fake results.
- The provided Compose setup and realm credentials are local development configuration. No production deployment was performed.
