@echo off
cd /d "%~dp0"
echo ============================================
echo  Starting backend (Spring Boot) on port 8080
echo  Keep this window OPEN while using the app.
echo  Close this window to stop the backend.
echo ============================================
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\run-backend.ps1"
echo.
echo Backend stopped. Press any key to close.
pause >nul

