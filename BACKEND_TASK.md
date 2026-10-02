# Advanced Chat Backend Tasks

This file is the source of truth for project scope, phase status, verification, and handoff.
Last reviewed: 2026-10-02. Update it after every completed phase.

## Project

Advanced real-time chat and messenger backend for a Flutter client. Flutter will
use versioned REST/JSON APIs for authentication, resources, and synchronization,
plus authenticated WebSocket events for live messaging. Spring Boot owns
business rules, authorization, persistence, and notification delivery.

- Application: `chat-backend`; Maven coordinates: `com.rindev:chat-backend:0.0.1-SNAPSHOT`.
- Backend package: `com.rindev.chat`; entry point: `ChatApplication`.
- Database: existing Docker-hosted MySQL `chat_db`, with 13 application tables.
- Architecture: controller → service → repository → JPA/Hibernate → MySQL, with
  separate DTO, mapper, security, configuration, exception, and WebSocket packages.
- Flutter is the intended client; no Flutter implementation exists in this repository.

## Tech Stack

These entries reflect the inspected repository, not a claim that every feature is implemented.

| Area | Actual dependency/configuration | Current use |
| --- | --- | --- |
| Java | Java 21 in `pom.xml` | Phase 1 verified using JDK 21.0.12.1 |
| Build | Maven wrapper 3.3.4, Maven distribution 3.9.16; Spring Boot Maven plugin | Compilation and test execution |
| Framework | Spring Boot parent 4.1.1 | Application bootstrap and auto-configuration |
| HTTP | `spring-boot-starter-webmvc` | Web server available; business controllers empty |
| Validation | `spring-boot-starter-validation` | Dependency present; request contracts/error integration pending |
| Persistence | `spring-boot-starter-data-jpa`, Jakarta Persistence, Hibernate | 13 entities and repositories; observed Hibernate 7.4.5.Final |
| Database | `mysql-connector-j` at runtime; MySQL in Docker | Existing `chat_db`; observed MySQL 9.7.2 in prior verification |
| Migrations | `spring-boot-starter-flyway`, `flyway-mysql` | Disabled; baseline strategy documented, migration directory empty |
| Security | `spring-boot-starter-security` | Stateless Bearer filter chain, database identity, BCrypt, safe 401/403 responses |
| JWT | JJWT 0.13.0: `jjwt-api`, runtime `jjwt-impl` and `jjwt-jackson` | HS256 access-token generation and strict signature/claim validation |
| Real-time transport | `spring-boot-starter-websocket` | Dependency present; WebSocket/STOMP behavior unimplemented |
| API documentation | `springdoc-openapi-starter-webmvc-ui` 3.0.2 | API docs and Swagger UI disabled; configuration placeholder empty |
| Boilerplate | Lombok, optional dependency | Entity getters, setters, and no-argument constructors |
| Development | `spring-boot-devtools`, optional runtime dependency | Development support |
| Tests | `spring-boot-starter-test`, `spring-boot-starter-security-test`; JUnit Jupiter and AssertJ in tests | Three read-only MySQL checks plus 107 security test cases |

Unless explicitly versioned above, dependency versions are managed by the existing
Spring Boot parent. FCM and S3-compatible storage are planned; no Firebase or
object-storage SDK is currently declared. No Testcontainers dependency, backend
Dockerfile, or Compose configuration currently exists in the repository.

## Agent Rules

- Inspect existing code before editing, including this file, relevant tests,
  configuration, README, and schema definitions.
- Work on one phase at a time.
- Do not automatically start another phase.
- Before implementation, explain the plan and identify schema mismatches or blockers.
- Wait for the user's approval if the user asks to review the plan first.
- Never claim tests passed unless actually executed. Clearly distinguish prior
  verified results from commands executed in the current task.
- Do not break completed phases; preserve their behavior and relevant checks.
- Do not change database schema unless the current phase requires it.
- Never expose secrets, password hashes in API responses, or credentials in logs.
  Do not log passwords, JWTs, refresh tokens, or other secrets.
- Use Java 21; check the JDK selected by Maven.
- Update `BACKEND_TASK.md` after every completed phase: checklists, current status,
  verification evidence, decisions, remaining issues, and phase history.
- Reuse existing classes and placeholders. Do not duplicate classes/tables or
  rename packages without a concrete need.
- Keep controllers focused on HTTP transport; place business rules and domain
  authorization in services. Use constructor injection and DTOs.
- Use transactions for operations that modify related records. Prefer lazy JPA
  relationships; do not expose entities directly or hide design problems with
  ad hoc JSON annotations.
- Derive ownership-sensitive identities from the authenticated principal, and
  enforce resource access, membership, ownership, roles, and block rules server-side.
- Use `/api/v1/...`, JSON for normal REST APIs, multipart for uploads, and appropriate HTTP status codes.
- Keep `ddl-auto=validate`; never use `create` or `create-drop` against the existing database.
- Preserve the 13 existing application tables. The SQL snapshot is reference
  material, not a startup script or a migration to execute against `chat_db`.
- Before any future schema migration, follow the reviewed baseline strategy:
  explicitly baseline the existing schema at version 1, then introduce changes
  from V2 onward. Keep automatic baselining disabled. Do not recreate existing tables.
- After verification, report files changed, commands/results, decisions, and
  unresolved issues; stop and wait for the user's next instruction.

## Current Status

**Phase 1 — Foundation / JPA: COMPLETE.**

**Phase 2 — API Foundation: NOT STARTED.** Its eight placeholders remain empty.

**Phase 3 — Spring Security + JWT: IN PROGRESS.** The user explicitly selected
Phase 3 before Phase 2 implementation. Security code compiles and all 107 isolated
security tests pass; full MySQL regression and application startup checks remain
pending authorized database credential access. Do not mark this phase complete yet.

Verified Phase 1 results:

- [x] 13 entities implemented.
- [x] 13 repositories implemented.
- [x] 11 enums implemented.
- [x] `application.yml` configured with environment-based credentials.
- [x] Existing schema reference stored under `docs/schema/existing-chat-db.sql`.
- [x] `ddl-auto=validate`.
- [x] Flyway disabled; `baseline-on-migrate=false`.
- [x] Maven compile passed.
- [x] Tests passed: 3 run, 0 failures, 0 errors, 0 skipped.
- [x] Spring Boot startup passed.
- [x] MySQL connection passed.
- [x] Hibernate schema validation passed.

Application startup requires **Java 21**, **`DB_PASSWORD`**, and **`JWT_SECRET`**.
The signing secret must be Base64-encoded random key material of at least 32 decoded
bytes. `JWT_ISSUER` defaults to `chat-backend`; `JWT_ACCESS_TOKEN_TTL` defaults to
`15m` and must be a positive duration in whole seconds. Tests generate temporary
signing keys in memory, so the full test suite requires `DB_PASSWORD`/MySQL but
does not require an externally supplied `JWT_SECRET`. Configuration also
supports `DB_USERNAME` (local default `root`), `DB_URL` (local default
`localhost:3306/chat_db` with UTC settings), and `SERVER_PORT` (default 8080).
Supply environment variables through the shell or IDE; a `.env` file is not
loaded automatically. See [README.md](README.md) for setup and the baseline strategy.

Implementation inventory:

- Entities: `User`, `Conversation`, `ConversationMember`, `Message`,
  `MessageAttachment`, `MessageReceipt`, `MessageReaction`, `PinnedMessage`,
  `BlockedUser`, `Device`, `Notification`, `UserSetting`, `Report`.
- Each entity has a corresponding `JpaRepository<Entity, Long>` interface.
  `UserRepository.findByUsername` supports database-backed authentication.
- Enums: `AttachmentType`, `ConversationType`, `DevicePlatform`, `MemberRole`,
  `MessageType`, `NotificationType`, `PrivacyLevel`, `ReportReason`,
  `ReportStatus`, `Theme`, `UserStatus`.
- Mappings preserve lowercase database enum values using `@EnumeratedValue`,
  lazy foreign-key relationships, nullable flags, and database-generated timestamps.
  Time values use `LocalDateTime` with UTC connection/session configuration.
- Unsigned attachment `INT` values use `Long`. Unsigned `BIGINT` IDs/file sizes
  currently support values through `Long.MAX_VALUE`; larger values need a coordinated change.
- `open-in-view=false`, `show-sql=false`, and SQL initialization is disabled.
- The 37 Java placeholders for controllers, DTOs, exceptions, mappers, business
  services, OpenAPI configuration, and WebSocket behavior remain unimplemented
  (empty files or a bare class skeleton).
- Security implements `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`,
  `CustomUserDetailsService`, `JwtProperties`, `ChatUserDetails`, and
  `SecurityErrorHandler`. The security response writer does not implement Phase 2's
  general MVC error contract or any Phase 4 authentication endpoint.
- `HELP.md` contains old generated package/version references; the actual package
  and Spring Boot version are established by source and `pom.xml`, as recorded above.

Phase 1 historical verification:

| Command/check | Previously executed result |
| --- | --- |
| `./mvnw clean compile` under Java 21 | Passed |
| `./mvnw test` under Java 21 with database credentials | Passed: 3 tests |
| `./mvnw spring-boot:run` under Java 21 with database credentials | Started successfully; stopped cleanly after verification |
| MySQL connection and Hibernate validation | Passed against existing `chat_db` |
| HTTP smoke check on port 8080 | HTTP 401 from default Spring Security protection |

These results were obtained during Phase 1 on 2026-10-01. Its generated report path is
`target/surefire-reports/com.rindev.chat.ChatBackendApplicationTests.txt`;
reports under `target/` are generated and may be removed by clean.
The test source is
[`ChatBackendApplicationTests.java`](src/test/java/com/rindev/chat/ChatBackendApplicationTests.java).

The three tests cover exact entity/repository registration, bounded reads for every
entity, and all 13 native enum columns against the 11 Java enums. They use read-only
transactions against the configured existing MySQL database. They do not establish
future authentication/business correctness or persistence write coverage. Hibernate
validation alone does not comprehensively check indexes, foreign keys, defaults,
or enum members; the schema review and explicit enum checks supplement it.

Phase 3 verification so far:

- `./mvnw --no-transfer-progress clean compile` with Java 21 passed on 2026-10-01.
- `./mvnw --no-transfer-progress -Dtest=JwtServiceTest,CustomUserDetailsServiceTest,SecurityIntegrationTest test`
  with Java 21 passed on 2026-10-02: **107 tests, 0 failures, 0 errors, 0 skipped**.
  This run also compiled all main and test sources.
- The 107 cases comprise 55 JWT cases, 16 identity cases, and 36 filter-chain /
  password-authentication cases. They use generated keys and mocked repositories.
- After isolating the security test configuration/controller from application
  component scanning, `./mvnw --no-transfer-progress -Dtest=SecurityIntegrationTest test`
  passed again on 2026-10-02: **36 tests, 0 failures, 0 errors, 0 skipped**.
- Full `./mvnw test` and application startup verification are pending. `DB_PASSWORD`
  is not set in the execution environment; automatic approval review rejected
  reading the existing container's password without explicit authorization.
  The user has been asked to approve that read for verification or supply an
  approved environment configuration. Do not print or persist credentials.
- Persistence mappings, schema reference, migrations, and Phase 2 implementation
  remain unchanged. The existing Phase 1 test only gained generated test-key setup.

## Current Phase

**Phase 3 — Spring Security + JWT**

**Status: IN PROGRESS — verification pending**

Phase 3 was explicitly requested by the user. Finish its remaining verification
before marking it complete. Phase 2 remains NOT STARTED; recommend completing it
before Phase 4. Do not automatically implement either phase.

## Development Phases

Checklist convention: `[x]` completed and verified; `[ ]` not completed.
The verification entries for Phase 2 and Phases 4–26 are planned checks, not executed results.
For each implementation phase, use Java 21, run `./mvnw clean compile` and
`./mvnw test`, fix failures caused by the changes, and preserve Phase 1 checks.
Tests requiring MySQL need the configured database and `DB_PASSWORD`.
Expand verification only as needed for that phase's behavior.

### Phase 1 — Foundation / JPA

**Goal:** Connect Spring Boot to the existing MySQL database and validate all mappings.

**Tasks:**

- [x] Inspect `pom.xml`, `ChatApplication.java`, configuration, placeholders, and actual schema.
- [x] Implement all 13 entities, 13 repositories, and 11 enums in existing files.
- [x] Match existing tables, columns, enum literals, relationships, and timestamp behavior.
- [x] Configure environment-based credentials, UTC, and `ddl-auto=validate`.
- [x] Keep Flyway disabled and document the future baseline strategy.
- [x] Save the schema-only reference and document local execution.
- [x] Replace the misplaced test with three read-only foundation integration tests.

**Definition of Done:**

- [x] All 13 entities and repositories load against the unchanged existing schema.
- [x] Compilation, tests, startup, MySQL connection, and Hibernate validation pass.

**Verification:**

- [x] `./mvnw clean compile` — passed under Java 21.
- [x] `./mvnw test` — 3 passed, no failures/errors/skips.
- [x] `./mvnw spring-boot:run` — successful startup and database validation; clean shutdown.

**Status:** COMPLETE.

### Phase 2 — API Foundation

**Goal:** Establish shared REST response, validation, and error contracts.

**Tasks:**

- [ ] Implement `ApiResponse` and `ErrorResponse` in existing placeholders.
- [ ] Implement `GlobalExceptionHandler`.
- [ ] Implement `ResourceNotFoundException`, `BadRequestException`,
  `UnauthorizedException`, `ForbiddenException`, and `ConflictException`.
- [ ] Integrate Bean Validation with consistent request and field error responses.
- [ ] Define DTO conventions and consistent error fields: `success`, `code`,
  `message`, `timestamp`, with appropriate HTTP status codes.
- [ ] Keep persistence entities and internal exception details out of API responses.

**Definition of Done:**

- [ ] Shared contracts serialize consistently and map validation/domain failures correctly.
- [ ] Relevant tests pass and completed persistence behavior remains intact.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test response serialization, malformed/invalid requests, exception status mappings,
  and safe handling of unexpected failures without adding unrelated business APIs.

**Status:** NOT STARTED.

### Phase 3 — Spring Security + JWT

**Goal:** Establish stateless Bearer JWT authentication and request protection.

**Tasks:**

- [x] Implement `SecurityConfig` using `SecurityFilterChain`.
- [x] Implement `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`,
  and a secure `PasswordEncoder`.
- [x] Read signing secrets and token settings from configuration/environment.
- [x] Permit required authentication routes and protect private API routes.
- [x] Resolve authenticated identity server-side and return consistent 401/403 errors.

**Definition of Done:**

- [x] Valid tokens authenticate; missing, malformed, expired, or invalid tokens cannot
  access protected routes.
- [x] Security uses modern configuration and does not log secrets.
- [ ] Complete full MySQL regression and application startup verification with authorized credentials.

**Verification:**

- [x] Run `./mvnw clean compile` — passed with Java 21.
- [x] Test token validation, principal resolution, public/protected rules, and access denial — 107 security cases passed.
- [ ] Run full `./mvnw test` against existing MySQL and verify application startup.

**Status:** IN PROGRESS — full verification pending.

### Phase 4 — Authentication API

**Goal:** Provide secure registration, login, token renewal, logout, and current-user access.

**Tasks:**

- [ ] Implement `POST /api/v1/auth/register`, `/login`, `/refresh`, and `/logout`.
- [ ] Implement `GET /api/v1/auth/me` and safe request/response DTOs.
- [ ] Validate registration, check username/email conflicts, hash passwords, and
  transactionally create the user and required default settings.
- [ ] Verify login credentials and issue authentication tokens.
- [ ] Implement server-side refresh-token storage, expiry, renewal, and revocation.
  Inspect the existing schema before designing required storage changes; use the
  baseline/migration process if this phase requires a schema change.

**Definition of Done:**

- [ ] The complete auth lifecycle works without exposing password hashes or storing plaintext passwords.
- [ ] Duplicate registration and invalid/revoked credentials are handled safely.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test registration, duplicates, password verification, login failures, refresh,
  logout/revocation, and authenticated `/me`.

**Status:** NOT STARTED.

### Phase 5 — Swagger / OpenAPI

**Goal:** Make the implemented REST API discoverable and testable with Bearer JWT.

**Tasks:**

- [ ] Implement `OpenApiConfig` with HTTP Bearer/JWT security.
- [ ] Enable and verify `/v3/api-docs` and Swagger UI.
- [ ] Document auth endpoints and security requirements using appropriate annotations.
- [ ] Configure documentation route access and demonstrate protected API testing.

**Definition of Done:**

- [ ] Swagger supports login → receive JWT → Authorize → authenticated `GET /api/v1/auth/me`.
- [ ] Published contracts accurately describe implemented responses and errors.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Inspect generated OpenAPI and execute the Swagger authentication workflow.

**Status:** NOT STARTED.

### Phase 6 — User API

**Goal:** Provide safe user discovery and authenticated profile management.

**Tasks:**

- [ ] Implement `GET /api/v1/users`, `/{id}`, `/search?q=`, and `/me`.
- [ ] Implement `PATCH /api/v1/users/me` and `/me/avatar`.
- [ ] Implement user DTOs, `UserMapper`, `UserService`, `UserServiceImpl`, and `UserController`.
- [ ] Paginate lists/search, enforce self-update ownership, and omit password hashes.

**Definition of Done:**

- [ ] User endpoints return safe DTOs with bounded lists and validated updates.
- [ ] A caller cannot change another user's private profile through self-service routes.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test lookup/search, pagination, profile updates, validation, and response privacy.

**Status:** NOT STARTED.

### Phase 7 — User Settings

**Goal:** Let authenticated users read and update their own preferences.

**Tasks:**

- [ ] Implement `GET /api/v1/settings` and `PATCH /api/v1/settings`.
- [ ] Support theme, message/group/reaction notifications, and read receipts.
- [ ] Support last-seen, profile-photo, and group-add privacy settings.
- [ ] Resolve the settings owner from authentication and validate partial updates.

**Definition of Done:**

- [ ] Settings persist correctly and are accessible only to their owner.
- [ ] Omitted PATCH fields retain existing values.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test defaults, partial updates, invalid values, and cross-user access denial.

**Status:** NOT STARTED.

### Phase 8 — Conversations

**Goal:** Create and retrieve authorized direct and group conversations.

**Tasks:**

- [ ] Implement `POST /api/v1/conversations`, `GET /api/v1/conversations`,
  `GET /api/v1/conversations/{id}`, and `PATCH /api/v1/conversations/{id}`.
- [ ] Support direct requests with `memberId` and group requests with `name`/`memberIds`.
- [ ] Derive creator identity from authentication and create groups transactionally.
- [ ] Define and enforce duplicate-direct-conversation rules.
- [ ] Enforce membership and permissions for private data and updates.

**Definition of Done:**

- [ ] Direct/group creation and retrieval work with consistent membership.
- [ ] Non-members cannot read or modify private conversations.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test direct/group creation, duplicates, rollback, member access, and forbidden access.

**Status:** NOT STARTED.

### Phase 9 — Conversation Members

**Goal:** Manage group participation and roles securely.

**Tasks:**

- [ ] Implement GET/POST `/api/v1/conversations/{conversationId}/members`.
- [ ] Implement DELETE `/api/v1/conversations/{conversationId}/members/{userId}`.
- [ ] Implement PATCH `/api/v1/conversations/{conversationId}/members/{userId}/role`.
- [ ] Enforce OWNER, ADMIN, and MEMBER permissions in services.
- [ ] Prevent privilege escalation and preserve valid ownership when membership changes.

**Definition of Done:**

- [ ] Authorized member/role changes work and unauthorized changes are rejected.
- [ ] Group ownership and membership invariants remain consistent.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test role combinations, duplicate membership, removals, and escalation attempts.

**Status:** NOT STARTED.

### Phase 10 — Messages

**Goal:** Persist authorized chat messages and support their lifecycle through REST.

**Tasks:**

- [ ] Implement POST/GET `/api/v1/conversations/{conversationId}/messages`.
- [ ] Implement GET/PATCH/DELETE `/api/v1/messages/{messageId}`.
- [ ] Support TEXT, IMAGE, VIDEO, AUDIO, FILE, LOCATION, and SYSTEM types with content validation.
- [ ] Support reply, forward, edit, and soft delete with valid resource references.
- [ ] Derive sender from authentication; enforce membership, ownership, and block rules.
- [ ] Use authenticate → authorize → validate → persist ordering.

**Definition of Done:**

- [ ] Message operations persist correct data and enforce resource access and ownership.
- [ ] Soft deletion, reply/forward references, and blocked-user behavior are tested.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test valid/invalid types, membership, forged sender IDs, edits/deletes,
  inaccessible references, and existing block relationships.

**Status:** NOT STARTED.

### Phase 11 — Message Pagination

**Goal:** Retrieve chat history efficiently without loading all messages.

**Tasks:**

- [ ] Implement bounded cursor/keyset pagination, such as `?before=1000&limit=30`.
- [ ] Define stable ordering, cursor validation, limits, and continuation metadata for Flutter.
- [ ] Inspect message-history queries and existing indexes; migrate only if required.

**Definition of Done:**

- [ ] Page boundaries are stable, bounded, and free of unintended duplicate/omitted messages.
- [ ] Query/index behavior supports efficient history access with authorization intact.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test first/next/empty pages, invalid cursors/limits, concurrent inserts, and query plans.

**Status:** NOT STARTED.

### Phase 12 — File Upload / Attachments

**Goal:** Accept validated media and store attachment metadata.

**Tasks:**

- [ ] Implement authenticated `POST /api/v1/uploads` using multipart/form-data.
- [ ] Validate file size, MIME/content type, and IMAGE/VIDEO/AUDIO/FILE categories.
- [ ] Create a storage abstraction with local development storage and future S3 compatibility.
- [ ] Persist attachment metadata/references without storing large binaries in MySQL.
- [ ] Enforce ownership/access for attaching and retrieving private uploads.

**Definition of Done:**

- [ ] Valid uploads produce usable metadata; unsupported/oversized content is rejected.
- [ ] Storage implementation can be replaced without changing domain/API responsibilities.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test accepted/rejected files, authentication, storage failures, and attachment access.

**Status:** NOT STARTED.

### Phase 13 — Reactions

**Goal:** Add, remove, and list authorized message reactions.

**Tasks:**

- [ ] Implement POST/GET `/api/v1/messages/{messageId}/reactions`.
- [ ] Implement DELETE `/api/v1/messages/{messageId}/reactions/{emoji}`.
- [ ] Check message/conversation access and reaction ownership.
- [ ] Prevent invalid duplicates using application rules and existing constraints.

**Definition of Done:**

- [ ] Reactions are consistent and unauthorized or duplicate operations are handled safely.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test add/remove/list, duplicate attempts, invalid emoji inputs, and access denial.

**Status:** NOT STARTED.

### Phase 14 — Delivery / Read Receipts

**Goal:** Track delivery and reading with repeatable, authorized operations.

**Tasks:**

- [ ] Implement `POST /api/v1/messages/{messageId}/delivered` and `/read`.
- [ ] Derive receipt user from authentication and enforce conversation access.
- [ ] Make receipt operations idempotent and respect applicable read-receipt settings.
- [ ] Decide whether to include `POST /api/v1/conversations/{conversationId}/read`
  during phase planning; implement and test it only if included.

**Definition of Done:**

- [ ] Repeated receipt requests preserve consistent state without duplicate rows.
- [ ] Callers cannot update another user's receipts or inaccessible messages.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test delivery/read transitions, retries, privacy settings, and unauthorized access.

**Status:** NOT STARTED.

### Phase 15 — Pinned Messages

**Goal:** Manage conversation pins with membership and role enforcement.

**Tasks:**

- [ ] Implement POST/DELETE `/api/v1/conversations/{conversationId}/pins/{messageId}`.
- [ ] Implement GET `/api/v1/conversations/{conversationId}/pins`.
- [ ] Validate membership, pin permissions, and message/conversation consistency.
- [ ] Handle duplicate pins consistently with existing uniqueness constraints.

**Definition of Done:**

- [ ] Authorized users can manage/list pins; unrelated messages and unauthorized callers are rejected.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test pin/unpin/list, duplicates, permissions, and cross-conversation message IDs.

**Status:** NOT STARTED.

### Phase 16 — Blocking

**Goal:** Manage block relationships and enforce them throughout direct messaging.

**Tasks:**

- [ ] Implement POST/DELETE `/api/v1/users/{id}/block`.
- [ ] Implement GET `/api/v1/users/me/blocked`.
- [ ] Derive blocker identity from authentication; handle self-blocking and duplicates.
- [ ] Integrate block rules into direct conversations and messages, including both directions.

**Definition of Done:**

- [ ] Block state is user-owned and direct-chat restrictions are enforced server-side.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test block/unblock/list, repeated requests, and direct-chat enforcement/regressions.

**Status:** NOT STARTED.

### Phase 17 — Devices / Notifications

**Goal:** Register client devices and manage each user's persistent notifications.

**Tasks:**

- [ ] Implement POST `/api/v1/devices` and DELETE `/api/v1/devices/{id}`.
- [ ] Implement GET `/api/v1/notifications` with pagination.
- [ ] Implement POST `/api/v1/notifications/{id}/read` and `/read-all`.
- [ ] Implement DELETE `/api/v1/notifications/{id}`.
- [ ] Validate device/platform/token metadata and enforce authenticated ownership.

**Definition of Done:**

- [ ] Device and notification operations are private, paginated where needed, and consistent.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test device registration/removal, notification pagination/read/delete, and cross-user denial.

**Status:** NOT STARTED.

### Phase 18 — Reports

**Goal:** Accept authenticated abuse reports with protected moderation state.

**Tasks:**

- [ ] Implement `POST /api/v1/reports` with reporter identity from authentication.
- [ ] Support SPAM, HARASSMENT, FAKE_ACCOUNT, INAPPROPRIATE, and OTHER reasons.
- [ ] Validate report targets, references, descriptions, and resource permissions.
- [ ] Prevent normal users from assigning arbitrary moderation status.

**Definition of Done:**

- [ ] Valid reports persist with correct reporter/targets and controlled initial status.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test valid/invalid reports, inaccessible targets, forged reporters, and status tampering.

**Status:** NOT STARTED.

### Phase 19 — WebSocket Foundation

**Goal:** Establish authenticated real-time transport after REST messaging works.

**Tasks:**

- [ ] Confirm prerequisite REST message behavior is verified before starting.
- [ ] Configure `/ws` and select/document STOMP usage where appropriate.
- [ ] Authenticate connections and authorize subscriptions/destinations.
- [ ] Define event contracts for `message:new`, `message:edited`, `message:deleted`,
  `message:delivered`, `message:read`, `reaction:added`, `reaction:removed`,
  `typing:start`, `typing:stop`, `user:online`, and `user:offline`.

**Definition of Done:**

- [ ] Authenticated clients connect; unauthorized connections/subscriptions cannot access private events.
- [ ] Transport contracts are ready for subsequent event producers.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test valid/invalid authentication, destination access, disconnects, and reconnect behavior.

**Status:** NOT STARTED.

### Phase 20 — Real-Time Messaging

**Goal:** Broadcast committed message and related persistence events to authorized clients.

**Tasks:**

- [ ] Integrate message creation/edit/delete, receipt, and reaction events with persistence.
- [ ] Enforce authenticate → authorize → validate → persist → commit → broadcast ordering.
- [ ] Route events only to permitted recipients and preserve REST history/synchronization.

**Definition of Done:**

- [ ] Successful commits generate appropriate events; rolled-back operations generate no success events.
- [ ] Event payloads and persisted state agree without leaking private conversation data.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test commit/rollback event behavior, recipient authorization, and REST/live consistency.

**Status:** NOT STARTED.

### Phase 21 — Typing / Presence

**Goal:** Provide temporary typing indicators and reliable user presence.

**Tasks:**

- [ ] Implement `typing:start` and `typing:stop` without persisting typing events.
- [ ] Implement `user:online` and `user:offline`.
- [ ] Track connection lifecycle and update persistent `last_seen_at` appropriately.
- [ ] Enforce conversation access and applicable privacy rules.

**Definition of Done:**

- [ ] Typing remains temporary; presence transitions and last-seen updates reflect connection state.
- [ ] Multiple connections, disconnects, and reconnects do not expose incorrect private state.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test typing lifecycle, multiple sessions, disconnect/reconnect, last-seen, and access rules.

**Status:** NOT STARTED.

### Phase 22 — FCM

**Goal:** Deliver push notifications to eligible offline devices.

**Tasks:**

- [ ] Introduce an FCM provider abstraction/integration with externalized credentials.
- [ ] After message commit, determine eligible offline recipients and active device tokens.
- [ ] Respect notification settings and handle provider failures/invalid tokens.
- [ ] Keep database persistence authoritative; FCM must not become message storage.

**Definition of Done:**

- [ ] Eligible offline recipients receive push requests without compromising persistence correctness.
- [ ] Provider failures are handled and no credentials/tokens are logged.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Test recipient selection and provider outcomes with a controlled test provider;
  verify real delivery only in an explicitly configured test environment.

**Status:** NOT STARTED.

### Phase 23 — Performance

**Goal:** Improve measured query bottlenecks while preserving correctness and authorization.

**Tasks:**

- [ ] Measure N+1 behavior, message history, conversation lists, unread counts, and notification queries.
- [ ] Review indexes and query plans before changing query/schema design.
- [ ] Apply targeted projections, entity graphs, fetch joins, purpose-built queries,
  or cursor improvements where measurements justify them.
- [ ] Keep relationships lazy by default and migrate any required index changes.

**Definition of Done:**

- [ ] Targeted improvements have recorded before/after evidence and preserve API/security behavior.
- [ ] No speculative complexity or global eager-fetching workaround is introduced.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Compare representative query counts, plans, and timings; run affected regression tests.

**Status:** NOT STARTED.

### Phase 24 — Testing

**Goal:** Complete meaningful coverage for critical backend behavior.

**Tasks:**

- [ ] Cover registration/duplicates, login/invalid login, JWT, and unauthorized requests.
- [ ] Cover conversation access, group roles, message creation/ownership, and block rules.
- [ ] Cover pagination, reactions, and idempotent receipts.
- [ ] Add integration tests where needed; use MySQL Testcontainers if the environment supports it.
- [ ] Preserve existing foundation tests and make test prerequisites explicit.

**Definition of Done:**

- [ ] Critical success/failure and authorization paths are covered by reliable tests.
- [ ] The complete test suite passes with documented environment requirements.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Review coverage against the listed critical behaviors and fix application-caused failures.

**Status:** NOT STARTED.

### Phase 25 — Production Readiness

**Goal:** Prepare the implemented backend for a documented production deployment.

**Tasks:**

- [ ] Review configuration, environment variables, credentials, JWT secrets, and logging.
- [ ] Review migrations/baseline operations, CORS, access controls, and error handling.
- [ ] Review upload storage, Swagger exposure, Docker deployment, and health checks.
- [ ] Create production-friendly Docker configuration when requested in the phase scope.
- [ ] Keep MySQL private in production and all production secrets out of the repository.

**Definition of Done:**

- [ ] Deployment configuration, migration procedure, health checks, and operational settings are documented and verified.
- [ ] Identified production issues are resolved or explicitly recorded without claiming completion prematurely.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test`.
- [ ] Exercise the production configuration and agreed deployment/health checks in a suitable environment.

**Status:** NOT STARTED.

### Phase 26 — Flutter API Handoff

**Goal:** Provide an accurate, usable backend contract to the Flutter client team.

**Tasks:**

- [ ] Document base URL, authentication, Bearer header, and auth/user/settings endpoints.
- [ ] Document conversation/member/message/upload/notification/device/report APIs and related features.
- [ ] Document cursor pagination, consistent errors, and example request/response JSON.
- [ ] Document WebSocket endpoint, authentication, subscriptions, and event payloads.
- [ ] Reconcile handoff documentation with generated OpenAPI and implemented behavior.
- [ ] Keep Flutter UI work outside scope unless explicitly requested.

**Definition of Done:**

- [ ] A client developer can authenticate, access authorized resources, paginate history,
  upload files, and consume live events using the handoff.
- [ ] Examples and Swagger agree with the verified backend.

**Verification:**

- [ ] Run `./mvnw clean compile` and `./mvnw test` as part of the final backend handoff.
- [ ] Check example requests/responses against the application and validate documented
  authentication, pagination, errors, and WebSocket flows.

**Status:** NOT STARTED.

## Phase History

### Phase 1

**Status: COMPLETE**

Completed and verified on 2026-10-01. Implemented the existing placeholders for
13 JPA entities, 13 repositories, and 11 enums against the actual MySQL schema.
Preserved lazy relationships, database timestamp generation, and lowercase enum
literals. Replaced the hardcoded database password with environment configuration,
retained schema validation, kept Flyway disabled, captured the schema reference,
and documented the future baseline procedure in README.

Corrected the application test package and added three read-only integration
checks. Java 21 compilation passed; all three tests passed; Spring Boot started,
connected to MySQL, and completed Hibernate schema validation. An HTTP smoke check
returned 401 under default Spring Security, and the application was stopped cleanly.
No application tables were recreated or altered. Later phases were not implemented.

### Task-file initialization

2026-10-01: Inspected source, configuration, build files, README, schema reference,
placeholders, and saved verification reports. Created this persistent task file
only. Preserved Phase 1 completion and selected Phase 2 as NOT STARTED. No builds,
tests, database operations, or application implementation were performed in this
documentation task.

### Phase 3

**Status: IN PROGRESS — full verification pending**

2026-10-01–2026-10-02: Implemented Phase 3 at the user's explicit request while
Phase 2 remains pending. Added stateless Bearer authentication, strict HS256
access-token generation/validation, database-backed principals, BCrypt password
authentication, and safe security-layer JSON 401/403 responses. Only the planned
POST register/login/refresh routes are public; their endpoint implementations
remain Phase 4 work. No global roles were invented from presence or conversation
roles. The general Phase 2 response/exception contracts remain unimplemented.

Files changed for this phase:

- Implemented existing security placeholders: `SecurityConfig.java`,
  `JwtService.java`, `JwtAuthenticationFilter.java`, and `CustomUserDetailsService.java`.
- Added `security/JwtProperties.java`, `security/ChatUserDetails.java`, and
  `security/SecurityErrorHandler.java` under `src/main/java/com/rindev/chat`.
- Added `findByUsername` to `repository/UserRepository.java` and environment-based
  JWT settings to `src/main/resources/application.yml`.
- Added `JwtServiceTest.java`, `CustomUserDetailsServiceTest.java`, and
  `SecurityIntegrationTest.java` under `src/test/java/com/rindev/chat/security`.
  Existing `ChatBackendApplicationTests.java` only gained generated signing-key
  setup. Security test fixtures are excluded from normal component scanning.
- Updated `README.md` and this task file with configuration, scope, and verification.

Java 21 compilation passed. All 107 isolated security cases passed; the affected
36 integration cases passed again after the test-isolation fix. Full database
regression and application startup checks remain pending authorized `DB_PASSWORD`
access. This phase is not complete until those checks pass. Persistence entities,
schema, migrations, and later-phase implementation remain unchanged.

## Agent Workflow

For every future phase:

```text
PLAN
↓
Discuss with user
↓
IMPLEMENT
↓
COMPILE
↓
TEST
↓
FIX
↓
VERIFY
↓
UPDATE BACKEND_TASK.md
↓
STOP
```

Read this file and inspect relevant existing code before planning. If the user
asks to review the plan first, stop before implementation until approval arrives.
Otherwise follow the authorization in the user's phase request. On failures,
repeat only the necessary implementation/compile/test/fix steps until verified or
a concrete blocker is documented.

Mark completion only when the phase's Definition of Done and verification are
satisfied. Record actual commands/results, files changed, decisions, and remaining
issues in this file and the completion report. Identify the next recommended phase
without starting it. Stop and wait for the user.
