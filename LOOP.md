---
type: Verification Playbooks
title: Agent Loops — Verification Playbooks
tags: [loops, verification]
status: stable
stale_after: 2027-02-26T00:00:00Z
---

# Agent Loops

No lint/typecheck: compiler + annotation processors + tests are the entire gate (Maven).

## Default loop

1. `mvn -q compile` after any Java edit — catches most breakage cheaply. `Unresolved compilation problem` error = shared-`target/` IDE contamination → `mvn clean`, repeat.
2. Targeted tests — `mvn test -Dtest=Class[#method]` (Docker required).
3. Full gate — `mvn test`. Never skip before declaring done; no CI enforces it.

Shortcut example: opencode `/verify` wraps these steps. Other harnesses wrap the same steps natively; this file stays canonical.

## Failure handling (fix & resync)

On any failing step:

1. Fix the smallest root cause — never widen scope.
2. If behavior/endpoints/auth/tooling changed, apply `AGENTS.md` → Change workflow now (tests + canonical docs in same pass).
3. Re-run the failed step first, then climb to the full gate.

## Entity-change loop

1. Edit `D[Entity].java`.
2. Regenerate migration SQL via IDE (`rest.api.GenerateDbMigration#main`; CLI fails — see `AGENTS.md`).
3. `mvn test`.
4. Tests use `ddlMode=dropCreate` → they never exercise new `dbmigration/*.sql`; review generated SQL manually.

## DTO/entity field loop

1. Change entity/DTO fields.
2. Update nested MapStruct `Convertor` if needed (mismatches fail at compile time).
3. Run feature service + handler tests.

## New-endpoint loop

1. Feature layout (`AGENTS.md` → Conventions): `[Feature]ToAdd` DTO, `@Transactional` handler method, route in static `routes()`.
2. Update `openapi.yaml` — path, params, schemas, security.
3. Add `Role.*` args if protected; public routes stay arg-free.
4. Test pattern: `GenreHandlerTest` (public) / `AuthorizationTest` (protected) — random port + Unirest + `new Q[Entity]().delete()` setup.
5. Compile → targeted → full gate.

## Authorization loop

1. Declare roles as route args (`app.post("genres", h, Role.MANAGER)`); semantics: `AGENTS.md` → Conventions.
2. Enforcement is centralized in `Authorization.wrap` — never hand-roll checks.
3. Tests (`RoleTest` + `AuthorizationTest` matrix): no/invalid/expired token → 401; below minimum → 403 and data unchanged; at-or-above → success; missing claim → `USER`; public routes unchanged.
4. Compile → targeted → full gate.
5. Update `docs/architecture.md` endpoint table + security section on any role change.

## Template changes

- `environment=local`: JTE hot-reloads from `src/main/resources/jte`.
- Otherwise: precompiled classes from last `mvn package` are used — repackage or nothing changes.

## Deployment

1. `build.*` (package + AppCDS) first.
2. Launch with `run.*` only — bare `java -jar` fails (see `HARNESS.md` failure modes).
3. Smoke: `GET http://localhost:8080/v1/genres` → 200 seeded / 404 envelope when empty.

## Definition of done

- [ ] `mvn test` green with Docker up
- [ ] Matching tests shipped for the change
- [ ] Affected MD docs updated when behavior/endpoints/architecture/tooling changed
- [ ] Doc edits neutral: tool names only as marked examples or wiring-table rows
- [ ] Code diff author-agnostic: no AI attribution markers, no IDE metadata staged, generated code untouched
- [ ] No new duplication: facts once in canonical home; code reuses helpers
- [ ] `openapi.yaml` updated if API surface changed
- [ ] No edited generated files in `git status`
- [ ] Migration SQL regenerated if entities changed
- [ ] App started via `run.*` where relevant
