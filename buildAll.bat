@echo off
setlocal enabledelayedexpansion

echo Building all version groups...
echo.

set "FAILED="
for %%F in (
    gradle.1_21_1.properties
    gradle.1_21_4_5.properties
    gradle.1_21_8_10.properties
    gradle.1_21_11.properties
) do (
    for /f "tokens=2 delims==" %%V in ('findstr /b "minecraft_version=" "%%F"') do (
        echo === Building for MC %%V ===
        call .\gradlew.bat build -Ptarget_minecraft_version=%%V
        if errorlevel 1 (
            echo FAILED: MC %%V
            set "FAILED=!FAILED! %%V"
        )
        echo.
    )
)

if defined FAILED (
    echo Build failed for:%FAILED%
    exit /b 1
)
echo All versions built successfully!
