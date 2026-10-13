@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
echo ==================================================================
echo   SIST CSE AI Capstone Project -- Building and Running Test Suite
echo ==================================================================

if not exist "backend\bin" mkdir backend\bin

powershell -NoProfile -Command "(Get-ChildItem -Path backend\src -Recurse -Filter *.java).FullName | Out-File -Encoding ascii sources_src.txt; javac -encoding UTF-8 -d backend\bin -cp 'backend\lib\*' '@sources_src.txt'; (Get-ChildItem -Path backend\test -Filter *.java).FullName | Out-File -Encoding ascii sources_test.txt; javac -encoding UTF-8 -d backend\bin -cp 'backend\bin;backend\lib\*' '@sources_test.txt'"
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    exit /b 1
)

java -cp "backend\bin;backend\lib\*" test.TestRunner
endlocal
