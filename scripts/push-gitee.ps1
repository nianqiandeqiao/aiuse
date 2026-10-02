# Push local commits to Gitee with a personal access token.
# The token is typed into this window (hidden) and stored in Windows Credential Manager,
# so later "git push" runs will not ask again.
$ErrorActionPreference = 'Continue'

$gitCmd = "C:\Program Files\Git\cmd"
if (Test-Path $gitCmd) { $env:Path = $env:Path + ";" + $gitCmd }

$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot

Write-Host "============================================================"
Write-Host " Push code to Gitee"
Write-Host " Remote: https://gitee.com/nianqingdeqiaojiangjun/aiuse.git"
Write-Host "============================================================"
Write-Host ""
Write-Host "Step 1/3  Gitee username"
$user = Read-Host "          (just press Enter for nianqingdeqiaojiangjun)"
if ([string]::IsNullOrWhiteSpace($user)) { $user = "nianqingdeqiaojiangjun" }

Write-Host ""
Write-Host "Step 2/3  Gitee personal access token (input is hidden)"
Write-Host "          Generate it at: Gitee - Settings - Security - Personal access tokens"
Write-Host "          Scope must include: projects"
$secure = Read-Host "          paste token then press Enter" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
$token = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host ""
    Write-Host "FAILED: token is empty. Nothing was pushed." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Saving credential to Windows Credential Manager ..."
$credential = "protocol=https`nhost=gitee.com`nusername=$user`npassword=$token`n`n"
$credential | git credential approve 2>&1 | Out-Null

Write-Host "Step 3/3  Pushing to Gitee ..."
Write-Host ""
git push -u origin master
$code = $LASTEXITCODE

Write-Host ""
if ($code -eq 0) {
    Write-Host "SUCCESS: your code is now on Gitee." -ForegroundColor Green
    Write-Host "Open https://gitee.com/nianqingdeqiaojiangjun/aiuse and refresh to see backend / docs / scripts."
} else {
    Write-Host "FAILED (exit code $code)." -ForegroundColor Red
    Write-Host "Common causes:"
    Write-Host "  1) token scope does not include 'projects' -> generate a new token"
    Write-Host "  2) wrong username -> use the account you log in to Gitee with"
    Write-Host "  3) the Gitee repo already has commits (README) -> tell Qiao, he will merge it"
    Write-Host ""
    Write-Host "Screenshot the lines above and send them to Qiao."
}

