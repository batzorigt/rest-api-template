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

# Read patterns (skip comments/empty lines)
mapfile -t PATTERNS < <(grep -v '^\s*#' "$TOKEN_IGNORE" | grep -v '^\s*$' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

# --- Update AGENTS.md ---
# Separate patterns by type
IDE_PATTERNS=()
ARTIFACT_PATTERNS=()
FILE_PATTERNS=()
HAS_NODE_MODULES=false

for p in "${PATTERNS[@]}"; do
    if [[ "$p" == .* ]]; then
        IDE_PATTERNS+=("$p")
    elif [[ "$p" == "node_modules" ]]; then
        HAS_NODE_MODULES=true
    elif [[ "$p" == **/query/Q*.java ]] || [[ "$p" == *'*'* ]]; then
        FILE_PATTERNS+=("$p")
    else
        ARTIFACT_PATTERNS+=("$p")
    fi
done

# Sort
IFS=$'\n' IDE_PATTERNS=($(sort <<<"${IDE_PATTERNS[*]}"))
IFS=$'\n' ARTIFACT_PATTERNS=($(sort <<<"${ARTIFACT_PATTERNS[*]}"))

# Build replacement lines
IDE_LINE="- Never open IDE/tool metadata: $(printf '\`%s\`, ' "${IDE_PATTERNS[@]}" | sed 's/, $//'). These are not source code."
ARTIFACT_LINE="- Never open generated/artifact paths: $(printf '\`%s\`, ' "${ARTIFACT_PATTERNS[@]}" | sed 's/, $//'). There is nothing to learn inside."
NODE_LINE="- Never open \`node_modules/\` — massive token waste (10k–50k files). Not source code."

# Use perl for multi-line replacement
perl -i -0pe '
    s/(## Token discipline\n\nContext is expensive \x{2014} these rules are mandatory in every session:\n\n)
       - Never open generated\/artifact paths:.*?\n
       - Never open IDE\/tool metadata:.*?\n
    /$1'"$ARTIFACT_LINE"'\n'"$IDE_LINE"'\n'"$NODE_LINE"'\n/sx
' "$AGENTS_FILE"

# Update the configure line
sed -i 's/Configure your harness to auto-ignore the above paths (opencode: `ignore` in `opencode\.json`)\./Configure your harness to auto-ignore the above paths (opencode: `ignore` in `opencode.json`) \x{2014} synced from `.token-ignore` via `scripts\/sync-token-ignore.sh`./' "$AGENTS_FILE"

echo "Updated AGENTS.md"

# --- Update opencode.json ---
# Build ignore array
IGNORE_ARRAY=()
for p in "${PATTERNS[@]}"; do
    if [[ "$p" == **/query/Q*.java ]] || [[ "$p" == *'*'* ]]; then
        # File glob pattern - add as-is
        IGNORE_ARRAY+=("\"$p\"")
    else
        # Directory pattern - add both directory and recursive
        IGNORE_ARRAY+=("\"**/$p/**\"")
        IGNORE_ARRAY+=("\"**/$p\"")
    fi
done

# Unique and sort
IFS=$'\n' IGNORE_ARRAY=($(sort -u <<<"${IGNORE_ARRAY[*]}"))

# Create JSON array string
IGNORE_JSON=$(printf '    %s,\n' "${IGNORE_ARRAY[@]}" | sed '$s/,$//')

# Update opencode.json using jq if available, else perl
if command -v jq >/dev/null 2>&1; then
    jq --argjson ignore "[$(printf '"%s",' "${IGNORE_ARRAY[@]}" | sed 's/,$//')]" '.ignore = $ignore' "$OPENCODE_FILE" > "$OPENCODE_FILE.tmp" && mv "$OPENCODE_FILE.tmp" "$OPENCODE_FILE"
else
    perl -i -0pe 's/"ignore"\s*:\s*\[.*?\]/"ignore": [\n'"$IGNORE_JSON"'\n  ]/s' "$OPENCODE_FILE"
fi

echo "Updated opencode.json"
echo "Sync complete. Patterns synced: ${#PATTERNS[@]}"