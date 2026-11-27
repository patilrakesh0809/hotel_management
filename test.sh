#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/main out/test
find src/main/java -name "*.java" > main-sources.txt
javac --release 21 -d out/main @main-sources.txt
rm -f main-sources.txt
cp -r src/main/resources/* out/main/
find src/test/java -name "*.java" > test-sources.txt
javac --release 21 -cp out/main -d out/test @test-sources.txt
rm -f test-sources.txt
exec java -cp "out/main:out/test" com.hotel.HotelApplicationTest
