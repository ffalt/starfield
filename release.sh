#!/bin/bash
set -e

./gradlew bumpReleaseVersion
./gradlew release
