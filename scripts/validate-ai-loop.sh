#!/bin/bash
# AI Loop Validation Script for Linux/macOS
# Validates the AI coding workflow structure and files

set -euo pipefail

BASE_DIR="${1:-.}"
AI_LOOP_DIR="$BASE_DIR/.ai-loop"

echo "AI Loop Validation Script"
echo "========================="
echo ""

# Check if .ai-loop directory exists
if [ ! -d "$AI_LOOP_DIR" ]; then
    echo "❌ FAIL: .ai-loop directory not found at $AI_LOOP_DIR"
    exit 1
fi
echo "✓ .ai-loop directory exists"

# Check required subdirectories
for dir in schema templates tasks; do
    if [ ! -d "$AI_LOOP_DIR/$dir" ]; then
        echo "❌ FAIL: Required directory .ai-loop/$dir not found"
        exit 1
    fi
    echo "✓ .ai-loop/$dir directory exists"
done

# Validate schema files
echo ""
echo "Validating schema files..."
for schema in state.schema.json verification.schema.json; do
    schema_file="$AI_LOOP_DIR/schema/$schema"
    if [ ! -f "$schema_file" ]; then
        echo "❌ FAIL: Schema file $schema not found"
        exit 1
    fi
    
    # Check if JSON is valid (try both python3 and python)
    if python3 -c "import json; json.load(open('$schema_file'))" 2>/dev/null; then
        echo "✓ $schema is valid JSON"
    elif python -c "import json; json.load(open('$schema_file'))" 2>/dev/null; then
        echo "✓ $schema is valid JSON"
    else
        echo "❌ FAIL: $schema is not valid JSON"
        exit 1
    fi
done

# Validate template files
echo ""
echo "Validating template files..."
for template in plan.template.md state.template.json verification.template.json; do
    template_file="$AI_LOOP_DIR/templates/$template"
    if [ ! -f "$template_file" ]; then
        echo "❌ FAIL: Template file $template not found"
        exit 1
    fi
    echo "✓ $template exists"
done

# Validate JSON templates
for template in state.template.json verification.template.json; do
    template_file="$AI_LOOP_DIR/templates/$template"
    if python3 -c "import json; json.load(open('$template_file'))" 2>/dev/null; then
        echo "✓ $template is valid JSON"
    elif python -c "import json; json.load(open('$template_file'))" 2>/dev/null; then
        echo "✓ $template is valid JSON"
    else
        echo "❌ FAIL: $template is not valid JSON"
        exit 1
    fi
done

# Validate task contracts using only the Python standard library.
PYTHON_BIN=""
if command -v python3 >/dev/null 2>&1; then
    PYTHON_BIN="python3"
elif command -v python >/dev/null 2>&1; then
    PYTHON_BIN="python"
else
    echo "FAIL: Python 3 is required to validate task contracts"
    exit 1
fi

"$PYTHON_BIN" - "$AI_LOOP_DIR" <<'PY'
import json
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
state_schema = json.loads((root / "schema/state.schema.json").read_text(encoding="utf-8"))
verification_schema = json.loads((root / "schema/verification.schema.json").read_text(encoding="utf-8"))
state_template = json.loads((root / "templates/state.template.json").read_text(encoding="utf-8"))
verification_template = json.loads((root / "templates/verification.template.json").read_text(encoding="utf-8"))
if not re.fullmatch(state_schema["properties"]["taskId"]["pattern"], state_template["taskId"]):
    raise SystemExit("FAIL: state.template.json violates the taskId schema constraint")
if not re.fullmatch(state_schema["properties"]["baseCommit"]["pattern"], state_template["baseCommit"]):
    raise SystemExit("FAIL: state.template.json violates the baseCommit schema constraint")
if not re.fullmatch(verification_schema["properties"]["taskId"]["pattern"], verification_template["taskId"]):
    raise SystemExit("FAIL: verification.template.json violates the taskId schema constraint")
if verification_template["overallResult"] not in verification_schema["properties"]["overallResult"]["enum"]:
    raise SystemExit("FAIL: verification.template.json violates the overallResult schema constraint")
print("[OK] JSON templates satisfy key schema constraints")

task_dirs = sorted(path for path in (root / "tasks").iterdir() if path.is_dir())
state_statuses = {"not_started", "planning", "implementation", "verification", "completed", "abandoned"}
results = {"pending", "passed", "failed", "warning", "rejected"}
required_state = {"taskId", "title", "description", "status", "baseCommit", "targetBranch", "createdAt", "updatedAt"}
required_checks = {"compileCheck", "targetedTests", "fullGateTests", "documentationIndex", "workflowIntegrity", "docSync", "neutralityCheck", "noDuplication"}

for task_dir in task_dirs:
    if not re.fullmatch(r"task-[0-9]{8}-[0-9]{6}", task_dir.name):
        raise SystemExit(f"FAIL: Invalid task directory name: {task_dir.name}")
    plan_file = task_dir / "plan.md"
    state_file = task_dir / "state.json"
    if not plan_file.is_file() or not state_file.is_file():
        raise SystemExit(f"FAIL: {task_dir.name} must contain plan.md and state.json")
    state = json.loads(state_file.read_text(encoding="utf-8"))
    missing = required_state - state.keys()
    if missing:
        raise SystemExit(f"FAIL: {task_dir.name}/state.json missing: {sorted(missing)}")
    if state["taskId"] != task_dir.name or state["status"] not in state_statuses:
        raise SystemExit(f"FAIL: {task_dir.name}/state.json has invalid taskId or status")
    if not re.fullmatch(r"[a-f0-9]{40}", state["baseCommit"]):
        raise SystemExit(f"FAIL: {task_dir.name}/state.json has invalid baseCommit")
    retry = state.get("retryAttempts", {})
    limits = {"primaryImplementationAttempt": 1, "correctiveAttempt": 1, "transientRetry": 2, "flakyTestRerun": 1}
    if any(retry.get(name, 0) > limit for name, limit in limits.items()):
        raise SystemExit(f"FAIL: {task_dir.name}/state.json exceeds retry limits")
    verification_file = task_dir / "verification.json"
    if state["status"] == "completed" and not verification_file.is_file():
        raise SystemExit(f"FAIL: {task_dir.name} is completed but has no verification.json")
    if verification_file.is_file():
        verification = json.loads(verification_file.read_text(encoding="utf-8"))
        if verification.get("taskId") != task_dir.name or verification.get("overallResult") not in results:
            raise SystemExit(f"FAIL: {task_dir.name}/verification.json has invalid taskId or overallResult")
        missing_checks = required_checks - verification.get("checks", {}).keys()
        if missing_checks:
            raise SystemExit(f"FAIL: {task_dir.name}/verification.json missing checks: {sorted(missing_checks)}")
    print(f"[OK] {task_dir.name} contract is valid")
PY

# Check for required files in tasks directory (empty is OK for fresh install)
echo ""
echo "Tasks directory check..."
if [ "$(ls -A $AI_LOOP_DIR/tasks 2>/dev/null)" ]; then
    task_count=$(ls -1 "$AI_LOOP_DIR/tasks" | wc -l)
    echo "✓ Tasks directory contains $task_count task(s)"
else
    echo "✓ Tasks directory is empty (ready for new tasks)"
fi

# Summary
echo ""
echo "========================="
echo "✓ All AI Loop validations passed!"
echo ""
echo "Next steps:"
echo "1. Create a new task: cp .ai-loop/templates/state.template.json .ai-loop/tasks/task-YYYYMMDD-hhmmss/state.json"
echo "2. Run validation: ./scripts/validate-ai-loop.sh"
echo ""

exit 0
