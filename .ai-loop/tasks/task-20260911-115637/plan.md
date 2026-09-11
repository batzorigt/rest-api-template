---
taskId: task-20260911-115637
status: completed
createdAt: 2026-09-11T03:56:37Z
---

# Task: Preserve template helper APIs

## Description

Restore static namespace interfaces and intentionally reusable template helpers removed by the consistency audit, while retaining safe structured JSON serialization and validated runtime fixes.

## Analysis

### Current State
- Base commit: `5b16768588024303ba222f53aff99ab0e219f135`
- Current branch: `feature/ai-task-20260910-project-consistency-audit`
- Git status: prior audit and entry-point rename changes remain uncommitted

### Requirements
- [x] Restore original interface/plain/abstract type forms
- [x] Restore reusable helper methods and their required resources/dependencies
- [x] Replace manual JSON construction with `ctx.json(...)` structured values
- [x] Add focused tests and run the full verification loop

### Files to Modify
| File | Change Type |
|------|-------------|
| `src/main/java/rest/api/ContextHelpers.java` | Modify |
| `src/main/java/rest/api/{SecureToken,TemplateEngines,Validators}.java` | Modify |
| `src/main/java/rest/api/{genre,member}/*.java` | Modify |
| `src/main/java/rest/api/Server.java` | Modify |
| `pom.xml` and i18n bundles | Modify |
| `AGENTS.md` and `docs/architecture.md` | Modify |
| `src/test/java/rest/api/ContextHelpersTest.java` | Modify |

### Implementation Plan
1. Restore original type declarations and template extension examples.
2. Restore utility methods with structured JSON output and safe serialization.
3. Restore required dependency/message resources and document the interface rule.
4. Run compile, focused tests, full tests, and validators.

### Verification Strategy
- Tests: `ContextHelpersTest`, `TemplateEnginesTest`, token tests, handler tests
- Docs: documentation index and type/dependency consistency searches
- Neutrality: keep canonical text tool-neutral

### Dependencies
- Javalin JSON mapper
- MobileDetect helper library

### Blockers
- None

---
Last updated: 2026-09-11T04:00:02Z
