# MYCHAT backend

Java 21, Maven wrapper, Spring Boot, and MySQL backend for MYCHAT. Phases 1–24
provide the existing REST APIs, JWT authentication and refresh tokens,
WebSocket/STOMP messaging, FCM notifications, file uploads, and tests. Phase 25
adds production configuration and deployment support while preserving those
contracts and the original 13 application tables.

[`BACKEND_TASK.md`](BACKEND_TASK.md) records phase status and executed verification
results. Phase 26 — Flutter API Handoff is the next phase and is outside this
change.

## Run locally

1. Select a Java 21 JDK with `JAVA_HOME`; confirm `./mvnw --version` reports Java 21.
   On macOS with Homebrew, an installation may be available at
   `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`.
2. Start the existing local MySQL database, for example `docker start chat`.
3. Supply `DB_PASSWORD`, `JWT_SECRET`, and Firebase Application Default
   Credentials through your shell, IDE, or runtime secret provider. Override
   `DB_USERNAME` and `DB_URL` when needed.
4. Run:

   ```sh
   ./mvnw clean compile
   ./mvnw test
   ./mvnw spring-boot:run
   ```

The default profile retains the existing localhost JDBC URL, `root` development
username, relative `uploads/` directory, enabled Swagger, and permissive local
WebSocket origins. There is no database-password or JWT-secret fallback. The
localhost JDBC URL disables TLS and permits public-key retrieval for local MySQL
authentication; production must supply its own JDBC URL and TLS settings. Keep
UTC connection/session settings when replacing the URL.

Spring Boot does not automatically load `.env` files. Export variables or inject
them into the process. Flyway remains disabled by default locally; enable it only
after reviewing the database history as described below. Hibernate always uses
`ddl-auto=validate`, and SQL initialization is disabled.

## Production environment

Select `prod` explicitly outside Docker. The Docker image selects it by default.
Required values must be supplied at runtime; none are baked into the image.

| Variable | Production requirement / default |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Set to `prod`; already set by the Docker image |
| `DB_URL` | Required JDBC URL for a private MySQL host, correct database, TLS policy, and UTC session settings; no production default |
| `DB_USERNAME` | Required deployment database account; no production default |
| `DB_PASSWORD` | Required runtime secret; no default |
| `JWT_SECRET` | Required Base64-encoded cryptographically random key with at least 32 decoded bytes; no default |
| `JWT_ISSUER` | `chat-backend`; nonblank without surrounding whitespace |
| `JWT_ACCESS_TOKEN_TTL` | `15m`; positive duration in whole seconds |
| `REFRESH_TOKEN_TTL` | `30d` |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to a runtime-mounted credential file when using file-based Firebase Application Default Credentials; omit when a managed workload identity supplies ADC |
| `UPLOAD_DIR` | Required writable, persistent directory; the Docker image sets `/app/uploads` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated exact trusted HTTP(S) browser origins; empty by default in production |
| `WEBSOCKET_ALLOWED_ORIGINS` | Comma-separated exact trusted HTTP(S) WebSocket origins; empty by default in production |
| `SWAGGER_ENABLED` | `false` in production; setting `true` enables both OpenAPI and Swagger UI |
| `FLYWAY_ENABLED` | `true` in production; disable only when a separate deployment step has already applied and validated the same migrations |
| `SERVER_PORT` | `8080` |

Use a secret manager or protected runtime injection for credentials. Do not put
values in checked-in YAML, Docker build arguments, command-line arguments,
application logs, or committed environment files. Keep database URLs free of
embedded passwords. Required missing/invalid credentials must be corrected before
the service is put into traffic.

For Maven-based production startup, after exporting the required values:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

For a packaged application:

```sh
./mvnw -DskipTests package
SPRING_PROFILES_ACTIVE=prod java -jar target/chat-backend-0.0.1-SNAPSHOT.jar
```

Run the verification commands separately before deploying. These startup commands
connect to the configured database and may apply pending Flyway migrations.

## Database and Flyway deployment

Keep MySQL on a private network, reachable only by the application and authorized
operators. Do not publish port 3306 to public interfaces. Use a dedicated account
with only the required privileges and a JDBC URL appropriate for the deployment's
TLS policy. The existing local `chat` container publishes port 3306 on all host
interfaces; that development configuration must not be copied into production.
Phase 25 does not reconfigure that existing container.

No migration was added or changed for Phase 25. The original 13 tables are
preserved. Existing migrations are:

- `V2__create_refresh_tokens.sql`: adds the refresh-token table.
- `V3__add_message_pagination_index.sql`: adds the message pagination index.

The inspected existing `chat_db` contains **14 application tables**, including
`refresh_tokens`, plus `flyway_schema_history`. Its recorded history has a
**version 2 BASELINE** and a successful **V3** migration. Preserve this established
history. Do not rebaseline that database at version 1, delete its history, or
replay V2: its refresh-token table already exists.

The reviewed strategy for a different database containing only the **original 13
tables**, with no Flyway history and no refresh-token table, remains an explicit
**version 1 baseline**, followed by V2 and V3. A baseline records the already
existing schema; it does not create it. Never select a baseline version solely to
silence a migration error.

Deployment procedure:

1. Back up the database and confirm restore procedures. Review the actual schema
   and `flyway_schema_history` against the intended release. The schema snapshot
   at [`docs/schema/existing-chat-db.sql`](docs/schema/existing-chat-db.sql) is
   reference material for the original tables, not an executable startup script.
2. If history already exists, preserve and validate it. If onboarding a reviewed
   original 13-table schema without history, perform an explicit Flyway baseline
   operation at version 1 using deployment-provided credentials. For other schema
   states, reconcile history with the actual schema before deployment.
3. Start one migration-capable deployment instance with the `prod` profile. Flyway
   is enabled by default, validates migration checksums/history, and applies only
   pending migrations before Hibernate validates the entities. Provide the
   controlled database privileges required by the reviewed pending migrations.
4. Verify successful startup, schema validation, migration history, and health
   before sending traffic. Coordinate releases and backups; do not use destructive
   automatic rollback or recreate the schema on failure.

Alternatively, a controlled external Flyway deployment job may validate/apply the
same release migrations using separate migration privileges. Set
`FLYWAY_ENABLED=false` on the application only after that job succeeds and the
resulting schema/history has been verified. Hibernate validation still runs.

Both profiles keep `baseline-on-migrate=false`, `clean-disabled=true`, and
`validate-on-migrate=true`. `ddl-auto=validate` and `spring.sql.init.mode=never`
remain in force. Empty-database provisioning requires a separately reviewed schema
process; this repository deliberately has no table-recreating V1 migration.

## Firebase credentials

Firebase uses Google Application Default Credentials (ADC). In production, prefer
an attached workload identity where supported, or mount a service-account JSON
file from a secret store and set `GOOGLE_APPLICATION_CREDENTIALS` to its runtime
path. Mount files read-only and permit the runtime user to read them. The service
account needs the appropriate Firebase project permissions for FCM.

Do not commit JSON credentials or copy them into the image. Do not add a repository
classpath fallback. The application requires usable ADC to initialize Firebase;
a health request does not test actual FCM delivery. Local runs and application
context integration tests also require ADC. Keep developer credentials outside
the repository.

## Upload storage

`FileStorageService` remains the storage abstraction; `LocalFileStorageService`
stores file bytes on disk and existing database metadata stays in MySQL. There is
no S3 dependency or binary storage in MySQL.

Set `UPLOAD_DIR` to a persistent writable path in production. The directory is
created if needed; failure to initialize it prevents startup. Container storage
uses `/app/uploads` owned by UID/GID **10001:10001**. Use a persistent volume or a
bind mount with matching write permissions, and include uploads in the backup and
restore plan. A custom `UPLOAD_DIR` requires a corresponding persistent mount.
Existing returned `/uploads/...` references retain their format.

`uploads/` is ignored by Git and excluded from the Docker context. Previously
tracked runtime uploads are removed from version control while retained locally.
Do not stage runtime uploads even if a deployment uses a different directory.

## Browser origins and API documentation

HTTP API CORS applies to `/api/**`. Configure `CORS_ALLOWED_ORIGINS` with exact
origins such as `https://chat.example.com,https://admin.example.com`. Bearer headers
are supported; cookie credentials are not enabled. Local HTTP defaults allow
`http://localhost:3000` and `http://localhost:5173`.

Configure `/ws` WebSocket/STOMP origins separately with
`WEBSOCKET_ALLOWED_ORIGINS`. Local development retains its wildcard default.
Production defaults for both origin lists are empty, permitting no cross-origin
browser clients. Same-origin behavior remains available. Production rejects
wildcards and malformed origins at startup; specify scheme, hostname, and optional
port, without paths, queries, or fragments. Origin controls do not replace JWT or
conversation authorization. Native clients must still authenticate normally.

Swagger UI at `/swagger-ui/index.html` and OpenAPI at `/v3/api-docs` remain enabled
locally. In `prod`, both are disabled by default, and SecurityConfig denies access
to documentation routes even for authenticated callers. `SWAGGER_ENABLED=true`
explicitly enables both and permits access; use it only when documentation
exposure is intended and apply deployment-level access restrictions as needed.

## Health and production logging

Unauthenticated `GET /actuator/health` (and `HEAD`) is permitted for load balancers
and container health checks. Healthy responses expose only the aggregate status:

```json
{"status":"UP"}
```

The health endpoint includes the configured contributors, including the database.
It does not expose component names, database metadata, exception messages, or
stack traces. All other actuator paths are denied, only health is exposed over
HTTP, discovery is disabled, and actuator JMX exposure is disabled. This endpoint
reports application health; it is not an end-to-end test of messaging or FCM.

```sh
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
```

Production defaults to INFO application logging with reduced framework logging;
SQL parameter/value logging and HTTP request-detail logging are disabled. HTTP
errors suppress internal exception details. STOMP error frames use safe messages.
Do not enable request/header/body or SQL bind tracing in production: it may reveal
passwords, JWTs, refresh/access tokens, Firebase credentials, or FCM tokens.

## Docker

The multi-stage Dockerfile builds with the Maven wrapper on Java 21 and runs on a
Java 21 JRE as non-root UID/GID **10001:10001**. The build excludes tests and never
needs database or Firebase credentials; run the full suite before deployment.
`.dockerignore` allows only required build/application files into the context.

```sh
docker build --tag mychat-backend:phase25 .
docker network create mychat-private
docker volume create mychat-uploads
```

Connect a separately provisioned MySQL service to the private deployment network
without publishing its port. Export the environment values from the table before
running the following example. `FIREBASE_CREDENTIALS_FILE` is the absolute path of
a credential file outside the repository, readable by the container user. Variable
names pass existing values to Docker without placing secret literals in the
command:

```sh
docker run --detach --name mychat-backend \
  --network mychat-private \
  --publish 127.0.0.1:8080:8080 \
  --env DB_URL --env DB_USERNAME --env DB_PASSWORD --env JWT_SECRET \
  --env CORS_ALLOWED_ORIGINS --env WEBSOCKET_ALLOWED_ORIGINS \
  --env GOOGLE_APPLICATION_CREDENTIALS=/run/secrets/firebase.json \
  --mount type=bind,source="${FIREBASE_CREDENTIALS_FILE}",target=/run/secrets/firebase.json,readonly \
  --mount type=volume,source=mychat-uploads,target=/app/uploads \
  --restart unless-stopped \
  mychat-backend:phase25
```

For managed workload identity, omit the credential-file environment variable and
bind mount. Put a TLS-terminating reverse proxy/load balancer in front of the
application and forward WebSocket upgrade requests. The example binds HTTP to
localhost; adapt ingress routing to the deployment network. Restrict access to
the Docker daemon, which can inspect container environment values.

The image runs the production profile and probes `/actuator/health` every 30
seconds, with a 60-second startup grace period; three failures mark it unhealthy. The probe
uses `SERVER_PORT` when overridden. Check the reported container health:

```sh
docker inspect --format '{{.State.Health.Status}}' mychat-backend
```

## Security and verification

Keep credentials and runtime files outside version control. Existing secret
fallbacks have been removed; any credential previously stored in source/history
should be rotated through the appropriate external system. Rotation of
`JWT_SECRET` invalidates existing access tokens signed by the old key. Never
include token values in diagnostics, issue reports, or shell tracing.

Retain stateless Bearer JWT authentication, existing refresh-token behavior,
resource access checks, WebSocket authentication, and safe error responses. Run
servers with synchronized clocks for JWT expiry validation. Production deployment
must preserve database and upload backups and protect TLS connections.

Use Java 21 and run:

```sh
./mvnw --version
./mvnw clean compile
./mvnw test
```

The complete suite requires the configured MySQL database, `DB_PASSWORD`, and
Firebase ADC for application context startup. Tests generate JWT keys in memory;
they do not need the deployment signing key. Foundation checks read the original
13 entities plus refresh tokens without writing database rows. The 14 auth API
integration cases remain opt-in with `CHAT_WRITE_TESTS=true`; run those only
against an appropriate test database because they write test records. Their
existing default skip behavior is preserved.

The user-provided pre-Phase-25 baseline is **188 tests, 0 failures, 0 errors, 14
skipped**. Current compile/test, production startup, health, Swagger/origin checks,
and Docker verification evidence is recorded in
[`BACKEND_TASK.md`](BACKEND_TASK.md). Do not infer a successful deployment merely
from the example commands in this README.
