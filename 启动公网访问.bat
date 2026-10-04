@echo off
cd /d "%~dp0"
set "NPM=npm.cmd"
where npm.cmd >nul 2>nul || set "NPM=C:\Program Files\nodejs\npm.cmd"

echo ============================================================
echo  Publish this system to the Internet
echo  (Cloudflare quick tunnel, free, no signup needed)
echo ============================================================
echo.

netstat -ano | findstr ":8081" | findstr "LISTENING" >nul
if not errorlevel 1 (
  echo [ERROR] Port 8081 is already in use.
  echo Close the running backend window first, then run this script again.
  echo The access password must be set BEFORE the backend starts.
  pause
  exit /b 1
)

if not exist "backend\src\main\resources\static\index.html" (
  echo Frontend build not found, building it first...
  pushd frontend
  if not exist "node_modules" call "%NPM%" install
  call "%NPM%" run build
  popd
)

if not exist "tools\cloudflared.exe" (
  echo Downloading cloudflared ^(about 55MB^), please wait...
  if not exist "tools" mkdir tools
  curl.exe -L -o "tools\cloudflared.exe" "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"
  if errorlevel 1 (
    echo [ERROR] Download failed. Check your network and retry.
    pause
    exit /b 1
  )
)

echo.
echo Visitors must type this password to see any data.
echo Do NOT leave it empty: the URL is public.
set /p ACCESS_PW=Set access password: 
if "%ACCESS_PW%"=="" (
  echo [ERROR] Empty password is refused for public access.
  pause
  exit /b 1
)
set "APP_ACCESS_PASSWORD=%ACCESS_PW%"

echo.
echo Starting backend with the access password...
start "med-backend" cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run-backend.ps1"
echo Waiting for the backend to become ready...
timeout /t 22 /nobreak >nul

echo.
echo Starting public tunnel. Your public URL appears below:
echo    https://xxxx-xxxx-xxxx.trycloudflare.com
echo Send that URL to others. Press Ctrl+C here to stop sharing.
echo.
"tools\cloudflared.exe" tunnel --url http://localhost:8081 --no-autoupdate
pause

