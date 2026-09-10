#!/usr/bin/env bash
# Sync AGENTS.md and opencode.json from .token-ignore (single source of truth)

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOKEN_IGNORE="$REPO_ROOT/.token-ignore"
AGENTS_FILE="$REPO_ROOT/AGENTS.md"
OPENCODE_FILE="$REPO_ROOT/opencode.json"

if [[ ! -f "$TOKEN_IGNORE" ]]; then
    echo "Error: .token-ignore not found at $TOKEN_IGNORE" >&2
    exit 1
fi

# Read categorized patterns.
AUTOMATIC_PATTERNS=()
NEVER_READ_PATTERNS=()
SECTION=""

while IFS= read -r raw_line || [[ -n "$raw_line" ]]; do
    line="$(printf '%s' "$raw_line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    case "$line" in
        '# [automatic-index-exclude]') SECTION="automatic"; continue ;;
        '# [never-read]') SECTION="never"; continue ;;
        ''|'#'*) continue ;;
    esac

    case "$SECTION" in
        automatic) AUTOMATIC_PATTERNS+=("$line") ;;
        never) NEVER_READ_PATTERNS+=("$line") ;;
        *)
            echo "Error: pattern '$line' is outside a recognized category" >&2
            exit 1
            ;;
    esac
done < "$TOKEN_IGNORE"

if [[ ${#AUTOMATIC_PATTERNS[@]} -eq 0 || ${#NEVER_READ_PATTERNS[@]} -eq 0 ]]; then
    echo "Error: .token-ignore must contain non-empty [automatic-index-exclude] and [never-read] categories" >&2
    exit 1
fi

mapfile -t PATTERNS < <(printf '%s\n' "${AUTOMATIC_PATTERNS[@]}" "${NEVER_READ_PATTERNS[@]}" | sort -u)

# --- Update AGENTS.md ---
format_patterns() {
    printf '`%s`, ' "$@" | sed 's/, $//'
}

mapfile -t SORTED_AUTOMATIC < <(printf '%s\n' "${AUTOMATIC_PATTERNS[@]}" | sort)
mapfile -t SORTED_NEVER_READ < <(printf '%s\n' "${NEVER_READ_PATTERNS[@]}" | sort)
AUTOMATIC_LINE="- Automatic broad indexing excludes $(format_patterns "${SORTED_AUTOMATIC[@]}"); targeted reads remain allowed when relevant, and \`.ai-loop/\` must be read when explicit task tracking is active."
NEVER_READ_LINE="- Never open generated/artifact/dependency paths: $(format_patterns "${SORTED_NEVER_READ[@]}"). There is nothing to learn inside."

agents_tmp="$(mktemp "${AGENTS_FILE}.tmp.XXXXXX")"
awk -v automatic_line="$AUTOMATIC_LINE" -v never_read_line="$NEVER_READ_LINE" '
    /^- Automatic broad indexing excludes / { print automatic_line; next }
    /^- Never open generated\/artifact\/dependency paths: / { print never_read_line; next }
    { print }
' "$AGENTS_FILE" > "$agents_tmp"
mv "$agents_tmp" "$AGENTS_FILE"

echo "Updated AGENTS.md"

# --- Update opencode.json ---
# Build ignore array
IGNORE_ARRAY=()
for p in "${PATTERNS[@]}"; do
    if [[ "$p" == **/query/Q*.java ]] || [[ "$p" == *'*'* ]]; then
        # File glob pattern - add as-is
        IGNORE_ARRAY+=("\"$p\"")
    else
        normalized_pattern="${p%/}"
        if [[ "$normalized_pattern" == *.* && "$normalized_pattern" != .* ]]; then
            # Exact file pattern
            IGNORE_ARRAY+=("\"**/$normalized_pattern\"")
        else
            # Directory pattern - add both directory and recursive
            IGNORE_ARRAY+=("\"**/$normalized_pattern/**\"")
            IGNORE_ARRAY+=("\"**/$normalized_pattern\"")
        fi
    fi
done

# Unique and sort
mapfile -t IGNORE_ARRAY < <(printf '%s\n' "${IGNORE_ARRAY[@]}" | sort -u)

IGNORE_JSON_COMPACT="[$(printf '%s,' "${IGNORE_ARRAY[@]}" | sed 's/,$//')]"

# Update only the ignore array so both platform scripts preserve surrounding formatting.
if command -v python3 >/dev/null 2>&1 || command -v python >/dev/null 2>&1; then
    if command -v python3 >/dev/null 2>&1; then
        PYTHON_COMMAND="python3"
    else
        PYTHON_COMMAND="python"
    fi
    IGNORE_JSON_COMPACT="$IGNORE_JSON_COMPACT" "$PYTHON_COMMAND" - "$OPENCODE_FILE" <<'PYTHON'
import json
import os
import re
import sys

path = sys.argv[1]
with open(path, "rb") as source:
    raw_content = source.read()
has_bom = raw_content.startswith(b"\xef\xbb\xbf")
content = raw_content.decode("utf-8-sig")
ignore = json.loads(os.environ["IGNORE_JSON_COMPACT"])
entries = ",\n".join(f'    {json.dumps(pattern)}' for pattern in ignore)
replacement = f'  "ignore": [\n{entries}\n  ]'
updated, replacements = re.subn(
    r'^\s*"ignore"\s*:\s*\[.*?^\s*\]',
    replacement,
    content,
    count=1,
    flags=re.MULTILINE | re.DOTALL,
)
if replacements != 1:
    raise SystemExit("Error: expected exactly one ignore array in opencode.json")
json.loads(updated)
encoding = "utf-8-sig" if has_bom else "utf-8"
with open(path, "w", encoding=encoding, newline="\n") as destination:
    destination.write(updated.rstrip("\r\n") + "\n")
PYTHON
else
    echo "Error: updating opencode.json requires python3 or python" >&2
    exit 1
fi

echo "Updated opencode.json"
echo "Sync complete. Patterns synced: ${#PATTERNS[@]}"
