@echo off
setlocal

set /p "javaFile=Enter the name of the Java file (without extension): "
if "%javaFile%"=="" (
    echo No file name provided.
    pause
    exit /b
)

javac "%javaFile%.java"

if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b
)

java "%javaFile%"
pause