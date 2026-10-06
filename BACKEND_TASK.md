# Advanced Chat Backend Tasks

This file is the source of truth for project scope, phase status, verification, and handoff.
Last reviewed: 2026-10-06. Update it after every completed phase.

## Project

Advanced real-time chat and messenger backend for a Flutter client. Flutter will
use versioned REST/JSON APIs for authentication, resources, and synchronization,
plus authenticated WebSocket events for live messaging. Spring Boot owns
business rules, authorization, persistence, and notification delivery.

- Application: `chat-backend`; Maven coordinates: `com.rindev:chat-backend:0.0.1-SNAPSHOT`.
- Backend package: `com.rindev.chat`; entry point: `ChatApplication`.
- Database: existing Docker-hosted MySQL `chat_db`, with the original 13 application
  tables plus `refresh_tokens`; Flyway history is stored separately.
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
| HTTP | `spring-boot-starter-webmvc` | Existing versioned REST business APIs preserved |
| Validation | `spring-boot-starter-validation` | DTO validation and safe shared error responses |
| Persistence | `spring-boot-starter-data-jpa`, Jakarta Persistence, Hibernate | Original 13 entities/repositories plus refresh tokens; Hibernate validation |
| Database | `mysql-connector-j` at runtime; MySQL in Docker | Existing `chat_db`; observed MySQL 9.7.2 in prior verification |
| Migrations | `spring-boot-starter-flyway`, `flyway-mysql` | Existing V2/V3; disabled locally by default, enabled in prod; explicit baselines only |
| Security | `spring-boot-starter-security` | Stateless Bearer filter chain, database identity, BCrypt, safe 401/403 responses |
| JWT | JJWT 0.13.0: `jjwt-api`, runtime `jjwt-impl` and `jjwt-jackson` | HS256 access-token generation and strict signature/claim validation |
| Real-time transport | `spring-boot-starter-websocket` | Authenticated STOMP with conversation authorization and configurable origins |
| API documentation | `springdoc-openapi-starter-webmvc-ui` 3.0.2 | Enabled locally, disabled by default in prod |
| Boilerplate | Lombok, optional dependency | Entity getters, setters, and no-argument constructors |
| Development | `spring-boot-devtools`, optional runtime dependency | Development support |
| Tests | `spring-boot-starter-test`, `spring-boot-starter-security-test`; JUnit Jupiter and AssertJ | Phase 25: 250 tests, 0 failures/errors, 14 opt-in integration cases skipped |
| Health | `spring-boot-starter-actuator` | Only aggregate health exposed |
| FCM | `firebase-admin` 9.11.0 | Existing Google ADC integration and push provider preserved |

Unless explicitly versioned above, dependency versions are managed by the existing
Spring Boot parent. Local upload storage remains behind the existing abstraction;
no S3 SDK or Testcontainers dependency is introduced. Phase 25 adds a production
Dockerfile and build-context allowlist; no Compose configuration is required.

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
- Before any future schema migration, inspect and preserve existing Flyway history.
  The current database already has a V2 BASELINE and applied V3; do not rebaseline it.
  Only a reviewed original 13-table schema without history uses an explicit V1
  baseline followed by V2 onward. Keep automatic baselining disabled and do not
  recreate existing tables. See the Phase 25 history and README for deployment.
- After verification, report files changed, commands/results, decisions, and
  unresolved issues; stop and wait for the user's next instruction.

## Current Status

**Phases 1–24: implemented, as established by the user's Phase 25 scope.**
The early-phase checklists/history below predate that handoff; their historical
labels do not describe the current implementation and were not retroactively
checked off in this task. Existing business APIs, JWT/refresh tokens, uploads,
FCM, WebSocket events, database mappings, migrations, and original tests remain.

**Phase 25 — Production Readiness: COMPLETE.** Verified on 2026-10-06:

- [x] Java 21.0.12.1 selected by Maven 3.9.16.
- [x] `./mvnw clean compile` — BUILD SUCCESS (3.878 s, 170 main sources).
- [x] `./mvnw test` — BUILD SUCCESS (17.754 s): **250 tests, 0 failures,
  0 errors, 14 skipped**. Preserves the prior 188-test baseline and adds 62 checks.
- [x] Production JAR startup, Firebase SDK initialization, MySQL connection,
  Hibernate validation, and existing Flyway history validation pass.
- [x] GET/HEAD `/actuator/health` return 200; GET body is only `{"status":"UP"}`.
- [x] Sensitive Actuator routes and disabled Swagger are inaccessible;
  explicitly enabled Swagger/OpenAPI return 200.
- [x] Trusted/untrusted HTTP CORS returns 200/403; WebSocket handshakes return 101/403.
- [x] Docker image builds; Java 21 JRE, production profile, UID 10001, writable
  upload volume, and container health `healthy` verified.
- [x] Original schema fingerprint and existing V2 BASELINE/V3 history unchanged;
  no migration added or modified, no application table recreated or altered.
- [x] No secret or runtime upload addition staged. Three previously tracked
  upload files are staged for removal from Git and retained on disk.

The production profile requires DB URL/username/password, JWT signing key, upload
directory, and Firebase ADC from runtime configuration. Local DB host/username,
Swagger, and WebSocket defaults remain convenient; credential fallbacks are
removed. See [README.md](README.md) for startup and deployment procedures.

Verification uses the existing database read-only tests and temporary synthetic
Firebase ADC. No push was sent; real FCM delivery is not claimed. Deployment
operators must supply real credentials, rotate the previously checked-in database
credential, and use a private MySQL network. The existing local MySQL container's
public port binding was observed and documented, not changed by this task.

## Current Phase

**Phase 25 — Production Readiness**

**Status: COMPLETE**

**Next: Phase 26 — Flutter API Handoff (NOT STARTED).** Stop after Phase 25.

## Development Phases

Checklist convention: `[x]` completed and verified; `[ ]` not completed.
Phase 1–24 entries below retain their historical planning state; the Current Status
above supersedes their old status labels. Phase 25 records this task's executed
verification. Phase 26 remains planned and unstarted.
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

- [x] Review configuration, environment variables, credentials, JWT secrets, and logging.
- [x] Review migrations/baseline operations, CORS, access controls, and error handling.
- [x] Review upload storage, Swagger exposure, Docker deployment, and health checks.
- [x] Create production-friendly Docker configuration when requested in the phase scope.
- [x] Document private MySQL deployment and remove production credential fallbacks;
  exclude credentials and runtime uploads from Git/image build inputs.

**Definition of Done:**

- [x] Deployment configuration, migration procedure, health checks, and operational settings are documented and verified.
- [x] Identified production issues are resolved or explicitly recorded without claiming completion prematurely.

**Verification:**

- [x] Java 21 `./mvnw clean compile` — BUILD SUCCESS, 3.878 s.
- [x] Java 21 `./mvnw test` — BUILD SUCCESS, 17.754 s; **250 tests, 0 failures,
  0 errors, 14 skipped**. Existing 188 tests retain their original outcomes.
- [x] Production JAR and Docker startup with runtime variables; existing DB/Flyway
  history and Hibernate schema validation succeed without executing a migration.
- [x] GET/HEAD aggregate health 200; details/components/groups absent; sensitive
  actuator routes denied; Swagger default disabled and explicit enable verified.
- [x] Trusted/untrusted HTTP CORS and WebSocket origins verified over real HTTP.
- [x] Docker build succeeds; Java 21 JRE, non-root UID 10001, writable upload
  volume, and Docker health `healthy` verified; verification container removed.
- [x] `git diff --check`, secret-fallback/package inspection, staged-file review,
  upload ignore checks, and unchanged schema fingerprint verified.

**Status:** COMPLETE.

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

### Phase 25

**Status: COMPLETE**

2026-10-06: Read the complete task file and inspected the backend before Phase 25
changes. The user's established Phases 1–24 implementation scope supersedes the
stale early-phase planning labels. Phase 26 was not started.

Decisions and preservation:

- Removed the hard-coded local database-password fallback and empty JWT fallback.
  Production DB URL/username/password, signing key, and upload path require runtime
  configuration. Firebase continues to use Google ADC without a classpath secret.
- Added an isolated `prod` profile, exact trusted origin validation, HTTP CORS,
  production Swagger opt-in, status-only Actuator health, restrained logging, and
  safe HTTP/STOMP errors. Boot 4 enables health probe groups by default; they are
  explicitly disabled to keep the public aggregate response status-only.
- Restricted client STOMP SEND to the existing authenticated typing handlers;
  direct broker publishing is rejected. CONNECT/STOMP authentication drops native
  credentials after parsing and erases the principal's password hash. Existing
  authorized subscriptions, REST business operations, FCM behavior, and upload
  response metadata remain intact. FCM failure logs contain only error codes.
- Retained `FileStorageService`; made local storage use `UPLOAD_DIR`. No S3,
  database binary storage, schema migration, or business API redesign introduced.
- Live schema contains the original 13 tables plus `refresh_tokens` and Flyway
  history. Actual history is V2 BASELINE and successful V3. Preserved it exactly;
  README distinguishes it from the reviewed explicit V1 baseline procedure for
  an original 13-table database without migration history. Both profiles disable
  automatic baselining and clean; Hibernate remains `ddl-auto=validate`.
- Production startup validated three migration entries and reported schema
  version 3 up to date, with no migration necessary. Before/after schema-only
  SHA-256: `246f689ae0079bb8c93ddaa6de7d0ce5e48b95c1b9e6e065bc28ab7f9ad54a1a`.
- Docker uses Java 21 JDK build/JRE runtime, Maven wrapper, UID/GID 10001, prod
  profile, persistent upload volume, health probe, and an allowlisted build
  context. No credentials supplied during build or configured in the image.

Files created:

- `Dockerfile`, `.dockerignore`
- `src/main/resources/application-prod.yml`
- `src/main/java/com/rindev/chat/config/OriginPolicy.java`
- `src/main/java/com/rindev/chat/websocket/SafeStompErrorHandler.java`
- `src/test/java/com/rindev/chat/config/ProductionConfigurationTest.java`
- `src/test/java/com/rindev/chat/security/ProductionSecurityIntegrationTest.java`
- `src/test/java/com/rindev/chat/storage/LocalFileStorageServiceTest.java`
- `src/test/java/com/rindev/chat/websocket/SafeStompErrorHandlerTest.java`
- `src/test/java/com/rindev/chat/websocket/WebSocketAuthInterceptorTest.java`

Files modified:

- `.gitignore`, `pom.xml`, `README.md`, `BACKEND_TASK.md`
- `src/main/resources/application.yml`
- `src/main/java/com/rindev/chat/config/OpenApiConfig.java`
- `src/main/java/com/rindev/chat/config/WebSocketConfig.java`
- `src/main/java/com/rindev/chat/security/SecurityConfig.java`
- `src/main/java/com/rindev/chat/storage/LocalFileStorageService.java`
- `src/main/java/com/rindev/chat/notification/FirebasePushNotificationProvider.java`
- `src/main/java/com/rindev/chat/websocket/WebSocketAuthInterceptor.java`

Runtime uploads removed from the Git index and retained on disk:

- `uploads/6ea52679-4d68-4fab-86ad-9791f31e27c6.jpg`
- `uploads/cbaeb662-324d-4861-aecb-2401f50c1bef.jpeg`
- `uploads/f5da4e59-fbdb-4667-8605-964792cabce2.jpeg`

Commands and executed verification:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./mvnw --version
./mvnw clean compile
./mvnw test
./mvnw -Dmaven.test.skip=true package
java -jar target/chat-backend-0.0.1-SNAPSHOT.jar
docker build --progress=plain -t mychat-backend:phase25 .
docker run --rm --entrypoint java mychat-backend:phase25 -version
git diff --check
git ls-files uploads
git diff --cached --diff-filter=ACMRT --name-only
```

The JAR command used Java 21 with `SPRING_PROFILES_ACTIVE=prod`,
`FLYWAY_ENABLED=true`, required runtime inputs, and temporary local ports. It ran
once without `SWAGGER_ENABLED` and once with `SWAGGER_ENABLED=true`. Temporary
verification orchestration (not committed) was executed as:

```sh
python3 /private/tmp/mychat-phase25/verify.py test
python3 /private/tmp/mychat-phase25/verify.py smoke
python3 /private/tmp/mychat-phase25/verify.py docker-smoke
```

The script supplied the already-configured local DB credential only in process
memory, generated an ephemeral JWT key, and generated/deleted synthetic Firebase
ADC for SDK initialization. Secrets were never printed or added to configuration.
The Docker runtime command passed only environment variable names on argv, mounted
that temporary ADC read-only, and published only an ephemeral localhost HTTP port:

```sh
docker run --detach --rm --name mychat-phase25-verification \
  --publish 127.0.0.1::8080 \
  --mount type=bind,source="$TEMP_ADC_PATH",target=/run/secrets/firebase.json,readonly \
  --env DB_URL --env DB_USERNAME --env DB_PASSWORD --env JWT_SECRET \
  --env GOOGLE_APPLICATION_CREDENTIALS \
  --env CORS_ALLOWED_ORIGINS --env WEBSOCKET_ALLOWED_ORIGINS \
  mychat-backend:phase25
docker exec mychat-phase25-verification id -u
docker exec mychat-phase25-verification sh -c 'test -w /app/uploads'
docker inspect --format '{{.State.Health.Status}}' mychat-phase25-verification
docker rm --force --volumes mychat-phase25-verification
```

`TEMP_ADC_PATH` represents the generated ephemeral path, not a committed file.
GET/HEAD health, Swagger, protected routes, HTTP OPTIONS requests, and raw
WebSocket upgrade requests were executed by this script using standard Python
HTTP/socket clients. The verification container and temporary credentials were
removed afterwards. Existing `chat` remained running.

Final results:

- Host Java/Maven: Homebrew 21.0.12.1 / Maven 3.9.16.
- Compile: BUILD SUCCESS, 3.878 s, 170 sources targeting Java 21.
- Tests: BUILD SUCCESS, 17.754 s; **250 run, 0 failures, 0 errors, 14 skipped**.
  Existing baseline 188 plus 62 Phase 25 tests; no original test source changed.
- Package: BUILD SUCCESS, 5.314 s. Tests skipped only for this post-test packaging step.
- Production JAR and image: startup, MySQL, Flyway validation, and Hibernate validation pass.
- Health: GET/HEAD 200; GET exactly `{"status":"UP"}`, no details/components/groups.
- Sensitive Actuator routes/private API: anonymous 401; focused filter-chain
  tests also verify authenticated users receive 403 on prohibited routes.
- Swagger/OpenAPI: anonymous 401 by default; both return 200 when explicitly enabled.
- HTTP CORS: allowed preflight 200, untrusted 403; JWT remains required on private APIs.
- WebSocket origins: trusted handshake 101, untrusted 403.
- Docker build: successful; image `mychat-backend:phase25`, manifest list
  `sha256:6ae277a6e8e21eb0a839440f3d07288b5ec5f08d711ac366941428db70fc1773`.
  Runtime Temurin Java 21.0.12.1, UID 10001, writable upload volume, health `healthy`.
- Git/package checks: no whitespace errors; no DB/JWT fallbacks in packaged
  profiles; image config has no credential variables; no staged additions or
  modifications, only three upload index deletions; uploads remain ignored.

Intermediate checks found a missing field in a new test fixture and Boot 4's
extra health-group field; both were corrected before the final successful runs.
An automatic approval usage-limit rejection interrupted verification; it was
retried successfully after the user's instruction to continue. No blocker remains.
Generated sanitized logs and Surefire reports are under ignored `target/`.

Deployment limitations recorded, not silently treated as verified: no real FCM
push was sent; deployment needs real ADC/secrets; operators must rotate the
previously checked-in DB credential and use a private MySQL network. The current
local database container exposes port 3306 on all host interfaces and was not
reconfigured. These are deployment responsibilities, not changes to the existing
application or claims of a live production rollout. Next phase is Phase 26;
implementation stops here.

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
