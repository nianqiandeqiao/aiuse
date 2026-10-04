@echo off
cd /d "%~dp0frontend"
set "NPM=npm.cmd"
where npm.cmd >nul 2>nul || set "NPM=C:\Program Files\nodejs\npm.cmd"
echo ============================================================
echo  Starting frontend dev server (Vite, http://localhost:5173)
echo  Please keep the backend running on port 8081.
echo  Close this window to stop the frontend.
echo ============================================================
call "%NPM%" run dev
echo.
echo Frontend stopped. Press any key to close.
pause >nul

