# Push local commits to GitHub (this is the repo Render deploys from).
# A GitHub token is normally already stored in Windows Credential Manager,
# so this usually pushes without asking anything. If it fails, the script
# asks for a personal access token once and stores it for later runs.
$ErrorActionPreference = 'Continue'

$gitCmd = "C:\Program Files\Git\cmd"
if (Test-Path $gitCmd) { $env:Path = $env:Path + ";" + $gitCmd }

$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot

Write-Host "============================================================"
Write-Host " Push code to GitHub (Render pulls from here)"
Write-Host " Remote: https://github.com/nianqiandeqiao/aiuse.git"
Write-Host "============================================================"
Write-Host ""

$remotes = (git remote) -split "`r?`n"
if ($remotes -notcontains "github") {
    Write-Host "Remote 'github' not found, adding it ..."
    git remote add github https://github.com/nianqiandeqiao/aiuse.git
}

Write-Host "Step 1/2  Pushing branch master to GitHub ..."
Write-Host ""
git push github master
$code = $LASTEXITCODE

if ($code -ne 0) {
    Write-Host ""
    Write-Host "Push failed (maybe the saved token expired)." -ForegroundColor Yellow
    Write-Host "Step 2/2  GitHub username and personal access token"
    Write-Host "          Create a token at: GitHub - Settings - Developer settings"
    Write-Host "          - Personal access tokens (classic), scope: repo"
    $user = Read-Host "          GitHub username (Enter for nianqiandeqiao)"
    if ([string]::IsNullOrWhiteSpace($user)) { $user = "nianqiandeqiao" }
    $secure = Read-Host "          paste token then press Enter" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    $token = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    if ([string]::IsNullOrWhiteSpace($token)) {
        Write-Host "FAILED: token is empty. Nothing was pushed." -ForegroundColor Red
        exit 1
    }
    "protocol=https`nhost=github.com`nusername=$user`npassword=$token`n`n" | git credential approve 2>&1 | Out-Null
    git push github master
    $code = $LASTEXITCODE
}

Write-Host ""
if ($code -eq 0) {
    Write-Host "SUCCESS: GitHub is up to date." -ForegroundColor Green
    Write-Host "Render will rebuild automatically in about a minute."
    Write-Host "Open https://github.com/nianqiandeqiao/aiuse to confirm."
} else {
    Write-Host "FAILED (exit code $code)." -ForegroundColor Red
    Write-Host "Screenshot the lines above and send them to Qiao."
}
