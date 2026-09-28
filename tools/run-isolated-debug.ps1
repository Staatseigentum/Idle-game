param(
    [ValidatePattern('^[A-Za-z0-9_-]{1,48}$')]
    [string]$Profile
)

# Build a self-contained desktop app and launch a private snapshot of it.
# Running :composeApp:run directly from build/ can invalidate live resource JARs
# when Gradle recompiles the project while the game is open.
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$gradle = Join-Path $projectRoot 'gradlew.bat'
$distribution = Join-Path $projectRoot 'composeApp\build\compose\binaries\main\app\Embercrown'
$debugRoot = Join-Path ([System.IO.Path]::GetTempPath()) 'embercrown-debug-runs'
$runName = 'Embercrown-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
$snapshot = Join-Path $debugRoot $runName

Push-Location $projectRoot
try {
    & $gradle ':composeApp:createDistributable' '--no-daemon' '--quiet'
    if ($LASTEXITCODE -ne 0) { throw 'Desktop distribution build failed.' }
} finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath (Join-Path $distribution 'Embercrown.exe'))) {
    throw "Desktop launcher not found in $distribution"
}

New-Item -ItemType Directory -Path $debugRoot -Force | Out-Null
Copy-Item -LiteralPath $distribution -Destination $snapshot -Recurse
$launcher = Join-Path $snapshot 'Embercrown.exe'
$previousProfile = [Environment]::GetEnvironmentVariable('EMBERCROWN_PROFILE', 'Process')
try {
    if ($Profile) { [Environment]::SetEnvironmentVariable('EMBERCROWN_PROFILE', $Profile, 'Process') }
    Start-Process -FilePath $launcher -WorkingDirectory $snapshot
} finally {
    [Environment]::SetEnvironmentVariable('EMBERCROWN_PROFILE', $previousProfile, 'Process')
}
Write-Output "Launched isolated Embercrown debug build: $launcher"
