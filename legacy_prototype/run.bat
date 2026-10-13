@echo off
setlocal enabledelayedexpansion

echo ==============================================================================
echo   SIST CSE AI Capstone Project — Project Resource Allocation System
echo   Pure Java SE + HttpServer + JDBC (MySQL 8.0)
echo ==============================================================================
echo.

:: 1. Check for Java
where javac >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 'javac' was not found in your PATH.
    echo Please install JDK 17+ or add the JDK bin directory to your PATH.
    pause
    exit /b 1
)

:: 2. Create output directory if needed
if not exist "backend\bin" (
    mkdir "backend\bin"
)

:: 3. Compile Java source files
echo [STEP 1/3] Compiling pure Java backend sources...
dir /s /b backend\src\*.java > sources.txt
javac -cp "backend\lib\*" -d backend\bin @sources.txt
set COMPILE_STATUS=%ERRORLEVEL%
del sources.txt

if %COMPILE_STATUS% neq 0 (
    echo [ERROR] Backend compilation failed! Check error output above.
    pause
    exit /b %COMPILE_STATUS%
)
echo [OK] Backend compiled successfully into backend\bin.

:: 4. Launch browser after a short delay
echo [STEP 2/3] Launching web browser...
start "" "http://localhost:8080/index.html"

:: 5. Run the Pure Java HTTP Server
echo [STEP 3/3] Starting pure Java HTTP Server on port 8080...
echo Server running at: http://localhost:8080
echo Press Ctrl+C in this terminal window to stop the server.
echo.
java -cp "backend\bin;backend\lib\*" Main

pause
