#!/bin/bash
#
# Build ForgeGradle 7.x from source and publish to mavenLocal.
#
# ForgeGradle 7.0.17 has an IBM_SEMERU bug (JvmVendorSpec.IBM_SEMERU removed
# in Gradle 9.0) via its bundled Mavenizer. The fix (Mavenizer 0.4.54) is on
# the FG_7.0 branch but not yet in a release.
#
# This script clones/updates the FG_7.0 branch at a pinned commit and publishes
# it as version 7.0.17 to mavenLocal, overriding the official release.
# Since mavenLocal is listed first in pluginManagement.repositories, Gradle
# resolves the patched version.
#
# Once ForgeGradle 7.0.18+ is released with the fix:
#   1. Remove this script
#   2. Remove mavenLocal() from settings.gradle pluginManagement.repositories
#   3. Update forgegradle_version to [7.0.18,8.0) in gradle.properties
#   4. Delete ~/.m2/repository/net/minecraftforge/forgegradle/7.0.17/
#   5. Delete ~/.m2/repository/net/minecraftforge/gradle/net.minecraftforge.gradle.gradle.plugin/7.0.17/
#
# Usage:
#   ./scripts/build-forgegradle-local.sh
#
# Prerequisites:
#   - JDK 21+ (for building ForgeGradle itself)
#   - Internet access (to clone the repo)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
FG_DIR="$PROJECT_ROOT/.local/ForgeGradle"
FG_REPO="https://github.com/MinecraftForge/ForgeGradle.git"
FG_BRANCH="FG_7.0"
# Pin to commit that includes Mavenizer 0.4.54 (IBM_SEMERU fix)
FG_COMMIT="841ce99"

echo "=== Building ForgeGradle from source ==="
echo "Repository: $FG_REPO"
echo "Branch:     $FG_BRANCH"
echo "Commit:     $FG_COMMIT"
echo "Output:     mavenLocal (version 7.0.17, patched)"
echo ""

if [ -d "$FG_DIR" ]; then
    echo "Updating existing clone..."
    cd "$FG_DIR"
    git fetch origin "$FG_BRANCH"
    git checkout "$FG_COMMIT"
else
    echo "Cloning ForgeGradle..."
    git clone --branch "$FG_BRANCH" "$FG_REPO" "$FG_DIR"
    cd "$FG_DIR"
    git checkout "$FG_COMMIT"
fi

echo ""
echo "Building and publishing to mavenLocal..."
./gradlew publishToMavenLocal --no-daemon

echo ""
echo "=== Done ==="
echo "ForgeGradle 7.0.17 (patched) published to mavenLocal."
echo ""
echo "Ensure settings.gradle has mavenLocal() first in pluginManagement.repositories"
echo "and forgegradle_version=[7.0.17,8.0) in gradle.properties."
