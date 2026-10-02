# Start the backend service (Ctrl+C to stop).
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot "env.ps1")

$projectRoot = Split-Path $PSScriptRoot -Parent
$backendDir = Join-Path $projectRoot "backend"

Push-Location $backendDir
try {
    & mvn spring-boot:run
} finally {
    Pop-Location
}

