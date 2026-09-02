# Rest API Template — Architecture Documentation

> **Preview diagrams:** `Ctrl+Shift+V` (Markdown Preview Enhanced) · PlantUML: `Alt+D` inside `.puml` files

---

## Table of Contents

1. [Overview](#overview)
2. [C4 Level 1 — System Context](#c4-level-1--system-context)
3. [C4 Level 2 — Container](#c4-level-2--container)
4. [C4 Level 3 — Component (API Layer)](#c4-level-3--component-api-layer)
5. [C4 Level 3 — Component (Domain Layer)](#c4-level-3--component-domain-layer)
6. [Request Flow](#request-flow)
7. [Security Architecture](#security-architecture)
8. [Database Schema](#database-schema)
9. [Package Structure](#package-structure)
10. [Technology Stack](#technology-stack)
11. [API Endpoints](#api-endpoints)
12. [Build & Deployment](#build--deployment)
13. [Testing Strategy](#testing-strategy)

---

## Overview

Layered REST API template built on **Javalin + Ebean + PostgreSQL**.

| Item         | Value               |
|--------------|---------------------|
| Group ID     | `batzorigt.rentsen` |
| Artifact ID  | `rest-api-template` |
| Version      | `1.0.0`             |
| Java         | `25`                |
| Context path | `/v1/`              |
| Default port | `8080`              |

---

## C4 Level 1 — System Context

Highest-level system view.

```plantuml
@startuml C4_Context
!theme plain
title C4 Level 1 — System Context

skinparam rectangle {
  BorderColor #666666
  BackgroundColor #f5f5f5
}
skinparam actor {
  BorderColor #0050ef
  BackgroundColor #dce8ff
}
skinparam ArrowColor #444444

actor "API Client\n(Browser / Mobile)" as user #dce8ff
actor "Monitoring System\n(Prometheus)" as prometheus #fff0cc

rectangle "Rest API Template\n[Java 25 / Javalin]" as system #d5e8d4

database "PostgreSQL\n[Database]" as db #ffe6cc

rectangle "SMTP Server\n(Gmail)" as smtp #f8cecc

user --> system : "HTTP/JSON\nREST calls"
system --> db : "JDBC\nSQL queries"
prometheus --> system : "Scrape /metrics\n(Basic Auth)"
system --> smtp : "SMTP\nEmail notifications"

@enduml
```

---

## C4 Level 2 — Container

Internal containers of the system.

```plantuml
@startuml C4_Container
!theme plain
title C4 Level 2 — Container Diagram

skinparam rectangle {
  BorderColor #666666
  BackgroundColor #f9f9f9
}
skinparam component {
  BorderColor #0050ef
  BackgroundColor #dce8ff
}
skinparam database {
  BorderColor #d6a520
  BackgroundColor #fff0cc
}
skinparam ArrowColor #444444

actor "API Client" as client #dce8ff

rectangle "Rest API Template" as boundary {

  component "HTTP Server\n[Javalin 7.2 / Jetty]\nRouting, filters,\nrequest lifecycle" as http #d5e8d4

  component "Security Layer\n[Custom Java]\nAuth, XSRF, Crypto,\nSecureToken" as security #f8cecc

  component "API Handlers\n[Java classes]\nMemberHandler\nGenreHandler" as handlers #dce8ff

  component "Business Services\n[Java interfaces]\nMemberService\nGenreService" as services #dce8ff

  component "Data Access\n[Ebean ORM 17.6]\nQueryBean, DDL,\nMigration" as orm #ffe6cc

  component "Configuration\n[Owner 1.0.12]\nEnv vars + file" as config #e1d5e7

  component "Monitoring\n[Micrometer + Prometheus]\n/metrics endpoint" as monitoring #f5f5f5

  component "Template Engine\n[JTE 3.2]\nServer-side HTML" as jte #f5f5f5

  component "Mail Service\n[Jakarta Mail]\nSMTP email" as mail #f5f5f5

}

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

## C4 Level 3 — Component (API Layer)

Components of the `rest.api` package.

```plantuml
@startuml C4_Component_API
!theme plain
title C4 Level 3 — API Layer Components

skinparam component {
  BorderColor #0050ef
  BackgroundColor #dce8ff
}
skinparam ArrowColor #444444

component "API\n(Entry point)" as api #d5e8d4
component "Config\n(Owner interface)" as config #e1d5e7
component "Authorization\n(Route RBAC wrapper)" as authz #f8cecc
component "Role\n(USER < MANAGER < ADMIN)" as role #f8cecc
component "Authentication\n(Cookie token auth)" as auth #f8cecc
component "XSRFFilter\n(CSRF protection)" as xsrf #f8cecc
component "ExceptionHandlers\n(Global error handling)" as ex #f8cecc
component "ContextHelpers\n(Request utilities)" as ctx #f5f5f5
component "Validators\n(Hibernate Validator)" as val #f5f5f5
component "PagedSearch\n(Pagination logic)" as paged #f5f5f5
component "I18N\n(i18n messages)" as i18n #f5f5f5
component "Micrometer\n(Metrics)" as micro #f5f5f5
component "Crypto\n(AES-GCM + HmacSHA256)" as crypto #ffcc99
component "SecureToken\n(Token gen/validate)" as token #ffcc99
component "XSRFToken\n(XSRF token gen)" as xtoken #ffcc99
component "Mail\n(SMTP sender)" as mail #f5f5f5
component "TemplateEngines\n(JTE factory)" as tmpl #f5f5f5

api --> config : "reads"
api --> authz : "router.handlerWrapper"
api --> xsrf : "registers (optional)"
api --> ex : "registers"
api --> micro : "registers"

authz --> role : "checks claim"
authz --> auth : "authenticates"
auth --> token : "uses"
xsrf --> xtoken : "uses"
token --> crypto : "uses"
xtoken --> crypto : "uses"

@enduml
```

---

## C4 Level 3 — Component (Domain Layer)

Components of the `member` and `genre` packages.

```plantuml
@startuml C4_Component_Domain
!theme plain
title C4 Level 3 — Domain Layer Components

skinparam component {
  BorderColor #0050ef
  BackgroundColor #dce8ff
}
skinparam ArrowColor #444444

package "member package" {
  component "MemberHandler\n(POST /members — public\nGET /members/{id} — USER+)" as mh #d5e8d4
  component "MemberService\n(Business logic)" as ms #dce8ff
  component "MemberToAdd\n(Request DTO)" as mta #f5f5f5
  component "Member\n(Response DTO)" as mdto #f5f5f5
  component "Phone\n(Response DTO)" as pdto #f5f5f5
  component "DMember\n(@Entity members)" as dm #ffe6cc
  component "DPhone\n(@Entity phone_numbers)" as dp #ffe6cc
}

package "genre package" {
  component "GenreHandler\n(GET /genres — public\nPOST /genres — MANAGER+\nDELETE /genres/{id} — ADMIN)" as gh #d5e8d4
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
title Request Flow — POST /v1/members

skinparam sequence {
  ArrowColor #444444
  ActorBorderColor #0050ef
  LifeLineBorderColor #aaaaaa
  ParticipantBackgroundColor #f5f5f5
  ParticipantBorderColor #666666
}

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

skinparam component {
  BorderColor #cc0000
  BackgroundColor #f8cecc
}
skinparam ArrowColor #444444

component "Authentication" as auth
component "Authorization\n(Route RBAC)" as authz
component "Role\n(USER < MANAGER < ADMIN)" as role
component "XSRFFilter" as xsrf
component "SecureToken" as st
component "XSRFToken" as xt
component "Crypto" as crypto

note right of crypto
  AES-256-GCM encryption
  HmacSHA256 signing
  Keys derived via HKDF-SHA256 (purpose-separated enc/sig)
end note

authz --> st : parse token\n(30 min timeout)
authz --> role : resolve claim,\ncompare with route minimum
auth --> st : parse token\n(30 min timeout)
xsrf --> xt : validate token\n(30 min timeout)
st --> crypto : encrypt / decrypt\nverify signature
xt --> crypto : sign / verify

note bottom of authz
  Wraps every endpoint (config.router.handlerWrapper)
  Route without Role args -> public, no checks
  Missing/invalid/expired token -> 401
  role claim below route minimum -> 403
end note

note bottom of auth
  Cookie: "secure-token"
  Sets member in ctx.attribute
end note

note bottom of xsrf
  GET: generate + set cookie "xsrf-token"
  POST/PUT/DELETE/PATCH: validate
  header "x-xsrf-token" == cookie
end note

@enduml
```

### Security Headers (every response)

| Header | Value |
|--------|-------|
| `Strict-Transport-Security` | `max-age=63072000; includeSubDomains; preload` |
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Content-Security-Policy` | `frame-ancestors 'none'; default-src 'self' style-src 'self' 'unsafe-inline';` |
| `Cache-Control` | `no-store` |
| `X-XSS-Protection` | `1; mode=block` |
| `Referrer-Policy` | `no-referrer` |
| `Cross-Origin-Resource-Policy` | `same-origin` |
| `Feature-Policy` | `none` |

### Authorization (RBAC)

`rest.api.Authorization` wraps every endpoint via `config.router.handlerWrapper(Authorization::wrap)` and enforces the roles declared on the route.

- **Role hierarchy:** `USER(0) < MANAGER(10) < ADMIN(20)` — user level must be ≥ the route minimum.
- **Role source:** `role` claim in the `secure-token` cookie's JSON payload (`rest.api.Role.claim`); missing/unknown → `USER`.
- **Public routes:** no role args = public, no checks.
- **Responses:** missing/invalid/expired token → `401`; insufficient role → `403`.

Current route matrix:

| Route | Minimum role |
|-------|--------------|
| `POST /v1/members` | public (registration) |
| `GET /v1/members/{id}` | `USER` (any authenticated) |
| `GET /v1/genres` | public |
| `POST /v1/genres` | `MANAGER` |
| `DELETE /v1/genres/{id}` | `ADMIN` |

---

## Database Schema

```plantuml
@startuml Database
!theme plain
title Database Schema

skinparam class {
  BorderColor #d6a520
  BackgroundColor #fff0cc
  HeaderBackgroundColor #ffe6a0
}
skinparam ArrowColor #444444

entity "members" as members {
  * id : INTEGER <<PK, AUTOINCREMENT>>
  --
  name : VARCHAR(10) NOT NULL
  sex : INTEGER
  created_at : TIMESTAMP NOT NULL
  updated_at : TIMESTAMP NOT NULL
}

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
├── main/
│   ├── java/rest/api/
│   │   ├── API.java                  # Entry point, Javalin config
│   │   ├── Config.java               # Configuration interface (Owner)
│   │   ├── Domain.java               # Base entity (id, createdAt, updatedAt)
│   │   ├── Authorization.java        # Route RBAC wrapper (handlerWrapper)
│   │   ├── Role.java                 # Role enum: USER < MANAGER < ADMIN
│   │   ├── Authentication.java       # Cookie-based auth filter
│   │   ├── XSRFFilter.java           # CSRF filter (cookie + header)
│   │   ├── XSRFToken.java            # XSRF token generator/validator
│   │   ├── SecureToken.java          # Encrypted secure token
│   │   ├── Crypto.java               # AES-GCM + HmacSHA256
│   │   ├── Base64.java               # URL-safe Base64
│   │   ├── ContextAttributes.java    # Context attribute key constants
│   │   ├── ContextHelpers.java       # HTTP request/response utilities
│   │   ├── Validators.java           # Jakarta Bean Validation
│   │   ├── ExceptionHandlers.java    # Global exception handlers
│   │   ├── PagedData.java            # Pagination response wrapper
│   │   ├── PagedSearch.java          # Pagination query logic
│   │   ├── NumberHelpers.java        # Numeric utilities
│   │   ├── I18N.java                 # Internationalization (Japanese-only bundle)
│   │   ├── IO.java                   # File/classpath read utilities
│   │   ├── Mail.java                 # Jakarta Mail SMTP sender
│   │   ├── TemplateEngines.java      # JTE engine factory
│   │   ├── Micrometer.java           # Prometheus metrics setup
│   │   ├── GenerateDbMigration.java  # DDL migration generator (dev only)
│   │   │
│   │   ├── member/                   # Member feature
│   │   │   ├── DMember.java          # @Entity → members table
│   │   │   ├── DPhone.java           # @Entity → phone_numbers table
│   │   │   ├── Member.java           # Response DTO + MapStruct Convertor
│   │   │   ├── Phone.java            # Response DTO
│   │   │   ├── MemberToAdd.java      # Request DTO
│   │   │   ├── MemberHandler.java    # Route handler
│   │   │   ├── MemberService.java    # Business logic
│   │   │   └── query/                # Generated QueryBeans (QDMember)
│   │   │
│   │   └── genre/                    # Genre feature
│   │       ├── DGenre.java           # @Entity → genres table
│   │       ├── Genre.java            # Response DTO + MapStruct Convertor
│   │       ├── GenreToAdd.java       # Request DTO (POST /genres)
│   │       ├── GenreHandler.java     # Route handler
│   │       ├── GenreService.java     # Business logic
│   │       └── query/                # Generated QueryBeans (QDGenre)
│   │
│   │
│   ├── resources/
│   │   ├── application.properties    # Maven-filtered datasource config (DB_* env placeholders)
│   │   ├── i18n_ja.properties        # Messages — Japanese is the ONLY shipped bundle
│   │   ├── log4j2.xml                # Logging config
│   │   ├── jte/                      # JTE templates (hello.jte)
│   │   └── dbmigration/              # Generated SQL migrations (created on demand by GenerateDbMigration)
│   │
│   └── jib/
│       ├── ebean-agent-17.6.0.jar    # Ebean bytecode agent
│       ├── log4j2.xml                # Docker logging config
│       └── jte-classes/              # Precompiled JTE templates
│
└── test/
    ├── java/rest/api/
    │   ├── AuthorizationTest.java   # RBAC 401/403/allow matrix (HTTP)
    │   ├── RoleTest.java            # Role hierarchy + parse unit tests
    │   ├── SecureTokenTest.java     # Token gen/parse/expiry
    │   ├── XSRFTokenTest.java       # XSRF token sign/validate
    │   ├── CryptoTest.java          # AES-GCM encrypt/decrypt/sign
    │   ├── AppConfigTest.java       # Owner config loading
    │   ├── ContextHelpersTest.java  # Response helpers + query params
    │   ├── PagedDataTest.java       # Pagination wrapper
    │   ├── PagedSearchTest.java     # Paged vs all-data finder logic
    │   ├── I18NTest.java            # i18n message resolution
    │   ├── I18NJapaneseOnlyTest.java# Guards Japanese-only bundle contract
    │   ├── IOTest.java              # Classpath/file read helpers
    │   ├── TemplateEnginesTest.java # JTE dev/precompiled modes
    │   ├── MailTest.java            # SMTP message building
    │   ├── genre/                   # GenreHandlerTest, GenreServiceTest, DGenreTest
    │   └── member/                  # MemberHandlerTest, MemberServiceTest
    └── resources/
        ├── application.properties   # Test DB config (Postgres Testcontainer :6433)
        └── ...                      # i18n bundles for tests, log4j2.xml
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
| DTO Mapping | MapStruct | 1.6.3 | Entity ↔ DTO conversion |
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
| `GET` | `/v1/metrics` | Prometheus metrics | — | Basic auth (monitoring) |

Roles column = minimum role enforced by the `Authorization` wrapper (`USER < MANAGER < ADMIN`, higher satisfies lower). Missing/invalid token → `401` on role-guarded routes.

### Pagination Query Params

| Param | Type | Description |
|-------|------|-------------|
| `pageNumber` | `Integer` | Page number (1-based); invalid/non-positive → `400` |
| `recordsPerPage` | `Integer` | Rows per page, capped at `10`; invalid/non-positive → `400`. Only when both params are absent does the endpoint return all rows |

> ⚠️ **Warning:** a param-less `GET` calls AllDataFinder. When writing an AllDataFinder, consider whether pulling every row fits memory; latency/DoS risk. Each such fetch logs a runtime `WARN` (`PagedSearch`). Production clients should always pass pagination params.
>
> To forbid full fetch, pass `null` as the `allDataFinder` argument of `PagedSearch.search(...)` — unpaginated calls then get `403 Forbidden` («Find all is not allowed!»).

### Error Responses

| Status | Description |
|--------|-------------|
| `400` | Validation error — field-level JSON errors |
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

skinparam rectangle {
  BorderColor #666666
  BackgroundColor #f5f5f5
}
skinparam ArrowColor #444444

rectangle "Source Code" as src #dce8ff

rectangle "Maven Build" as maven #d5e8d4 {
  rectangle "Lombok\nannotation processing" as lombok
  rectangle "MapStruct\nmapper generation" as mapstruct
  rectangle "Ebean Plugin\nQueryBean + DDL" as ebean
  rectangle "JTE Plugin\ntemplate precompile" as jte
}

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
  build → jdk-download → jlink JRE
  → AppCDS dump → runtime
  JVM flags:
  -javaagent:ebean-agent.jar
  -XX:+UseZGC -Xshare:on
  -Dlog4j2.contextSelector=AsyncLogger
end note

@enduml
```

### Quick Start

```bash
# Build
mvn package            # or
build.bat              # Windows (includes AppCDS)

# Run
run.bat                # Windows (optimal JVM flags)
run.sh                 # Linux/macOS

# Docker
docker build -t rest-api-template .          # docker
podman build --format docker -t rest-api-template .   # podman (--format is a podman flag)
docker run -p 8080:8080 \
  -e DB_HOST_NAME=host.docker.internal \
  -e DB_PASSWORD=password \
  rest-api-template
```

---

## Testing Strategy

| Layer | Tooling | Notes |
|---------|---------|---------|
| Unit tests | JUnit 5 + Mockito | Service/utility tests (`RoleTest`, etc.) |
| Integration tests | JUnit 5 + Ebean Test | Tests against a real DB |
| Container tests | Testcontainers | Dockerized PostgreSQL |
| HTTP tests | Unirest | API endpoint tests (`AuthorizationTest` — RBAC 401/403/allow matrix) |
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
| `encryptionKey` | `1234567890123456` | AES key (≥16 chars) |
| `xsrfProtectionEnabled` | `false` | Enable the XSRF filter |
| `isSecure` | `false` | HTTPS cookie flag |
| `httpMaxRequestSize` | `1024` | Max request bytes |
| `httpAsyncTimeout` | `5000` | Async timeout ms |
| `requestLoggingEnabled` | `false` | HTTP request log via Javalin's native request logger (method/path/status/duration; no headers or bodies). Silence per environment with log4j2: `<Logger name="rest.api.RequestLogger" level="warn"/>` |
| `requestLoggingVerbose` | `false` | Adds safe headers, `REDACTED(len=N)` for Cookie/Authorization, and body size to the HTTP log — token and body contents are never logged |
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
