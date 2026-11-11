# Kiro PreToolUse hook for shell tools: ask before destructive git commands.
# Emits a permissionDecision "ask" JSON when the command looks destructive, so the user confirms.
. "$PSScriptRoot\_common.ps1"
$hookInput = Read-HookInput
if (-not $hookInput) { exit 0 }
$command = $hookInput.tool_input.command
if (-not $command) { $command = $hookInput.command }
if (-not $command) { exit 0 }

$patterns = @(
    'git\s+push\s+.*(--force|-f\b)',
    'git\s+reset\s+--hard',
    'git\s+clean\s+-[a-z]*f',
    'git\s+branch\s+-D',
    'Remove-Item\s+.*-Recurse.*(app[\\/]src|\.kiro)',
    'rm\s+-rf\s+.*(app/src|\.kiro)'
)
foreach ($p in $patterns) {
    if ($command -match $p) {
        $decision = @{ hookSpecificOutput = @{ permissionDecision = "ask"; permissionDecisionReason = "Destructive command detected ($p). TapeBack never overwrites existing work without confirmation." } }
        $decision | ConvertTo-Json -Compress -Depth 4
        exit 0
    }
}
exit 0
