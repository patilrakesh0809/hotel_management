@echo off
setlocal
if not exist out\main mkdir out\main
if not exist out\test mkdir out\test
dir /s /b src\main\java\*.java > main-sources.txt
javac --release 21 -d out\main @main-sources.txt
if errorlevel 1 (
    del main-sources.txt
    exit /b 1
)
del main-sources.txt
xcopy /E /I /Y src\main\resources\* out\main\ >nul

dir /s /b src\test\java\*.java > test-sources.txt
javac --release 21 -cp out\main -d out\test @test-sources.txt
if errorlevel 1 (
    del test-sources.txt
    exit /b 1
)
del test-sources.txt

java -cp out\main;out\test com.hotel.HotelApplicationTest
