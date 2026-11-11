# Kiro hook: enforce the Kotlin coding standards from .kiro/steering/coding-standards.md
# on saved Kotlin files. Run by hand with: powershell -ExecutionPolicy Bypass -File scripts/kiro/kotlin-guard.ps1 [-All]
param([switch]$All)
. "$PSScriptRoot\_common.ps1"

$hookInput = Read-HookInput
if ($All) {
    $files = Get-ChildItem (Join-Path $RepoRoot "app/src") -Recurse -Filter *.kt | ForEach-Object FullName
} else {
    $files = Get-HookFilePaths $hookInput | Where-Object { $_ -like "*.kt" }
}

$rules = @(
    @{ Pattern = '!!(?![=])'; Message = "Non-null assertion (!!) - handle nulls explicitly"; TestsAllowed = $true },
    @{ Pattern = '\bprintln\('; Message = "println() - use Log.w/e with a TAG for genuine failures"; TestsAllowed = $true },
    @{ Pattern = 'printStackTrace\('; Message = "printStackTrace() - log with Log.w/e instead"; TestsAllowed = $false },
    @{ Pattern = '\bLog\.d\('; Message = "Log.d() spam - remove debug logging"; TestsAllowed = $true },
    @{ Pattern = '\b(TODO|FIXME)\b'; Message = "TODO/FIXME left in code - finish it or remove it"; TestsAllowed = $false },
    @{ Pattern = 'GlobalScope'; Message = "GlobalScope - use viewModelScope or AppContainer.appScope"; TestsAllowed = $false }
)

$violations = @()
foreach ($file in $files) {
    $isTest = $file -match '[\\/](test|androidTest)[\\/]'
    $lineNo = 0
    foreach ($line in Get-Content $file) {
        $lineNo++
        $code = ($line -replace '//.*$', '')
        foreach ($rule in $rules) {
            if ($isTest -and $rule.TestsAllowed) { continue }
            $target = if ($rule.Message -like "TODO*") { $line } else { $code }
            if ($target -match $rule.Pattern) {
                $rel = $file.Substring($RepoRoot.Length + 1)
                $violations += "${rel}:${lineNo}: $($rule.Message)"
            }
        }
    }
}

if ($violations.Count -gt 0) {
    Write-Output "kotlin-guard: $($violations.Count) issue(s) found"
    $violations | ForEach-Object { Write-Output "  $_" }
    exit 1
}
Write-Output "kotlin-guard: OK ($(@($files).Count) file(s) checked)"
exit 0
