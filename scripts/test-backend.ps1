# Run JUnit tests (needs MySQL running and application-local.yml configured).
# Tests are @Transactional, so they roll back and leave no data behind.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot "env.ps1")

$projectRoot = Split-Path $PSScriptRoot -Parent
$backendDir = Join-Path $projectRoot "backend"

Push-Location $backendDir
try {
    & mvn test
    $code = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $code
