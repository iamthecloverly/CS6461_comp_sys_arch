@echo off
REM ==============================================================================
REM  Setup Script for the CSCI6461 Assembler Project (Windows)
REM ==============================================================================
REM  This script checks for the required development tools on Windows and provides
REM  instructions for installation if they are missing.
REM ==============================================================================

echo.
echo [INFO] Starting environment check for the CSCI6461 Assembler project...
echo.

REM --- 1. Check for Java Development Kit (JDK) ---
echo [INFO] Step 1: Checking for Java Development Kit (JDK)...
where java >nul 2>nul
if %errorlevel% equ 0 (
    FOR /F "tokens=3" %%g IN ('java -version 2^>^&1 ^| findstr /i "version"') do (
        SET "JAVA_VERSION=%%g"
    )
    REM Remove quotes from the version string
    SET "JAVA_VERSION=%JAVA_VERSION:"=%"
    echo [OK] Java JDK is installed (Version: %JAVA_VERSION%).
) else (
    echo [ACTION] Java JDK is not installed or not found in your PATH.
    winget --version >nul 2>nul
    if %errorlevel% equ 0 (
        echo [ACTION] You can install the latest OpenJDK using winget by running this command:
        echo     winget install -e --id Microsoft.OpenJDK.17
    ) else (
        echo [ACTION] Please download and install the Java JDK (version 8 or later) manually.
        echo     Recommended Download URL: https://adoptium.net/
    )
)
echo.

REM --- 2. Check for IntelliJ IDEA ---
echo [INFO] Step 2: Checking for IntelliJ IDEA...
SET "IDEA_PATH_COMMUNITY=%ProgramFiles%\JetBrains\IntelliJ IDEA Community Edition"
SET "IDEA_PATH_ULTIMATE=%ProgramFiles%\JetBrains\IntelliJ IDEA"

IF EXIST "%IDEA_PATH_COMMUNITY%" (
    echo [OK] IntelliJ IDEA Community Edition is installed.
) ELSE IF EXIST "%IDEA_PATH_ULTIMATE%" (
    echo [OK] IntelliJ IDEA Ultimate Edition is installed.
) ELSE (
    echo [ACTION] IntelliJ IDEA was not found in the default installation directory.
    winget --version >nul 2>nul
    if %errorlevel% equ 0 (
        echo [ACTION] You can install the Community Edition using winget by running this command:
        echo     winget install -e --id JetBrains.IntelliJIDEA.Community
    ) else (
        echo [ACTION] Please download and install IntelliJ IDEA manually from the official website.
        echo     Download URL: https://www.jetbrains.com/idea/download/
    )
)
echo.

REM --- 3. Final Summary ---
echo [INFO] Environment check complete.
echo [INFO] Please ensure all checks above show [OK] before proceeding.
echo [INFO] Once all tools are installed, follow the instructions in README.md to build and run the assembler.
echo.
pause
