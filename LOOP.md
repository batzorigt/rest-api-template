---
type: Verification Playbooks
title: Agent Loops — Verification Playbooks
tags: [loops, verification]
status: stable
stale_after: 2027-02-26T00:00:00Z
---

# Agent Loops

No lint/typecheck: compiler + annotation processors + tests are the entire gate (Maven).

## Retry Policy

A retry is a repeat of the **same implementation or verification approach** after a transient failure. A substantially different implementation approach is not a retry or corrective attempt—it requires an updated implementation plan and explicit human approval.

### Operation Types

| Type | Definition | Use Case | Counter |
|---|---|---|---|
| `PRIMARY_IMPLEMENTATION_ATTEMPT` | The first and only implementation execution for an approved plan | Code generation, refactoring, or feature implementation following an approved plan | Tracked separately (not a retry) |
| `CORRECTIVE_ATTEMPT` | One narrowly scoped code correction based on new deterministic evidence from build, test, lint, type-check, static analysis, or generated-file verification | Implementation failed due to a bug identified in the code itself (not the approach); must address the root cause and remain within the approved scope | Corrective counter |
| `TRANSIENT_RETRY` | A repetition of the same failed operation without changing tracked project files | Network timeout, DB connection failure, CI pipeline flakiness; only when evidence indicates a temporary external or infrastructure failure | Transient retry counter (per operation) |
| `FLAKY_TEST_RERUN` | One repetition of the exact same test without changing code, configuration, command arguments, or test inputs | Test failures that may be nondeterministic; used only to determine if failure is timing- or race-condition-related | Flaky test counter (per test) |

### Failure Classifications

| Classification | Definition | Retryable? | Action |
|---|---|---|---|
| **Transient** | Temporary external/infrastructure failure (network, DB, CI) | Yes (up to `maxTransientRetriesPerOperation`) | Retry same operation; document evidence |
| **Deterministic Code** | Bug in code identified by build, test, lint, or static analysis | Corrective attempt only (up to `maxCorrectiveAttempts`) | Fix root cause within approved scope |
| **Authentication** | Invalid/expired credentials, missing token | No | Stop; requires credential fix or new plan |
| **Authorization** | Insufficient permissions for operation | No | Stop; requires permission change or new plan |
| **Configuration** | Missing/invalid config, environment mismatch | No | Stop; requires config fix |
| **Schema/Validation** | API contract violation, schema mismatch | Corrective attempt only if code fixable within scope | Fix code or stop; requires new plan if approach wrong |
| **Verifier Rejection** | Human or automated verifier marked result as rejected | No | Stop; requires new plan with human approval |

### Important Notes

- **A substantially different implementation approach is not a retry or corrective attempt.** It requires an updated implementation plan and explicit human approval.
- **Corrective attempts must address an identified root cause** and remain within the approved scope. They are not free-form debugging.
- **Transient retries require evidence** of temporary external/infrastructure failure (e.g., timeout logs, service health check failure).
- **Flaky test reruns are limited to one re-run per test** to determine non-determinism. If the rerun fails, treat it as a real failure and investigate the root cause.

### Counter Semantics

- **Primary implementation counter**: Starts at 0, increments to 1 after execution. Never exceeds 1. Separate from retry counters.
- **Corrective counter**: Starts at 0, increments to 1 after first corrective attempt. One failed corrective = counter = 1.
- **Transient retry counter**: Starts at 0 (initial execution), increments after each retry. "2 transient retries" means counter = 2 (3 total executions: initial + retry 1 + retry 2).
- **Flaky test counter**: Starts at 0 (initial run), increments to 1 after first rerun. One failed rerun = counter = 1.

### Default Limits

Unless the repository already defines stricter limits (none exist), use these defaults:

| Operation Type | Max Attempts | Counted per |
|---|---|---|
| `maxPrimaryImplementationAttempts` | 1 | Task |
| `maxCorrectiveAttempts` | 1 | Task |
| `maxTransientRetriesPerOperation` | 2 | Operation |
| `maxFlakyTestRerunsPerTest` | 1 | Test |
| `maxRetriesAfterVerifierRejection` | 0 | N/A (verifier rejection is final) |

### Enforcement

- Each retry must be logged with its type and attempt count
- `maxRetriesAfterVerifierRejection: 0` means a verifier rejection is final—no automatic retries, must re-plan and get human approval
- `maxPrimaryImplementationAttempts: 1` means only one implementation execution per approved plan; failure requires plan revision and approval
- Transient retries are counted per-operation; flaky test reruns are counted per-test; other retries are counted per-task

### Example Workflow

1. **Primary implementation attempt** executes the approved plan → counter goes from 0 to 1. If it fails:
   - If deterministic code failure (clear, local, in-scope bug): proceed to corrective attempt
   - If approach failure: stop; create new plan with human approval
   - Counter stays at 1 (no retries of primary implementation)

2. **Corrective attempt** (if applicable) applies a bug fix based on deterministic evidence → corrective counter goes from 0 to 1. If it fails:
   - Corrective counter = 1 (not 2)
   - Stop; assess root cause; create new plan with human approval

3. **Transient retry** (e.g., DB connection timeout):
   - Initial execution fails → counter = 0
   - First retry → counter = 1
   - Second retry → counter = 2
   - After 2 retries (counter = 2 = max): mark task blocked, human review required. Evidence of temporary failure must be documented.

4. **Flaky test rerun**:
   - Initial run fails → counter = 0
   - First rerun → counter = 1
   - If rerun fails (counter = 1 = max): treat as real failure, investigate root cause (code or test issue, not infrastructure).

5. **Verifier rejection**: No automatic retries. Counter = 0 for `maxRetriesAfterVerifierRejection`. Stop; create new plan with human approval.

## Default loop

1. `mvn -q compile` after any Java edit — catches most breakage cheaply. `Unresolved compilation problem` error = shared-`target/` IDE contamination → `mvn clean`, repeat.
2. Targeted tests — `mvn test -Dtest=Class[#method]` (Docker required).
3. Full gate — run `bash ./scripts/validate-doc-index.sh` (Linux/macOS) or `.\scripts\validate-doc-index.ps1` (Windows), then `mvn test`. Never skip either check before declaring done; no CI enforces them.

Shortcut example: some harnesses provide a `/verify` command that wraps these steps. Other harnesses wrap the same steps natively; this file stays canonical.

## Failure handling (fix & resync)

On any failing step:

1. **Classify the failure** using the Failure Classifications table above:
   - If **transient**: proceed to transient retry (if within limits)
   - If **deterministic code**: proceed to corrective attempt (if within limits and scope)
   - If **auth/authz/config**: stop; requires human intervention
   - If **verifier rejection**: stop; no automatic retries
   - If **approach failure**: stop; create new plan with human approval

2. **Check applicable counter and limits**:
   - Transient: `transientRetry` counter < `maxTransientRetriesPerOperation`?
   - Corrective: `correctiveAttempt` counter < `maxCorrectiveAttempts`?
   - Note: Primary implementation counter is separate and maxes at 1

3. **Fix the smallest root cause** — never widen scope. For corrective attempts, address the identified bug and stay within approved scope.

4. If behavior/endpoints/auth/tooling changed, apply `AGENTS.md` → Change workflow now (tests + canonical docs in same pass).

5. Re-run the failed step first, then climb to the full gate.

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

## Documentation-change loop

1. Open `docs/index.md` first; use its exact anchors to read only the relevant canonical sections.
2. Edit each fact in its canonical home (`HARNESS.md` → Canonical-home map) and update pointers without duplicating the fact.
3. When an indexed `docs/architecture.md` heading changes, update the matching backticked anchor in `docs/index.md`.
4. Run `bash ./scripts/validate-doc-index.sh` (Linux/macOS) or `.\scripts\validate-doc-index.ps1` (Windows).
5. Run `mvn test` as the full gate.

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
- [ ] PlantUML diagrams render without errors
- [ ] Dependencies verified; no duplicate code/docs introduced
- [ ] Documentation index validation passed (`./scripts/validate-doc-index.*`)
- [ ] AI loop validation passed (`./scripts/validate-ai-loop.*`)
- [ ] AI task state tracked in `.ai-loop/tasks/<task-id>/`
- [ ] Retry policy respected: primary implementation executed once (counter 0→1, separate from retries); corrective attempts counted separately (failed corrective = counter 1, not 2); transient retries counted per-operation (2 retries = counter 2, 3 total executions); flaky test reruns counted per-test; verifier rejection prevents automatic retries; human approval obtained when limits exceeded or approach changes needed
