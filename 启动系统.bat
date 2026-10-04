@echo off
cd /d "%~dp0"
set "NPM=npm.cmd"
where npm.cmd >nul 2>nul || set "NPM=C:\Program Files\nodejs\npm.cmd"

if not exist "backend\src\main\resources\static\index.html" (
  echo Frontend build not found, building it first...
  if not exist "frontend\node_modules" (
    pushd frontend
    call "%NPM%" install
    popd
  )
  pushd frontend
  call "%NPM%" run build
  popd
)

echo ============================================================
echo  Starting full system (backend serves the built frontend)
echo  URL: http://localhost:8081/
echo  Close this window to stop the system.
echo ============================================================
start "" powershell -NoProfile -WindowStyle Hidden -Command "Start-Sleep -Seconds 18; Start-Process 'http://localhost:8081/'"
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\run-backend.ps1"
pause >nul

