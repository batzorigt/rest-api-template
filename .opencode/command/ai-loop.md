---
description: AI Loop validation from LOOP.md (schema + templates + state verification)
agent: general
---
Execute the repo's AI coding workflow validation.

Run `scripts/validate-ai-loop.ps1` (Windows) or `scripts/validate-ai-loop.sh` (Linux/macOS) to validate:

1. Directory structure: `.ai-loop/schema/`, `.ai-loop/templates/`, `.ai-loop/tasks/`
2. Schema files: `state.schema.json`, `verification.schema.json` (valid JSON)
3. Template files: `plan.template.md`, `state.template.json`, `verification.template.json` (valid JSON)
4. Tasks directory: verify it exists and report task count

On any failure, report the specific error and suggest a fix.