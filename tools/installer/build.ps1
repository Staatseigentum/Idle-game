param(
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$MsiPath,
    [string]$OutputDir
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
$wixDir = Join-Path $projectRoot 'build\wix311'
$wixCandidates = @($wixDir)
if ($env:WIX) { $wixCandidates += $env:WIX }
$candleOnPath = Get-Command candle.exe -ErrorAction SilentlyContinue
if ($candleOnPath) { $wixCandidates += Split-Path -Parent $candleOnPath.Source }
if (${env:ProgramFiles(x86)}) {
    $wixCandidates += Join-Path ${env:ProgramFiles(x86)} 'WiX Toolset v3.14\bin'
    $wixCandidates += Join-Path ${env:ProgramFiles(x86)} 'WiX Toolset v3.11\bin'
}
$wixTools = $wixCandidates | ForEach-Object {
    $candidateCandle = Join-Path $_ 'candle.exe'
    $candidateLight = Join-Path $_ 'light.exe'
    if ((Test-Path -LiteralPath $candidateCandle) -and (Test-Path -LiteralPath $candidateLight)) {
        @{ Candle = $candidateCandle; Light = $candidateLight }
    }
} | Select-Object -First 1
if (-not $wixTools) {
    throw 'WiX 3 is missing. Install WiX 3 and run .\gradlew.bat :launcher:packageWindowsSetup.'
}
$candle = $wixTools.Candle
$light = $wixTools.Light
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    throw "Expected a three-part release version, got '$Version'."
}
$msi = (Resolve-Path -LiteralPath $MsiPath).Path
$cover = (Resolve-Path -LiteralPath (Join-Path $projectRoot 'tools\cover\embercrown-cover-installer-314x249.png')).Path
$titlebar = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'titlebar.png')).Path
$titlebarClose = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'titlebar-close.png')).Path
$titlebarMinimize = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'titlebar-minimize.png')).Path
$icon = (Resolve-Path -LiteralPath (Join-Path $projectRoot 'composeApp\build\generated\icons\icon.ico')).Path
$theme = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'EmbercrownTheme.xml')).Path
$localization = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'EmbercrownTheme.wxl')).Path
if (-not $OutputDir) { $OutputDir = Join-Path $projectRoot 'build\installer' }
$OutputDir = [System.IO.Path]::GetFullPath($OutputDir)
$objDir = Join-Path $OutputDir 'obj'
New-Item -ItemType Directory -Path $objDir -Force | Out-Null
$wixObject = Join-Path $objDir 'EmbercrownSetup.wixobj'
$setupExe = Join-Path $OutputDir 'Embercrown-Setup.exe'

& $candle -nologo -ext WixBalExtension "-dVersion=$Version" "-dMsiPath=$msi" "-dIconPath=$icon" "-dCoverPath=$cover" "-dTitlebarPath=$titlebar" "-dTitlebarClosePath=$titlebarClose" "-dTitlebarMinimizePath=$titlebarMinimize" "-dThemePath=$theme" "-dLocalizationPath=$localization" -out $wixObject (Join-Path $PSScriptRoot 'EmbercrownSetup.wxs')
if ($LASTEXITCODE -ne 0) { throw "WiX candle failed with exit code $LASTEXITCODE" }
& $light -nologo -ext WixBalExtension -out $setupExe $wixObject
if ($LASTEXITCODE -ne 0) { throw "WiX light failed with exit code $LASTEXITCODE" }
if (-not (Test-Path -LiteralPath $setupExe)) { throw "WiX did not create $setupExe" }
Write-Output "Created $setupExe"
