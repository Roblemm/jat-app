@echo off
setlocal

rem Creates or refreshes the Desktop shortcut and its local icon.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-desktop-shortcut.ps1" %*

if errorlevel 1 (
  echo.
  echo The Jat Desktop shortcut was not created. Read the message above for the next step.
  pause
  exit /b %errorlevel%
)

echo.
echo Jat Desktop shortcut is ready.
pause
endlocal
