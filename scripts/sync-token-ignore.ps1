#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Sync AGENTS.md and opencode.json from .token-ignore (single source of truth)
#>

$repoRoot = Split-Path -Parent $PSScriptRoot
$tokenIgnoreFile = Join-Path $repoRoot '.token-ignore'
$agentsFile = Join-Path $repoRoot 'AGENTS.md'
$opencodeFile = Join-Path $repoRoot 'opencode.json'

if (-not (Test-Path $tokenIgnoreFile)) {
    Write-Error ".token-ignore not found at $tokenIgnoreFile"
    exit 1
}

# Read patterns from .token-ignore (skip comments/empty lines)
$patterns = Get-Content $tokenIgnoreFile | Where-Object { $_ -and $_ -notmatch '^\s*#' } | ForEach-Object { $_.Trim() }

# --- Update AGENTS.md ---
$agentsContent = Get-Content $agentsFile -Raw

# Build the IDE/tool metadata line
$idePatterns = $patterns | Where-Object { $_ -match '^\.' } | Sort-Object
$ideLine = "- Never open IDE/tool metadata: " + (($idePatterns | ForEach-Object { "`"$_`"" }) -join ', ') + ". These are not source code."

# Build the generated/artifact paths line (directory patterns only, no wildcards)
$artifactPatterns = $patterns | Where-Object { $_ -notmatch '^\.' -and $_ -notmatch 'node_modules' -and $_ -notmatch '^\*\*' -and $_ -notmatch '\*' } | Sort-Object
$artifactLine = "- Never open generated/artifact paths: " + (($artifactPatterns | ForEach-Object { "`"$_`"" }) -join ', ') + ". There is nothing to learn inside."

# Build the node_modules line
$nodeModulesLine = '- Never open `node_modules/` — massive token waste (10k–50k files). Not source code.'

# Replace the first two bullet points in Token discipline section
$pattern = '(?s)(## Token discipline\n\nContext is expensive \x2014 these rules are mandatory in every session:\n\n)(- Never open generated/artifact paths:.*?\n)(- Never open IDE/tool metadata:.*?\n)'
$replacement = '$1' + $artifactLine + "`n" + $ideLine + "`n" + $nodeModulesLine + "`n"
$newAgentsContent = $agentsContent -replace $pattern, $replacement

# Also update the "Configure your harness" line
$newAgentsContent = $newAgentsContent -replace 'Configure your harness to auto-ignore the above paths \(opencode: `ignore` in `opencode\.json`\)\.', 'Configure your harness to auto-ignore the above paths (opencode: `ignore` in `opencode.json`) \x2014 synced from `.token-ignore` via `scripts/sync-token-ignore.ps1`.'

Set-Content -Path $agentsFile -Value $newAgentsContent -Encoding UTF8
Write-Host "Updated AGENTS.md"

# --- Update opencode.json ---
$opencodeJson = Get-Content $opencodeFile -Raw | ConvertFrom-Json
$ignoreList = @()
foreach ($p in $patterns) {
    if ($p -match '^\*\*' -or $p -match '\*') {
        # File glob pattern - add as-is
        $ignoreList += $p
    } else {
        # Directory pattern - add both directory and recursive
        $ignoreList += "**/$p/**"
        $ignoreList += "**/$p"
    }
}
$opencodeJson.ignore = $ignoreList | Sort-Object -Unique

$opencodeJson | ConvertTo-Json -Depth 10 | Set-Content -Path $opencodeFile -Encoding UTF8
Write-Host "Updated opencode.json"

Write-Host "Sync complete. Patterns synced: $($patterns.Count)"