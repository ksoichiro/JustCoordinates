# Just Coordinates

## Overview

- Displays coordinates as a HUD, just like Minecraft Bedrock Edition
- No configuration, no mod dependencies, client-side
    - Fabric requires Fabric API
- Supports a wide range of loaders and versions

## Description examples

GitHub:

> A lightweight client-side mod that displays only XYZ coordinates, inspired by Bedrock Edition. No minimap, no extra info, no dependencies beyond Fabric API.

CurseForge:

> Displays simple XYZ coordinates on screen, like Bedrock Edition. Client-side, lightweight, and dependency-free (Fabric API only).

Modrinth:

> Just Coordinates shows only XYZ coordinates on your screen — nothing more. Inspired by Bedrock Edition, designed for simplicity and beginners.

## Notes to include in README, etc.

Explain concisely, including the following points. Keep it as short as possible, since overly long explanations won't be read.

### Difference from F3 debug screen

- Just Coordinates does not replace the F3 debug screen. It only displays XYZ coordinates and does not show direction, biome, light level, or any other debug information.
- The HUD is hidden while the F3 debug screen is displayed

### HUD visibility (F1 key)

- When the HUD is hidden using the F1 key, the coordinates will also be hidden. This behavior is intentional and follows vanilla HUD rules.

## HUD display image

- Displays coordinates in the top-left corner of the screen with a gray background and white text, same as Minecraft Bedrock Edition
    - Format:
	- EN: Position: X, Y, Z
	- JA: 座標: X, Y, Z

## Architecture

- Minecraft mod with minimal dependencies
    - Only Fabric API in Fabric
    - No Architectury API
- Compatible with both Fabric, NeoForge, and Forge
    - And Quilt in the future
- Initially implemented for Minecraft version 1.21.1
- The project will be configured with Gradle as a multi-project setup

## Directory structure

- common-shared
    - Common code without loader dependencies or version dependencies. Not a Gradle subproject, but incorporated as one of the srcDirs from each version-specific subproject
- common-1.21.1
    - Common code for Minecraft 1.21.1 without loader dependencies. Gradle subproject.
- fabric-base
    - Code for Fabric without Minecraft version dependencies. Gradle subproject.
- fabric-1.21.1
    - Code for Fabric and Minecraft 1.21.1. Gradle subproject. Depends on fabric-base.
- neoforge-base
    - Code for NeoForge without Minecraft version dependencies. Gradle subproject.
- neoforge-1.21.1
    - Code for NeoForge and Minecraft 1.21.1. Gradle subproject. Depends on neoforge-base.

## License

- LGPL-3.0-only
