@echo off
setlocal

rem Keep this wrapper tiny: PowerShell owns the real startup logic.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-jat.ps1" %*

if errorlevel 1 (
  echo.
  echo Jat did not start successfully. Read the message above for the next step.
  pause
  exit /b %errorlevel%
)

endlocal
