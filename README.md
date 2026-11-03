# MPL One AI Smart TV backend

Java 21 / Spring Boot 4.1.2 modular monolith in package `vn.mpl.one`. The complete Lesson module demonstrates a reusable JPA core, validated DTOs, tenant authorization, Keycloak JWT authentication, filtering and optimistic locking. Other business areas expose extension interfaces only.

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
backend/
├── pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/
├── Dockerfile, docker-compose.yml, .env.example
├── src/main/java/vn/mpl/one/
│   ├── MplOneApplication.java
│   ├── config/                  Security and OpenAPI
│   ├── core/
│   │   ├── entity/              UUID, timestamps, version, TenantOwned
│   │   ├── repository/          BaseRepository, ScopedAggregate
│   │   ├── service/             BaseService
│   │   ├── filter/, specification/, pagination/
│   │   ├── response/, exception/, security/, audit/, util/
│   └── modules/
│       ├── identity/            User profile and /me
│       ├── organization/        Organizations, memberships, ports
│       ├── lesson/              api, application, domain, infrastructure
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

Modules follow `api → application → domain/infrastructure`. Controllers only validate/bind input and delegate; entities are never returned. Lesson uses composition around the internal generic core, so no inherited unrestricted CRUD methods become its public application interface. Tenant context is immutable and explicit, not a thread-local. The core depends on `TenantMembershipLookup`; the organization module supplies its JPA adapter, keeping the core independent of business modules. Every Lesson operation checks database membership and composes organization scope separately from user filters. PLATFORM_ADMIN has no implicit membership bypass. Mutations require both a TEACHER or ORG_ADMIN JWT role and a TEACHER or ORG_ADMIN membership. Reads require an allowed human realm role and active membership; DEVICE tokens cannot access these user APIs. This initial policy allows members to read all Lesson statuses.

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
cd backend
cp .env.example .env
# Edit .env as desired; these are disposable local credentials.
docker compose up -d app-db keycloak-db keycloak
# Wait until this returns the realm configuration:
curl --fail http://localhost:8180/realms/mpl-one/.well-known/openid-configuration
set -a
. ./.env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The database services have readiness checks. Keycloak imports the realm on first startup. Repeat the discovery request if Keycloak is still starting. Maven application startup runs Flyway, then Hibernate validates the schema. HikariCP defaults to ten connections, configurable using `DB_POOL_SIZE`. Open-in-view is disabled.

Or build and run the Java application in Docker:

```sh
docker compose --profile api up -d --build
```

API: `http://localhost:8081`; Swagger: `http://localhost:8081/swagger-ui/index.html`; OpenAPI: `http://localhost:8081/v3/api-docs`; health: `http://localhost:8081/actuator/health`; Keycloak admin: `http://localhost:8180/admin`. PostgreSQL application port is bound only to localhost:5433; Keycloak's database has no host port. Container-to-container JWKS uses `http://keycloak:8080` while issuer validation always uses the externally visible `http://localhost:8180/realms/mpl-one`.

```sh
docker compose logs -f keycloak
# Stop services, retaining database volumes:
docker compose --profile api down
# Explicitly destructive local reset, only when you want to erase demo databases:
docker compose --profile api down -v
```

Realm imports do not overwrite an existing realm. Use the admin console for existing installations or intentionally reset disposable volumes. The `local` profile adds the separate demo migration location; never activate this profile against production. Production databases must start with only `db/migration`, and application user profiles/memberships must be provisioned by an authorized future organization management adapter.

## Authentication and demo users

Realm `mpl-one` defines PLATFORM_ADMIN, ORG_ADMIN, TEACHER, STUDENT, PARENT, BUSINESS_MANAGER and DEVICE. `mpl-web` is public, uses authorization code + mandatory PKCE S256, and has password grants disabled. Its audience mapper adds `mpl-api` to access tokens. `mpl-api` is an API audience, not a login client. JWT verification checks JWKS signature, issuer, audience and time validity. Realm and `mpl-api` client roles map to `ROLE_*` authorities; roles from unrelated clients are ignored.

Use Swagger's **Authorize → keycloak** flow with client ID `mpl-web`, scopes `openid profile`, and no client secret. Its callback is pre-registered. A browser frontend may use `http://localhost:3000/*` and PKCE. The Bearer option also accepts an already issued access token. For curl examples below, obtain your **access token** from the authorized browser session; never use an ID token or admin credentials.

| Username | Development password | Role | Organization |
|---|---|---|---|
| teacher | demo-change-me | TEACHER | `11111111-1111-1111-1111-111111111111` |
| student | demo-change-me | STUDENT (read only) | same |

These are intentionally public development credentials, not production secrets. Keycloak admin credentials come exclusively from `.env` and are never exposed by the API. Demo user IDs match `user_profiles.subject` in the local migration. The second organization `22222222-2222-2222-2222-222222222222` has no demo memberships.

## Lesson requests

`X-Organization-Id` selects a tenant; it never grants access. All endpoints require authentication and active membership. Invalid inputs are 400, missing/invalid authentication 401, insufficient permission 403, absent or cross-tenant resources within an authorized tenant 404, stale versions/constraints 409, and unexpected errors a generic 500. Security-filter errors also return structured JSON. Server responses do not disclose database details or stack traces.

```sh
export TOKEN='<access-token>'
export ORG='11111111-1111-1111-1111-111111111111'
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/v1/me

curl -X POST http://localhost:8081/api/v1/lessons \
  -H "Authorization: Bearer $TOKEN" -H "X-Organization-Id: $ORG" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Math foundations","description":"Fractions and algebra"}'

# Set this to data.id from the create response:
export LESSON_ID='<lesson-uuid>'
curl -H "Authorization: Bearer $TOKEN" -H "X-Organization-Id: $ORG" \
  "http://localhost:8081/api/v1/lessons/$LESSON_ID"

curl -G http://localhost:8081/api/v1/lessons \
  -H "Authorization: Bearer $TOKEN" -H "X-Organization-Id: $ORG" \
  --data-urlencode 'page=1' --data-urlencode 'size=10' \
  --data-urlencode 'sort=title:asc' \
  --data-urlencode 'filter=title:like:math' \
  --data-urlencode 'filter=status:in:DRAFT,PUBLISHED'

curl -X PUT "http://localhost:8081/api/v1/lessons/$LESSON_ID" \
  -H "Authorization: Bearer $TOKEN" -H "X-Organization-Id: $ORG" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Math revised","description":"Updated","version":0}'

curl -X POST "http://localhost:8081/api/v1/lessons/$LESSON_ID/publish" \
  -H "Authorization: Bearer $TOKEN" -H "X-Organization-Id: $ORG" \
  -H 'Content-Type: application/json' -d '{"version":1}'

curl -X DELETE "http://localhost:8081/api/v1/lessons/$LESSON_ID?version=2" \
  -H "Authorization: Bearer $TOKEN" -H "X-Organization-Id: $ORG"
```

Use the latest response `version` for each mutation; example versions assume exactly those preceding successful changes. Creation always starts in DRAFT. Publishing moves to PUBLISHED and rejects ARCHIVED entities. ARCHIVED is reserved for a future authorized archive use case; arbitrary status changes are not accepted by update DTOs.

Success envelope: `{success,data,message,timestamp}`. Pagination data: `{count,rows,page,size,totalPages,hasNext,hasPrevious}`. Errors: `{success:false,status,message,errors,timestamp}`; bean-validation errors map field names to messages.

### Query contract

Filters: `field:operator:value`; repeated `filter` parameters or `&&` separators mean AND. Null operators take only `field:isnull` or `field:notnull`. `in` accepts comma-separated values (not JSON). Values can contain colons, for example ISO timestamps. LIKE is case-insensitive literal substring matching: `%`, `_`, and the escape character are escaped. Supported operators: eq, neq, gt, gte, lt, lte, like, in, isnull, notnull. Empty values and unsupported types/operators/fields are rejected. At most 20 predicates are accepted.

Filter fields: title, description, status, createdAt, updatedAt, version. Enums use exact names; timestamps use ISO-8601 instants. Sort fields: title, status, createdAt, updatedAt, id. Sort syntax is `field:asc` or `field:desc`, with repeated parameters or `&&` for multiple sorts. A UUID tiebreaker makes ordering stable; default ordering is newest created first. Page numbering begins at 1; size is 1–100. Tenant IDs, JPA paths and relation loading are never accepted in filters.

## Extend the foundation

1. Add `modules/<name>/{api,application,domain,infrastructure}`. Keep controllers thin and use validated request/response records.
2. Add an entity extending `BaseEntity` and, when appropriate, implementing `TenantOwned`. Add an immutable organization ID column and a Flyway migration with its foreign key and index. Do not enable automatic schema changes.
3. Define a repository extending `BaseRepository<Entity,UUID>` and a transactional application service. Inject dependencies via constructors. Compose `BaseService` for generic persistence and keep it internal to the authorized service; its methods are not an HTTP authorization boundary.
4. Authorize the subject against application membership on every use case, then combine the mandatory tenant specification using `.and(...)`. Never let user filters replace tenant predicates. Put `@PreAuthorize` on application operations and check membership write permissions too.
5. Add explicit filter type and sort whitelists. Use domain-specific specifications when a relationship or complex query is necessary; do not expose arbitrary paths.
6. Map entities to DTOs inside the transaction. Add actual PostgreSQL integration tests for tenant and concurrency behavior.

Example custom specification:

```java
Specification<Lesson> published = (root, query, cb) ->
    cb.equal(root.get("status"), LessonStatus.PUBLISHED);
Specification<Lesson> authorized = LessonSpecifications.tenant(tenant).and(published);
```

`BaseService` provides findById, findByCriteria, findOneByCriteria (requires at most one match), findOneOrFailByCriteria, paginate, exists, count, create, update, delete, bulkCreate and bulkUpdate. Updates accept a scope and mutation callback rather than blindly merging arbitrary request entities. Bulk operations use one caller transaction, not partial commits. The generic service's transactional annotations apply when registered as a Spring bean; the Lesson composition deliberately runs under its enclosing service transaction. Extend it only for internal non-HTTP use, or prefer composition as demonstrated. For aggregation, implement `ScopedAggregate<Entity,DomainMetric,Result>` with a closed metric enum and Criteria API expressions; always require the authorized scope and avoid unscoped global aggregation.

Smart TV registration, pairing and device authorization are distinct interfaces. They are not implemented through user passwords or browser user sessions. Classroom real-time transport, workflow engines, AI provider calls/usage billing, organization administration and membership provisioning remain extension work; there are no fake successful implementations.

## Verification

```sh
./mvnw clean verify
# Unit, JWT signature validation, and HTTP security tests without requiring Docker:
./mvnw -Dtest=CoreQueryTest,BaseServiceTest,SecurityUnitTest,SecurityHttpTest,JwtValidationTest test
# Real PostgreSQL tests; Docker must be running:
./mvnw -Dtest=LessonIntegrationTest test
```

The PostgreSQL integration class uses Testcontainers `postgres:18`, Flyway and Hibernate validation. It tests CRUD/publish, 401/403/404/409, filtering/pagination, membership and cross-tenant access, and two independent persistence contexts competing on `@Version`. It explicitly skips when Docker is unavailable. A skipped test is not evidence that database behavior passed. Unit tests use Mockito only where a database is irrelevant. JWT tests sign real RSA tokens and serve local JWKS to exercise issuer, audience, signature and expiry validation.

With Keycloak and the local API running, `python3 scripts/smoke_local.py` exercises real authorization code + PKCE, token validation, `/me`, and Lesson CRUD/publish/filtering. It uses the documented demo teacher, never prints tokens, and deletes its own test lesson.

See `VERIFICATION.md` for the actual environment results from initialization. Before production, provide externally managed database credentials and TLS Keycloak endpoints, provision actual identities/memberships, and deploy Keycloak with production configuration. Swagger is disabled outside `local` unless `API_DOCS_ENABLED=true`. This repository supplies the foundation, not production deployment infrastructure or all 32 mini-apps.
