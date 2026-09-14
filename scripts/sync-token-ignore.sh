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

# Build the formatted ignore block.
ignore_entries=""
for i in "${!IGNORE_ARRAY[@]}"; do
    if [[ $i -eq $((${#IGNORE_ARRAY[@]} - 1)) ]]; then
        ignore_entries+=$'    '"${IGNORE_ARRAY[i]}"
    else
        ignore_entries+=$'    '"${IGNORE_ARRAY[i]},"$'\n'
    fi
done
ignore_block="  \"ignore\": ["$'\n'"$ignore_entries"$'\n'"  ]"

# Detect BOM if present.
has_bom=0
if [[ $(head -c 3 "$OPENCODE_FILE" 2>/dev/null) == $'\xef\xbb\xbf' ]]; then
    has_bom=1
fi

opencode_tmp="$(mktemp "${OPENCODE_FILE}.tmp.XXXXXX")"
if [[ $has_bom -eq 1 ]]; then
    printf '\xef\xbb\xbf' > "$opencode_tmp"
fi

awk -v replacement="$ignore_block" '
BEGIN {
    in_ignore = 0
    match_count = 0
}
{
    sub(/\r$/, "")
    if (NR == 1) {
        sub(/^\xef\xbb\xbf/, "")
    }
    if (!in_ignore && $0 ~ /^[[:space:]]*"ignore"[[:space:]]*:[[:space:]]*\[/) {
        match_count++
        in_ignore = 1
        print replacement
        next
    }
    if (in_ignore) {
        if ($0 ~ /^[[:space:]]*\]/) {
            in_ignore = 0
        }
        next
    }
    print
}
END {
    if (match_count != 1 || in_ignore != 0) {
        print "Error: expected exactly one ignore array in opencode.json" > "/dev/stderr"
        exit 1
    }
}
' "$OPENCODE_FILE" >> "$opencode_tmp"

mv "$opencode_tmp" "$OPENCODE_FILE"

echo "Updated opencode.json"
echo "Sync complete. Patterns synced: ${#PATTERNS[@]}"
