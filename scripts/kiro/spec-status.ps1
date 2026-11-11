# Kiro SessionStart hook: give the agent a one-glance view of spec progress and repo state.
. "$PSScriptRoot\_common.ps1"
$null = Read-HookInput
$tasksFile = Join-Path $RepoRoot ".kiro/specs/tapeback-voice-diary/tasks.md"
$lines = Get-Content $tasksFile
$done = ($lines | Where-Object { $_ -match '^\s*- \[x\]' }).Count
$open = $lines | Where-Object { $_ -match '^\s*- \[ \]' }
$total = $done + $open.Count
Write-Output "TapeBack spec progress: $done/$total tasks complete (.kiro/specs/tapeback-voice-diary/tasks.md)"
if ($open.Count -gt 0) {
    Write-Output "Next open tasks:"
    $open | Select-Object -First 5 | ForEach-Object { Write-Output "  $($_.Trim())" }
}
Push-Location $RepoRoot
try {
    $branch = git rev-parse --abbrev-ref HEAD 2>$null
    $dirty = @(git status --porcelain 2>$null).Count
    $last = git log -1 --format="%h %s" 2>$null
    Write-Output "Git: branch $branch, $dirty uncommitted change(s), last commit: $last"
} finally { Pop-Location }
exit 0
