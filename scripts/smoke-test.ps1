# Day-1 smoke test: elder profile -> medication plan -> check-in.
# Usage: start the backend first (scripts/run-backend.ps1), then run this script.
param(
    [string]$BaseUrl = "http://localhost:8080/api"
)
$ErrorActionPreference = 'Stop'

$script:failed = $false

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body
    )
    $uri = "$BaseUrl$Path"
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 5
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        $resp = Invoke-WebRequest -Uri $uri -Method $Method -Body $bytes `
            -ContentType "application/json; charset=utf-8" -UseBasicParsing
    } else {
        $resp = Invoke-WebRequest -Uri $uri -Method $Method -UseBasicParsing
    }
    $text = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    return $text | ConvertFrom-Json
}

function Assert([bool]$condition, [string]$message) {
    if ($condition) {
        Write-Host "  PASS: $message" -ForegroundColor Green
    } else {
        $script:failed = $true
        Write-Host "  FAIL: $message" -ForegroundColor Red
    }
}

function Step([string]$title, [scriptblock]$action) {
    Write-Host ""
    Write-Host "=== $title ===" -ForegroundColor Cyan
    try {
        return (& $action)
    } catch {
        $script:failed = $true
        Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Chinese test data built from code points so this script stays ASCII-only (PowerShell 5.1 reads .ps1 as ANSI).
$elderName   = ([char]0x5F20).ToString() + ([char]0x4E09).ToString()                                  # Zhang San
$drugName    = ([char]0x963F).ToString() + ([char]0x53F8).ToString() + ([char]0x5339).ToString() + ([char]0x6797).ToString()
$disease     = ([char]0x9AD8).ToString() + ([char]0x8840).ToString() + ([char]0x538B).ToString()        # hypertension
$afterMeal   = ([char]0x996D).ToString() + ([char]0x540E).ToString()                                    # after meal
$today       = (Get-Date).ToString("yyyy-MM-dd")

# 1. health
$health = Step "1. health check GET /health" {
    $r = Invoke-Api -Method Get -Path "/health"
    Assert ($r.code -eq 0) "health code=0"
    Assert ($r.data.db -eq "UP") "database reachable: db=UP"
    Assert ($r.data.mysqlVersion -like "9.*") "mysql version = $($r.data.mysqlVersion)"
    return $r
}
if (-not $health) {
    Write-Host ""
    Write-Host "Backend is not reachable. Start it first: scripts/run-backend.ps1" -ForegroundColor Red
    exit 1
}

# 2. create elder
$elder = Step "2. create elder POST /elders" {
    $r = Invoke-Api -Method Post -Path "/elders" -Body @{
        name           = $elderName
        age            = 78
        gender         = "M"
        phone          = "13800001234"
        chronicDisease = $disease
        note           = "smoke test"
    }
    Assert ($r.code -eq 0) "create code=0"
    Assert ($r.data.name -eq $elderName) "name round-trip ok: $($r.data.name)"
    Assert ($null -ne $r.data.id) "generated id = $($r.data.id)"
    return $r.data
}
if (-not $elder) { exit 1 }

# 3. list elders
Step "3. list elders GET /elders" {
    $r = Invoke-Api -Method Get -Path "/elders"
    Assert ($r.code -eq 0) "list code=0"
    Assert ($r.data.Count -ge 1) "elder count = $($r.data.Count)"
}

# 4. create medication plan
$plan = Step "4. create plan POST /plans" {
    $r = Invoke-Api -Method Post -Path "/plans" -Body @{
        elderId      = $elder.id
        drugName     = $drugName
        dosage       = "1 tablet"
        frequency    = "twice a day"
        takeRelation = $afterMeal
        remindTimes  = "08:00,20:00"
        startDate    = $today
    }
    Assert ($r.code -eq 0) "create plan code=0"
    Assert ($r.data.elderId -eq $elder.id) "plan belongs to elder $($elder.id)"
    return $r.data
}
if (-not $plan) { exit 1 }

# 5. check in
Step "5. check-in POST /checkins" {
    $r = Invoke-Api -Method Post -Path "/checkins" -Body @{
        elderId    = $elder.id
        planId     = $plan.id
        takeDate   = $today
        remindTime = "08:00"
        status     = "TAKEN"
    }
    Assert ($r.code -eq 0) "check-in code=0"
    Assert ($r.data.status -eq "TAKEN") "status = $($r.data.status)"
}

# 6. duplicate check-in must be rejected
Step "6. duplicate check-in validation" {
    $r = Invoke-Api -Method Post -Path "/checkins" -Body @{
        elderId    = $elder.id
        planId     = $plan.id
        takeDate   = $today
        remindTime = "08:00"
        status     = "TAKEN"
    }
    Assert ($r.code -eq 400) "duplicate check-in returns code=400"
}

# 7. query check-in records
Step "7. list check-ins GET /checkins" {
    $r = Invoke-Api -Method Get -Path "/checkins?elderId=$($elder.id)&date=$today"
    Assert ($r.code -eq 0) "check-in list code=0"
    Assert ($r.data.Count -ge 1) "check-in count = $($r.data.Count)"
}

# 8. parameter validation
Step "8. validation: missing name" {
    $r = Invoke-Api -Method Post -Path "/elders" -Body @{ age = 70 }
    Assert ($r.code -eq 400) "missing name returns code=400"
}

# 9. unknown resource
Step "9. unknown resource GET /elders/999999" {
    $r = Invoke-Api -Method Get -Path "/elders/999999"
    Assert ($r.code -eq 404) "unknown id returns code=404"
}

# 10. soft delete
Step "10. delete elder DELETE /elders/{id}" {
    $r = Invoke-Api -Method Delete -Path "/elders/$($elder.id)"
    Assert ($r.code -eq 0) "delete code=0"
    $check = Invoke-Api -Method Get -Path "/elders/$($elder.id)"
    Assert ($check.code -eq 404) "deleted elder returns code=404"
}

Write-Host ""
if ($script:failed) {
    Write-Host "SMOKE TEST FAILED" -ForegroundColor Red
    exit 1
}
Write-Host "SMOKE TEST PASSED" -ForegroundColor Green

