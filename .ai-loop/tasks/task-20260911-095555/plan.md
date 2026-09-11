---
taskId: task-20260911-095555
status: completed
createdAt: 2026-09-11T01:55:55Z
---

# Task: Rename API entry point to Server

## Description

Rename the `rest.api.API` class and source file to `rest.api.Server`, then align build configuration, tests, source references, and canonical documentation.

## Analysis

### Current State
- Base commit: `5b16768588024303ba222f53aff99ab0e219f135`
- Current branch: `feature/ai-task-20260910-project-consistency-audit`
- Git status: prior consistency-audit changes remain uncommitted

### Requirements
- [x] Rename the production entry-point class and all Java references
- [x] Update Maven entry-point configuration and related test naming
- [x] Update canonical documentation and verify no stale class references remain
- [x] Run compile, targeted tests, full gate, and repository validators

### Files to Modify
| File | Change Type |
|------|-------------|
| `src/main/java/rest/api/API.java` | Delete (rename source) |
| `src/main/java/rest/api/Server.java` | Create (renamed source) |
| `src/main/java/rest/api/*.java` | Modify references |
| `src/test/java/rest/api/**/*.java` | Modify references and rename behavior test |
| `pom.xml` | Modify main class |
| `AGENTS.md`, `docs/architecture.md`, `docs/architecture-standards.md`, `llms.txt` | Modify entry-point documentation |

### Implementation Plan
1. Rename source and behavior-test files and symbols.
2. Replace class references in production code, tests, Maven configuration, and docs.
3. Search for stale references and run the required verification loop.

### Verification Strategy
- Tests: `ServerBehaviorTest`, `AuthorizationTest`, `GenreHandlerTest`, `MemberHandlerTest`, `CryptoTest`, `SecureTokenTest`
- Docs: validate `docs/index.md` anchors and search for stale `API` class references
- Neutrality: retain tool-neutral canonical documentation

### Dependencies
- Javalin runtime entry point
- Maven jar, shade, and Jib main-class configuration

### Blockers
- None

---
Last updated: 2026-09-11T02:02:20Z
