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

# Check tasks directory
Write-Host ""
Write-Host "Tasks directory check..." -ForegroundColor Cyan
$tasksDir = Join-Path $aiLoopDir "tasks"
if ((Get-ChildItem $tasksDir -Directory -ErrorAction SilentlyContinue).Count -gt 0) {
    $taskCount = (Get-ChildItem $tasksDir -Directory).Count
    Write-Host "[OK] Tasks directory contains $taskCount task(s)" -ForegroundColor Green
}
else {
    Write-Host "[OK] Tasks directory is empty (ready for new tasks)" -ForegroundColor Green
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
