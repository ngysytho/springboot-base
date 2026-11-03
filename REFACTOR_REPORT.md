# MPL One — báo cáo refactor

## Phạm vi và trạng thái

Đã sửa trực tiếp dự án hiện có ở `projects/springboot-base`, không tạo lại dự án hoặc tạo `backend/backend`. Đã đọc các implementation Java, cấu hình, migration, test và bản clone thư viện NestJS đã dùng ở lần khởi tạo (commit `8c2896a2061da44c6a37328b1dd507a6f0185226`). Không thay đổi dependency/BOM trong pom.xml.

**Theo yêu cầu của người dùng, không chạy Maven, compile, test, package, Docker, application hoặc smoke test trong lần refactor này. Không có kết luận build/test thành công cho mã mới.** Kết quả 35 test trong tài liệu cũ chỉ là lịch sử trước refactor. Artifact và report trong `target/` nếu còn tồn tại là bản cũ.

## Thay đổi chính

- `BaseEntity` giữ UUID, Instant timestamps và @Version. Auditing chuyển từ application entry point sang `AuditConfig`; `JpaConfig` quản lý transaction configuration. Không thêm cột audit actor khi chưa có yêu cầu lưu giữ dữ liệu.
- `BaseRepository` tiếp tục dùng Spring Data JPA và Specifications. `BaseService` nay abstract, helper protected, bắt buộc scope; save/saveAll/deleteById và bulk helper giữ lifecycle/version, không dùng bulk SQL. Public adapter methods mở transaction để lỗi giữa batch rollback toàn bộ.
- `TenantContext` và `TenantOwned` chuyển sang `core.tenant`. Membership vẫn được tra từ PostgreSQL; không tin organization ID hoặc role claim như bằng chứng membership.
- `FilterField` khai báo type và toán tử hợp lệ theo từng trường. Parser và SpecificationBuilder từ chối trường/đường dẫn/toán tử không cho phép; chuyển UUID/date/number/enum chặt chẽ. Null operators dùng IS_NULL/IS_NOT_NULL nhưng giữ URL aliases cũ.
- `PaginationRequest`/`PageResponse` là value objects không phụ thuộc Spring Data. `PaginationUtils` chuyển đổi Pageable/Sort/Page ở tầng persistence; giữ page 1-based, size 1–100 và thứ tự ổn định.
- `ErrorResponse` bổ sung error/path và giữ errors cho field validation. Controller advice giữ status HTTP chuẩn, không biến lỗi 400/405/415 thành 500; security entry point/access-denied handler dùng cùng envelope.
- `CorsConfig` nhận danh sách origin cụ thể qua env; không cho wildcard hoặc cookie credentials.
- Các module đều có package boundaries chi tiết được mô tả bằng package-info, không tạo service giả. Contract classroom/AI/device/workflow cũ được giữ nguyên public signatures.

## Luồng Lesson

```text
HTTP request + JWT + X-Organization-Id
  → LessonController + request validation
  → LessonApiMapper → application command
  → LessonService / CreateLessonUseCase
  → JWT role checks + active database membership
  → pure domain Lesson business rules
  → domain LessonRepository interface
  → LessonRepositoryAdapter + mandatory organization predicate
  → LessonJpaRepository / LessonJpaEntity / PostgreSQL
  → LessonPersistenceMapper → domain Lesson
  → LessonApiMapper → LessonResponse / PageResponse
```

Lesson domain không import Hibernate, Spring MVC hoặc Keycloak. Application không import persistence implementation/API DTO. JPA model tách riêng; adapter quản lý mapping, kiểm tra version và tenant scope. Các endpoint POST/GET/PUT/DELETE/publish, trường JSON, quyền read/write và trạng thái DRAFT/PUBLISHED/ARCHIVED giữ nguyên. Rule ARCHIVED không được publish nằm trong domain.

## Keycloak và database

Giữ realm `mpl-one`, clients `mpl-web` (authorization code + PKCE) và `mpl-api` (audience), demo roles/users, JWKS signature verification, issuer/audience validation và method security. Không thêm API password/login hoặc gọi Admin API trên mỗi request.

Không sửa nội dung migration V1 hay local demo migration V2, không thêm migration vì không đổi schema. JPQL entity names cũ được giữ khi chuyển entity sang infrastructure. Keycloak DB vẫn tách biệt. `ddl-auto=validate`, `open-in-view=false`, Flyway enabled được giữ nguyên. Local migration vẫn tách khỏi migration production để không seed demo vào production.

Port PostgreSQL mặc định giữ 5433 để không tự ý chuyển sang port 5432 có thể đã dùng; chỉnh `DB_PORT=5432` nếu muốn. API 8081, Keycloak 8180. Host issuer: `http://localhost:8180/realms/mpl-one`; container JWKS: `http://keycloak:8080/realms/mpl-one/protocol/openid-connect/certs`.

## Mã test đã bổ sung/cập nhật, chưa chạy

- BaseService: missing resource, required scope, cross-tenant writes, scoped pagination/count/exists, lifecycle-based bulk creation.
- Core queries: field/operator policies, malformed UUID/date/number/enum, raw criteria revalidation, null aliases, bounds/defaults/stable sorting.
- LessonService: membership before persistence, draft creation, stale version, publish transitions.
- LessonController: request validation, mapping and response status, malformed pagination.
- PostgreSQL Testcontainers: CRUD/publish, tenant isolation for all mutations, BaseRepository auditing/specifications, LessonRepositoryAdapter mapping/versioning, actual optimistic locking, rollback on partial bulk failure.
- Security: role conversion, tenant membership, missing/invalid/rejected-audience tokens and authorized requests. Real RSA/JWKS issuer/audience validation test is preserved; HTTP error translation tests use a decoder mock.

## Lệnh chạy sau này

```sh
cd /Users/ngysytho/workspace/GallopTech_Projects/projects/springboot-base
cp .env.example .env
# Chỉnh .env nếu cần; yêu cầu Java 21 khi chạy trên host.
docker compose up -d app-db keycloak-db keycloak
set -a
. ./.env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Hoặc `docker compose --profile api up -d --build`. Swagger: `http://localhost:8081/swagger-ui/index.html`.

Các lệnh kiểm tra được cung cấp để chạy khi bạn muốn, **chưa được thực thi**:

```sh
./mvnw clean test
./mvnw package -DskipTests
```

## Giới hạn và công việc còn lại

- Mã refactor chưa được compiler/runtime xác nhận theo yêu cầu không chạy kiểm tra; cần build/test trước khi triển khai.
- Internal Java package/API của persistence được đổi nhất quán trong source và tests; extension bên ngoài repo cần cập nhật imports và gọi qua domain repository thay cho public BaseService CRUD cũ.
- AI, pairing/authorization cho thiết bị, transport classroom và workflow engine vẫn là contracts, không có implementation giả. Organization administration chưa có endpoint ngoài phạm vi yêu cầu.
- JUnit và các dependency giữ version theo Spring Boot BOM hiện có; không ép hạ version JUnit độc lập với Boot.

## Cây thư mục

Xem `PROJECT_TREE.txt` để có toàn bộ cây file (loại trừ `.git` và `target`). Cấu trúc chính:

```text
springboot-base/
├── src/main/java/vn/mpl/one/
│   ├── config/{SecurityConfig,JpaConfig,CorsConfig,OpenApiConfig}.java
│   ├── core/{entity,repository,service,filter,specification,pagination,response,exception,security,tenant,audit,validation,util}/
│   └── modules/{identity,organization,lesson,device,classroom,workflow,ai}/
│       ├── api/{controller,dto/request,dto/response,mapper}/
│       ├── application/{service,usecase}/
│       ├── domain/{model,enums,repository}/
│       └── infrastructure/{persistence/entity,persistence/repository,persistence/mapper,specification}/
├── src/main/resources/{application.yml,application-local.yml,db,keycloak}/
├── src/test/java/vn/mpl/one/
├── Dockerfile, docker-compose.yml, .env.example
└── README.md, VERIFICATION.md, REFACTOR_REPORT.md, PROJECT_TREE.txt
```

## File mới (bao gồm đích chuyển package)

- `PROJECT_TREE.txt`
- `REFACTOR_REPORT.md`
- `src/main/java/vn/mpl/one/config/CorsConfig.java`
- `src/main/java/vn/mpl/one/config/JpaConfig.java`
- `src/main/java/vn/mpl/one/core/audit/AuditConfig.java`
- `src/main/java/vn/mpl/one/core/exception/BusinessException.java`
- `src/main/java/vn/mpl/one/core/exception/InvalidFilterException.java`
- `src/main/java/vn/mpl/one/core/exception/ResourceNotFoundException.java`
- `src/main/java/vn/mpl/one/core/filter/FilterField.java`
- `src/main/java/vn/mpl/one/core/pagination/PageResponse.java`
- `src/main/java/vn/mpl/one/core/pagination/PaginationUtils.java`
- `src/main/java/vn/mpl/one/core/response/ErrorResponse.java`
- `src/main/java/vn/mpl/one/core/tenant/TenantContext.java`
- `src/main/java/vn/mpl/one/core/tenant/TenantOwned.java`
- `src/main/java/vn/mpl/one/core/validation/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/api/controller/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/api/dto/request/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/api/dto/response/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/api/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/application/service/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/application/usecase/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/domain/enums/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/domain/model/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/domain/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/infrastructure/persistence/entity/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/infrastructure/persistence/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/infrastructure/persistence/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/ai/infrastructure/specification/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/api/controller/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/api/dto/request/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/api/dto/response/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/api/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/application/service/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/application/usecase/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/domain/enums/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/domain/model/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/domain/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/infrastructure/persistence/entity/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/infrastructure/persistence/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/infrastructure/persistence/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/classroom/infrastructure/specification/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/api/controller/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/api/dto/request/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/api/dto/response/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/api/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/application/service/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/application/usecase/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/domain/enums/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/domain/model/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/domain/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/infrastructure/persistence/entity/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/infrastructure/persistence/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/infrastructure/persistence/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/device/infrastructure/specification/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/api/controller/MeController.java`
- `src/main/java/vn/mpl/one/modules/identity/api/dto/request/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/api/dto/response/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/api/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/application/service/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/application/usecase/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/domain/enums/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/domain/model/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/domain/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/infrastructure/persistence/entity/UserProfileJpaEntity.java`
- `src/main/java/vn/mpl/one/modules/identity/infrastructure/persistence/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/infrastructure/persistence/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/identity/infrastructure/specification/package-info.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/controller/LessonController.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/dto/request/CreateLessonRequest.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/dto/request/UpdateLessonRequest.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/dto/request/VersionRequest.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/dto/response/LessonResponse.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/mapper/LessonApiMapper.java`
- `src/main/java/vn/mpl/one/modules/lesson/application/service/LessonService.java`
- `src/main/java/vn/mpl/one/modules/lesson/application/usecase/CreateLessonCommand.java`
- `src/main/java/vn/mpl/one/modules/lesson/application/usecase/CreateLessonUseCase.java`
- `src/main/java/vn/mpl/one/modules/lesson/application/usecase/UpdateLessonCommand.java`
- `src/main/java/vn/mpl/one/modules/lesson/domain/enums/LessonStatus.java`
- `src/main/java/vn/mpl/one/modules/lesson/domain/model/Lesson.java`
- `src/main/java/vn/mpl/one/modules/lesson/domain/repository/LessonRepository.java`
- `src/main/java/vn/mpl/one/modules/lesson/infrastructure/persistence/entity/LessonJpaEntity.java`
- `src/main/java/vn/mpl/one/modules/lesson/infrastructure/persistence/mapper/LessonPersistenceMapper.java`
- `src/main/java/vn/mpl/one/modules/lesson/infrastructure/persistence/repository/LessonJpaRepository.java`
- `src/main/java/vn/mpl/one/modules/lesson/infrastructure/persistence/repository/LessonRepositoryAdapter.java`
- `src/main/java/vn/mpl/one/modules/lesson/infrastructure/specification/LessonSpecifications.java`
- `src/main/java/vn/mpl/one/modules/organization/api/controller/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/api/dto/request/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/api/dto/response/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/api/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/application/service/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/application/usecase/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/domain/enums/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/domain/model/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/domain/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/infrastructure/persistence/entity/OrganizationJpaEntity.java`
- `src/main/java/vn/mpl/one/modules/organization/infrastructure/persistence/entity/OrganizationMembershipJpaEntity.java`
- `src/main/java/vn/mpl/one/modules/organization/infrastructure/persistence/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/organization/infrastructure/persistence/repository/JpaTenantMembershipLookup.java`
- `src/main/java/vn/mpl/one/modules/organization/infrastructure/persistence/repository/MembershipJpaRepository.java`
- `src/main/java/vn/mpl/one/modules/organization/infrastructure/specification/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/api/controller/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/api/dto/request/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/api/dto/response/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/api/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/application/service/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/application/usecase/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/domain/enums/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/domain/model/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/domain/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/infrastructure/persistence/entity/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/infrastructure/persistence/mapper/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/infrastructure/persistence/repository/package-info.java`
- `src/main/java/vn/mpl/one/modules/workflow/infrastructure/specification/package-info.java`
- `src/test/java/vn/mpl/one/LessonControllerTest.java`
- `src/test/java/vn/mpl/one/LessonServiceTest.java`

## File sửa tại đường dẫn cũ

- `.env.example`
- `README.md`
- `VERIFICATION.md`
- `docker-compose.yml`
- `src/main/java/vn/mpl/one/MplOneApplication.java`
- `src/main/java/vn/mpl/one/config/SecurityConfig.java`
- `src/main/java/vn/mpl/one/core/exception/ConflictException.java`
- `src/main/java/vn/mpl/one/core/exception/GlobalExceptionHandler.java`
- `src/main/java/vn/mpl/one/core/exception/NotFoundException.java`
- `src/main/java/vn/mpl/one/core/filter/FilterOperator.java`
- `src/main/java/vn/mpl/one/core/filter/FilterParser.java`
- `src/main/java/vn/mpl/one/core/pagination/PaginationRequest.java`
- `src/main/java/vn/mpl/one/core/security/TenantAuthorizationService.java`
- `src/main/java/vn/mpl/one/core/service/BaseService.java`
- `src/main/java/vn/mpl/one/core/specification/SpecificationBuilder.java`
- `src/main/resources/application-local.yml`
- `src/main/resources/application.yml`
- `src/test/java/vn/mpl/one/BaseServiceTest.java`
- `src/test/java/vn/mpl/one/CoreQueryTest.java`
- `src/test/java/vn/mpl/one/LessonIntegrationTest.java`
- `src/test/java/vn/mpl/one/SecurityHttpTest.java`

## Đường dẫn cũ đã chuyển/thay thế

Các implementation được tách hoặc chuyển vào cấu trúc mới; không bỏ use case hiện có.

- `src/main/java/vn/mpl/one/core/entity/TenantOwned.java`
- `src/main/java/vn/mpl/one/core/response/ApiError.java`
- `src/main/java/vn/mpl/one/core/response/PageResponse.java`
- `src/main/java/vn/mpl/one/core/security/TenantContext.java`
- `src/main/java/vn/mpl/one/modules/identity/api/MeController.java`
- `src/main/java/vn/mpl/one/modules/identity/domain/UserProfile.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/CreateLessonRequest.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/LessonController.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/LessonResponse.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/UpdateLessonRequest.java`
- `src/main/java/vn/mpl/one/modules/lesson/api/VersionRequest.java`
- `src/main/java/vn/mpl/one/modules/lesson/application/LessonService.java`
- `src/main/java/vn/mpl/one/modules/lesson/application/LessonSpecifications.java`
- `src/main/java/vn/mpl/one/modules/lesson/domain/Lesson.java`
- `src/main/java/vn/mpl/one/modules/lesson/domain/LessonStatus.java`
- `src/main/java/vn/mpl/one/modules/lesson/infrastructure/LessonRepository.java`
- `src/main/java/vn/mpl/one/modules/organization/domain/Organization.java`
- `src/main/java/vn/mpl/one/modules/organization/domain/OrganizationMembership.java`
- `src/main/java/vn/mpl/one/modules/organization/infrastructure/JpaTenantMembershipLookup.java`
- `src/main/java/vn/mpl/one/modules/organization/infrastructure/MembershipRepository.java`
