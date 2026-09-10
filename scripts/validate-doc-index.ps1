#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Verifies that every architecture anchor in docs/index.md is an exact heading in docs/architecture.md.

.PARAMETER BasePath
    Repository root. Defaults to the parent of this script's directory.
#>

param(
    [string]$BasePath = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'

$indexFile = Join-Path $BasePath 'docs/index.md'
$architectureFile = Join-Path $BasePath 'docs/architecture.md'

foreach ($file in @($indexFile, $architectureFile)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        Write-Error "Required documentation file not found: $file"
        exit 1
    }
}

$anchors = @()
$inAnchorSection = $false

foreach ($line in Get-Content -LiteralPath $indexFile) {
    if ($line -match '^## architecture\.md\b') {
        $inAnchorSection = $true
        continue
    }
    if ($inAnchorSection -and $line -match '^## ') {
        $inAnchorSection = $false
    }
    if ($inAnchorSection -and $line.StartsWith('- ')) {
        if ($line -notmatch '^- `([^`]+)`') {
            Write-Error "Malformed architecture anchor in docs/index.md: $line"
            exit 1
        }
        $anchors += $Matches[1]
    }
}

if ($anchors.Count -eq 0) {
    Write-Error 'No architecture anchors found in docs/index.md'
    exit 1
}

$duplicates = $anchors | Group-Object | Where-Object Count -gt 1
if ($duplicates) {
    $names = ($duplicates | ForEach-Object Name) -join ', '
    Write-Error "Duplicate architecture anchors in docs/index.md: $names"
    exit 1
}

$headings = @(
    Get-Content -LiteralPath $architectureFile |
        Where-Object { $_ -match '^#{2,6}\s+(.+?)\s*$' } |
        ForEach-Object { $Matches[1] }
)

$missing = @($anchors | Where-Object { $headings -cnotcontains $_ })
if ($missing.Count -gt 0) {
    Write-Error "Architecture anchors missing from docs/architecture.md: $($missing -join ', ')"
    exit 1
}

Write-Host "[OK] Validated $($anchors.Count) architecture anchors from docs/index.md"
