# Shared helpers for TapeBack Kiro hook scripts.
$ErrorActionPreference = "Stop"
$script:RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

function Read-HookInput {
    # Kiro passes hook context as JSON on stdin. Return $null when run by hand.
    try {
        if ([Console]::IsInputRedirected) {
            $raw = [Console]::In.ReadToEnd()
            if ($raw -and $raw.Trim().StartsWith("{")) { return $raw | ConvertFrom-Json }
        }
    } catch { }
    return $null
}

function Get-HookFilePaths($hookInput) {
    # Look for any file path the hook payload carries; fall back to files changed in git.
    $paths = @()
    if ($hookInput) {
        foreach ($name in @("file_path", "filePath", "path", "file")) {
            $value = $hookInput.$name
            if ($value) { $paths += $value }
        }
        if ($hookInput.tool_input) {
            foreach ($name in @("file_path", "path", "targetFile")) {
                $value = $hookInput.tool_input.$name
                if ($value) { $paths += $value }
            }
        }
    }
    if ($paths.Count -eq 0) {
        Push-Location $script:RepoRoot
        try {
            $paths = @(git diff --name-only HEAD 2>$null) + @(git ls-files --others --exclude-standard 2>$null)
        } finally { Pop-Location }
    }
    return $paths | Where-Object { $_ } | ForEach-Object {
        if ([IO.Path]::IsPathRooted($_)) { $_ } else { Join-Path $script:RepoRoot $_ }
    } | Where-Object { Test-Path $_ } | Select-Object -Unique
}
