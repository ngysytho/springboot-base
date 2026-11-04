# MPL One AI Smart TV backend

Java 21 / Spring Boot 4.1.2 modular monolith in package `vn.mpl.one`. The refactored Lesson module demonstrates a reusable JPA core, validated DTOs, tenant authorization, Keycloak OIDC session authentication, filtering and optimistic locking. Other business areas expose extension interfaces only.

## Architecture and reference inspection

The reference [core-nestjs-typeorm](https://github.com/ngysytho/core-nestjs-typeorm) was inspected at commit `8c2896a2061da44c6a37328b1dd507a6f0185226` before implementation, including `core/services/base.service.ts`, `entity-relation.service.ts`, query decorators, helpers, response classes and the validation pipe. No standalone reusable repository abstraction was present; its base service wraps TypeORM Repository.

| Reference capability | Java adaptation |
|---|---|
| BaseService query, count, exists, bulk persistence | `BaseService<T>` composes Spring Data repository/specification APIs |
| Query parameter decorators | Explicit HTTP parameters, `FilterParser`, `PaginationRequest` |
| TypeORM where/order helpers | Typed Criteria API predicates and whitelisted Spring Data Sort |
| findOneOrFail | Domain not-found exception mapped to 404 |
| Response wrappers / validation pipe | Records, Jakarta validation, controller advice |
| Arbitrary aggregate field / relation include | Closed domain metric extension (`ScopedAggregate`); no public relation loading |

The reference accepts nested paths, has its sort whitelist commented out, and permits an unlimited page mode. Those behaviors are intentionally replaced with explicit field whitelists, no relationship paths, and a 100-row limit. Multiple predicates are combined with AND, including repeated predicates on the same field. No original password/JWT helpers are copied; Keycloak owns identity.

```text
springboot-base/
├── pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/
├── Dockerfile, docker-compose.yml, .env.example
├── src/main/java/vn/mpl/one/
│   ├── MplOneApplication.java
│   ├── config/                  Security, JPA, CORS and OpenAPI
│   ├── core/
│   │   ├── entity/              UUID, timestamps, version
│   │   ├── repository/          BaseRepository, ScopedAggregate
│   │   ├── service/             BaseService
│   │   ├── filter/, specification/, pagination/
│   │   ├── response/, exception/, security/, tenant/, audit/, validation/, util/
│   └── modules/
│       ├── identity/            User profile and /me
│       ├── organization/        Organizations, memberships, ports
│       ├── lesson/              DTO/controller/mapper → service/usecase → domain repository/model → JPA adapter
│       ├── device/              Registration, pairing, authorization, status ports
│       ├── classroom/           Session, participants, events ports
│       ├── workflow/            Centre Care, Shift Hub, Family Circle state port
│       └── ai/                  Generation, provider and usage ports
├── src/main/resources/
│   ├── application.yml, application-local.yml
│   ├── db/migration/V1__initial_schema.sql
│   ├── db/local/V2__local_demo_data.sql
│   └── keycloak/mpl-one-realm.json
└── src/test/java/vn/mpl/one/   Unit, HTTP security, JWT, PostgreSQL integration tests
```

Controllers depend on application services; application services depend on domain repository contracts. Infrastructure implements those contracts. `Lesson` is an immutable domain record without Hibernate annotations; `LessonJpaEntity` owns persistence mapping. Only the persistence adapter extends the protected, abstract `BaseService`. API mapping produces response DTOs without exposing entities. Tenant context is immutable and explicit, not a thread-local. The core depends on `TenantMembershipLookup`; the organization module supplies its JPA adapter, keeping the core independent of business modules. Every Lesson operation checks database membership and composes organization scope separately from user filters. PLATFORM_ADMIN has no implicit membership bypass. Mutations require both a TEACHER or ORG_ADMIN application role and a TEACHER or ORG_ADMIN membership. Reads require an allowed human realm role and active membership; DEVICE tokens cannot access these user APIs. This initial policy allows members to read all Lesson statuses.

## Versions and requirements

- JDK **21**, enforced as `[21,22)` by Maven; Java 25 is rejected.
- Spring Boot **4.1.2**, with its dependency BOM managing Spring, Hibernate, Flyway, PostgreSQL driver, JUnit, Mockito and Testcontainers.
- Maven **3.9.11** downloaded by the Apache Maven Wrapper (internet required initially).
- PostgreSQL **18**, Keycloak **26.8.1**.
- springdoc **3.2.0**, the Boot 4 generation: [official documentation](https://springdoc.org/).
- Docker Engine/Desktop and Compose v2 for infrastructure and Testcontainers. No H2 replacement.
- Keycloak PostgreSQL 18 support and container configuration: [database guide](https://www.keycloak.org/server/db), [container guide](https://www.keycloak.org/server/containers).

Set `JAVA_HOME` to a Java 21 JDK and ensure `$JAVA_HOME/bin` is on PATH. The application image uses Java 21; the official Keycloak image uses its own supported runtime.

## Run locally

From the projects folder:

```sh
cd springboot-base
cp .env.example .env
# Edit .env as desired; these are disposable local credentials.
docker compose up -d app-db keycloak-db keycloak
docker compose run --rm keycloak-config
# Wait until this returns the realm configuration:
curl --fail http://localhost:8180/realms/mpl-one/.well-known/openid-configuration
set -a
. ./.env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The database services have readiness checks. Keycloak configuration is applied explicitly via the one-shot `keycloak-config` service, including updates to existing realms. See [configuration-as-code workflow](KEYCLOAK_CONFIG.md). Repeat the discovery request if Keycloak is still starting. Maven application startup runs Flyway, then Hibernate validates the schema. HikariCP defaults to ten connections, configurable using `DB_POOL_SIZE`. Open-in-view is disabled.

Or build and run the Java application in Docker:

```sh
docker compose up -d app-db keycloak-db keycloak
docker compose run --rm keycloak-config
docker compose --profile api up -d --build api
```

API: `http://localhost:8081`; Swagger: `http://localhost:8081/swagger-ui/index.html`; OpenAPI: `http://localhost:8081/v3/api-docs`; health: `http://localhost:8081/actuator/health`; Keycloak admin: `http://localhost:8180/admin`. PostgreSQL application port defaults to localhost:5433 (set `DB_PORT=5432` if free); Keycloak's database has no host port. Container-to-container JWKS uses `http://keycloak:8080` while issuer validation always uses the externally visible `http://localhost:8180/realms/mpl-one`.

```sh
docker compose logs -f keycloak
# Stop services, retaining database volumes:
docker compose --profile api down
# Explicitly destructive local reset, only when you want to erase demo databases:
docker compose --profile api down -v
```

Realm imports do not overwrite an existing realm. Use the admin console for existing installations or intentionally reset disposable volumes. The `local` profile adds the separate demo migration location; never activate this profile against production. Production databases must start with only `db/migration`, and application user profiles/memberships must be provisioned by an authorized future organization management adapter.

## Authentication and demo users

Browser authentication now uses the confidential `mpl-bff` OIDC client. Open
`/oauth2/authorization/mpl-bff` to sign in. The backend exchanges the authorization
code and stores tokens in the HTTP session; the browser receives an opaque,
HttpOnly session cookie and safe profile data. Bearer headers are not accepted by
the browser API. See [BFF migration and setup](BFF_MIGRATION.md) for Keycloak,
CSRF, frontend integration and production Redis configuration.

| Username | Development password | Role | Organization |
|---|---|---|---|
| teacher | KEYCLOAK_LOCAL_DEMO_PASSWORD | TEACHER | `11111111-1111-1111-1111-111111111111` |
| student | KEYCLOAK_LOCAL_DEMO_PASSWORD | STUDENT (read only) | same |

These accounts are seeded only by the explicit local override described in [KEYCLOAK_CONFIG.md](KEYCLOAK_CONFIG.md); the normal realm sync does not manage users. Passwords are supplied through the local environment. Keycloak admin credentials come exclusively from `.env` and are never exposed by the API. Demo user IDs match `user_profiles.subject` in the local migration. The second organization `22222222-2222-2222-2222-222222222222` has no demo memberships.

## Lesson requests

`X-Organization-Id` selects a tenant; it never grants access. All endpoints require authentication and active membership. Invalid inputs are 400, missing/invalid authentication 401, insufficient permission 403, absent or cross-tenant resources within an authorized tenant 404, stale versions/constraints 409, and unexpected errors a generic 500. Security-filter errors also return structured JSON. Server responses do not disclose database details or stack traces.

Use the same-origin `bffFetch` example in [BFF_MIGRATION.md](BFF_MIGRATION.md).
For example, create a lesson with:

```js
await bffFetch('/api/v1/lessons', {
  method: 'POST',
  headers: {'Content-Type': 'application/json', 'X-Organization-Id': organizationId},
  body: JSON.stringify({title: 'Math foundations', description: 'Fractions and algebra'})
});
```


Use the latest response `version` for each mutation; example versions assume exactly those preceding successful changes. Creation always starts in DRAFT. Publishing moves to PUBLISHED and rejects ARCHIVED entities. ARCHIVED is reserved for a future authorized archive use case; arbitrary status changes are not accepted by update DTOs.

Success envelope: `{success,data,message,timestamp}`. Pagination data: `{count,rows,page,size,totalPages,hasNext,hasPrevious}`. Errors: `{success:false,status,error,message,path,timestamp,errors}`; bean-validation errors map field names to messages.

### Query contract

Filters: `field:operator:value`; repeated `filter` parameters or `&&` separators mean AND. Null operators take only `field:isnull` or `field:notnull`. `in` accepts comma-separated values (not JSON). Values can contain colons, for example ISO timestamps. LIKE is case-insensitive literal substring matching: `%`, `_`, and the escape character are escaped. Supported operators: eq, neq, gt, gte, lt, lte, like, in, isnull, notnull. `is_null`, `is_not_null` and `isnotnull` are accepted aliases. Each field has an explicit operator allowlist: text accepts equality, IN, LIKE and null checks; status accepts equality, IN and null checks; timestamps/version accept ordered comparisons, equality, IN and null checks. Empty values and unsupported types/operators/fields are rejected. At most 20 predicates are accepted.

Filter fields: title, description, status, createdAt, updatedAt, version. Enums use exact names; timestamps use ISO-8601 instants. Sort fields: title, status, createdAt, updatedAt, id. Sort syntax is `field:asc` or `field:desc`, with repeated parameters or `&&` for multiple sorts. A UUID tiebreaker makes ordering stable; default ordering is newest created first. Page numbering begins at 1; size is 1–100. Tenant IDs, JPA paths and relation loading are never accepted in filters.

## Lesson request flow and extension guide

`LessonController → LessonApiMapper → LessonService / CreateLessonUseCase → domain LessonRepository → LessonRepositoryAdapter → LessonJpaRepository → PostgreSQL`.

The controller validates HTTP DTOs and parses query syntax. The API mapper creates application command records and maps domain results into response DTOs. The transactional application service checks session roles and database membership before calling the domain repository. Domain methods enforce title/description rules, version expectations and publish transitions. The adapter independently applies organization scope, validates typed filter/operator policies and maps JPA state to domain records. Updates load the managed row within scope, compare versions, copy content and flush; database `@Version` still detects concurrent writes.

1. Add a module with `api/controller`, `api/dto/{request,response}`, `api/mapper`, `application/{service,usecase}`, `domain/{model,enums,repository}`, `infrastructure/persistence/{entity,repository,mapper}`, and `infrastructure/specification`.
2. Keep domain records and repository interfaces free of Hibernate, MVC and Keycloak dependencies. Application services inject domain contracts, never Spring Data repositories or concrete persistence adapters.
3. Extend `BaseEntity` only in infrastructure. Implement `core.tenant.TenantOwned` for tenant-specific entities. Add a new versioned migration only for real schema changes.
4. Implement repository adapters with explicit organization arguments on every query and mutation. Only those adapters may extend `BaseService`; expose narrow domain operations instead of generic CRUD.
5. Declare `Map<String, FilterField>` and sortable-field policies in the infrastructure specification package. `FilterParser.validate` and `SpecificationBuilder` enforce both field and operator allowlists; typed conversion rejects malformed UUIDs, dates, numbers and enum values. AND composition preserves all predicates.
6. Use `PaginationRequest` and `PageResponse` as framework-free values. `PaginationUtils` converts to/from Spring Data types inside infrastructure. Map domain rows to DTOs in the API mapper.
7. Put `@Transactional` on public adapter/application write methods. Protected base helpers are internal calls, so their annotations alone are not a substitute for the public transaction boundary. `saveAll` and bulk helpers flush normal entity operations and rely on that single transaction to roll back every row on failure.

`BaseService` is abstract and offers protected findById, findByCriteria, findOneByCriteria, findOneOrFailByCriteria, paginate, exists, count, save, saveAll, delete and deleteById, plus internal create/update/bulk conveniences. Reads require an explicit specification. Tenant writes additionally require an organization ID matching the entity. Updates accept only scoped managed entities, preserving lifecycle callbacks and optimistic locking. There are no public unscoped CRUD operations on domain services.

Custom specification example, inside infrastructure:

```java
Specification<LessonJpaEntity> published = (root, query, cb) ->
    cb.equal(root.get("status"), LessonStatus.PUBLISHED);
Specification<LessonJpaEntity> authorized = LessonSpecifications.tenant(organizationId).and(published);
```

For aggregation, implement `ScopedAggregate<Entity,DomainMetric,Result>` with a closed metric enum, authorized scope and Criteria API expressions. Never accept raw SQL or property paths from users.

Auditing is enabled in `AuditConfig`, while `JpaConfig` enables transaction management. BaseEntity retains UUIDs, Instant timestamps and optimistic locking. Optional createdBy/updatedBy columns were not added because existing requirements do not define actor retention; security audit events remain separate from entity timestamps.

`CorsConfig` allows only explicit `CORS_ALLOWED_ORIGINS`. The local profile defaults to `http://localhost:3000`; other profiles default to no cross-origin access. Credentialed CORS is enabled; CSRF remains required. Wildcard origins are rejected.

Other module contracts, including `InteractiveSession`, `ParticipantManagement` and `SessionEvents`, retain their existing packages and public method signatures. Planned packages contain documentation only. AI, device pairing, real-time sessions and workflow execution still need defined implementations.

## Verification

```sh
./mvnw clean verify
# Unit, JWT signature validation, and HTTP security tests without requiring Docker:
./mvnw -Dtest=CoreQueryTest,BaseServiceTest,LessonServiceTest,LessonControllerTest,SecurityUnitTest,SecurityHttpTest,JwtValidationTest test
# Real PostgreSQL tests; Docker must be running:
./mvnw -Dtest=LessonIntegrationTest test
```

The updated PostgreSQL integration class uses Testcontainers `postgres:18`, Flyway and Hibernate validation. It contains tests for CRUD/publish, 401/403/404/409, filtering/pagination, membership and cross-tenant access, repository mapping, BaseRepository auditing, full rollback on bulk failure, and two independent persistence contexts competing on `@Version`. These updated tests have not been executed. It explicitly skips when Docker is unavailable. A skipped test is not evidence that database behavior passed. Unit tests use Mockito only where a database is irrelevant. JWT tests sign real RSA tokens and serve local JWKS to exercise issuer, audience, signature and expiry validation.

The legacy Bearer smoke script is retired and sends no requests. Existing JWT HTTP tests describe the pre-migration API and need adaptation before being used for BFF verification. No tests were added or executed for this migration, as requested.

**Current refactor: build, tests, application startup and smoke tests were not run, as explicitly requested.** Historical results in `VERIFICATION.md` apply only to the previous implementation. See `REFACTOR_REPORT.md` for the new structure, changed-file list, compatibility notes and remaining work. Before production, provide externally managed database credentials and TLS Keycloak endpoints, provision actual identities/memberships, and deploy Keycloak with production configuration. Swagger is disabled outside `local` unless `API_DOCS_ENABLED=true`. This repository supplies the foundation, not production deployment infrastructure or all 32 mini-apps.
