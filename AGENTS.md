# AGENTS.md

## Project

Javalin 7 + Ebean 17 REST API on **Java 25** and PostgreSQL. Single Maven module, entry point `rest.api.API#main` (port 8080, context path `/v1/`). Request flow: filters → handlers → services → Ebean entities → PostgreSQL.

Vendor-neutral playbooks referenced by this file:

- `HARNESS.md` — environment contract (JDK 25, Docker on port 6433), artifact map, failure triage, safety rails.
- `LOOP.md` — verification loop orders: compile check → targeted test → full gate; per-change-type loops; definition of done.

Read both before making changes; follow `LOOP.md`'s default loop for every edit.

## Commands

Maven wrapper is available (`./mvnw` / `mvnw.cmd`).

- `mvn test` — all tests. **Requires a running Docker daemon**: integration/service/handler tests boot a PostgreSQL Testcontainer on port 6433 with `ddlMode=dropCreate` (see `src/test/resources/application.properties`).
- Single test: `mvn test -Dtest=GenreHandlerTest`
- `mvn package` — builds `target/rest-api-template-1.0.0.jar`, precompiles JTE templates, and copies `ebean-agent-<ebean.version>.jar` into `src/main/jib/` (generated, not in git).
- `build.bat` / `build.sh` — `mvn clean package -DskipTests` plus AppCDS archive (`app-cds.jsa`) generation.
- `run.bat` / `run.sh` — start the app with required JVM flags; both fail unless `mvn package` ran first.

Do NOT run the fat jar plainly: `java -jar target/...jar` fails because Ebean entities are enhanced at runtime by the javaagent. Always pass `-javaagent:src/main/jib/ebean-agent-<version>.jar` (version must match `<ebean.version>` in `pom.xml`) — or just use the run scripts. Tests get their agents injected via the Surefire `argLine` in `pom.xml`.

## Generated code — never hand-edit

- `[feature]/query/Q*.java` — Ebean QueryBeans from the `querybean-generator` annotation processor.
- MapStruct converter implementations (interfaces live nested inside DTO classes).
- Precompiled JTE classes (`jte-classes/`, `src/main/jib/jte-classes/`) — edit `src/main/resources/jte/*.jte` instead.
- `app-cds.jsa`, `target/` — build artifacts.

## Database migrations

After changing entities, run `rest.api.GenerateDbMigration#main` to emit SQL into `src/main/resources/dbmigration/` (applied on startup via `ebean.migration.run=true`). This class is **excluded from the Maven build** (compiler excludes in `pom.xml`) — launch it from the IDE.

## Conventions (differ from framework defaults)

- No Spring, no DI container: static helper methods and direct instantiation everywhere.
- Transactions are declared on **handler** methods (`io.ebean.annotation.Transactional`), not services. Services stay transaction-free business logic.
- Feature package layout: `D[Entity].java` (Ebean `@Entity` extends `Domain`), `[Entity].java` (DTO with nested MapStruct `Convertor`), `[Feature]ToAdd.java` (request DTO), `[Feature]Handler.java` (static `routes()` registration), `[Feature]Service.java`.
- New routes go in the feature handler's static `routes()`, called from `API.config`.
- Lombok `accessors.chain = true`: setters return `this`.

## Configuration

- `Config.java` (Owner lib) merges the optional file `/rapit.config` with OS env vars; defaults live in its annotations.
- DB connection comes from env vars `DB_HOST_NAME`, `DB_USER_NAME`, `DB_PASSWORD`, `DB_NAME` (defaults: localhost / postgres / postgres / rapit) via placeholders in `application.properties` (a Maven-filtered resource).
- `environment=local` enables JTE dev mode (live reload from `src/main/resources/jte`); any other value uses precompiled template classes.
- Auth and XSRF filters are disabled by default (`xsrfProtectionEnabled=false`; `AuthFilter.handle` commented out in `API`).

## Gotchas

- Default locale is Japan: `API.start()` loads `Locale.JAPAN`, and some test assertions compare against Japanese messages from `i18n_ja.properties` (English fallback in `i18n.properties`).
- `.kiro/steering/architecture-standards.md` holds additional team standards: C4/PlantUML docs required for new features, and hard rules (must use Javalin/Ebean/MapStruct/JTE/Log4j2; no Spring, no XML config, no raw JDBC).
