@echo off
cd /d "%~dp0"
set "PATH=%PATH%;C:\Program Files\Git\cmd"
echo ============================================================
echo  Push local commits to Gitee
echo  Remote: https://gitee.com/nianqingdeqiaojiangjun/aiuse.git
echo.
echo  A login window may appear. Fill in:
echo    Username : nianqingdeqiaojiangjun
echo    Password : your Gitee personal access token (not the login password)
echo ============================================================
echo.
git push -u origin master
echo.
echo ---------------- result above ----------------
echo If you can see "master -^> master", the upload succeeded.
echo If it says Authentication failed, generate a new token on Gitee and retry.
pause

