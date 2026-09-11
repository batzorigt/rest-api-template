---
taskId: task-20260910-164758
status: completed
createdAt: 2026-09-10T08:51:30Z
---

# Task: Validate documentation index anchors

## Description

Make documentation navigation mandatory and verifiable, and separate broad-index exclusions from paths agents must never read.

## Analysis

### Current State
- Base commit: `16656c4c5188cc4f6a680aaf5467c1fb357ae363`
- Current branch: `feature/ai-task-20260910-doc-index-validator`
- Git status: clean before implementation

### Requirements
- [x] Direct documentation work through `docs/index.md` and remove volatile line counts
- [x] Validate every indexed architecture heading on Windows and Linux/macOS
- [x] Add the validator to the documentation loop and full gate
- [x] Separate automatic indexing exclusions from never-read paths
- [x] Permit targeted `.ai-loop` reads for explicit task tracking

### Files to Modify
| File | Change Type |
|------|-------------|
| `AGENTS.md` | Modify |
| `.token-ignore` | Modify |
| `docs/index.md` | Modify |
| `LOOP.md` | Modify |
| `HARNESS.md` | Modify |
| `.agents/skills/repo-loops/SKILL.md` | Modify |
| `opencode.json` | Modify |
| `scripts/sync-token-ignore.ps1` | Modify |
| `scripts/sync-token-ignore.sh` | Modify |
| `scripts/validate-doc-index.ps1` | Create |
| `scripts/validate-doc-index.sh` | Create |

### Implementation Plan
1. Categorize token context rules and update their synchronization scripts.
2. Give architecture anchors a machine-readable format and add paired validators.
3. Wire validation into documentation changes, the full gate, and its skill summary.
4. Run script checks, AI-loop validation, and the Maven full gate.

### Verification Strategy
- Scripts: run both documentation-index validators and syntax-check both shell scripts
- Sync: run the PowerShell sync twice and verify the second run produces no diff
- Full gate: run `mvn test`
- Docs: confirm volatile line-count claims are absent
- Neutrality: inspect the final diff and generated/artifact status

### Dependencies
- PowerShell 7 for Windows scripts
- Bash and awk for the portable validator
- Maven/JDK 25 and Docker for the repository full gate

### Blockers
- None

---
Last updated: 2026-09-10T09:16:02Z
