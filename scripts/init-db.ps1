# Create the project database (idempotent).
param(
    [string]$MysqlExe = "F:\download\mysql-9.6.0-winx64\mysql-9.6.0\bin\mysql.exe",
    [string]$DbUser = "root",
    [string]$DbPassword = $env:MED_DB_PASSWORD,
    [string]$DbName = "med_reminder"
)
$ErrorActionPreference = 'Stop'

if (-not (Test-Path $MysqlExe)) {
    throw "mysql client not found: $MysqlExe (use -MysqlExe to override)"
}
if (-not $DbPassword) {
    throw "Please pass -DbPassword or set the MED_DB_PASSWORD environment variable"
}

$env:MYSQL_PWD = $DbPassword

& $MysqlExe "--user=$DbUser" "-e" "CREATE DATABASE IF NOT EXISTS $DbName CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
if ($LASTEXITCODE -ne 0) {
    throw "CREATE DATABASE failed with exit code $LASTEXITCODE"
}

& $MysqlExe "--user=$DbUser" "--database=$DbName" "-e" "SELECT DATABASE() AS current_db, VERSION() AS mysql_version;"
if ($LASTEXITCODE -ne 0) {
    throw "Verify database failed with exit code $LASTEXITCODE"
}

$env:MYSQL_PWD = $null
Write-Host "[db] database '$DbName' is ready"
