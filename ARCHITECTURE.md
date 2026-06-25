# Rest API Template — Architecture Documentation

> **Диаграм харах:** `Ctrl+Shift+V` → Markdown Preview Enhanced  
> **PlantUML тусдаа харах:** `.puml` файл дотор `Alt+D`

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

`rest-api-template` нь **Javalin + Ebean + PostgreSQL** дээр суурилсан layered REST API template.

| Зүйл | Утга |
|------|------|
| Group ID | `batzorigt.rentsen` |
| Artifact ID | `rest-api-template` |
| Version | `1.0.0` |
| Java | 25 |
| Context path | `/v1/` |
| Default port | `8080` |

---

## C4 Level 1 — System Context

Системийн хамгийн дээд түвшний дүр зураг.

```plantuml
@startuml C4_Context
!theme plain
skinparam rectangle {
  BorderColor #666666
  BackgroundColor #f5f5f5
}
skinparam actor {
  BorderColor #0050ef
  BackgroundColor #dce8ff
}
skinparam ArrowColor #444444

title C4 Level 1 — System Context

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

Системийн дотоод container-уудыг харуулна.

```plantuml
@startuml C4_Container
!theme plain
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

title C4 Level 2 — Container Diagram

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
http --> security : "before filter"
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

`rest.api` package-ийн бүрэлдэхүүн хэсгүүд.

```plantuml
@startuml C4_Component_API
!theme plain
skinparam component {
  BorderColor #0050ef
  BackgroundColor #dce8ff
}
skinparam ArrowColor #444444

title C4 Level 3 — API Layer Components

component "API\n(Entry point)" as api #d5e8d4
component "Config\n(Owner interface)" as config #e1d5e7
component "AuthFilter\n(Cookie token auth)" as auth #f8cecc
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
api --> auth : "registers (optional)"
api --> xsrf : "registers (optional)"
api --> ex : "registers"
api --> micro : "registers"

auth --> token : "uses"
xsrf --> xtoken : "uses"
token --> crypto : "uses"
xtoken --> crypto : "uses"

@enduml
```

---

## C4 Level 3 — Component (Domain Layer)

`member` болон `genre` package-ийн бүрэлдэхүүн хэсгүүд.

```plantuml
@startuml C4_Component_Domain
!theme plain
skinparam component {
  BorderColor #0050ef
  BackgroundColor #dce8ff
}
skinparam ArrowColor #444444

title C4 Level 3 — Domain Layer Components

package "member package" {
  component "MemberHandler\n(POST /members\nGET /members/{id})" as mh #d5e8d4
  component "MemberService\n(Business logic)" as ms #dce8ff
  component "MemberToAdd\n(Request DTO)" as mta #f5f5f5
  component "Member\n(Response DTO)" as mdto #f5f5f5
  component "Phone\n(Response DTO)" as pdto #f5f5f5
  component "DMember\n(@Entity members)" as dm #ffe6cc
  component "DPhone\n(@Entity phone_numbers)" as dp #ffe6cc
}

package "genre package" {
  component "GenreHandler\n(GET /genres)" as gh #d5e8d4
  component "GenreService\n(Business logic)" as gs #dce8ff
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

HTTP хүсэлтийн дамжих замыг харуулна.

```plantuml
@startuml RequestFlow
!theme plain
skinparam sequence {
  ArrowColor #444444
  ActorBorderColor #0050ef
  LifeLineBorderColor #aaaaaa
  ParticipantBackgroundColor #f5f5f5
  ParticipantBorderColor #666666
}

title Request Flow — POST /v1/members

actor "Client" as client
participant "Javalin\nHTTP Server" as server
participant "AuthFilter\n(optional)" as auth
participant "XSRFFilter\n(optional)" as xsrf
participant "MemberHandler" as handler
participant "Validators" as validator
participant "MemberService" as service
participant "Ebean ORM\n(DMember)" as orm
database "PostgreSQL" as db

client -> server : POST /v1/members\n{name, phones}
server -> auth : before()
auth --> server : 401 or continue
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

---

## Security Architecture

```plantuml
@startuml Security
!theme plain
skinparam component {
  BorderColor #cc0000
  BackgroundColor #f8cecc
}
skinparam ArrowColor #444444

title Security Architecture

component "AuthFilter" as auth
component "XSRFFilter" as xsrf
component "SecureToken" as st
component "XSRFToken" as xt
component "Crypto" as crypto

note right of crypto
  AES-256-GCM encryption
  HmacSHA256 signing
  Keys derived via SHA-256
end note

auth --> st : parse token\n(30 min timeout)
xsrf --> xt : validate token\n(30 min timeout)
st --> crypto : encrypt / decrypt\nverify signature
xt --> crypto : sign / verify

note bottom of auth
  Cookie: "secure-token"
  Sets member in ctx.attribute
end note

note bottom of xsrf
  GET: generate + set cookie "xsrf-token"
  POST/PUT/DELETE: validate
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
| `Content-Security-Policy` | `frame-ancestors 'none'; default-src 'self'` |
| `Cache-Control` | `no-store` |
| `X-XSS-Protection` | `1; mode=block` |
| `Referrer-Policy` | `no-referrer` |
| `Cross-Origin-Resource-Policy` | `same-origin` |

---

## Database Schema

```plantuml
@startuml Database
!theme plain
skinparam class {
  BorderColor #d6a520
  BackgroundColor #fff0cc
  HeaderBackgroundColor #ffe6a0
}
skinparam ArrowColor #444444

title Database Schema

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
│   │   ├── AuthFilter.java           # Cookie-based auth filter
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
│   │   ├── I18N.java                 # Internationalization (ja / en)
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
│   │   │   └── MemberService.java    # Business logic
│   │   │
│   │   └── genre/                    # Genre feature
│   │       ├── DGenre.java           # @Entity → genres table
│   │       ├── Genre.java            # Response DTO + MapStruct Convertor
│   │       ├── GenreHandler.java     # Route handler
│   │       └── GenreService.java     # Business logic
│   │
│   ├── resources/
│   │   ├── application.properties    # Datasource config
│   │   ├── i18n.properties           # English messages
│   │   ├── i18n_ja.properties        # Japanese messages
│   │   ├── log4j2.xml                # Logging config
│   │   ├── jte/                      # JTE templates
│   │   └── dbmigration/              # Generated SQL migrations
│   │
│   └── jib/
│       ├── ebean-agent-17.6.0.jar    # Ebean bytecode agent
│       ├── log4j2.xml                # Docker logging config
│       └── jte-classes/              # Precompiled JTE templates
│
└── test/
    ├── java/rest/api/
    │   ├── genre/                    # Genre tests
    │   ├── member/                   # Member tests
    │   ├── CryptoTest.java
    │   ├── I18NTest.java
    │   ├── PagedSearchTest.java
    │   └── ...
    └── resources/
        ├── application.properties    # Test DB config (Testcontainers)
        └── ...
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
| Validation | Hibernate Validator | 9.1.0 | Bean validation (JSR-380) |
| DTO Mapping | MapStruct | 1.6.3 | Entity ↔ DTO conversion |
| Configuration | Owner | 1.0.12 | Externalized config |
| Templates | JTE | 3.2.4 | Server-side HTML |
| Logging | Log4j2 | 2.26.0 | Async structured logging |
| Async Logging | LMAX Disruptor | 4.0.0 | Lock-free async log queue |
| Monitoring | Micrometer | 1.16.5 | Prometheus metrics |
| Mail | Jakarta Mail | 2.1.5 | SMTP email |
| JSON | Jackson | 2.21.3 | JSON serialization |
| JSON | org.json | 20251224 | JSON object building |
| Mobile Detect | mobiledetect | 1.1.1 | User-agent parsing |
| Utils | Commons Lang3 | 3.20.0 | String utilities |
| Utils | Commons Collections4 | 4.5.0 | Collection utilities |
| Code Gen | Lombok | 1.18.46 | Getters/Setters/Builders |
| Testing | JUnit 5 | 6.0.3 | Unit testing |
| Testing | Mockito | 5.23.0 | Mocking |
| Testing | JMockit | 1.50 | Alternative mocking |
| Testing | Ebean Test | 17.6.0 | DB testing support |
| Testing | Testcontainers | (via Ebean) | Docker-based DB tests |
| Testing | Unirest | 3.14.5 | HTTP client for tests |

---

## API Endpoints

| Method | Path | Description | Transaction |
|--------|------|-------------|-------------|
| `GET` | `/v1/genres` | Genre жагсаалт (pagination) | `readOnly` |
| `GET` | `/v1/members/{id}` | Member ID-р хайх | `readOnly` |
| `POST` | `/v1/members` | Member үүсгэх | `readWrite` |
| `GET` | `/v1/metrics` | Prometheus metrics | — |

### Pagination Query Params

| Param | Type | Description |
|-------|------|-------------|
| `pageNumber` | `Integer` | Хуудасны дугаар (1-based) |
| `recordsPerPage` | `Integer` | Нэг хуудасны мөрийн тоо |

### Error Responses

| Status | Тайлбар |
|--------|---------|
| `400` | Validation алдаа — field-level JSON errors |
| `401` | Auth token байхгүй/хугацаа дууссан |
| `403` | XSRF token буруу |
| `404` | Өгөгдөл олдсонгүй |
| `500` | Системийн алдаа |

---

## Build & Deployment

```plantuml
@startuml Build
!theme plain
skinparam rectangle {
  BorderColor #666666
  BackgroundColor #f5f5f5
}
skinparam ArrowColor #444444

title Build & Deployment Pipeline

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
  rectangle "src/main/jib/ebean-agent.jar" as agent
}

rectangle "Deployment" as deploy #f8cecc {
  rectangle "Docker Image\n(bellsoft/liberica-openjre-alpine)" as docker
  rectangle "app-cds.jsa\n(AppCDS optimization)" as cds
}

src --> maven
maven --> artifacts
artifacts --> deploy

note right of deploy
  JVM flags:
  -javaagent:ebean-agent.jar
  -XX:+UseZGC
  -Xshare:on (AppCDS)
  -Dlog4j2.contextSelector=AsyncLogger
end note

@enduml
```

### Quick Start

```bash
# Build
mvn package            # или
build.bat              # Windows (AppCDS хамт)

# Run
run.bat                # Windows (optimal JVM flags)
run.sh                 # Linux/macOS

# Docker
docker build --format docker -t rest-api-template .
docker run -p 8080:8080 \
  -e DB_HOST_NAME=host.docker.internal \
  -e DB_PASSWORD=password \
  rest-api-template
```

---

## Testing Strategy

| Давхарга | Хэрэгсэл | Тайлбар |
|---------|---------|---------|
| Unit tests | JUnit 5 + Mockito | Service, utility class тест |
| Integration tests | JUnit 5 + Ebean Test | DB-тэй хамт тест |
| Container tests | Testcontainers | Docker PostgreSQL дээр |
| HTTP tests | Unirest | API endpoint тест |
| Mocking | Mockito / JMockit | External dependency isolation |

Test DB тохиргоо (`src/test/resources/application.properties`):

```properties
ebean.test.platform   = postgres
ebean.test.port       = 6433
ebean.test.ddlMode    = dropCreate
ebean.test.dbName     = test
ebean.test.useDocker  = true
```

---

## Configuration Reference

| Property | Default | Тайлбар |
|----------|---------|---------|
| `portNo` | `8080` | Server port |
| `contextPath` | `/v1/` | API root path |
| `allowedOrigins` | `localhost:8080,4200,4201` | CORS |
| `encryptionKey` | `1234567890123456` | AES key (≥16 chars) |
| `xsrfProtectionEnabled` | `false` | XSRF filter идэвхжүүлэх |
| `isSecure` | `false` | HTTPS cookie flag |
| `httpMaxRequestSize` | `1024` | Max request bytes |
| `httpAsyncTimeout` | `5000` | Async timeout ms |
| `monitoringUsername` | `micro` | Metrics basic auth |
| `monitoringPassword` | `meter` | Metrics basic auth |
| `environment` | `local` | `local` = JTE dev mode |
| `smtpHost` | `smtp.gmail.com` | Mail server |
| `smtpPort` | `587` | Mail port |
