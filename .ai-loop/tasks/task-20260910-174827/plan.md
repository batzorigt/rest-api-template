---
taskId: task-20260910-174827
status: planning
createdAt: 2026-09-10T09:48:27Z
---

# Task: Audit project consistency and duplication

## Description

Audit relevant source, tests, configuration, scripts, and canonical documentation for code-to-code, code-to-documentation, and documentation-to-documentation inconsistencies or duplication, then apply minimal corrections.

## Analysis

### Current State
- Base commit: `5b16768588024303ba222f53aff99ab0e219f135`
- Current branch: `feature/ai-task-20260910-project-consistency-audit`
- Git status: clean before implementation

### Requirements
- [x] Audit all relevant non-generated project files
- [x] Consolidate duplicated code or documentation according to canonical homes
- [x] Correct stale code/documentation claims and broken navigation
- [x] Preserve application behavior unless a proven defect requires a tested fix
- [x] Run targeted validation and the full repository gate

### Files to Modify

The implementation spans application security, pagination, member validation,
tests, OpenAPI, canonical documentation, and tracked workflow infrastructure.
Generated/artifact paths remain untouched.

### Implementation Plan
1. Inventory source, tests, configuration, scripts, and documentation while excluding generated artifacts.
2. Run parallel read-only audits for code-to-code, code-to-documentation, and documentation-to-documentation consistency.
3. Verify each reported issue against its canonical source and remove false positives.
4. Apply minimal corrections and matching tests or validators where behavior changes.
5. Run documentation validation, targeted checks, AI-loop validation, and the Maven full gate.

### Verification Strategy
- Targeted tests or script validators for every behavioral/tooling correction
- `bash ./scripts/validate-doc-index.sh` and `.\scripts\validate-doc-index.ps1`
- `.\scripts\validate-ai-loop.ps1`
- `.\mvnw.cmd test`
- Final neutrality, duplication, generated-file, and diff checks

### Dependencies
- JDK 25 and Maven wrapper
- Docker/Testcontainers PostgreSQL on port 6433
- PowerShell and Bash for paired repository scripts

### Blockers
- None

---
Last updated: 2026-09-11T01:16:00Z
