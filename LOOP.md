# Agent Loops — Verification Playbooks

There is no lint or typecheck step in this repo. The compiler + annotation processors + tests are the entire gate, run via Maven.

## Default loop (always, in order)

1. **Fast compile check** — `mvn -q compile` after any Java edit. Lombok, MapStruct, and Ebean querybean generation all fire here; this catches most breakage without paying test startup cost.
   - If a test dies with `java.lang.Error: Unresolved compilation problem`: not a source bug — shared-`target/` IDE contamination; run `mvn clean` and repeat this step (triage details: `HARNESS.md` → Known failure modes).
2. **Targeted tests** — `mvn test -Dtest=GenreHandlerTest` (class) or `mvn test -Dtest=GenreServiceTest#dataExistingCase` (single method). Requires Docker.
3. **Full gate** — `mvn test`. Green here = done. Handler tests boot the real API on a random port 1000–9999 and drive it with Unirest, so expect several seconds of startup per test class.

Do not skip step 3 before declaring work finished; no CI currently enforces it.

Shortcut (example: opencode's `/verify`, defined in `.opencode/command/verify.md`) runs exactly these three steps in order — prefer such a wrapper over re-deriving them each session. Other harnesses should wrap these same steps in their native command/skill format; this file stays the canonical source.

## Failure handling — fix & resync sub-loop

Any failing step triggers this before re-running:

1. **Fix the smallest root cause** — never widen scope mid-loop.
2. **Resync docs in the same pass** — if the fix changes behavior, endpoints, auth, or tooling, apply `AGENTS.md` → Change workflow immediately (matching tests + canonical doc updates). A green gate must never coexist with stale docs.
3. **Re-run the failed step first**, then continue upward toward the full gate.

This makes every loop self-healing: errors are corrected together with their documentation, automatically, on every iteration.

## Entity-change loop

1. Edit `D[Entity].java` / `D*.java`.
2. Regenerate migration SQL: run `rest.api.GenerateDbMigration#main` from the IDE (why CLI fails: `AGENTS.md` → Database migrations).
3. `mvn test`.
4. Gotcha: tests use `ddlMode=dropCreate`, so schema is derived straight from entities — **tests never exercise your new `dbmigration/*.sql`**. Migration correctness only shows up at real startup (`ebean.migration.run=true`). Review generated SQL manually.

## DTO/entity field loop

1. Change entity and/or DTO fields.
2. Update the nested MapStruct `Convertor` if mappings need guidance; missing/ambiguous mappings fail at compile time.
3. Run the feature's service + handler tests (`mvn test -Dtest=MemberServiceTest,MemberHandlerTest`).

## New-endpoint loop

1. Follow feature layout (`AGENTS.md` → Conventions): request DTO `[Feature]ToAdd`, handler method with `@Transactional`, route registered in that handler's static `routes()`.
2. Decide access level while registering the route — add `Role.*` args if it must be protected (see authorization loop below); public routes stay arg-free.
3. Pattern the test on `GenreHandlerTest` (public endpoint) or `AuthorizationTest` (protected endpoint): random port + Unirest + `new Q[Entity]().delete()` in `@BeforeEach`.
4. Compile → targeted test → full gate.

## Authorization / route-protection loop

1. Declare allowed roles as extra route args (`app.post("genres", h, Role.MANAGER)`); public routes stay arg-free. Hierarchy semantics: `AGENTS.md` → Conventions.
2. Enforcement is centralized in `Authorization.wrap` — never hand-roll auth checks inside handlers. Expected codes are covered by the test matrix below.
3. Matching tests, modeled on `RoleTest` + `AuthorizationTest`:
   - no token / invalid token / expired token → 401;
   - role below minimum → 403 **and** data unchanged;
   - exactly-at-minimum and higher roles → success;
   - missing `role` claim → treated as `USER`;
   - previously-public behavior unchanged where applicable.
4. Compile → targeted tests → full gate.
5. Update `docs/architecture.md` endpoint table (Roles column) and the security section whenever routes gain or lose roles.

## Template-change loop

- `environment=local` (default): JTE templates hot-reload from `src/main/resources/jte` at runtime — no rebuild needed to see changes.
- Any other environment value: precompiled classes from the last `mvn package` are used (`src/main/jib/jte-classes`) — repackage or nothing changes.

## Fat-jar / deployment loop

1. `build.*` (package + AppCDS) — required before any run attempt.
2. `run.*` to launch. Bare `java -jar` fails (missing ebean javaagent) — see `HARNESS.md` failure modes.
3. Smoke check: `GET http://localhost:8080/v1/genres` returns 200 with seeded data, 404 envelope when empty.

## Definition of done

- [ ] `mvn test` green with Docker actually up
- [ ] Matching tests written or updated for the change (no behavior change ships untested)
- [ ] Affected MD docs updated in the same task (`docs/architecture.md`, `README.md`, `docs/architecture-standards.md`) when behavior, endpoints, architecture, or tooling changed
- [ ] Doc edits pass the neutrality check: no LLM/agent/IDE assumptions in canonical docs (tool names only as marked examples or `HARNESS.md` wiring-table rows); harness specifics confined to adapter files
- [ ] Code diff passes the same neutrality check: no AI/agent attribution comments or markers, no tool-/IDE-specific files or paths staged (`.settings/`, `.vscode/`, `.idea/` stay local), generated code touched only via its generators
- [ ] No new duplication: doc facts placed once in their canonical home (`HARNESS.md` → Canonical-home map) with pointers elsewhere; code reuses existing helpers/services instead of copying logic
- [ ] `git status` shows no edited generated files (`Q*`, MapStruct impls, `jte-classes`)
- [ ] Migration SQL regenerated if entities changed
- [ ] App started (if relevant) via `run.*`, not bare `java -jar`
