@echo off
cd /d "%~dp0frontend"
set "NPM=npm.cmd"
where npm.cmd >nul 2>nul || set "NPM=C:\Program Files\nodejs\npm.cmd"
echo ============================================================
echo  Building frontend into backend static resources
echo  Output: backend\src\main\resources\static
echo ============================================================
if not exist "node_modules" (
  echo Installing dependencies with npmmirror registry...
  call "%NPM%" install
  if errorlevel 1 goto :failed
)
call "%NPM%" run build
if errorlevel 1 goto :failed
echo.
echo Build finished. Now start the backend and open http://localhost:8081/
pause >nul
exit /b 0

:failed
echo.
echo Build failed. Check the log above.
pause >nul
exit /b 1

