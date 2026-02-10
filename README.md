# Just Coordinates

A lightweight client-side mod that displays only XYZ coordinates, inspired by Bedrock Edition. No minimap, no extra info, no dependencies beyond Fabric API.

## Features

- Displays coordinates in the top-left corner with a gray background and white text, matching the Bedrock Edition style
- Format: `Position: X, Y, Z` (English) / `座標: X, Y, Z` (Japanese)
- Client-side only — no server installation required

## F3 Debug Screen

This mod does not replace the F3 debug screen. It only shows XYZ coordinates — no direction, biome, light level, or other debug information. The coordinates HUD is automatically hidden while the F3 screen is displayed.

## HUD Visibility (F1 Key)

When the HUD is hidden using F1, the coordinates will also be hidden. This follows vanilla HUD behavior.

## Supported Versions

| Minecraft | Fabric | NeoForge | Forge |
|-----------|--------|----------|-------|
| 1.21.1    | Yes    | Yes      | -     |
| 1.20.1    | Yes    | -        | Yes   |

## Build

Build a specific platform for a target Minecraft version using `-Ptarget_mc_version`:

```
./gradlew :fabric:build -Ptarget_mc_version=1.21.1
./gradlew :neoforge:build -Ptarget_mc_version=1.21.1
./gradlew :fabric:build -Ptarget_mc_version=1.20.1
./gradlew :forge:build -Ptarget_mc_version=1.20.1
```

The default `target_mc_version` is `1.21.1` (defined in `gradle.properties`), so the following also works:

```
./gradlew :fabric:build
```

Build outputs:

- Fabric 1.21.1: `fabric-1.21.1/build/libs/`
- NeoForge 1.21.1: `neoforge-1.21.1/build/libs/`
- Fabric 1.20.1: `fabric-1.20.1/build/libs/`
- Forge 1.20.1: `forge-1.20.1/build/libs/`

## License

LGPL-3.0-only
