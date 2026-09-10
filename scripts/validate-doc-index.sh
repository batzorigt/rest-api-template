#!/usr/bin/env bash
# Verify that every architecture anchor in docs/index.md is an exact heading in docs/architecture.md.

set -euo pipefail

REPO_ROOT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
INDEX_FILE="$REPO_ROOT/docs/index.md"
ARCHITECTURE_FILE="$REPO_ROOT/docs/architecture.md"

for file in "$INDEX_FILE" "$ARCHITECTURE_FILE"; do
    if [[ ! -f "$file" ]]; then
        echo "Error: required documentation file not found: $file" >&2
        exit 1
    fi
done

awk '
    FNR == NR {
        sub(/\r$/, "")
        if ($0 ~ /^## architecture[.]md/) {
            in_section = 1
            next
        }
        if (in_section && $0 ~ /^## /) {
            in_section = 0
        }
        if (in_section && $0 ~ /^- /) {
            anchor_count++
            if ($0 !~ /^- `[^`]+`/) {
                printf "Error: malformed architecture anchor in docs/index.md: %s\n", $0 > "/dev/stderr"
                failed = 1
                next
            }
            anchor = $0
            sub(/^- `/, "", anchor)
            sub(/`.*/, "", anchor)
            anchors[anchor]++
        }
        next
    }
    {
        sub(/\r$/, "")
        if ($0 ~ /^##[#]* /) {
            heading = $0
            sub(/^##[#]* /, "", heading)
            headings[heading] = 1
        }
    }
    END {
        if (anchor_count == 0) {
            print "Error: no architecture anchors found in docs/index.md" > "/dev/stderr"
            failed = 1
        }
        for (anchor in anchors) {
            if (anchors[anchor] > 1) {
                printf "Error: duplicate architecture anchor in docs/index.md: %s\n", anchor > "/dev/stderr"
                failed = 1
            }
            if (!(anchor in headings)) {
                printf "Error: architecture anchor missing from docs/architecture.md: %s\n", anchor > "/dev/stderr"
                failed = 1
            }
        }
        if (failed) {
            exit 1
        }
        printf "[OK] Validated %d architecture anchors from docs/index.md\n", anchor_count
    }
' "$INDEX_FILE" "$ARCHITECTURE_FILE"
