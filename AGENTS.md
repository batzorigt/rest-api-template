# AGENTS.md

## Project

Javalin 7 + Ebean 17 REST API on **Java 25** and PostgreSQL. Single Maven module, entry point `rest.api.API#main` (port 8080, context path `/v1/`). Request flow: filters → handlers → services → Ebean entities → PostgreSQL.

Vendor-neutral playbooks referenced by this file:

- `HARNESS.md` — environment contract (JDK 25, Docker on port 6433), artifact map, failure triage, safety rails.
- `LOOP.md` — verification loop orders: compile check → targeted test → full gate; per-change-type loops; definition of done.

Both are on-demand playbooks: load them via the repo skills `repo-harness` / `repo-loops` (`.agents/skills/`, auto-discovered by opencode and other skill-capable harnesses) or read them directly; follow `LOOP.md`'s default loop for every edit. `AGENTS.md` is the only always-loaded file — wire your harness to it (per-tool recipes: `HARNESS.md` → *Harness wiring*).

## Token discipline

Context is expensive — these rules are mandatory in every session:

- Never open generated/artifact paths: `jte-classes/`, `src/main/jib/`, `target/`, `app-cds.jsa`. There is nothing to learn inside.
- Never open IDE/tool metadata: `.github/`, `.mvn/`, `.opencode/`, `.settings/`, `.vscode/`. These are not source code.
- Grep before Read, always. Big files are section-anchored: `docs/architecture.md` (~580 lines) has stable headings (`Security Architecture`, `Database Schema`, `Package Structure`, `API Endpoints`, …) — grep the heading, then read only that slice.
- Delegate wide, multi-file searches to your harness's explore/general subagent; pull back summaries, not raw file dumps.
- Playbooks are on-demand: invoke the repo skills `repo-harness` (before env/tooling/triage work) and `repo-loops` (before verifying); without a skill mechanism, read `HARNESS.md`/`LOOP.md` directly at those same moments.
- Frontmatter `stale_after` dates are a trust signal: when today is past the date, re-verify the doc's facts against their source (pom.xml versions, code) before relying on them, and refresh the date after confirming.
- Keep terminal output lean: `.mvn/maven.config` already sets `--no-transfer-progress`; add `-q` yourself for compile checks (`mvn -q compile`).
- Iterate with targeted tests (`mvn test -Dtest=Class[#method]`); pay the full-gate cost once at the end.
- Prefer a harness shortcut over re-deriving the loop (e.g., opencode ships `/verify`); elsewhere run LOOP.md's three steps verbatim.
- Configure your harness to auto-ignore the above paths (opencode: `ignore` in `opencode.json`) — synced from `.token-ignore` via `scripts/sync-token-ignore.ps1`.

## Pre-work (mandatory before any change)

- Read related docs and code first — understand current architecture, conventions, and patterns
- Research frameworks/libraries in use (Javalin, Ebean, MapStruct, JTE, Log4j2, etc.) — know their capabilities and limits
- Reuse existing solutions — grep for helpers/services before adding new ones; feature-local logic stays in its feature, genuinely shared logic moves to `rest.api` root helpers
- Check for duplication — if a change creates overlap with existing code/docs, consolidate instead of adding

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
- Route protection (RBAC): declare allowed roles as extra route args (`app.post("genres", h, Role.MANAGER)`). `rest.api.Role` is `USER < MANAGER < ADMIN`; higher levels satisfy lower requirements; routes without roles stay public. Enforcement is centralized in `Authorization.wrap` (registered via `config.router.handlerWrapper`) — never hand-roll auth checks inside handlers.
- Session identity: `Authentication.handle` validates the `secure-token` cookie and stores its JSON payload as the `member` context attribute; the user's role comes from that payload's `role` claim (missing/unknown → `USER`). A future login endpoint must embed the role claim when minting tokens.
- Lombok `accessors.chain = true`: setters return `this`.

## Change workflow (mandatory)

Every change ships, in the same task, with:

1. **Matching tests** — unit tests for new logic, handler-level HTTP tests for endpoints/auth changes (patterns per `LOOP.md`). Run compile → targeted → full gate before declaring done. Reuse before adding: grep for an existing helper/service first; feature-local logic stays in its feature, genuinely shared logic moves to `rest.api` root helpers.
2. **Doc updates** — anything touching behavior, endpoints, architecture, or tooling updates the relevant MD files in the same task: `docs/architecture.md` (C4 diagrams, package tree, endpoint table), `README.md`, plus `HARNESS.md` / `LOOP.md` / `docs/architecture-standards.md` when contracts or standards change, and `openapi.yaml` whenever endpoints, request/response payloads, parameters, or auth surface change (create it if missing). Place each new fact in its canonical home once (`HARNESS.md` → *Canonical-home map*); everywhere else points, never restates.
3. **Neutrality pass** — docs *and* code stay model-, agent-, and IDE-neutral. Docs: canonical files name no LLM/coding-agent/IDE except as marked examples or rows in `HARNESS.md`'s wiring table; harness-specific automation lives only in adapter files (`opencode.json`, `.opencode/`), which may automate but never define contracts. Code: no AI/agent attribution comments or markers anywhere in tracked sources, no tool-/IDE-specific files or paths in the diff (IDE metadata like `.settings/`, `.vscode/`, `.idea/` stays untracked), generated code produced only by its generators, build reproducible without any IDE. Full rules: `HARNESS.md` → *Neutrality mechanism*.
4. **Dependency & duplication check** — on every code or doc change, verify related dependencies; if duplication exists, consolidate instead of adding.

No code-only drift without docs, and no doc-only claims without a green `mvn test`.

## Configuration

- Resolution order, defaults, and DB env vars: `HARNESS.md` → Runtime config resolution.
- `environment=local` enables JTE dev mode (hot reload from `src/main/resources/jte`); any other value uses precompiled classes — behavioral details: `LOOP.md` → Template-change loop.
- Auth surface summary: no global auth filter; route-level RBAC only — see Conventions above and `HARNESS.md` → Safety rails.

## Gotchas

- Default locale is Japan: `API.start()` loads `Locale.JAPAN`, and some test assertions compare against Japanese messages from `i18n_ja.properties`. Main resources ship **Japanese only** — there is no English `i18n.properties` bundle (`I18NJapaneseOnlyTest` guards this).
- `docs/architecture-standards.md` holds additional team standards: C4/PlantUML docs required for new features, and hard rules (must use Javalin/Ebean/MapStruct/JTE/Log4j2; no Spring, no XML config, no raw JDBC).
