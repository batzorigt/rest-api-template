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

validate_json() {
    local file="$1"
    awk '
    function validate(    line, len, i, ch, in_str, esc, sp, stack, prev, had_token) {
        sp = 0; in_str = 0; esc = 0; prev = ""; had_token = 0
        while ((getline line) > 0) {
            sub(/\r$/, "", line)
            if (FNR == 1) sub(/^\xef\xbb\xbf/, "", line)
            len = length(line)
            for (i = 1; i <= len; i++) {
                ch = substr(line, i, 1)
                if (in_str) {
                    if (esc) {
                        esc = 0
                    } else if (ch == "\\") {
                        esc = 1
                    } else if (ch == "\"") {
                        in_str = 0
                        prev = "VAL"
                    }
                    continue
                }
                if (ch ~ /[ \t\n\r]/) {
                    if (prev == "NUM") prev = "VAL"
                    continue
                }
                had_token = 1
                if (ch == "\"") {
                    if (prev == "NUM") prev = "VAL"
                    if (prev == "VAL") return 1
                    in_str = 1
                    continue
                }
                if (ch == "{" || ch == "[") {
                    if (prev == "NUM") prev = "VAL"
                    if (prev == "VAL") return 1
                    stack[++sp] = ch
                    prev = ch
                } else if (ch == "}") {
                    if (prev == "NUM") prev = "VAL"
                    if (sp == 0 || stack[sp] != "{") return 1
                    if (prev != "{" && prev != "VAL") return 1
                    sp--
                    prev = "VAL"
                } else if (ch == "]") {
                    if (prev == "NUM") prev = "VAL"
                    if (sp == 0 || stack[sp] != "[") return 1
                    if (prev != "[" && prev != "VAL") return 1
                    sp--
                    prev = "VAL"
                } else if (ch == ":") {
                    if (prev == "NUM") prev = "VAL"
                    if (prev != "VAL") return 1
                    prev = ":"
                } else if (ch == ",") {
                    if (prev == "NUM") prev = "VAL"
                    if (prev != "VAL") return 1
                    prev = ","
                } else if (ch ~ /[0-9a-zA-Z_.-]/) {
                    if (prev != "NUM" && prev == "VAL") return 1
                    prev = "NUM"
                } else {
                    return 1
                }
            }
            if (in_str) return 1
            if (prev == "NUM") prev = "VAL"
        }
        if (!had_token || sp != 0 || in_str != 0 || prev != "VAL") return 1
        return 0
    }
    BEGIN { exit validate() }
    ' "$file"
}

# Validate schema files
echo ""
echo "Validating schema files..."
for schema in state.schema.json verification.schema.json; do
    schema_file="$AI_LOOP_DIR/schema/$schema"
    if [ ! -f "$schema_file" ]; then
        echo "❌ FAIL: Schema file $schema not found"
        exit 1
    fi
    
    if validate_json "$schema_file"; then
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
    if validate_json "$template_file"; then
        echo "✓ $template is valid JSON"
    else
        echo "❌ FAIL: $template is not valid JSON"
        exit 1
    fi
done

# Validate task contracts using pure Bash and POSIX awk.
awk -v root="$AI_LOOP_DIR" '
function validate_state(file, task_name,    line, len, i, ch, in_str, esc, sp, stack, prev, key, val, parent_key, keys, retries, prev_colon, str_val, num_val, req_fields, rf, f, st) {
    sp = 0; in_str = 0; esc = 0; prev = ""; parent_key = ""; prev_colon = 0
    split("", keys); split("", retries)
    str_val = ""

    while ((getline line < file) > 0) {
        sub(/\r$/, "", line)
        if (FNR == 1) sub(/^\xef\xbb\xbf/, "", line)
        len = length(line)
        for (i = 1; i <= len; i++) {
            ch = substr(line, i, 1)
            if (in_str) {
                if (esc) {
                    esc = 0
                    str_val = str_val ch
                } else if (ch == "\\") {
                    esc = 1
                } else if (ch == "\"") {
                    in_str = 0
                    prev = "VAL"
                    if (prev_colon) {
                        prev_colon = 0
                        if (sp == 1) {
                            keys[key] = str_val
                        }
                    } else {
                        key = str_val
                    }
                } else {
                    str_val = str_val ch
                }
                continue
            }
            if (ch ~ /[ \t\n\r]/) {
                if (prev == "NUM") {
                    if (sp == 2 && parent_key == "retryAttempts") {
                        retries[key] = num_val + 0
                    }
                    prev = "VAL"
                }
                continue
            }
            if (ch == "\"") {
                if (prev == "NUM") prev = "VAL"
                in_str = 1
                str_val = ""
                continue
            }
            if (ch == "{" || ch == "[") {
                if (prev == "NUM") prev = "VAL"
                prev_colon = 0
                stack[++sp] = ch
                prev = ch
                if (ch == "{" && sp == 2) parent_key = key
            } else if (ch == "}") {
                if (prev == "NUM") {
                    if (sp == 2 && parent_key == "retryAttempts") {
                        retries[key] = num_val + 0
                    }
                    prev = "VAL"
                }
                if (sp == 2) parent_key = ""
                sp--
                prev = "VAL"
            } else if (ch == "]") {
                if (prev == "NUM") prev = "VAL"
                sp--
                prev = "VAL"
            } else if (ch == ":") {
                if (prev == "NUM") prev = "VAL"
                prev = ":"
                prev_colon = 1
            } else if (ch == ",") {
                if (prev == "NUM") {
                    if (sp == 2 && parent_key == "retryAttempts") {
                        retries[key] = num_val + 0
                    }
                    prev = "VAL"
                }
                prev = ","
                prev_colon = 0
            } else if (ch ~ /[0-9a-zA-Z_.-]/) {
                if (prev != "NUM") {
                    num_val = ch
                } else {
                    num_val = num_val ch
                }
                prev = "NUM"
            }
        }
        if (prev == "NUM") {
            if (sp == 2 && parent_key == "retryAttempts") {
                retries[key] = num_val + 0
            }
            prev = "VAL"
        }
    }
    close(file)

    req_fields[1] = "taskId"; req_fields[2] = "title"; req_fields[3] = "description"
    req_fields[4] = "status"; req_fields[5] = "baseCommit"; req_fields[6] = "targetBranch"
    req_fields[7] = "createdAt"; req_fields[8] = "updatedAt"
    for (rf = 1; rf <= 8; rf++) {
        f = req_fields[rf]
        if (!(f in keys)) {
            printf "FAIL: %s/state.json missing: ['%s']\n", task_name, f > "/dev/stderr"
            return 1
        }
    }

    if (keys["taskId"] != task_name) {
        printf "FAIL: %s/state.json has invalid taskId or status\n", task_name > "/dev/stderr"
        return 1
    }
    st = keys["status"]
    LAST_STATE_STATUS = st
    if (st != "not_started" && st != "planning" && st != "implementation" && st != "verification" && st != "completed" && st != "abandoned") {
        printf "FAIL: %s/state.json has invalid taskId or status\n", task_name > "/dev/stderr"
        return 1
    }
    if (keys["baseCommit"] !~ /^[a-f0-9]{40}$/) {
        printf "FAIL: %s/state.json has invalid baseCommit\n", task_name > "/dev/stderr"
        return 1
    }

    if (retries["primaryImplementationAttempt"] > 1 || retries["correctiveAttempt"] > 1 || retries["transientRetry"] > 2 || retries["flakyTestRerun"] > 1) {
        printf "FAIL: %s/state.json exceeds retry limits\n", task_name > "/dev/stderr"
        return 1
    }

    return 0
}

function validate_verification(file, task_name,    line, len, i, ch, in_str, esc, sp, stack, prev, key, val, parent_key, keys, checks, prev_colon, str_val, req_checks, rc, c, res) {
    sp = 0; in_str = 0; esc = 0; prev = ""; parent_key = ""; prev_colon = 0
    split("", keys); split("", checks)
    str_val = ""

    while ((getline line < file) > 0) {
        sub(/\r$/, "", line)
        if (FNR == 1) sub(/^\xef\xbb\xbf/, "", line)
        len = length(line)
        for (i = 1; i <= len; i++) {
            ch = substr(line, i, 1)
            if (in_str) {
                if (esc) {
                    esc = 0
                    str_val = str_val ch
                } else if (ch == "\\") {
                    esc = 1
                } else if (ch == "\"") {
                    in_str = 0
                    prev = "VAL"
                    if (prev_colon) {
                        prev_colon = 0
                        if (sp == 1) {
                            keys[key] = str_val
                        }
                    } else {
                        key = str_val
                        if (sp == 2 && parent_key == "checks") {
                            checks[key] = 1
                        }
                    }
                } else {
                    str_val = str_val ch
                }
                continue
            }
            if (ch ~ /[ \t\n\r]/) continue
            if (ch == "\"") { in_str = 1; str_val = ""; continue }
            if (ch == "{" || ch == "[") {
                prev_colon = 0
                stack[++sp] = ch
                prev = ch
                if (ch == "{" && sp == 2) parent_key = key
            } else if (ch == "}") {
                if (sp == 2) parent_key = ""
                sp--
                prev = "VAL"
            } else if (ch == "]") {
                sp--
                prev = "VAL"
            } else if (ch == ":") {
                prev = ":"
                prev_colon = 1
            } else if (ch == ",") {
                prev = ","
                prev_colon = 0
            } else if (ch ~ /[0-9a-zA-Z_.-]/) {
                prev = "VAL"
            }
        }
    }
    close(file)

    if (keys["taskId"] != task_name) {
        printf "FAIL: %s/verification.json has invalid taskId or overallResult\n", task_name > "/dev/stderr"
        return 1
    }
    res = keys["overallResult"]
    if (res != "pending" && res != "passed" && res != "failed" && res != "warning" && res != "rejected") {
        printf "FAIL: %s/verification.json has invalid taskId or overallResult\n", task_name > "/dev/stderr"
        return 1
    }

    req_checks[1] = "compileCheck"; req_checks[2] = "targetedTests"; req_checks[3] = "fullGateTests"
    req_checks[4] = "documentationIndex"; req_checks[5] = "workflowIntegrity"; req_checks[6] = "docSync"
    req_checks[7] = "neutralityCheck"; req_checks[8] = "noDuplication"
    for (rc = 1; rc <= 8; rc++) {
        c = req_checks[rc]
        if (!(c in checks)) {
            printf "FAIL: %s/verification.json missing checks: ['%s']\n", task_name, c > "/dev/stderr"
            return 1
        }
    }

    return 0
}

BEGIN {
    if (validate_state(root "/templates/state.template.json", "task-20000101-000000") != 0) {
        print "FAIL: state.template.json violates the taskId or baseCommit schema constraint" > "/dev/stderr"
        exit 1
    }
    if (validate_verification(root "/templates/verification.template.json", "task-20000101-000000") != 0) {
        print "FAIL: verification.template.json violates the taskId or overallResult schema constraint" > "/dev/stderr"
        exit 1
    }
    print "[OK] JSON templates satisfy key schema constraints"
    print ""
    print "Validating task contracts..."

    cmd = "find \"" root "/tasks\" -mindepth 1 -maxdepth 1 -type d | sort"
    while ((cmd | getline task_dir) > 0) {
        sub(/\r$/, "", task_dir)
        task_name = task_dir
        sub(/.*\//, "", task_name)
        if (task_name !~ /^task-[0-9]{8}-[0-9]{6}$/) {
            printf "FAIL: Invalid task directory name: %s\n", task_name > "/dev/stderr"
            exit 1
        }
        plan_file = task_dir "/plan.md"
        state_file = task_dir "/state.json"
        if ((getline dummy < plan_file) < 0 || (getline dummy < state_file) < 0) {
            printf "FAIL: %s must contain plan.md and state.json\n", task_name > "/dev/stderr"
            exit 1
        }
        close(plan_file)
        close(state_file)

        st_res = validate_state(state_file, task_name)
        if (st_res != 0) exit 1

        ver_file = task_dir "/verification.json"
        is_completed = (LAST_STATE_STATUS == "completed")
        ver_exists = ((getline dummy < ver_file) >= 0)
        close(ver_file)

        if (is_completed && !ver_exists) {
            printf "FAIL: %s is completed but has no verification.json\n", task_name > "/dev/stderr"
            exit 1
        }
        if (ver_exists) {
            ver_res = validate_verification(ver_file, task_name)
            if (ver_res != 0) exit 1
        }
        printf "[OK] %s contract is valid\n", task_name
    }
    close(cmd)
}
'

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
