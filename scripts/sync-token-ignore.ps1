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

# Read categorized patterns from .token-ignore.
$automaticPatterns = @()
$neverReadPatterns = @()
$section = $null

foreach ($rawLine in Get-Content -LiteralPath $tokenIgnoreFile -Encoding UTF8) {
    $line = $rawLine.Trim()
    if ($line -eq '# [automatic-index-exclude]') {
        $section = 'automatic'
        continue
    }
    if ($line -eq '# [never-read]') {
        $section = 'never'
        continue
    }
    if (-not $line -or $line.StartsWith('#')) {
        continue
    }

    switch ($section) {
        'automatic' { $automaticPatterns += $line }
        'never' { $neverReadPatterns += $line }
        default {
            Write-Error "Pattern '$line' is outside a recognized category"
            exit 1
        }
    }
}

if ($automaticPatterns.Count -eq 0 -or $neverReadPatterns.Count -eq 0) {
    Write-Error '.token-ignore must contain non-empty [automatic-index-exclude] and [never-read] categories'
    exit 1
}

$patterns = @($automaticPatterns + $neverReadPatterns) | Sort-Object -Unique

# --- Update AGENTS.md ---
$agentsContent = Get-Content -LiteralPath $agentsFile -Raw -Encoding UTF8

# Build the categorized AGENTS.md lines.
$formatPatterns = {
    param([string[]]$items)
    return (($items | Sort-Object | ForEach-Object { ([char]96) + $_ + ([char]96) }) -join ', ')
}

$automaticLine = '- Automatic broad indexing excludes ' + (& $formatPatterns $automaticPatterns) + '; targeted reads remain allowed when relevant, and `.ai-loop/` must be read when explicit task tracking is active.'
$neverReadLine = '- Never open generated/artifact/dependency paths: ' + (& $formatPatterns $neverReadPatterns) + '. There is nothing to learn inside.'

$newAgentsContent = $agentsContent -replace '(?m)^- Automatic broad indexing excludes .*$', $automaticLine
$newAgentsContent = $newAgentsContent -replace '(?m)^- Never open generated/artifact/dependency paths: .*$', $neverReadLine

$newAgentsContent = ($newAgentsContent -replace "`r`n", "`n").TrimEnd("`r", "`n") + "`n"
[System.IO.File]::WriteAllText($agentsFile, $newAgentsContent, [System.Text.UTF8Encoding]::new($true))
Write-Host "Updated AGENTS.md"

# --- Update opencode.json ---
$opencodeContent = Get-Content -LiteralPath $opencodeFile -Raw -Encoding UTF8
$null = $opencodeContent | ConvertFrom-Json
$ignoreList = @()
foreach ($p in $patterns) {
    if ($p -match '^\*\*' -or $p -match '\*') {
        # File glob pattern - add as-is
        $ignoreList += $p
    } else {
        $normalizedPattern = $p.TrimEnd('/')
        if ($normalizedPattern -match '\.[^/]+$' -and -not $normalizedPattern.StartsWith('.')) {
            # Exact file pattern
            $ignoreList += "**/$normalizedPattern"
        } else {
            # Directory pattern - add both directory and recursive
            $ignoreList += "**/$normalizedPattern/**"
            $ignoreList += "**/$normalizedPattern"
        }
    }
}
$ignoreList = $ignoreList | Sort-Object -Unique
$ignoreEntries = ($ignoreList | ForEach-Object { '    "' + $_ + '"' }) -join ",`n"
$ignoreBlock = "  `"ignore`": [`n$ignoreEntries`n  ]"
$ignorePattern = '(?ms)^\s*"ignore"\s*:\s*\[.*?^\s*\]'
if ([regex]::Matches($opencodeContent, $ignorePattern).Count -ne 1) {
    Write-Error 'Expected exactly one ignore array in opencode.json'
    exit 1
}
$newOpencodeContent = [regex]::Replace($opencodeContent, $ignorePattern, $ignoreBlock, 1)
$newOpencodeContent = ($newOpencodeContent -replace "`r`n", "`n").TrimEnd("`r", "`n") + "`n"
[System.IO.File]::WriteAllText($opencodeFile, $newOpencodeContent, [System.Text.UTF8Encoding]::new($true))
Write-Host "Updated opencode.json"

Write-Host "Sync complete. Patterns synced: $($patterns.Count)"
