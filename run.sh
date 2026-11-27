#!/usr/bin/env bash
set -euo pipefail
mkdir -p out/main
find src/main/java -name "*.java" > sources.txt
javac --release 21 -d out/main @sources.txt
rm -f sources.txt
cp -r src/main/resources/* out/main/
echo "Starting Grand Horizon Hotel Management Web App..."
exec java -cp out/main com.hotel.Main "$@"
