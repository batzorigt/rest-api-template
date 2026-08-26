---
type: Environment Contract
title: Agent Harness — Environment & Tooling Contract
tags: [harness, environment, triage]
status: stable
stale_after: 2027-02-26T00:00:00Z
---

# Agent Harness

Setup rules before editing this repo. Verification loops live in `LOOP.md`; project facts in `AGENTS.md`.

## Prerequisites

- **JDK 25** (`pom.xml` pins source/target=25).
- **Docker daemon up** — DB tests boot a PostgreSQL Testcontainer on fixed host port **6433**; free it if busy.
- **Wrapper over local Maven** — `./mvnw` / `mvnw.cmd` (repo scripts call plain `mvn` if installed).
- **Quiet output** — `.mvn/maven.config` pins `--no-transfer-progress`; leave it.

## Harness wiring

All behavior lives in three plain Markdown files (`AGENTS.md`, this file, `LOOP.md`). No harness is required; tool files are optional sugar that only automate what these mandate.

| Harness | Wire-up |
|---|---|
| opencode | done: auto-loads only `AGENTS.md`; skills deliver HARNESS/LOOP on demand; `/verify` wraps the default loop |
| Any skill-capable | scan `.agents/skills/*/SKILL.md` (shared layout) |
| Claude Code | same skills work; optionally import `@AGENTS.md` into `CLAUDE.md` |
| Gemini CLI | `context.fileName: AGENTS.md`, or a one-line `GEMINI.md` pointing here |
| Cursor | `.cursor/rules/` referencing `AGENTS.md` |
| Aider | `/read AGENTS.md` + `/read LOOP.md` |

New harness = add a row, never duplicate contracts.

### Neutrality layers

| Layer | Files | Rules |
|---|---|---|
| **Canonical** | `AGENTS.md`, `HARNESS.md`, `LOOP.md`, `docs/*.md`, `README.md` | Generic "agent/harness/model" wording; tool names only as marked examples or wiring-table rows |
| **Adapter** | `opencode.json`, `.opencode/**`, future `CLAUDE.md` / `.cursor/rules` / `GEMINI.md` | Automates mandates only; never defines contracts; deleting one loses nothing but convenience |
| **Source** | everything tracked | Author-agnostic: no AI/agent attribution markers; no IDE/tool metadata committed (`.settings/`, `.vscode/`, `.idea/` are gitignored); generated code only from generators; build reproducible without any IDE — Maven owns `target/`, so `mvn clean` after any IDE build |

Update order: canonical docs first → mirror into adapters if automation changed. Gate: canonical diffs stay actionable by any harness; code stays free of tool metadata.

## Canonical-home map (anti-duplication)

Each fact lives fully once; elsewhere = pointer or sanctioned summary.

| Fact | Canonical home | Sanctioned secondary |
|---|---|---|
| Project facts, commands, conventions, RBAC semantics | `AGENTS.md` | `README.md` quickstart |
| Migration regeneration (IDE-only) | `AGENTS.md` → Database migrations | `/migration` adapter may restate |
| Environment, artifacts, triage, config resolution, neutrality, this map | `HARNESS.md` | — |
| Loops, failure handling, definition of done | `LOOP.md` | command adapters |
| Team standards (C4, security, tech) | `docs/architecture-standards.md` | — |
| Endpoints, schema, package tree, request flow | `docs/architecture.md` | — |
| OpenAPI spec | `openapi.yaml` (root) | authoritative for shapes; endpoint table stays summary-level |
| Playbook summaries | `.agents/skills/repo-harness`, `repo-loops` | point back; never extend rules |
| Web-agent index | `llms.txt` | pointers and one-line facts only |
| Lifecycle metadata (`type`, `status`, `stale_after`) | each playbook/skill frontmatter | indexes describe, never restate dates |
| Architecture-doc section anchors | `docs/index.md` | grep targets only |

Rules: new knowledge goes to its canonical home **once**; sanctioned homes summarize, never extend or contradict; conflicts resolve in favor of canonical (fix stale copy immediately — `LOOP.md` → Failure handling); retire docs by deletion unless history must stay reproducible (`status: deprecated` + pointer).

## Commands

| Task | Linux/macOS | Windows |
|---|---|---|
| Build jar + JTE + ebean agent (no tests) | `./build.sh` | `build.bat` |
| Same without AppCDS | `mvn package` | same |
| Run app (needs prior package) | `./run.sh` | `run.bat` |
| Tests / single test | `mvn test [-Dtest=Class[#method]]` | same |

## Artifacts (never commit, never hand-edit)

| Path | Origin |
|---|---|
| `target/` | Maven output |
| `app-cds.jsa` | `build.*` AppCDS dump |
| `jte-classes/` (root), `src/main/jib/jte-classes/` | JTE precompile goal |
| `src/main/jib/ebean-agent-<ver>.jar` | copied by `mvn package`; version tracks `<ebean.version>` |
| `[feature]/query/Q*.java`, MapStruct `*Impl` | annotation processors at compile time |

Fix generated-code problems in the generating source, never in outputs.

## Runtime config resolution

1. Defaults annotated on `rest.api.Config`.
2. Owner lib merges env vars + optional `/rapit.config`.
3. DB comes only from `DB_HOST_NAME`, `DB_USER_NAME`, `DB_PASSWORD`, `DB_NAME` (defaults localhost/postgres/postgres/rapit) via placeholders in Maven-filtered `application.properties`.

## Known failure modes

| Symptom | Cause | Fix |
|---|---|---|
| Bare `java -jar target/rest-api-template-1.0.0.jar` dies on entity load | javaagent enhancement missing | use `run.*` or pass `-javaagent:src/main/jib/ebean-agent-<ebean.version>.jar` |
| Test throws `java.lang.Error: Unresolved compilation problem` | Eclipse/JDT IDE (e.g., VS Code Java) wrote error-stub classes into shared `target/classes` | `mvn clean` then rerun; disable IDE autobuild while gating |
| Chained-setter compile errors / `NoSuchMethodError: ...setId(...)` | same ECJ contamination | purge `target/classes` + `target/test-classes`; if locked, reload the IDE language server |
| First DB-backed run after incremental build fails with `ServiceConfigurationError: ... EntityClassRegister ... not found` | incremental compile drops querybean-generator registration | prefer `mvn clean test` after adding/removing classes |
| Unexpected 401/403 in handler tests | route declares roles; cookie missing or `role` claim too low | mint token: `SecureToken.generate(new JSONObject().put("role", "..."))` as cookie (see `AuthorizationTest`) |
| Tests can't reach `localhost:6433` / report missing docker | daemon down or port busy | start Docker/Podman; free 6433 |
| JVM aborts with shared-archive mismatch | stale `app-cds.jsa` after dependency change | delete it or rerun `build.*` |
| Compile errors in `Q*.java` / MapStruct impls | edited generated code | revert; change the source and rebuild |
| Message assertions fail | default locale Japan; expectations in `i18n_ja.properties` | assert Japanese text or pass explicit `Locale` |

## Safety rails

- No global auth filter; XSRF defaults off (`xsrfProtectionEnabled=false`). Route RBAC is separate and always active where roles are declared — semantics: `AGENTS.md` → Conventions.
- `dbmigration/*.sql` is generator output; regeneration contract: `AGENTS.md` → Database migrations.
- Hard rules (`docs/architecture-standards.md`): Javalin/Ebean/MapStruct/JTE/Log4j2 only; no Spring, XML config, raw JDBC.
