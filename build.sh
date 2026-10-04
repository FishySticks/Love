#!/bin/sh
set -e
if [ ! -f ./gradlew ]; then
  echo "Gradle wrapper is not included; run with a system Gradle installation or generate the wrapper with: gradle wrapper"
fi
gradle build
