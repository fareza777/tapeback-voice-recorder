# Kiro hook: keep user-facing copy consistent.
# - strings.xml must be well-formed XML with escaped apostrophes
# - every R.string / R.plurals referenced from Kotlin must exist (and report unused ones)
. "$PSScriptRoot\_common.ps1"
$null = Read-HookInput
$problems = @()

$stringsPath = Join-Path $RepoRoot "app/src/main/res/values/strings.xml"
try { [xml]$xml = Get-Content $stringsPath -Raw -Encoding UTF8 } catch { Write-Output "strings-guard: strings.xml is not valid XML: $_"; exit 1 }

$defined = @{}
foreach ($s in $xml.resources.string) {
    $defined[$s.name] = $true
    if ($s.InnerText -match "(?<!\\)'") { $problems += "Unescaped apostrophe in string '$($s.name)'" }
}
$plurals = @{}
foreach ($p in $xml.resources.plurals) { $plurals[$p.name] = $true }

$used = @{}
Get-ChildItem (Join-Path $RepoRoot "app/src/main") -Recurse -Include *.kt, *.xml | Where-Object { $_.FullName -ne $stringsPath } | ForEach-Object {
    $text = Get-Content $_.FullName -Raw
    foreach ($m in [regex]::Matches($text, 'R\.string\.([a-z0-9_]+)|@string/([a-z0-9_]+)')) {
        $name = if ($m.Groups[1].Success) { $m.Groups[1].Value } else { $m.Groups[2].Value }
        $used[$name] = $true
        if (-not $defined.ContainsKey($name)) { $problems += "Missing string '$name' referenced in $($_.Name)" }
    }
    foreach ($m in [regex]::Matches($text, 'R\.plurals\.([a-z0-9_]+)')) {
        if (-not $plurals.ContainsKey($m.Groups[1].Value)) { $problems += "Missing plurals '$($m.Groups[1].Value)'" }
    }
}
# Shortcut labels live in build-type resource folders.
Get-ChildItem (Join-Path $RepoRoot "app/src") -Recurse -Filter shortcuts.xml | ForEach-Object {
    foreach ($m in [regex]::Matches((Get-Content $_.FullName -Raw), '@string/([a-z0-9_]+)')) { $used[$m.Groups[1].Value] = $true }
}
$unused = $defined.Keys | Where-Object { -not $used.ContainsKey($_) } | Sort-Object

if ($problems.Count -gt 0) {
    Write-Output "strings-guard: FAILED"
    $problems | Select-Object -Unique | ForEach-Object { Write-Output "  $_" }
    exit 1
}
if ($unused.Count -gt 0) { Write-Output "strings-guard: note - unused strings: $($unused -join ', ')" }
Write-Output "strings-guard: OK ($($defined.Count) strings, all references resolve)"
exit 0
