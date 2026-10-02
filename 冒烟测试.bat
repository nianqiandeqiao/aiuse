@echo off
cd /d "%~dp0"
echo ============================================
echo  Smoke test: elder -^> plan -^> check-in
echo  Backend must be running first!
echo ============================================
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\smoke-test.ps1"
echo.
pause >nul

