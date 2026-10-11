@echo off
cd /d "%~dp0"

echo ============================================================
echo  Start the public tunnel only
echo  (backend must already be running on port 8081)
echo  We force http2 because QUIC is blocked on some networks,
echo  which is what causes "Error 1033 Cloudflare Tunnel error".
echo ============================================================
echo.

netstat -ano | findstr ":8081" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [ERROR] Nothing is listening on port 8081.
  echo Start the backend first: double-click 启动后端.bat
  pause
  exit /b 1
)

if not exist "tools\cloudflared.exe" (
  echo [ERROR] tools\cloudflared.exe not found.
  pause
  exit /b 1
)

echo.
echo Keep this window open while you share the link.
echo Look for the line "Your quick Tunnel has been created" below.
echo The address changes every time you restart this window.
echo.

"tools\cloudflared.exe" tunnel --protocol http2 --url http://localhost:8081 --no-autoupdate

pause
