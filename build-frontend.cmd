@echo off
chcp 65001 >nul
cd /d "%~dp0frontend"
call npm ci
if errorlevel 1 exit /b 1
call npm run build
if errorlevel 1 exit /b 1
echo Frontend build completed: src\main\resources\static\app
