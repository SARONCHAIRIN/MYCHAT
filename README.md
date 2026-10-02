# Chat backend

Java 21, Maven, Spring Boot 4.1.1, and the existing MySQL `chat_db` schema for a
Flutter chat client. Phase 1 is complete with 13 JPA entities, 13 repositories,
and 11 enums. Phase 3 adds stateless Bearer JWT security infrastructure.
Phase 2's shared API response/error contracts remain unimplemented, as do the
Phase 4 authentication endpoints and later business/real-time APIs.

[`BACKEND_TASK.md`](BACKEND_TASK.md) records phase status, scope, and actual
verification results.

## Run locally

1. Select a Java 21 JDK with `JAVA_HOME` and confirm `./mvnw --version` reports it.
   On this Mac the installed JDK is
   `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`.
2. Start Docker Desktop and the existing MySQL container (`docker start chat`).
3. Supply the environment variables below in your shell or IDE run configuration.
4. Run:

   ```sh
   ./mvnw clean compile
   ./mvnw test
   ./mvnw spring-boot:run
   ```

| Variable | Default / purpose |
| --- | --- |
| `DB_PASSWORD` | Required MySQL password; no checked-in default |
| `DB_USERNAME` | `root` for the existing local development database |
| `DB_URL` | Local MySQL on port 3306, database `chat_db`, UTC session |
| `JWT_SECRET` | Required for application startup: Base64-encoded cryptographically random signing key containing at least 32 decoded bytes; no default |
| `JWT_ISSUER` | `chat-backend`; must be nonblank without surrounding whitespace |
| `JWT_ACCESS_TOKEN_TTL` | `15m`; a positive duration containing a whole number of seconds |
| `SERVER_PORT` | `8080` |

The default JDBC URL disables TLS for localhost and permits public-key retrieval
for local MySQL authentication. Override `DB_URL` with the appropriate connection
and TLS settings outside local development. Keep the UTC connection/session
settings when overriding the URL. Environment variables must be exported or set
in the IDE; Spring Boot does not automatically read a `.env` file.

Keep the signing key outside source control and logs. Retain the same key across
application restarts when existing access tokens should remain usable; replacing
it invalidates tokens signed with the previous key. Missing, malformed, or short
signing keys fail application startup. Tests generate their own keys in memory,
so `JWT_SECRET` is not needed for the test commands below.

Startup must connect to MySQL and complete Hibernate `ddl-auto=validate`. It does
not create or alter tables. SQL initialization and Flyway are disabled. Custom
JWT protection replaces default Spring Security login behavior. OpenAPI
auto-exposure is disabled until Phase 5.

## Security foundation

Supply access tokens through `Authorization: Bearer <access-token>`. The JWT
service uses only HS256, validates the signature, and requires these claims:

| Claim | Requirement |
| --- | --- |
| `sub` | Canonical positive decimal user ID within the Java `Long` range |
| `iss` | Exact configured issuer |
| `iat` | Issue time, required and no later than the current time |
| `exp` | Expiry time, required and later than both the issue time and current time |
| `token_type` | Exactly `access` |

Token lifetime cannot exceed the configured access-token TTL. Validation uses
zero clock skew; servers must keep their clocks synchronized. JWT timestamps use
whole-second precision. The service can issue tokens internally, but no HTTP
login or token-issuance endpoint exists yet.

Each authenticated request reloads the user from MySQL by the verified subject
ID. Deleted or unknown users are rejected. The principal contains the current
database username and ID, and credentials are erased after authentication.
The schema has no global roles, so principals receive no invented authorities;
conversation membership roles remain resource-specific. `ONLINE`/`OFFLINE`
indicates presence and does not disable accounts.

The filter chain accepts credentials only from the explicit Bearer header.
Sessions, HTTP Basic, form login, cookies, and query parameters do not authenticate
requests. CSRF protection is disabled for this stateless header-based mechanism;
any future cookie-based authentication must revisit that decision. Duplicate or
malformed Authorization headers are rejected, including invalid Bearer tokens
supplied on public routes.

Only these method/path combinations permit unauthenticated requests:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`

These are access rules for future Phase 4 endpoints; the endpoints themselves
are not implemented. Every other client request requires authentication,
including other methods on these paths, `/api/v1/auth/me`, and
`/api/v1/auth/logout`. Internal servlet `ERROR` dispatches are permitted so
container errors retain their status; direct requests to `/error` remain
protected.

Security failures return JSON with `success: false`, `code`, `message`, and an
ISO 8601 UTC `timestamp`. A 401 uses `UNAUTHORIZED` / `Authentication required`
and a `WWW-Authenticate: Bearer` header; a 403 uses `FORBIDDEN` / `Access denied`.
These responses exclude credentials, claims, exception details, and stack
traces. A security-local writer provides this contract while Phase 2's shared
MVC response and exception handling remain pending.

A database-backed authentication manager and BCrypt password encoder with cost
12 are available for Phase 4. Registration, login, refresh-token storage/rotation,
logout, and per-token revocation are not implemented. Existing passwords and
database records are unchanged.

## Schema and mapping decisions

[`docs/schema/existing-chat-db.sql`](docs/schema/existing-chat-db.sql) is a
schema-only snapshot read from the existing MySQL container. It is reference
material, not a startup script or Flyway migration. Do not execute it against the
existing `chat_db`.

- Uppercase Java enum constants use JPA 3.2 `@EnumeratedValue` to read/write the
  existing lowercase MySQL enum literals.
- Foreign-key relationships are lazy. No cascade persistence/removal or
  bidirectional collections are introduced; existing database constraints remain
  authoritative.
- MySQL owns generated creation/update timestamps. Hibernate retrieves them after
  writes. `DATETIME` and `TIMESTAMP` map to `LocalDateTime`, interpreted as UTC;
  JDBC connection/session and Hibernate time zones are configured accordingly.
- Nullable flags use `Boolean`. Java initializers match the existing enum and
  flag defaults while still allowing SQL nulls where the schema permits them.
- Unsigned `INT` attachment dimensions/duration use `Long` to retain their full
  range. Unsigned `BIGINT` identifiers/file sizes use `Long`, supporting values
  through `Long.MAX_VALUE`; larger values would require a coordinated mapping
  and API change.
- Text columns map to strings with the existing MySQL `TEXT` type. Entities are
  persistence objects and are not REST response contracts.

## Verification

Run the full suite under Java 21:

```sh
./mvnw clean compile
./mvnw test
```

The full suite requires the configured existing MySQL database and
`DB_PASSWORD`, plus `DB_USERNAME`/`DB_URL` overrides when needed. Tests generate
random JWT keys in memory, so no deployment signing key is required.

The Phase 1 integration tests run read-only transactions: no test rows are
inserted, updated, or deleted. They validate application context startup, all
13 entity/repository registrations, bounded entity reads, and every native enum
column against the Java enum values. Hibernate validation does not
comprehensively verify foreign keys, indexes, defaults, or enum members; the
schema reference and explicit enum checks cover the corresponding Phase 1 review.

Run only the security tests without MySQL or `DB_PASSWORD`:

```sh
./mvnw -Dtest=JwtServiceTest,CustomUserDetailsServiceTest,SecurityIntegrationTest test
```

Security tests cover JWT signatures/claims/expiry/configuration, identity and
credential handling, the production filter chain's public/protected rules,
stateless requests, safe 401/403 responses, and BCrypt authentication. Their
controllers and mocked repositories exist only in test code. See
[`BACKEND_TASK.md`](BACKEND_TASK.md) for executed commands and results; the commands
here describe how to verify the project.

## Flyway baseline strategy

Flyway stays disabled, and `baseline-on-migrate` stays false. No schema
history table or baseline is created automatically.

Before enabling migrations in a later phase:

1. Back up the existing database and compare its schema with the reviewed
   reference snapshot.
2. Explicitly baseline that existing schema at version **1** using Flyway's
   baseline operation with deployment-provided credentials. This records the
   baseline without recreating application tables.
3. Add future changes as `V2__description.sql`, `V3__description.sql`, and so on
   under `src/main/resources/db/migration/`, then enable Flyway and validate the
   history before migration.
4. Keep `ddl-auto=validate`. Provisioning a new empty database needs a separately
   reviewed initial-schema process; do not apply a table-creating V1 migration to
   the existing database.

The recommended next work is the pending **Phase 2 — API Foundation**, before
starting **Phase 4 — Authentication API**. No later phase starts automatically.
