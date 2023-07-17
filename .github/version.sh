#!/bin/bash

CWD="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
PARENT="$(cd "$CWD"/.. >/dev/null 2>&1 && pwd)"

set -e

SEMVER_REG="([[:digit:]]+(\.[[:digit:]]+)+)"

README_FILE="$PARENT/README.md"
VERSION_FILE="$PARENT/gradle/libs.versions.toml"

NEW_VERSION="$ORG_GRADLE_PROJECT_VERSION_NAME"
if [ -z "$NEW_VERSION" ]; then
  NEW_VERSION="$1"
  if [ -n "$NEW_VERSION" ]; then
    echo "Update README with version: '$NEW_VERSION'"

    # Update artifact versions in README.md
    sed -i '' -E "s/\:$SEMVER_REG\"\)/\:$NEW_VERSION\"\)/" "$README_FILE"

    # Update version catalog in README.md
    sed -i '' -E "s/materialKolor = \"$SEMVER_REG\"/materialKolor = \"$NEW_VERSION\"/" "$README_FILE"
  fi
fi

# Update Kotlin badge in README.md
LIBS_KOTLIN_VERSION=$(grep "kotlin = " "$VERSION_FILE" | cut -d= -f2 | tr -d ' "')
if [ -z "$LIBS_KOTLIN_VERSION" ]; then
  echo "Unable to find Kotlin version in '$VERSION_FILE'"
else
  echo "Updating Kotlin version: '$LIBS_KOTLIN_VERSION'"
  sed -i '' -E "s/kotlin-v$SEMVER_REG/kotlin-v$LIBS_KOTLIN_VERSION/" "$README_FILE"
fi

# Update Compose Multiplatform badge in README.md
LIBS_COMPOSE_VERSION=$(grep "compose-multiplatform = " "$VERSION_FILE" | cut -d= -f2 | tr -d ' "')
if [ -z "$LIBS_COMPOSE_VERSION" ]; then
  echo "Unable to find Compose Multiplatform version in '$VERSION_FILE'"
else
  echo "Updating Compose version: '$LIBS_COMPOSE_VERSION'"
  sed -i '' -E "s/Compose%20Multiplatform-v$SEMVER_REG/Compose%20Multiplatform-v$LIBS_COMPOSE_VERSION/" "$README_FILE"
fi
