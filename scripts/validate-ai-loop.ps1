#!/usr/bin/env pwsh
<#
.SYNOPSIS
    AI Loop Validation Script for Windows
    Validates the AI coding workflow structure and files

.EXAMPLE
    .\scripts\validate-ai-loop.ps1
    .\scripts\validate-ai-loop.ps1 -BasePath "d:\path\to\repo"

.PARAMETER BasePath
    Path to the repository root (defaults to current directory)
#>

param(
    [string]$BasePath = "."
)

$ErrorActionPreference = "Stop"

Write-Host "AI Loop Validation Script" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan
Write-Host ""

$aiLoopDir = Join-Path $BasePath ".ai-loop"

# Check if .ai-loop directory exists
if (-not (Test-Path $aiLoopDir -PathType Container)) {
    Write-Host "FAIL: .ai-loop directory not found at $aiLoopDir" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] .ai-loop directory exists" -ForegroundColor Green

# Check required subdirectories
foreach ($dir in @("schema", "templates", "tasks")) {
    $dirPath = Join-Path $aiLoopDir $dir
    if (-not (Test-Path $dirPath -PathType Container)) {
        Write-Host "FAIL: Required directory .ai-loop/$dir not found" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] .ai-loop/$dir directory exists" -ForegroundColor Green
}

# Validate schema files
Write-Host ""
Write-Host "Validating schema files..." -ForegroundColor Cyan
foreach ($schema in @("state.schema.json", "verification.schema.json")) {
    $schemaFile = Join-Path (Join-Path $aiLoopDir "schema") $schema
    if (-not (Test-Path $schemaFile)) {
        Write-Host "FAIL: Schema file $schema not found" -ForegroundColor Red
        exit 1
    }
    
    # Try to parse as JSON
    try {
        $jsonContent = Get-Content $schemaFile -Raw | ConvertFrom-Json
        Write-Host "[OK] $schema is valid JSON" -ForegroundColor Green
    }
    catch {
        Write-Host "FAIL: $schema is not valid JSON" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Validate template files
Write-Host ""
Write-Host "Validating template files..." -ForegroundColor Cyan
foreach ($template in @("plan.template.md", "state.template.json", "verification.template.json")) {
    $templateFile = Join-Path (Join-Path $aiLoopDir "templates") $template
    if (-not (Test-Path $templateFile)) {
        Write-Host "FAIL: Template file $template not found" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] $template exists" -ForegroundColor Green
}

# Validate JSON templates
foreach ($template in @("state.template.json", "verification.template.json")) {
    $templateFile = Join-Path (Join-Path $aiLoopDir "templates") $template
    try {
        $jsonContent = Get-Content $templateFile -Raw | ConvertFrom-Json
        Write-Host "[OK] $template is valid JSON" -ForegroundColor Green
    }
    catch {
        Write-Host "FAIL: $template is not valid JSON" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

$stateSchema = Get-Content (Join-Path $aiLoopDir "schema/state.schema.json") -Raw | ConvertFrom-Json
$verificationSchema = Get-Content (Join-Path $aiLoopDir "schema/verification.schema.json") -Raw | ConvertFrom-Json
$stateTemplate = Get-Content (Join-Path $aiLoopDir "templates/state.template.json") -Raw | ConvertFrom-Json
$verificationTemplate = Get-Content (Join-Path $aiLoopDir "templates/verification.template.json") -Raw | ConvertFrom-Json
if ($stateTemplate.taskId -notmatch $stateSchema.properties.taskId.pattern -or
    $stateTemplate.baseCommit -notmatch $stateSchema.properties.baseCommit.pattern) {
    throw "state.template.json violates taskId or baseCommit schema constraints"
}
if ($verificationTemplate.taskId -notmatch $verificationSchema.properties.taskId.pattern -or
    $verificationTemplate.overallResult -notin $verificationSchema.properties.overallResult.enum) {
    throw "verification.template.json violates taskId or overallResult schema constraints"
}
Write-Host "[OK] JSON templates satisfy key schema constraints" -ForegroundColor Green

# Validate task contracts
Write-Host ""
Write-Host "Validating task contracts..." -ForegroundColor Cyan
$tasksDir = Join-Path $aiLoopDir "tasks"
$taskDirs = @(Get-ChildItem $tasksDir -Directory -ErrorAction SilentlyContinue)
$validStateStatuses = @("not_started", "planning", "implementation", "verification", "completed", "abandoned")
$validResults = @("pending", "passed", "failed", "warning", "rejected")

foreach ($taskDir in $taskDirs) {
    if ($taskDir.Name -notmatch '^task-[0-9]{8}-[0-9]{6}$') {
        throw "Invalid task directory name: $($taskDir.Name)"
    }
    $planFile = Join-Path $taskDir.FullName "plan.md"
    $stateFile = Join-Path $taskDir.FullName "state.json"
    if (-not (Test-Path $planFile -PathType Leaf) -or -not (Test-Path $stateFile -PathType Leaf)) {
        throw "$($taskDir.Name) must contain plan.md and state.json"
    }
    $state = Get-Content $stateFile -Raw | ConvertFrom-Json
    foreach ($property in @("taskId", "title", "description", "status", "baseCommit", "targetBranch", "createdAt", "updatedAt")) {
        if ($null -eq $state.$property) {
            throw "$($taskDir.Name)/state.json is missing required property '$property'"
        }
    }
    if ($state.taskId -ne $taskDir.Name -or $state.status -notin $validStateStatuses -or
        $state.baseCommit -notmatch '^[a-f0-9]{40}$') {
        throw "$($taskDir.Name)/state.json violates taskId, status, or baseCommit constraints"
    }
    if ($state.retryAttempts.primaryImplementationAttempt -gt 1 -or
        $state.retryAttempts.correctiveAttempt -gt 1 -or
        $state.retryAttempts.transientRetry -gt 2 -or
        $state.retryAttempts.flakyTestRerun -gt 1) {
        throw "$($taskDir.Name)/state.json exceeds retry limits"
    }
    $verificationFile = Join-Path $taskDir.FullName "verification.json"
    if ($state.status -eq "completed" -and -not (Test-Path $verificationFile -PathType Leaf)) {
        throw "$($taskDir.Name) is completed but has no verification.json"
    }
    if (Test-Path $verificationFile -PathType Leaf) {
        $verification = Get-Content $verificationFile -Raw | ConvertFrom-Json
        if ($verification.taskId -ne $taskDir.Name -or $verification.overallResult -notin $validResults) {
            throw "$($taskDir.Name)/verification.json violates taskId or overallResult constraints"
        }
        foreach ($check in @("compileCheck", "targetedTests", "fullGateTests", "documentationIndex", "workflowIntegrity", "docSync", "neutralityCheck", "noDuplication")) {
            if ($null -eq $verification.checks.$check) {
                throw "$($taskDir.Name)/verification.json is missing check '$check'"
            }
        }
    }
    Write-Host "[OK] $($taskDir.Name) contract is valid" -ForegroundColor Green
}

if ($taskDirs.Count -eq 0) {
    Write-Host "[OK] Tasks directory is empty (ready for new tasks)" -ForegroundColor Green
} else {
    Write-Host "[OK] Tasks directory contains $($taskDirs.Count) valid task(s)" -ForegroundColor Green
}

# Summary
Write-Host ""
Write-Host "=========================" -ForegroundColor Cyan
Write-Host "[OK] All AI Loop validations passed!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Create a new task: Copy .ai-loop/templates/state.template.json to .ai-loop/tasks/task-YYYYMMDD-hhmmss/state.json"
Write-Host "2. Run validation: .\scripts\validate-ai-loop.ps1"
Write-Host ""

exit 0
