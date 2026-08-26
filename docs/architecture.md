# Rest API Template â€” Architecture Documentation

> **Preview diagrams:** `Ctrl+Shift+V` (Markdown Preview Enhanced) Â· PlantUML: `Alt+D` inside `.puml` files

---

## Table of Contents


---

## Overview

Layered REST API template built on **Javalin + Ebean + PostgreSQL**.

| Item | Value |
| Group ID | `batzorigt.rentsen` |
| Artifact ID | `rest-api-template` |
| Version | `1.0.0` |
| Java | 25 |
| Context path | `/v1/` |
| Default port | `8080` |

---

## C4 Level 1 â€” System Context

Highest-level system view.

```plantuml
@startuml C4_Context
!theme plain
title C4 Level 1 â€” System Context


database "PostgreSQL" as db #fff0cc

client --> http : "HTTP/JSON"
http --> security : "handlerWrapper +\nbefore filter"
http --> handlers : "route"
handlers --> services : "call"
services --> orm : "query"
orm --> db : "JDBC"
config ..> http : "configure"
monitoring ..> http : "instrument"

@enduml
```

---

## C4 Level 3 â€” Component (API Layer)

Components of the `rest.api` package.

```plantuml
@startuml C4_Component_API
!theme plain
title C4 Level 3 â€” API Layer Components


package "genre package" {
  component "GenreHandler\n(GET /genres â€” public\nPOST /genres â€” MANAGER+\nDELETE /genres/{id} â€” ADMIN)" as gh #d5e8d4
  component "GenreService\n(Business logic)" as gs #dce8ff
  component "GenreToAdd\n(Request DTO)" as gta #f5f5f5
  component "Genre\n(Response DTO)" as gdto #f5f5f5
  component "DGenre\n(@Entity genres)" as dg #ffe6cc
}

component "Domain\n(Base entity: id, createdAt, updatedAt)" as domain #ffcc99

mh --> ms : "calls"
ms --> dm : "persist/query"
ms --> mdto : "maps via MapStruct"
dm --> dp : "OneToMany"
dm --|> domain
dp --|> domain

gh --> gs : "calls"
gs --> dg : "query"
gs --> gdto : "maps via MapStruct"
dg --|> domain

@enduml
```

---

## Request Flow

Path of an HTTP request through the system.

```plantuml
@startuml RequestFlow
!theme plain
title Request Flow â€” POST /v1/members


actor "Client" as client
participant "Javalin\nHTTP Server" as server
participant "XSRFFilter\n(optional)" as xsrf
participant "MemberHandler" as handler
participant "Validators" as validator
participant "MemberService" as service
participant "Ebean ORM\n(DMember)" as orm
database "PostgreSQL" as db

client -> server : POST /v1/members\n{name, phones}
server -> xsrf : before()
xsrf --> server : 403 or continue
server -> handler : addMember(ctx)
handler -> validator : validate(ctx, MemberToAdd.class)
validator --> handler : MemberToAdd DTO\nor 400 Bad Request
handler -> service : addMember(input)
service -> orm : new DMember()\n.save()
orm -> db : INSERT INTO members
db --> orm : generated id
orm --> service : DMember entity
service --> handler : Member DTO\n(MapStruct)
handler --> server : 201 Created\n{id, name, phones, ...}
server --> client : HTTP 201 JSON

@enduml
```

Role-protected routes (`Role.*` args) pass through `Authorization` before the
handler runs: it authenticates the `secure-token` cookie (401 on
missing/invalid/expired) and compares the payload's `role` claim with the route
minimum (403 when insufficient). Public routes skip this step entirely.

---

## Security Architecture

```plantuml
@startuml Security
!theme plain
title Security Architecture


entity "phone_numbers" as phones {
  * id : INTEGER <<PK, AUTOINCREMENT>>
  --
  * member_id : INTEGER <<FK>>
  phone_no : VARCHAR
  is_home_phone_no : BOOLEAN
  created_at : TIMESTAMP NOT NULL
  updated_at : TIMESTAMP NOT NULL
}

entity "genres" as genres {
  * id : INTEGER <<PK, AUTOINCREMENT>>
  --
  name : VARCHAR(10) NOT NULL
  key : VARCHAR
  image_path : VARCHAR
  image_key : VARCHAR
  order_number : INTEGER
  created_at : TIMESTAMP NOT NULL
  updated_at : TIMESTAMP NOT NULL
}

members ||--o{ phones : "has many"

@enduml
```

---

## Package Structure

```
src/
â”œâ”€â”€ main/
â”‚   â”œâ”€â”€ java/rest/api/
â”‚   â”‚   â”œâ”€â”€ API.java                  # Entry point, Javalin config
â”‚   â”‚   â”œâ”€â”€ Config.java               # Configuration interface (Owner)
â”‚   â”‚   â”œâ”€â”€ Domain.java               # Base entity (id, createdAt, updatedAt)
â”‚   â”‚   â”œâ”€â”€ Authorization.java        # Route RBAC wrapper (handlerWrapper)
â”‚   â”‚   â”œâ”€â”€ Role.java                 # Role enum: USER < MANAGER < ADMIN
â”‚   â”‚   â”œâ”€â”€ Authentication.java       # Cookie-based auth filter
â”‚   â”‚   â”œâ”€â”€ XSRFFilter.java           # CSRF filter (cookie + header)
â”‚   â”‚   â”œâ”€â”€ XSRFToken.java            # XSRF token generator/validator
â”‚   â”‚   â”œâ”€â”€ SecureToken.java          # Encrypted secure token
â”‚   â”‚   â”œâ”€â”€ Crypto.java               # AES-GCM + HmacSHA256
â”‚   â”‚   â”œâ”€â”€ Base64.java               # URL-safe Base64
â”‚   â”‚   â”œâ”€â”€ ContextAttributes.java    # Context attribute key constants
â”‚   â”‚   â”œâ”€â”€ ContextHelpers.java       # HTTP request/response utilities
â”‚   â”‚   â”œâ”€â”€ Validators.java           # Jakarta Bean Validation
â”‚   â”‚   â”œâ”€â”€ ExceptionHandlers.java    # Global exception handlers
â”‚   â”‚   â”œâ”€â”€ PagedData.java            # Pagination response wrapper
â”‚   â”‚   â”œâ”€â”€ PagedSearch.java          # Pagination query logic
â”‚   â”‚   â”œâ”€â”€ NumberHelpers.java        # Numeric utilities
â”‚   â”‚   â”œâ”€â”€ I18N.java                 # Internationalization (Japanese-only bundle)
â”‚   â”‚   â”œâ”€â”€ IO.java                   # File/classpath read utilities
â”‚   â”‚   â”œâ”€â”€ Mail.java                 # Jakarta Mail SMTP sender
â”‚   â”‚   â”œâ”€â”€ TemplateEngines.java      # JTE engine factory
â”‚   â”‚   â”œâ”€â”€ Micrometer.java           # Prometheus metrics setup
â”‚   â”‚   â”œâ”€â”€ GenerateDbMigration.java  # DDL migration generator (dev only)
â”‚   â”‚   â”‚
â”‚   â”‚   â”œâ”€â”€ member/                   # Member feature
â”‚   â”‚   â”‚   â”œâ”€â”€ DMember.java          # @Entity â†’ members table
â”‚   â”‚   â”‚   â”œâ”€â”€ DPhone.java           # @Entity â†’ phone_numbers table
â”‚   â”‚   â”‚   â”œâ”€â”€ Member.java           # Response DTO + MapStruct Convertor
â”‚   â”‚   â”‚   â”œâ”€â”€ Phone.java            # Response DTO
â”‚   â”‚   â”‚   â”œâ”€â”€ MemberToAdd.java      # Request DTO
â”‚   â”‚   â”‚   â”œâ”€â”€ MemberHandler.java    # Route handler
â”‚   â”‚   â”‚   â”œâ”€â”€ MemberService.java    # Business logic
â”‚   â”‚   â”‚   â””â”€â”€ query/                # Generated QueryBeans (QDMember)
â”‚   â”‚   â”‚
â”‚   â”‚   â””â”€â”€ genre/                    # Genre feature
â”‚   â”‚       â”œâ”€â”€ DGenre.java           # @Entity â†’ genres table
â”‚   â”‚       â”œâ”€â”€ Genre.java            # Response DTO + MapStruct Convertor
â”‚   â”‚       â”œâ”€â”€ GenreToAdd.java       # Request DTO (POST /genres)
â”‚   â”‚       â”œâ”€â”€ GenreHandler.java     # Route handler
â”‚   â”‚       â”œâ”€â”€ GenreService.java     # Business logic
â”‚   â”‚       â””â”€â”€ query/                # Generated QueryBeans (QDGenre)
â”‚   â”‚
â”‚   â”‚
â”‚   â”œâ”€â”€ resources/
â”‚   â”‚   â”œâ”€â”€ application.properties    # Maven-filtered datasource config (DB_* env placeholders)
â”‚   â”‚   â”œâ”€â”€ i18n_ja.properties        # Messages â€” Japanese is the ONLY shipped bundle
â”‚   â”‚   â”œâ”€â”€ log4j2.xml                # Logging config
â”‚   â”‚   â”œâ”€â”€ jte/                      # JTE templates (hello.jte)
â”‚   â”‚   â””â”€â”€ dbmigration/              # Generated SQL migrations (created on demand by GenerateDbMigration)
â”‚   â”‚
â”‚   â””â”€â”€ jib/
â”‚       â”œâ”€â”€ ebean-agent-17.6.0.jar    # Ebean bytecode agent
â”‚       â”œâ”€â”€ log4j2.xml                # Docker logging config
â”‚       â””â”€â”€ jte-classes/              # Precompiled JTE templates
â”‚
â””â”€â”€ test/
    â”œâ”€â”€ java/rest/api/
    â”‚   â”œâ”€â”€ AuthorizationTest.java   # RBAC 401/403/allow matrix (HTTP)
    â”‚   â”œâ”€â”€ RoleTest.java            # Role hierarchy + parse unit tests
    â”‚   â”œâ”€â”€ SecureTokenTest.java     # Token gen/parse/expiry
    â”‚   â”œâ”€â”€ XSRFTokenTest.java       # XSRF token sign/validate
    â”‚   â”œâ”€â”€ CryptoTest.java          # AES-GCM encrypt/decrypt/sign
    â”‚   â”œâ”€â”€ AppConfigTest.java       # Owner config loading
    â”‚   â”œâ”€â”€ ContextHelpersTest.java  # Response helpers + query params
    â”‚   â”œâ”€â”€ PagedDataTest.java       # Pagination wrapper
    â”‚   â”œâ”€â”€ PagedSearchTest.java     # Paged vs all-data finder logic
    â”‚   â”œâ”€â”€ I18NTest.java            # i18n message resolution
    â”‚   â”œâ”€â”€ I18NJapaneseOnlyTest.java# Guards Japanese-only bundle contract
    â”‚   â”œâ”€â”€ IOTest.java              # Classpath/file read helpers
    â”‚   â”œâ”€â”€ TemplateEnginesTest.java # JTE dev/precompiled modes
    â”‚   â”œâ”€â”€ MailTest.java            # SMTP message building
    â”‚   â”œâ”€â”€ genre/                   # GenreHandlerTest, GenreServiceTest, DGenreTest
    â”‚   â””â”€â”€ member/                  # MemberHandlerTest, MemberServiceTest
    â””â”€â”€ resources/
        â”œâ”€â”€ application.properties   # Test DB config (Postgres Testcontainer :6433)
        â””â”€â”€ ...                      # i18n bundles for tests, log4j2.xml
```

---

## Technology Stack

| Category | Library | Version | Purpose |
|----------|---------|---------|---------|
| Web Framework | Javalin | 7.2.0 | HTTP server, routing |
| Web Server | Jetty | (bundled) | Embedded servlet container |
| ORM | Ebean | 17.6.0 | Database access, QueryBean |
| DB Driver | PostgreSQL JDBC | 42.7.11 | PostgreSQL connectivity |
| DB Migration | Ebean Migration | 14.3.0 | Schema versioning |
| Validation | Hibernate Validator | 9.1.0.Final | Bean validation (JSR-380) |
| DTO Mapping | MapStruct | 1.6.3 | Entity â†” DTO conversion |
| Configuration | Owner | 1.0.12 | Externalized config |
| Templates | JTE | 3.2.4 | Server-side HTML |
| Logging Facade | SLF4J | 2.0.17 | API used by Javalin/Ebean |
| Logging | Log4j2 | 2.26.0 | Async structured logging |
| Async Logging | LMAX Disruptor | 4.0.0 | Lock-free async log queue |
| Monitoring | Micrometer | 1.16.5 | Prometheus metrics |
| Mail API | Jakarta Mail | 2.1.5 | SMTP email API |
| Mail Provider | Angus Mail | 2.0.5 | Jakarta Mail implementation |
| JSON | Jackson | 2.21.3 | JSON serialization |
| JSON | org.json | 20251224 | JSON object building |
| Mobile Detect | mobiledetect | 1.1.1 | User-agent parsing |
| Utils | Commons Lang3 | 3.20.0 | String utilities |
| Utils | Commons Collections4 | 4.5.0 | Collection utilities |
| Code Gen | Lombok | 1.18.46 | Getters/Setters/Builders |
| Testing | JUnit Jupiter | 6.0.3 | Unit testing |
| Testing | Mockito | 5.23.0 | Mocking |
| Testing | JMockit | 1.50 | Alternative mocking |
| Testing | Ebean Test | 17.6.0 | DB testing support |
| Testing | Testcontainers | (via Ebean) | Docker-based DB tests |
| Testing | Unirest | 3.14.5 | HTTP client for tests |

---

## API Endpoints

| Method | Path | Description | Transaction | Roles |
|--------|------|-------------|-------------|-------|
| `GET` | `/v1/genres` | List genres (paginated) | `readOnly` | public |
| `POST` | `/v1/genres` | Add genre | `readWrite` | `MANAGER+` |
| `DELETE` | `/v1/genres/{id}` | Delete genre | `readWrite` | `ADMIN` |
| `GET` | `/v1/members/{id}` | Find member by ID | `readOnly` | `USER+` (authenticated) |
| `POST` | `/v1/members` | Create member (registration) | `readWrite` | public |
| `GET` | `/v1/metrics` | Prometheus metrics | â€” | Basic auth (monitoring) |

Roles column = minimum role enforced by the `Authorization` wrapper (`USER < MANAGER < ADMIN`, higher satisfies lower). Missing/invalid token â†’ `401` on role-guarded routes.

### Pagination Query Params

| Param | Type | Description |
|-------|------|-------------|
| `pageNumber` | `Integer` | Page number (1-based); invalid/non-positive â†’ `400` |
| `recordsPerPage` | `Integer` | Rows per page, capped at `10`; invalid/non-positive â†’ `400`. Only when both params are absent does the endpoint return all rows |

> âš ï¸ **Warning:** a param-less `GET` calls AllDataFinder. When writing an AllDataFinder, consider whether pulling every row fits memory; latency/DoS risk. Each such fetch logs a runtime `WARN` (`PagedSearch`). Production clients should always pass pagination params.
>
> To forbid full fetch, pass `null` as the `allDataFinder` argument of `PagedSearch.search(...)` â€” unpaginated calls then get `403 Forbidden` (Â«Find all is not allowed!Â»).

### Error Responses

| Status | Description |
|--------|-------------|
| `400` | Validation error â€” field-level JSON errors |
| `401` | Missing/invalid/expired `secure-token` (role-guarded routes) |
| `403` | Invalid XSRF token **or** insufficient role (RBAC) |
| `404` | Data not found |
| `500` | Internal system error |

---

## Build & Deployment

```plantuml
@startuml Build
!theme plain
title Build & Deployment Pipeline


rectangle "Artifacts" as artifacts #fff0cc {
  rectangle "target/rest-api-template-1.0.0.jar" as jar
  rectangle "target/lib/*.jar" as libs
  rectangle "ebean-agent.jar\n(downloaded in-image)" as agent
}

rectangle "Optimized Runtime" as deploy #f8cecc {
  rectangle "jlink custom JRE\n(jdeps-computed modules)" as jre
  rectangle "app-cds.jsa\n(AppCDS dump stage)" as cds
  rectangle "Final image\n(bellsoft/alpaquita-linux-base:musl)\nnon-root user + healthcheck" as docker
}

src --> maven
maven --> artifacts
artifacts --> jre : "jdeps + jlink"
artifacts --> cds : "-Xshare:dump"
jre --> docker
cds --> docker

note right of docker
  Multi-stage Dockerfile:
  build â†’ jdk-download â†’ jlink JRE
  â†’ AppCDS dump â†’ runtime
  JVM flags:
  -javaagent:ebean-agent.jar
  -XX:+UseZGC -Xshare:on
  -Dlog4j2.contextSelector=AsyncLogger
end note

@enduml
```

### Quick Start

Build/run/docker commands: see README.md (canonical quickstart).
---

## Testing Strategy

| Layer | Tooling | Notes |
|---------|---------|---------|
| Unit tests | JUnit 5 + Mockito | Service/utility tests (`RoleTest`, etc.) |
| Integration tests | JUnit 5 + Ebean Test | Tests against a real DB |
| Container tests | Testcontainers | Dockerized PostgreSQL |
| HTTP tests | Unirest | API endpoint tests (`AuthorizationTest` â€” RBAC 401/403/allow matrix) |
| Mocking | Mockito / JMockit | External dependency isolation |

Test DB config (`src/test/resources/application.properties`):

```properties
ebean.test.platform   = postgres
ebean.test.port       = 6433
ebean.test.ddlMode    = dropCreate
ebean.test.dbName     = test
ebean.test.useDocker  = true
```

---

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `portNo` | `8080` | Server port |
| `contextPath` | `/v1/` | API root path |
| `allowedOrigins` | `http://localhost:8080, http://localhost:4200, http://localhost:4201, http://batzorigt.com:4200` | CORS allow-host list |
| `encryptionKey` | `1234567890123456` | AES key (â‰¥16 chars) |
| `xsrfProtectionEnabled` | `false` | Enable the XSRF filter |
| `isSecure` | `false` | HTTPS cookie flag |
| `httpMaxRequestSize` | `1024` | Max request bytes |
| `httpAsyncTimeout` | `5000` | Async timeout ms |
| `requestLoggingEnabled` | `false` | HTTP request log via Javalin's native request logger (method/path/status/duration; no headers or bodies). Silence per environment with log4j2: `<Logger name="rest.api.RequestLogger" level="warn"/>` |
| `requestLoggingVerbose` | `false` | Adds safe headers, `REDACTED(len=N)` for Cookie/Authorization, and body size to the HTTP log â€” token and body contents are never logged |
| `monitoringUsername` | `micro` | Metrics basic auth |
| `monitoringPassword` | `meter` | Metrics basic auth |
| `environment` | `local` | `local` = JTE dev mode + dev logging; non-local refuses the default encryptionKey at startup |
| `jteClassesDir` | `jte-classes` | Precompiled JTE class dir |
| `smtpHost` | `smtp.gmail.com` | Mail server |
| `smtpPort` | `587` | Mail port |
| `smtpUsername` | `any@email.address` | SMTP auth user |
| `smtpPassword` | `anypassword` | SMTP auth password |
| `smtpAuth` | `true` | SMTP auth on/off |
| `smtpStartTls` | `true` | STARTTLS on/off |
