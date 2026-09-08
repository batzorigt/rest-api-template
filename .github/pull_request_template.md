# Pull Request Template

## Description

[Describe the changes in this PR]

## Related Task

- Task ID: `task-YYYYMMDD-hhmmss`
- Status: [planning | implementation | verification | completed]
- Link: `.ai-loop/tasks/task-YYYYMMDD-hhmmss/`

## Changes

### Files Modified
- [File 1]
- [File 2]

### Files Created
- [File 1]
- [File 2]

## Testing

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Compile check passed: `mvn -q compile`
- [ ] Targeted tests passed: `mvn test -Dtest=Class[#method]`
- [ ] Full gate tests passed: `mvn test`
- [ ] Validation script passed: `./scripts/validate-ai-loop.sh` / `.\scripts\validate-ai-loop.ps1`

## Documentation

- [ ] Architecture docs updated (`docs/architecture.md`)
- [ ] API docs updated (`openapi.yaml`)
- [ ] README updated if needed
- [ ] Neutrality pass: no AI markers, no IDE metadata

## Verification Checklist

- [ ] Tests shipped for all new/changed behavior
- [ ] Documentation synced with code changes
- [ ] No new duplication introduced
- [ ] Generated files not edited (`[feature]/query/Q*.java`, MapStruct impls, etc.)
- [ ] Migration SQL regenerated if entities changed (run from IDE)

## Reviewer Notes

[Any additional notes for reviewers]

## Sign-off

- [ ] Human verification completed
- [ ] PR reviewed and approved