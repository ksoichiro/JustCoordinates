# Just Coordinates

A lightweight client-side mod that displays only XYZ coordinates, inspired by Bedrock Edition. No minimap, no extra info — supports Fabric, NeoForge, and Forge.

![Just Coordinates](docs/screenshots/featured-for-readme.png)

## Features

- Displays coordinates with a gray background and white text, matching the Bedrock Edition style
- Format: `Position: X, Y, Z`
- Toggle visibility with a keybind (default: `J` key)
- Share your coordinates in chat with `/justcoordinates share`, or an assignable keybind
- Choose where the HUD appears — 6 preset positions (top-left, top-center, top-right, bottom-left, bottom-center, bottom-right)
- Client-side only — no server installation required

## Toggle Keybind

Press `J` to toggle the coordinates HUD on or off. The key can be rebound in Options > Controls > Key Binds under the "Just Coordinates" category.

## Sharing Coordinates

Run `/justcoordinates share` to post your current coordinates to chat as
`X: 100, Y: 64, Z: -200`. The command is handled by the mod on your own client, so it works on any
server without permissions. You can also assign a key to "Share Coordinates" in
Options > Controls > Key Binds under the "Just Coordinates" category (unbound by default).

On Forge 1.16.5 and 1.17.1 only the keybinding is available — those Forge versions have no
client-command API.

## HUD Position

Open the settings screen to choose where the coordinates HUD appears (default: top-left, matching previous releases):

- Assign a key to "Open Settings" in Options > Controls > Key Binds under the "Just Coordinates" category (unbound by default)
- On Forge and NeoForge, use the **Config** button on the Mods screen
- On Fabric, install [ModMenu](https://modrinth.com/mod/modmenu) for a **Config** button on the Mods screen (optional)

The chosen position is saved to `config/justcoordinates.json` when you close the settings screen.

## F3 Debug Screen

This mod does not replace the F3 debug screen. It only shows XYZ coordinates — no direction, biome, light level, or other debug information. The coordinates HUD is automatically hidden while the F3 screen is displayed.

On Minecraft 1.21.9 and later, debug entries pinned to "Always" in the Debug Options screen (F3+F6) do not hide the coordinates HUD — it stays visible alongside them. If the two overlap at the top-left, move the HUD with the position setting.

## HUD Visibility (F1 Key)

When the HUD is hidden using F1, the coordinates will also be hidden. This follows vanilla HUD behavior.

## Supported Versions

| Minecraft | Fabric | Quilt | NeoForge | Forge |
|-----------|--------|-------|----------|-------|
| 26.3      | Yes    | Yes   | Yes      | -     |
| 26.2      | Yes    | Yes   | Yes      | Yes   |
| 26.1.2    | Yes    | Yes   | Yes      | Yes   |
| 26.1.1    | Yes    | Yes   | Yes      | Yes   |
| 26.1      | Yes    | Yes   | Yes      | -     |
| 1.21.11   | Yes    | Yes   | Yes      | Yes   |
| 1.21.10   | Yes    | Yes   | Yes      | Yes   |
| 1.21.9    | Yes    | Yes   | Yes      | Yes   |
| 1.21.8    | Yes    | Yes   | Yes      | Yes   |
| 1.21.7    | Yes    | Yes   | Yes      | Yes   |
| 1.21.6    | Yes    | Yes   | Yes      | Yes   |
| 1.21.5    | Yes    | Yes   | Yes      | Yes   |
| 1.21.4    | Yes    | Yes   | Yes      | Yes   |
| 1.21.3    | Yes    | Yes   | Yes      | Yes   |
| 1.21.1    | Yes    | Yes   | Yes      | Yes   |
| 1.20.1    | Yes    | Yes   | -        | Yes   |
| 1.19.2    | Yes    | Yes   | -        | Yes   |
| 1.18.2    | Yes    | Yes   | -        | Yes   |
| 1.17.1    | Yes    | Yes   | -        | Yes   |
| 1.16.5    | Yes    | Yes   | -        | Yes   |

## Build

Build a specific platform for a target Minecraft version using `-Ptarget_mc_version`:

```
./gradlew :fabric:build -Ptarget_mc_version=1.21.11
./gradlew :neoforge:build -Ptarget_mc_version=1.21.11
./gradlew :forge:build -Ptarget_mc_version=1.21.11
```

The default `target_mc_version` is `1.21.1` (defined in `gradle.properties`), so the following also works:

```
./gradlew :fabric:build
```

Build outputs are located in `<platform>/<mc_version>/build/libs/` (e.g. `fabric/1.21.11/build/libs/`).

## License

LGPL-3.0-only
