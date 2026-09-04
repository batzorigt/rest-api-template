#!/usr/bin/env pwsh
# Check if main-first workflow is followed
# Returns exit code 0 if main branch, no unmerged changes
# Returns exit code 2 if unmerged changes exist (block task execution)

# Check if on main branch
$current_branch = git branch --show-current
if ($current_branch -ne "main") {
  Write-Error "ERROR: You are on branch '$current_branch'. Switch to main first."
  exit 2
}

# Check for unmerged changes
$unmerged = git status --porcelain | Select-String '^M'
if ($unmerged) {
  Write-Error "ERROR: Unmerged changes detected. Please merge to main first:"
  Write-Output $unmerged
  exit 2
}

exit 0
