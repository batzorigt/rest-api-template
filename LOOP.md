# Agent Loops — Verification Playbooks

There is no lint or typecheck step in this repo. The compiler + annotation processors + tests are the entire gate, run via Maven.

## Default loop (always, in order)

1. **Fast compile check** — `mvn -q compile` after any Java edit. Lombok, MapStruct, and Ebean querybean generation all fire here; this catches most breakage without paying test startup cost.
   - If a test ever dies with `java.lang.Error: Unresolved compilation problem`, that is NOT a source bug: the IDE (Eclipse/JDT) shares `target/` with Maven and left stale error-stub classes. Run `mvn clean` and repeat from this step.
2. **Targeted tests** — `mvn test -Dtest=GenreHandlerTest` (class) or `mvn test -Dtest=GenreServiceTest#dataExistingCase` (single method). Requires Docker.
3. **Full gate** — `mvn test`. Green here = done. Handler tests boot the real API on a random port 1000–9999 and drive it with Unirest, so expect several seconds of startup per test class.

Do not skip step 3 before declaring work finished; no CI currently enforces it.

## Entity-change loop

1. Edit `D[Entity].java` / `D*.java`.
2. Regenerate migration SQL: run `rest.api.GenerateDbMigration#main` from the IDE (excluded from Maven build).
3. `mvn test`.
4. Gotcha: tests use `ddlMode=dropCreate`, so schema is derived straight from entities — **tests never exercise your new `dbmigration/*.sql`**. Migration correctness only shows up at real startup (`ebean.migration.run=true`). Review generated SQL manually.

## DTO/entity field loop

1. Change entity and/or DTO fields.
2. Update the nested MapStruct `Convertor` if mappings need guidance; missing/ambiguous mappings fail at compile time.
3. Run the feature's service + handler tests (`mvn test -Dtest=MemberServiceTest,MemberHandlerTest`).

## New-endpoint loop

1. Follow feature layout (`AGENTS.md` → Conventions): request DTO `[Feature]ToAdd`, handler method with `@Transactional`, route registered in that handler's static `routes()`.
2. Pattern the test on `GenreHandlerTest`: random port + Unirest + `new Q[Entity]().delete()` in `@BeforeEach`.
3. Compile → targeted test → full gate.

## Template-change loop

- `environment=local` (default): JTE templates hot-reload from `src/main/resources/jte` at runtime — no rebuild needed to see changes.
- Any other environment value: precompiled classes from the last `mvn package` are used (`src/main/jib/jte-classes`) — repackage or nothing changes.

## Fat-jar / deployment loop

1. `build.*` (package + AppCDS) — required before any run attempt.
2. `run.*` to launch. Bare `java -jar` fails (missing ebean javaagent) — see `HARNESS.md` failure modes.
3. Smoke check: `GET http://localhost:8080/v1/genres` returns 200 with seeded data, 404 envelope when empty.

## Definition of done

- [ ] `mvn test` green with Docker actually up
- [ ] `git status` shows no edited generated files (`Q*`, MapStruct impls, `jte-classes`)
- [ ] Migration SQL regenerated if entities changed
- [ ] App started (if relevant) via `run.*`, not bare `java -jar`
