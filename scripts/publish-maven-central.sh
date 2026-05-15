#!/usr/bin/env bash
# Loads Sonatype tokens from .gradle/gradle.properties into the environment so
# Gradle's providers.gradleProperty("mavenCentralUsername") can see them.
set -euo pipefail
cd "$(dirname "$0")/.."

PROPS_FILE=".gradle/gradle.properties"
if [[ ! -f "$PROPS_FILE" ]]; then
  echo "Missing $PROPS_FILE — copy gradle.properties.example and fill in credentials." >&2
  exit 1
fi

read_prop() {
  local key="$1"
  local line
  line="$(grep -E "^${key}=" "$PROPS_FILE" | tail -1 || true)"
  if [[ -z "$line" ]]; then
    echo "Missing $key in $PROPS_FILE" >&2
    exit 1
  fi
  echo "${line#*=}"
}

export ORG_GRADLE_PROJECT_mavenCentralUsername="$(read_prop mavenCentralUsername)"
export ORG_GRADLE_PROJECT_mavenCentralPassword="$(read_prop mavenCentralPassword)"

exec ./gradlew publishToMavenCentral "$@"
