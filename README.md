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

| Minecraft | Fabric | NeoForge |
|-----------|--------|----------|
| 1.21.1    | Yes    | Yes      |

## Build

```
./gradlew build
```

Build outputs:

- Fabric: `fabric-1.21.1/build/libs/`
- NeoForge: `neoforge-1.21.1/build/libs/`

## License

LGPL-3.0-only
