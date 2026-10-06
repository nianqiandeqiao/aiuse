# Interface smoke test: elder profile -> medication plan -> check-in -> today list -> stats.
# Usage: start the backend first (scripts/run-backend.ps1), then run this script.
param(
    [string]$BaseUrl = "http://localhost:8081/api"
)
$ErrorActionPreference = 'Stop'

$script:passed = 0
$script:failed = 0
# Every data API requires login now; the whole script reuses one session.
$script:session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body
    )
    $uri = "$BaseUrl$Path"
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 6
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        $resp = Invoke-WebRequest -Uri $uri -Method $Method -Body $bytes `
            -ContentType "application/json; charset=utf-8" -UseBasicParsing -WebSession $script:session
    } else {
        $resp = Invoke-WebRequest -Uri $uri -Method $Method -UseBasicParsing -WebSession $script:session
    }
    $text = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    return $text | ConvertFrom-Json
}

function Assert([bool]$condition, [string]$message) {
    if ($condition) {
        $script:passed++
        Write-Host "  PASS: $message" -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host "  FAIL: $message" -ForegroundColor Red
    }
}

function Step([string]$title, [scriptblock]$action) {
    Write-Host ""
    Write-Host "=== $title ===" -ForegroundColor Cyan
    try {
        return (& $action)
    } catch {
        $script:failed++
        Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Chinese test data built from code points so this script stays ASCII-only
# (Windows PowerShell 5.1 reads .ps1 files with the ANSI code page).
function Zh([int[]]$codePoints) {
    return ($codePoints | ForEach-Object { [char]$_ }) -join ''
}
$elderName   = Zh @(0x5F20, 0x4E09)                             # Zhang San
$newName     = Zh @(0x674E, 0x56DB)                             # Li Si
$drugName    = Zh @(0x963F, 0x53F8, 0x5339, 0x6797)             # aspirin
$drugName2   = Zh @(0x534E, 0x6CD5, 0x6797)                     # warfarin
$disease     = Zh @(0x9AD8, 0x8840, 0x538B)                     # hypertension
$afterMeal   = Zh @(0x996D, 0x540E)                             # after meal
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

# 1.5 register a test account (required because data APIs need login)
Step "1.5 register test account POST /auth/register" {
    $username = "smoke" + (Get-Date -Format "MMddHHmmss")
    $r = Invoke-Api -Method Post -Path "/auth/register" -Body @{
        username    = $username
        password    = "smoke123456"
        displayName = "smoke test"
    }
    Assert ($r.code -eq 0) "register code=0"
    Assert ($r.data.username -eq $username) "registered username = $username"
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

# 4. search elders by keyword (Chinese)
Step "4. search elders GET /elders?keyword=" {
    $encoded = [System.Uri]::EscapeDataString($elderName)
    $r = Invoke-Api -Method Get -Path "/elders?keyword=$encoded"
    Assert ($r.code -eq 0) "search code=0"
    Assert ($r.data.Count -ge 1) "search hit count = $($r.data.Count)"
}

# 5. update elder
Step "5. update elder PUT /elders/{id}" {
    $r = Invoke-Api -Method Put -Path "/elders/$($elder.id)" -Body @{
        name           = $newName
        age            = 79
        gender         = "M"
        phone          = "13900000000"
        chronicDisease = $disease
        note           = "updated"
    }
    Assert ($r.code -eq 0) "update code=0"
    Assert ($r.data.name -eq $newName) "name updated to $($r.data.name)"
    Assert ($r.data.age -eq 79) "age updated to $($r.data.age)"
}

# 6. create plan
$plan = Step "6. create plan POST /plans" {
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
    Assert ($r.data.remindTimes -eq "08:00,20:00") "remind times = $($r.data.remindTimes)"
    return $r.data
}
if (-not $plan) { exit 1 }

# 7. list plans by elder
Step "7. list plans GET /plans?elderId=" {
    $r = Invoke-Api -Method Get -Path "/plans?elderId=$($elder.id)"
    Assert ($r.code -eq 0) "plan list code=0"
    Assert ($r.data.Count -ge 1) "plan count = $($r.data.Count)"
}

# 8. update plan
Step "8. update plan PUT /plans/{id}" {
    $r = Invoke-Api -Method Put -Path "/plans/$($plan.id)" -Body @{
        drugName     = $drugName2
        dosage       = "2 tablets"
        frequency    = "twice a day"
        takeRelation = $afterMeal
        remindTimes  = "08:00,20:00"
        startDate    = $today
    }
    Assert ($r.code -eq 0) "update plan code=0"
    Assert ($r.data.drugName -eq $drugName2) "drug name updated to $($r.data.drugName)"
    Assert ($r.data.dosage -eq "2 tablets") "dosage updated to $($r.data.dosage)"
}

# 9. today dose list (before check-in)
Step "9. today list GET /plans/today (before check-in)" {
    $r = Invoke-Api -Method Get -Path "/plans/today?elderId=$($elder.id)&date=$today"
    Assert ($r.code -eq 0) "today list code=0"
    Assert ($r.data.total -eq 2) "total doses today = $($r.data.total)"
    Assert ($r.data.taken -eq 0) "taken = $($r.data.taken)"
    Assert ($r.data.items[0].remindTime -eq "08:00") "first remind time = $($r.data.items[0].remindTime)"
}

# 10. check-in
$log = Step "10. check-in POST /checkins" {
    $r = Invoke-Api -Method Post -Path "/checkins" -Body @{
        elderId    = $elder.id
        planId     = $plan.id
        takeDate   = $today
        remindTime = "08:00"
        status     = "TAKEN"
    }
    Assert ($r.code -eq 0) "check-in code=0"
    Assert ($r.data.status -eq "TAKEN") "status = $($r.data.status)"
    return $r.data
}

# 11. duplicate check-in
Step "11. duplicate check-in validation" {
    $r = Invoke-Api -Method Post -Path "/checkins" -Body @{
        elderId    = $elder.id
        planId     = $plan.id
        takeDate   = $today
        remindTime = "08:00"
        status     = "TAKEN"
    }
    Assert ($r.code -eq 400) "duplicate check-in returns code=400"
}

# 12. today list after check-in
Step "12. today list after check-in" {
    $r = Invoke-Api -Method Get -Path "/plans/today?elderId=$($elder.id)&date=$today"
    Assert ($r.data.taken -eq 1) "taken = $($r.data.taken)"
    Assert ($r.data.pending -eq 1) "pending = $($r.data.pending)"
    Assert ($r.data.items[0].taken -eq $true) "08:00 marked as taken"
}

# 13. stats
Step "13. stats GET /checkins/stats" {
    $r = Invoke-Api -Method Get -Path "/checkins/stats?elderId=$($elder.id)&from=$today&to=$today"
    Assert ($r.code -eq 0) "stats code=0"
    Assert ($r.data.expected -eq 2) "expected doses = $($r.data.expected)"
    Assert ($r.data.taken -eq 1) "taken doses = $($r.data.taken)"
    Assert ($r.data.missed -eq 1) "missed doses = $($r.data.missed)"
    Assert ($r.data.rate -eq 0.5) "on-time rate = $($r.data.rate)"
}

# 14. list check-ins by date range
Step "14. list check-ins GET /checkins?from=&to=" {
    $r = Invoke-Api -Method Get -Path "/checkins?elderId=$($elder.id)&from=$today&to=$today"
    Assert ($r.code -eq 0) "range query code=0"
    Assert ($r.data.Count -eq 1) "records in range = $($r.data.Count)"
}

# 15. undo check-in
Step "15. undo check-in DELETE /checkins/{id}" {
    if (-not $log) { throw "no check-in record to undo" }
    $r = Invoke-Api -Method Delete -Path "/checkins/$($log.id)"
    Assert ($r.code -eq 0) "undo code=0"
    $after = Invoke-Api -Method Get -Path "/plans/today?elderId=$($elder.id)&date=$today"
    Assert ($after.data.taken -eq 0) "taken back to 0 after undo"
    $missing = Invoke-Api -Method Delete -Path "/checkins/$($log.id)"
    Assert ($missing.code -eq 404) "undo again returns code=404"
}

# 16. stop plan, then today list should be empty
Step "16. stop plan PUT /plans/{id}/status?status=0" {
    $r = Invoke-Api -Method Put -Path "/plans/$($plan.id)/status?status=0"
    Assert ($r.code -eq 0) "stop plan code=0"
    Assert ($r.data.status -eq 0) "plan status = $($r.data.status)"
    $todayList = Invoke-Api -Method Get -Path "/plans/today?elderId=$($elder.id)&date=$today"
    Assert ($todayList.data.total -eq 0) "today list is empty after stop"
}

# 17. resume plan
Step "17. resume plan PUT /plans/{id}/status?status=1" {
    $r = Invoke-Api -Method Put -Path "/plans/$($plan.id)/status?status=1"
    Assert ($r.code -eq 0) "resume plan code=0"
    $todayList = Invoke-Api -Method Get -Path "/plans/today?elderId=$($elder.id)&date=$today"
    Assert ($todayList.data.total -eq 2) "today list restored to 2"
    $bad = Invoke-Api -Method Put -Path "/plans/$($plan.id)/status?status=7"
    Assert ($bad.code -eq 400) "illegal status returns code=400"
}

# 18. validation: missing name / bad remind times
Step "18. parameter validation" {
    $r = Invoke-Api -Method Post -Path "/elders" -Body @{ age = 70 }
    Assert ($r.code -eq 400) "missing elder name returns code=400"
    $badTime = "8" + (Zh @(0x70B9))
    $bad = Invoke-Api -Method Post -Path "/plans" -Body @{
        elderId     = $elder.id
        drugName    = $drugName
        dosage      = "1 tablet"
        remindTimes = $badTime
    }
    Assert ($bad.code -eq 400) "bad remindTimes format returns code=400"
}

# 19. unknown resources
Step "19. unknown resources" {
    $r = Invoke-Api -Method Get -Path "/elders/999999"
    Assert ($r.code -eq 404) "unknown elder returns code=404"
    $p = Invoke-Api -Method Get -Path "/plans/999999"
    Assert ($p.code -eq 404) "unknown plan returns code=404"
}

# 20. cleanup: delete plan, then elder
Step "20. cleanup DELETE /plans/{id} and /elders/{id}" {
    $r = Invoke-Api -Method Delete -Path "/plans/$($plan.id)"
    Assert ($r.code -eq 0) "delete plan code=0"
    $d = Invoke-Api -Method Delete -Path "/elders/$($elder.id)"
    Assert ($d.code -eq 0) "delete elder code=0"
    $check = Invoke-Api -Method Get -Path "/elders/$($elder.id)"
    Assert ($check.code -eq 404) "deleted elder returns code=404"
}

# 21. OCR: draw a drug photo with GDI+, then upload it to the OCR endpoint
Step "21. OCR recognize drug photo POST /ocr/drug" {
    Add-Type -AssemblyName System.Drawing
    $imgPath = Join-Path $env:TEMP "smoke-drug.png"
    $bmp = New-Object System.Drawing.Bitmap 900, 320
    $graphics = [System.Drawing.Graphics]::FromImage($bmp)
    $graphics.Clear([System.Drawing.Color]::White)
    $font = New-Object System.Drawing.Font("Microsoft YaHei", 46)
    $graphics.DrawString(($drugName + (Zh @(0x80A0, 0x6EB6, 0x7247))), $font, [System.Drawing.Brushes]::Black, 20, 30)
    $graphics.DrawString(($drugName2 + (Zh @(0x94A0, 0x7247))), $font, [System.Drawing.Brushes]::Black, 20, 160)
    $graphics.Dispose()
    $bmp.Save($imgPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    $outFile = Join-Path $env:TEMP "smoke-ocr.json"
    # curl does not share the PowerShell session, so pass the login cookie manually
    $cookie = $script:session.Cookies.GetCookies([Uri]$BaseUrl) |
        Where-Object { $_.Name -eq "JSESSIONID" } | Select-Object -First 1
    $cookieHeader = "JSESSIONID=" + $(if ($cookie) { $cookie.Value } else { "" })
    curl.exe -sS --max-time 60 -X POST -H "Cookie: $cookieHeader" -F "file=@$imgPath" `
        "$BaseUrl/ocr/drug" -o $outFile | Out-Null
    $r = [System.IO.File]::ReadAllText($outFile, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
    Assert ($r.code -eq 0) "ocr code=0"
    Assert ($r.data.lineCount -ge 2) "ocr recognized lines = $($r.data.lineCount)"
    Assert ($r.data.drugCount -ge 2) "ocr matched drugs = $($r.data.drugCount)"
    $names = $r.data.drugs | ForEach-Object { $_.standardName }
    Assert ($names -contains $drugName) "matched standard drug: $drugName"
    Assert ($names -contains $drugName2) "matched standard drug: $drugName2"
}

# 22. interaction check by drug names: warfarin + aspirin should be a severe conflict
Step "22. interaction check by drug names POST /interactions/check-drugs" {
    $r = Invoke-Api -Method Post -Path "/interactions/check-drugs" -Body @{
        drugNames = @($drugName2, $drugName)
    }
    Assert ($r.code -eq 0) "check code=0"
    Assert ($r.data.conflictCount -ge 1) "conflict count = $($r.data.conflictCount)"
    Assert ($r.data.maxSeverity -eq 1) "max severity = $($r.data.maxSeverity) ($($r.data.maxSeverityLabel))"
    $first = $r.data.conflicts[0]
    Assert ($first.type -eq "INTERACTION") "conflict type = $($first.type)"
    Assert ($null -ne $first.source) "rule source present: $($first.source)"
}

# 23. interaction rule library
Step "23. interaction rules GET /interactions/rules" {
    $r = Invoke-Api -Method Get -Path "/interactions/rules"
    Assert ($r.code -eq 0) "rules code=0"
    Assert ($r.data.Count -ge 10) "rule count = $($r.data.Count)"
}

# 24. interaction check by elder (creates two plans: aspirin + warfarin)
Step "24. interaction check by elder POST /interactions/check" {
    $e = Invoke-Api -Method Post -Path "/elders" -Body @{
        name = (Zh @(0x51B2, 0x7A81, 0x6D4B, 0x8BD5))   # conflict test elder
        age  = 81
    }
    $p1 = Invoke-Api -Method Post -Path "/plans" -Body @{
        elderId     = $e.data.id
        drugName    = $drugName
        dosage      = "1 tablet"
        remindTimes = "08:00"
        startDate   = $today
    }
    $p2 = Invoke-Api -Method Post -Path "/plans" -Body @{
        elderId     = $e.data.id
        drugName    = $drugName2
        dosage      = "1 tablet"
        remindTimes = "20:00"
        startDate   = $today
    }
    $r = Invoke-Api -Method Post -Path "/interactions/check" -Body @{ elderId = $e.data.id }
    Assert ($r.code -eq 0) "elder check code=0"
    Assert ($r.data.drugCount -eq 2) "checked drugs = $($r.data.drugCount)"
    Assert ($r.data.maxSeverity -eq 1) "elder conflict max severity = $($r.data.maxSeverity)"

    Invoke-Api -Method Delete -Path "/plans/$($p1.data.id)" | Out-Null
    Invoke-Api -Method Delete -Path "/plans/$($p2.data.id)" | Out-Null
    Invoke-Api -Method Delete -Path "/elders/$($e.data.id)" | Out-Null
}

# 25. stock and expiry alerts
Step "25. stock/expiry alerts GET /plans/alerts" {
    $e = Invoke-Api -Method Post -Path "/elders" -Body @{ name = (Zh @(0x5E93, 0x5B58, 0x6D4B, 0x8BD5)); age = 83 }
    $soon = (Get-Date).AddDays(15).ToString("yyyy-MM-dd")
    $plan = Invoke-Api -Method Post -Path "/plans" -Body @{
        elderId       = $e.data.id
        drugName      = $drugName
        dosage        = "1 tablet"
        remindTimes   = "08:00"
        startDate     = $today
        expireDate    = $soon
        stockQuantity = 2
        stockUnit     = "tablet"
    }
    Assert ($plan.code -eq 0) "plan with expire/stock created"
    $alerts = Invoke-Api -Method Get -Path "/plans/alerts?elderId=$($e.data.id)"
    Assert ($alerts.code -eq 0) "alerts code=0"
    $types = $alerts.data | ForEach-Object { $_.type }
    Assert ($types -contains "EXPIRING") "expiring alert present"
    Assert ($types -contains "LOW_STOCK") "low stock alert present"
    Invoke-Api -Method Delete -Path "/plans/$($plan.data.id)" | Out-Null
    Invoke-Api -Method Delete -Path "/elders/$($e.data.id)" | Out-Null
}

# 26. adherence report (week)
Step "26. adherence report GET /reports/adherence" {
    $e = Invoke-Api -Method Post -Path "/elders" -Body @{ name = (Zh @(0x62A5, 0x544A, 0x6D4B, 0x8BD5)); age = 84 }
    $plan = Invoke-Api -Method Post -Path "/plans" -Body @{
        elderId     = $e.data.id
        drugName    = $drugName
        dosage      = "1 tablet"
        remindTimes = "08:00,20:00"
        startDate   = (Get-Date).AddDays(-3).ToString("yyyy-MM-dd")
    }
    Invoke-Api -Method Post -Path "/checkins" -Body @{
        elderId = $e.data.id; planId = $plan.data.id; takeDate = $today; remindTime = "08:00"; status = "TAKEN"
    } | Out-Null
    $report = (Invoke-Api -Method Get -Path "/reports/adherence?elderId=$($e.data.id)&period=week").data
    Assert ($report.daily.Count -eq 7) "weekly report has 7 days"
    Assert ($report.expected -ge 2) "expected doses = $($report.expected)"
    Assert ($report.taken -eq 1) "taken doses = $($report.taken)"
    Assert ($report.missed -ge 1) "missed doses = $($report.missed)"
    Assert ($null -ne $report.summary) "report summary present"
    Invoke-Api -Method Delete -Path "/plans/$($plan.data.id)" | Out-Null
    Invoke-Api -Method Delete -Path "/elders/$($e.data.id)" | Out-Null
}

# 27. AI assistant (rule fallback when no LLM key is configured)
Step "27. AI assistant POST /ai/ask and /ai/medicine-analysis" {
    $e = Invoke-Api -Method Post -Path "/elders" -Body @{ name = (Zh @(0x95EE, 0x836F, 0x6D4B, 0x8BD5)); age = 85 }
    $p1 = Invoke-Api -Method Post -Path "/plans" -Body @{
        elderId = $e.data.id; drugName = $drugName; dosage = "1 tablet"; remindTimes = "08:00"
    }
    $p2 = Invoke-Api -Method Post -Path "/plans" -Body @{
        elderId = $e.data.id; drugName = $drugName2; dosage = "1 tablet"; remindTimes = "20:00"
    }
    $ask = Invoke-Api -Method Post -Path "/ai/ask" -Body @{
        elderId = $e.data.id; question = "what can I take together"; dialect = "henan"
    }
    Assert ($ask.code -eq 0) "ai ask code=0"
    Assert (($ask.data.source -eq "RULE") -or ($ask.data.source -eq "LLM")) "answer source = $($ask.data.source)"
    Assert ($ask.data.answer.Length -gt 5) "answer not empty"

    $analysis = Invoke-Api -Method Post -Path "/ai/medicine-analysis" -Body @{
        elderId = $e.data.id
        text    = "aspirin 08:00 expire 2020-01-01"
    }
    Assert ($analysis.code -eq 0) "medicine analysis code=0"
    Assert ($analysis.data.advices.Count -ge 1) "advices count = $($analysis.data.advices.Count)"

    Invoke-Api -Method Delete -Path "/plans/$($p1.data.id)" | Out-Null
    Invoke-Api -Method Delete -Path "/plans/$($p2.data.id)" | Out-Null
    Invoke-Api -Method Delete -Path "/elders/$($e.data.id)" | Out-Null
}

Write-Host ""
Write-Host "----------------------------------------"
Write-Host ("PASS: {0}   FAIL: {1}" -f $script:passed, $script:failed)
if ($script:failed -gt 0) {
    Write-Host "SMOKE TEST FAILED" -ForegroundColor Red
    exit 1
}
Write-Host "SMOKE TEST PASSED" -ForegroundColor Green
