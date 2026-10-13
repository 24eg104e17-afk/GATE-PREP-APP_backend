@echo off
rem Double-click to start the backend with the Aiven database settings.
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found. Install Maven and add its bin folder to PATH, then try again.
  pause
  exit /b 1
)
echo Starting the backend. When you see "Started GatePrepApplication" it is ready on http://localhost:8080
echo Keep this window open. Press Ctrl+C to stop.
call mvn spring-boot:run -Dspring-boot.run.profiles=aiven
pause
