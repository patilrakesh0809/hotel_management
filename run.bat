@echo off
setlocal
if not exist out\main mkdir out\main
dir /s /b src\main\java\*.java > sources.txt
javac --release 21 -d out\main @sources.txt
if errorlevel 1 (
    del sources.txt
    exit /b 1
)
del sources.txt
xcopy /E /I /Y src\main\resources\* out\main\ >nul
echo Starting Grand Horizon Hotel Management Web App...
java -cp out\main com.hotel.Main %*
