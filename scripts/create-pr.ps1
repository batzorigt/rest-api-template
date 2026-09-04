#!/usr/bin/env powershell
# Create a new branch and PR if there are uncommitted changes
# Exit codes:
#   0 - PR created successfully or not needed
#   2 - Error occurred

# Check if we are on main branch
$currentBranch = git branch --show-current
if ($currentBranch -ne "main") {
  Write-Host "INFO: Not on main branch. Skipping PR creation."
  exit 0
}

# Check if there are uncommitted changes
$status = git status --porcelain
if ([string]::IsNullOrWhiteSpace($status)) {
  Write-Host "INFO: No uncommitted changes. Skipping PR creation."
  exit 0
}

Write-Host "INFO: Changes detected. Starting PR workflow..."

# Check if we already have a feature branch (avoid creating nested branches)
if ($currentBranch -match '^(feature|fix|refactor|docs)/') {
  Write-Host "INFO: Already on feature branch. Pushing changes..."
  git push origin $currentBranch
  exit 0
}

# Generate branch name based on timestamp
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$branchName = "feature/changes-$timestamp"

Write-Host "INFO: Creating feature branch: $branchName"

# Create and push new branch
git checkout -b $branchName
if ($LASTEXITCODE -ne 0) {
  Write-Error "ERROR: Failed to create branch."
  exit 2
}

git add -A
git commit -m "Auto-commit: $(Get-Date)"
if ($LASTEXITCODE -ne 0) {
  Write-Host "INFO: No changes to commit."
  exit 0
}

git push origin $branchName
if ($LASTEXITCODE -ne 0) {
  Write-Error "ERROR: Failed to push branch."
  exit 2
}

Write-Host "INFO: Branch created and pushed: $branchName"

# Create PR using GitHub CLI
gh pr create --title "[$branchName] Changes" --body "Auto-generated PR from $(Get-Date)" --base main --head $branchName
if ($LASTEXITCODE -ne 0) {
  Write-Error "ERROR: Failed to create PR."
  exit 2
}

Write-Host "INFO: PR created successfully."
exit 0
