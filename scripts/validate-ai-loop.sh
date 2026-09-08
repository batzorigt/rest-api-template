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