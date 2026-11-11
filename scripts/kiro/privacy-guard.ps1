# Kiro hook: TapeBack is offline by construction. Fail if anything introduces network access.
# Checks: source manifest removes INTERNET, merged manifest (if built) has no INTERNET,
# Kotlin sources use no networking APIs, build files add no network/analytics libraries.
. "$PSScriptRoot\_common.ps1"
$null = Read-HookInput
$problems = @()

$manifest = Join-Path $RepoRoot "app/src/main/AndroidManifest.xml"
$m = Get-Content $manifest -Raw
if ($m -notmatch 'android\.permission\.INTERNET"\s+tools:node="remove"') {
    $problems += "AndroidManifest.xml must declare INTERNET with tools:node=`"remove`""
}

Get-ChildItem (Join-Path $RepoRoot "app/build/intermediates") -Recurse -Filter AndroidManifest.xml -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match 'merged_manifest' } | ForEach-Object {
        if ((Get-Content $_.FullName -Raw) -match 'uses-permission[^>]+android\.permission\.INTERNET') {
            $problems += "Merged manifest requests INTERNET: $($_.FullName.Substring($RepoRoot.Length + 1))"
        }
    }

$forbiddenCode = 'java\.net\.(URL|HttpURLConnection|Socket)|okhttp3|retrofit2|io\.ktor|com\.google\.firebase|com\.google\.android\.gms\.ads|WebView'
Get-ChildItem (Join-Path $RepoRoot "app/src/main") -Recurse -Filter *.kt | ForEach-Object {
    $hits = Select-String -Path $_.FullName -Pattern $forbiddenCode
    foreach ($h in $hits) { $problems += "Network/analytics API in $($_.Name):$($h.LineNumber): $($h.Line.Trim())" }
}

$forbiddenDeps = 'okhttp|retrofit|ktor|firebase|play-services-ads|analytics|crashlytics|coil|glide'
foreach ($f in @("app/build.gradle.kts", "gradle/libs.versions.toml")) {
    $hits = Select-String -Path (Join-Path $RepoRoot $f) -Pattern $forbiddenDeps
    foreach ($h in $hits) { $problems += "Network-capable dependency in ${f}:$($h.LineNumber): $($h.Line.Trim())" }
}

if ($problems.Count -gt 0) {
    Write-Output "privacy-guard: FAILED"
    $problems | ForEach-Object { Write-Output "  $_" }
    exit 1
}
Write-Output "privacy-guard: OK (no INTERNET permission, no network APIs or libraries)"
exit 0
