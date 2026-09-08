---
name: repo-ai-loop
description: AI coding workflow structure for tracking tasks through planning, implementation, and verification phases. Load when working with AI-generated code changes.
type: Playbook
title: Repo AI Loop quick playbook
resource: /ai-loop/
tags: [ai-loop, workflow, task-management]
status: stable
stale_after: 2027-02-26T00:00:00Z
---
Distilled from `.ai-loop/` (canonical — read it fully before starting AI coding tasks).

## When to load

Before starting any AI coding task that will be tracked through the verification workflow.

## Structure

```
.ai-loop/
├── schema/
│   ├── state.schema.json      # Task state schema (taskId, status, files modified/created)
│   └── verification.schema.json # Verification results schema (checks, overall result, sign-off)
├── templates/
│   ├── plan.template.md       # Task planning template
│   ├── state.template.json    # Task state template
│   └── verification.template.json # Verification template
└── tasks/
    └── <task-id>/
        ├── plan.md            # Task plan
        ├── state.json         # Current task state
        └── verification.json  # Verification results
```

## Facts

- **Task tracking**: Use `state.template.json` as a template for new tasks
- **Verification flow**: `not_started` → `planning` → `implementation` → `verification` → `completed`
- **Validation**: Run `./scripts/validate-ai-loop.sh` (Linux/macOS) or `.\scripts\validate-ai-loop.ps1` (Windows)
- **Git isolation**: Each task should use a separate branch (`feature/ai-task-YYYYMMDD`)

## Post-conditions

Every AI coding task follows the workflow:
1. State tracked in `.ai-loop/tasks/<task-id>/state.json`
2. Plan documented in `.ai-loop/tasks/<task-id>/plan.md`
3. Verification results in `.ai-loop/tasks/<task-id>/verification.json`
4. All checks pass before PR submission