# Agent Harness — Environment & Tooling Contract

Everything an agent must set up or avoid before editing this repo. Verification loop orders live in `LOOP.md`; quick project facts live in `AGENTS.md`.

## Prerequisites

- **JDK 25** — `pom.xml` pins `source/target=25`; older JDKs fail the build.
- **Docker daemon running** — every DB-backed test boots a PostgreSQL Testcontainer mapped to fixed host port **6433** (`ebean.test.useDocker=true` in `src/test/resources/application.properties`). Free port 6433 if occupied; tests cannot run without Docker.
- **No local Maven needed** — use the wrapper (`./mvnw`, `mvnw.cmd`). Repo scripts call plain `mvn`, which works if installed.

## Command surface

| Task | Linux/macOS | Windows |
|---|---|---|
| Build jar + precompile JTE + copy ebean agent (skips tests) | `./build.sh` | `build.bat` |
| Same without AppCDS archive | `mvn package` | same |
| Run app (requires prior package) | `./run.sh` | `run.bat` |
| Tests / single test | `mvn test [-Dtest=Class[#method]]` | same |

## Artifact & state map (never commit, never hand-edit)

| Path | Origin |
|---|---|
| `target/` | Maven output |
| `app-cds.jsa` (root) | `build.*` AppCDS dump |
| `jte-classes/` (root), `src/main/jib/jte-classes/` | JTE `precompile` goal (`process-classes` phase) |
| `src/main/jib/ebean-agent-<ver>.jar` | copied by `mvn package` from `<ebean.version>` in `pom.xml` — version must stay in sync |
| `[feature]/query/Q*.java` | `querybean-generator` annotation processor at compile time |
| MapStruct `*Impl` classes | generated from nested `Convertor` interfaces in DTOs |

Fix generated-code problems by changing the source (entity/DTO/interface), never by editing outputs.

## Runtime config resolution

1. Defaults annotated on `rest.api.Config`.
2. Owner lib merges OS env vars + optional file `/rapit.config` (`@LoadPolicy(MERGE)`).
3. DB endpoint comes only from env vars `DB_HOST_NAME`, `DB_USER_NAME`, `DB_PASSWORD`, `DB_NAME` (defaults localhost/postgres/postgres/rapit) via placeholders in `application.properties` — a **Maven-filtered** resource.

## Known failure modes

| Symptom | Cause | Fix |
|---|---|---|
| Bare `java -jar target/rest-api-template-1.0.0.jar` dies on Ebean entity load | entities are enhanced at runtime by javaagent | run via `run.*` scripts or pass `-javaagent:src/main/jib/ebean-agent-<pom ebean.version>.jar` |
| Test throws `java.lang.Error: Unresolved compilation problem` at runtime | Eclipse/JDT (VS Code Java) compiles into the **same** `target/classes` + `target/test-classes` as Maven (see `.classpath`); stale ECJ error-stub classes get executed | `mvn clean` then rerun; never trust results without clean after an IDE build |
| Tests fail with chained-setter compile errors or `NoSuchMethodError: ...setId(...)` | same ECJ contamination: stale void-setter classes mixed with javac output in `target/` | purge `target/classes` + `target/test-classes`, recompile; if a file refuses to delete, the VS Code Java language server holds it — reload/close it first |
| Unexpected 401/403 in handler tests | route declares `Role.*` args; request lacks the `secure-token` cookie or its payload's `role` claim is below the minimum | mint a token in-test: `SecureToken.generate(new JSONObject().put("role", "..."))` and send as cookie (pattern: `AuthorizationTest`) |
| Tests error connecting to `localhost:6433` or report missing docker | daemon down / port busy | start Docker; free 6433 |
| JVM aborts with shared-archive/classpath mismatch at startup | stale `app-cds.jsa` after dependency change | delete `app-cds.jsa` or rerun `build.*` to regenerate |
| Compile errors inside `Q*.java` / MapStruct impls | edited generated code | revert; change source entities/DTOs and rebuild |
| Test assertions fail comparing messages | default locale is Japan; expectations come from `i18n_ja.properties` | assert against Japanese text or pass explicit `Locale` |

## Safety rails

- There is no global auth filter: `API.commonRequestFilter` only runs XSRF, which defaults to disabled (`xsrfProtectionEnabled=false`). Route-level RBAC is separate and **always active** for routes that declare roles: `config.router.handlerWrapper(Authorization::wrap)` validates the `secure-token` cookie and its `role` claim (`rest.api.Role`: `USER < MANAGER < ADMIN`). Routes without roles stay public. Re-enable XSRF only deliberately, never casually.
- Treat `src/main/resources/dbmigration/*.sql` as generator output: regenerate with `rest.api.GenerateDbMigration#main` after entity changes rather than hand-editing (class is excluded from the Maven build — launch from IDE).
- Team hard rules (`.kiro/steering/architecture-standards.md`): Javalin/Ebean/MapStruct/JTE/Log4j2 only — no Spring, no XML config, no raw JDBC.
