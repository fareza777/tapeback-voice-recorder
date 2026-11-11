# TapeBack quality gate, used by Kiro hooks and before every push.
#   -Fast : static guards + JVM unit tests
#   -Full : static guards + unit tests + Android lint + debug assemble
param([switch]$Fast, [switch]$Full)
. "$PSScriptRoot\_common.ps1"
$null = Read-HookInput
if (-not $Fast -and -not $Full) { $Fast = $true }

if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}

$failed = @()
foreach ($guard in @("kotlin-guard.ps1", "privacy-guard.ps1", "strings-guard.ps1")) {
    $psArgs = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot $guard))
    if ($guard -eq "kotlin-guard.ps1") { $psArgs += "-All" }
    $out = & powershell @psArgs
    $out | ForEach-Object { Write-Output $_ }
    if ($LASTEXITCODE -ne 0) { $failed += $guard }
}

$tasks = @("testDebugUnitTest")
if ($Full) { $tasks += @("lintDebug", "assembleDebug") }

Push-Location $RepoRoot
try {
    Write-Output "quality-gate: gradlew $($tasks -join ' ')"
    $log = & .\gradlew.bat @tasks --console=plain 2>&1
    $code = $LASTEXITCODE
    $log | Select-String -Pattern '^e: |FAILED|What went wrong|BUILD (SUCCESSFUL|FAILED)|Lint found' | ForEach-Object { Write-Output "  $($_.Line)" }
    if ($code -ne 0) { $failed += "gradle" }

    $results = Get-ChildItem "app/build/test-results/testDebugUnitTest" -Filter *.xml -ErrorAction SilentlyContinue
    if ($results) {
        $total = 0; $failures = 0
        foreach ($r in $results) {
            $suite = ([xml](Get-Content $r.FullName)).testsuite
            $total += [int]$suite.tests
            $failures += [int]$suite.failures + [int]$suite.errors
        }
        Write-Output "quality-gate: unit tests $($total - $failures)/$total passing"
    }
} finally { Pop-Location }

if ($failed.Count -gt 0) {
    Write-Output "quality-gate: FAILED ($($failed -join ', '))"
    exit 1
}
Write-Output "quality-gate: PASSED ($(if ($Full) { 'full' } else { 'fast' }))"
exit 0
