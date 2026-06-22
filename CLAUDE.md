# Project: Just Coordinates

Minecraft client-side mod displaying XYZ coordinates HUD. Multi-loader (Fabric, NeoForge, Forge) across MC 1.16.5–26.1.

## Build

### Prerequisites

Shared build/release tasks live in the `gradle/shared` git submodule
([minecraft-mod-gradle-scripts](https://github.com/ksoichiro/minecraft-mod-gradle-scripts)).
Initialize it after cloning:

```
git submodule update --init
```

ForgeGradle 7.0.17 has a bug (IBM_SEMERU) on Gradle 9. Run this once after cloning to build a patched version locally:

```
./scripts/build-forgegradle-local.sh
```

This publishes a patched ForgeGradle to mavenLocal. When FG 7.0.18+ is released, follow the removal instructions in the script.

### Commands

Build a specific platform for a target Minecraft version:

```
./gradlew :fabric:build -Ptarget_mc_version=1.21.11
./gradlew :neoforge:build -Ptarget_mc_version=1.21.11
./gradlew :forge:build -Ptarget_mc_version=1.21.11
```

Build all platforms for a version:

```
./gradlew clean build -Ptarget_mc_version=26.1 -x test
```

### Release

Release tasks come from the `gradle/shared` submodule. Build all versions and
collect JARs, then upload (tokens via `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN`):

```
./gradlew release          # cleanAll + buildAll + collectJars -> build/release/
./gradlew releaseModrinth  # upload build/release/*.jar to Modrinth
./gradlew releaseCurseForge
```

Per-version platforms (incl. Forge) come from `enabled_platforms` in
`props/{version}.properties`. Fabric builds are also tagged Quilt-compatible
(`release_quilt_compatible=true`).

## Architecture

- `common/shared/` — Constants (JustCoordinates.java), compiled into each platform via srcDir
- `common/{version}/` — CoordinatesHudRenderer, version-specific MC API
- `fabric/base/` — Shared Fabric entry point (pre-26.1 versions)
- `fabric/{version}/` — Fabric mod metadata; own Java source when base is incompatible (26.1+)
- `neoforge/base/` — Shared NeoForge entry point (pre-26.1 versions)
- `neoforge/{version}/` — NeoForge mod metadata; own Java source when base is incompatible
- `forge/{version}/` — Forge entry point and metadata (no base, each version has own source)
- `props/{version}.properties` — Version-specific dependency versions

### Build Plugins by MC Version

| MC Version | Fabric | NeoForge | Forge |
|------------|--------|----------|-------|
| 1.16.5 | Architectury Loom | - | Architectury Loom |
| 1.17.1–1.20.1 | Fabric Loom | - | legacyforge |
| 1.21.1–1.21.11 | Fabric Loom | ModDevGradle | ForgeGradle 7.x |
| 26.1+ | Fabric Loom (new ID) | ModDevGradle | ForgeGradle 7.x |
