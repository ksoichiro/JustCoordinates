# Implementation Plan: Just Coordinates

## Summary

A client-side mod that displays XYZ coordinates as a HUD in the top-left corner of the screen, similar to Minecraft Bedrock Edition.
Multi-loader architecture supporting Fabric / NeoForge with minimal dependencies.

## Decisions

| Item | Value |
|------|-------|
| Mod ID | `justcoordinates` |
| Package | `com.justcoordinates` |
| License | LGPL-3.0-only |
| Minecraft Version | 1.21.1 |
| Java Version | 21 |
| Initial Loaders | Fabric, NeoForge |
| Deferred | Forge, Quilt |
| Mappings | Mojang official mappings (shared across both loaders) |

## Directory Structure

```
JustCoordinates/
├── build.gradle          # Root: shared settings
├── settings.gradle       # Subproject definitions
├── gradle.properties         # Version constants
├── gradle/                   # Gradle wrapper
├── common-shared/            # *Not a Gradle subproject
│   └── src/main/java/com/justcoordinates/
│       └── JustCoordinates.java         # Constants (MOD_ID, etc.)
├── common-1.21.1/            # Gradle subproject (ModDevGradle Vanilla mode)
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/justcoordinates/
│       │   └── CoordinatesHudRenderer.java  # HUD rendering logic
│       └── resources/
│           └── assets/justcoordinates/lang/
│               ├── en_us.json
│               └── ja_jp.json
├── fabric-base/              # Gradle subproject
│   ├── build.gradle
│   └── src/main/java/com/justcoordinates/fabric/
│       └── JustCoordinatesFabric.java  # Base Fabric ClientModInitializer
├── fabric-1.21.1/            # Gradle subproject (depends on fabric-base)
│   ├── build.gradle
│   └── src/main/resources/
│       └── fabric.mod.json
├── neoforge-base/            # Gradle subproject
│   ├── build.gradle
│   └── src/main/java/com/justcoordinates/neoforge/
│       └── JustCoordinatesNeoForge.java  # Base NeoForge entry point
├── neoforge-1.21.1/          # Gradle subproject (depends on neoforge-base)
│   ├── build.gradle
│   └── src/main/resources/
│       └── META-INF/neoforge.mods.toml
├── .gitignore
├── LICENSE
└── README.md
```

## Source Sharing Strategy

Code is shared using the [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) Configuration approach, without using Architectury API.

### common-shared (Not a Gradle subproject)

Compiled by directly adding to each subproject's srcDirs.

```groovy
// In each subproject's build.gradle
sourceSets.main { java { srcDir rootProject.file('common-shared/src/main/java') } }
```

### common-1.21.1 (Gradle subproject)

Configured using ModDevGradle Vanilla mode (`neoFormVersion`), exposing sources and resources to both Fabric and NeoForge via Gradle Configurations.

**Note: Do NOT use Fabric Loom for the common module.** It causes circular dependencies (`prepareRemapJar -> jar -> compileJava -> remapJar`).

```groovy
// common-1.21.1/build.gradle
plugins {
    id 'java'
    id 'net.neoforged.moddev'
}
neoForge {
    neoFormVersion = '1.21.1-20240808.144430' // Vanilla mode
}
// Expose sources via Configuration
configurations {
    commonJava { canBeConsumed = true; canBeResolved = false }
    commonResources { canBeConsumed = true; canBeResolved = false }
}
artifacts {
    commonJava sourceSets.main.java.srcDirs.first()
    commonResources sourceSets.main.resources.srcDirs.first()
}
```

### fabric-1.21.1 / neoforge-1.21.1 (Source consumers)

Common-1.21.1 sources and resources are consumed via Configurations and compiled with each loader's toolchain.

```groovy
// In each loader's build.gradle
configurations {
    commonJava { canBeResolved = true }
    commonResources { canBeResolved = true }
}
dependencies {
    compileOnly project(':common-1.21.1')
    commonJava project(path: ':common-1.21.1', configuration: 'commonJava')
    commonResources project(path: ':common-1.21.1', configuration: 'commonResources')
}
tasks.named('compileJava') { dependsOn configurations.commonJava; source configurations.commonJava }
tasks.named('processResources') { dependsOn configurations.commonResources; from configurations.commonResources }
```

### Mapping Compatibility

Both loaders use Mojang official mappings, so class names and method names in the shared source are identical.
Differences in runtime intermediate mappings (Fabric=Intermediary, NeoForge=SRG) are automatically remapped by the toolchain during build, so they do not affect source sharing.

## Phases

### Phase 1: Project Skeleton

Build the Gradle multi-project structure.

1. Generate Gradle wrapper (Gradle 8.11)
2. `settings.gradle` — Subproject definitions (common-shared is not included)
3. `gradle.properties` — Version constants (see "Key Dependencies" below)
4. Root `build.gradle` — Shared Java settings (Java 21, encoding, etc.), plugin declarations (`apply false`)
5. Each subproject's `build.gradle`
   - `common-1.21.1`: ModDevGradle Vanilla mode, Configuration exports
   - `fabric-base`: Fabric Loom plugin, Fabric API dependency
   - `fabric-1.21.1`: Fabric Loom, common-1.21.1 Configuration import + fabric-base dependency
   - `neoforge-base`: ModDevGradle plugin
   - `neoforge-1.21.1`: ModDevGradle, common-1.21.1 Configuration import + neoforge-base dependency
6. `.gitignore`

### Phase 2: Common Code

Implement loader-independent common code.

1. `common-shared/src/main/java/com/justcoordinates/JustCoordinates.java`
   - MOD_ID constant
2. `common-1.21.1/src/main/java/com/justcoordinates/CoordinatesHudRenderer.java`
   - HUD rendering logic (using Mojang mappings names)
   - Draw gray background with `GuiGraphics.fill()`
   - Draw white text with `GuiGraphics.drawString()` (alpha channel required for colors: `0xFFFFFFFF`)
   - Hide when F3 debug screen is visible
   - Hide when F1 HUD-hidden mode is active
   - Coordinate format: uses translation keys
3. `common-1.21.1/src/main/resources/assets/justcoordinates/lang/`
   - `en_us.json`: `"justcoordinates.hud.position": "Position: %s, %s, %s"`
   - `ja_jp.json`: `"justcoordinates.hud.position": "座標: %s, %s, %s"`

### Phase 3: Fabric Implementation

Implement the Fabric entry point and mod configuration.

1. `fabric-base/.../JustCoordinatesFabric.java`
   - Implement `ClientModInitializer`
   - Register `CoordinatesHudRenderer` via `HudRenderCallback.EVENT.register()`
2. `fabric-1.21.1/src/main/resources/fabric.mod.json`
   - Mod metadata (id, name, version, description, license, etc.)
   - Entrypoints: client
   - Depends: fabricloader, fabric-api, minecraft
3. Build verification: `./gradlew :fabric-1.21.1:build`

### Phase 4: NeoForge Implementation

Implement the NeoForge entry point and mod configuration.

1. `neoforge-base/.../JustCoordinatesNeoForge.java`
   - Define entry point with `@Mod` annotation
   - `@EventBusSubscriber(modid, value=Dist.CLIENT, bus=Bus.MOD)`
   - Register HUD as a `LayeredDraw.Layer` via `RegisterGuiLayersEvent` (`registerAboveAll`)
2. `neoforge-1.21.1/src/main/resources/META-INF/neoforge.mods.toml`
   - Mod metadata
3. Build verification: `./gradlew :neoforge-1.21.1:build`

### Phase 5: Documentation & Packaging

1. `LICENSE` — LGPL-3.0-only text
2. `README.md` — Content based on the "Notes to include in README" section of draft.md
   - Mod overview
   - Differences from the F3 debug screen
   - HUD show/hide with the F1 key
   - Supported versions/loaders
   - Build instructions
3. Full build verification for all loaders: `./gradlew build`

## Technical Notes

### HUD Rendering Details

- **Position**: Top-left corner of the screen (approximately x=2, y=2 margin)
- **Background**: Semi-transparent gray (approximately `0x90505050`, similar to Bedrock Edition)
- **Text**: White (`0xFFFFFFFF`), Minecraft default font, with shadow
- **Coordinate values**: No decimal places (block coordinates, using `Mth.floor()`)
- **Color specification**: Must include alpha channel (upper 8 bits). Omitting it causes nothing to render

### F3 Debug Screen Check

| Loader | Method |
|--------|--------|
| Common (Mojang mappings) | `minecraft.getDebugOverlay().showDebugScreen()` |

Since common code uses Mojang mappings names, there is no difference between loaders.

### F1 HUD Visibility Check

| Loader | Method | Auto-handling |
|--------|--------|---------------|
| Fabric (`HudRenderCallback`) | `minecraft.options.hideGui` | **Manual check required** — The callback fires even when F1-hidden |
| NeoForge (`RegisterGuiLayersEvent`) | `minecraft.options.hideGui` | **Manual check required** — Vanilla conditions are not automatically applied to custom layers |

> **Perform the `minecraft.options.hideGui` check inside the common code (`CoordinatesHudRenderer`)**

### Common Renderer Design

The common renderer depends only on Minecraft vanilla classes and is called from each loader's entry point.

```java
// CoordinatesHudRenderer.java (common-1.21.1, Mojang mappings)
public class CoordinatesHudRenderer {
    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;                          // F1 check
        if (mc.getDebugOverlay().showDebugScreen()) return;      // F3 check
        if (mc.player == null) return;
        // Render using GuiGraphics.fill() + GuiGraphics.drawString()
    }
}
```

```java
// Fabric: HudRenderCallback.EVENT.register((guiGraphics, tickCounter) ->
//     CoordinatesHudRenderer.render(guiGraphics));
```

```java
// NeoForge: RegisterGuiLayersEvent.registerAboveAll(id, (guiGraphics, deltaTracker) ->
//     CoordinatesHudRenderer.render(guiGraphics));
```

### Fabric API Known Issue

In Fabric API 1.21.x, content drawn via `HudRenderCallback` may render below some UI elements (such as chat) ([Issue #3908](https://github.com/FabricMC/fabric/issues/3908)). This is not a significant issue for lightweight use cases like a coordinates overlay.

In Fabric API 0.116 and later, `HudRenderCallback` is deprecated and migration to `HudLayerRegistrationCallback` is recommended, but `HudRenderCallback` is still valid for 1.21.1.

### NeoForge API Details

- `RegisterGuiLayersEvent` fires on the **mod event bus** (`Bus.MOD`)
- `LayeredDraw.Layer` render signature: `void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker)`
- `VanillaGuiLayers` defines the vanilla layer list used as placement references
- `registerAboveAll(ResourceLocation id, LayeredDraw.Layer layer)` places the layer above all others

### Key Dependencies

| Dependency | Version |
|-----------|---------|
| Minecraft | 1.21.1 |
| Java | 21 |
| Gradle | 8.11 |
| Fabric Loom plugin | 1.9.2 |
| Fabric Loader | 0.16.9 |
| Fabric API | 0.116.7+1.21.1 |
| ModDevGradle plugin | 2.0.140 |
| NeoForge | 21.1.219 |
| NeoForm (common Vanilla mode) | 1.21.1-20240808.144430 |

### Build Pitfalls

- **Do not use Fabric Loom for the common module**: It causes circular dependencies. Use ModDevGradle Vanilla mode (`neoFormVersion`)
- **Property expansion in `processResources`**: When expanding variables like `${version}`, configure `filesMatching` to ensure it also applies to common resources
- **Conflicts with loader-specific resources**: Files with the same name in both common and loader modules may be overwritten
- **Cannot access loader-specific code from common module**: This is a design constraint. Use the ServiceLoader pattern if needed (not required for this mod)

## Forge Support (1.20.x) Research Results

Research results for extending to Forge support for 1.20.x and below.

### Gradle Plugin

**Do not use ForgeGradle.** Use ModDevGradle legacyforge (`net.neoforged.moddev.legacyforge`) instead.

| Item | ForgeGradle 6.x | ModDevGradle legacyforge |
|------|-----------------|------------------------|
| Plugin ID | `net.minecraftforge.gradle` | `net.neoforged.moddev.legacyforge` |
| Latest Version | 6.0.47 | 2.0.140 |
| Gradle 8.11 Support | Uncertain | Supported |
| Recommendation | Not recommended | Officially recommended by NeoForged |

MultiLoader-Template also migrated from ForgeGradle to ModDevGradle legacyforge in February 2025.

### Version Constraints

| Item | Value |
|------|-----|
| Supported MC | **1.17 to 1.20.1 only** (legacyforge limitation) |
| MC 1.20.4 | Not supported by legacyforge. Either use ForgeGradle or skip support |
| Java | 17 (1.20.x does not use Java 21) |
| Forge 1.20.1 | 47.4.10 (Recommended) |

### Forge 1.20.x HUD API

Same API across 1.20.1 to 1.20.4. Replaced by Mojang's LayeredDraw system in 1.20.5+ and deprecated.

#### IGuiOverlay (Forge-specific interface)

```java
@FunctionalInterface
public interface IGuiOverlay {
    void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick,
                int screenWidth, int screenHeight);
}
```

This has a different signature from NeoForge 1.21.x's `LayeredDraw.Layer` (`render(GuiGraphics, DeltaTracker)`).

#### Registration Method

```java
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("coordinates_hud",
            (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
                CoordinatesHudRenderer.render(guiGraphics);
            });
    }
}
```

- `RegisterGuiOverlaysEvent` fires on the **mod event bus**
- `registerAboveAll(String id, IGuiOverlay overlay)` — id is a string (mod namespace is automatically prepended)
- Placement references can be specified via the `VanillaGuiOverlay` enum

#### F1 / F3 Checks

| Check | Method | Auto-handling |
|-------|--------|---------------|
| F1 (HUD hidden) | `gui.getMinecraft().options.hideGui` | **Manual check required** |
| F3 (Debug) | `gui.getMinecraft().getDebugOverlay().showDebugScreen()` | **Manual check required** |

> The checks inside the common renderer (`CoordinatesHudRenderer`) work as-is.

### Mod Metadata

| Loader | File | Path |
|--------|------|------|
| Forge 1.20.x | `mods.toml` | `META-INF/mods.toml` |
| NeoForge 1.21.x | `neoforge.mods.toml` | `META-INF/neoforge.mods.toml` |

```toml
# META-INF/mods.toml (Forge 1.20.x)
modLoader="javafml"
loaderVersion="[47,)"
license="LGPL-3.0-only"

[[mods]]
modId="justcoordinates"
version="${file.jarVersion}"
displayName="Just Coordinates"
description="Displays simple XYZ coordinates on screen."

[[dependencies.justcoordinates]]
modId="forge"
mandatory=true
versionRange="[47,)"
ordering="NONE"
side="CLIENT"

[[dependencies.justcoordinates]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1, 1.21)"
ordering="NONE"
side="CLIENT"
```

### Compatibility with Configuration Approach

Proven on MultiLoader-Template 1.20.1 branch. Verified to work with Gradle 8.11.

```groovy
// common-1.20.1/build.gradle
plugins {
    id 'java'
    id 'net.neoforged.moddev.legacyforge'
}
legacyForge {
    mcpVersion = '1.20.1' // Vanilla mode (not the Forge version)
}
// Configuration export follows the same pattern as common-1.21.1
```

```groovy
// forge-1.20.1/build.gradle
plugins {
    id 'net.neoforged.moddev.legacyforge'
}
legacyForge {
    version = '1.20.1-47.4.10'
}
// Configuration import follows the same pattern as fabric-1.21.1 / neoforge-1.21.1
```

### Full Mapping Overview for 3-Loader Support

Mojang official mappings are available across all 3 loaders.

| Loader | Plugin | Development Mappings | Production Output |
|--------|--------|---------------------|-------------------|
| Fabric | fabric-loom | Mojang (officialMojangMappings()) | Intermediary |
| NeoForge | net.neoforged.moddev | Mojang (default) | Mojang |
| Forge | net.neoforged.moddev.legacyforge | Mojang (default) | SRG (reobf) |

> **Common sources are written in Mojang mappings and can be shared across all loaders**

### Additional Directories for Forge Support

```
JustCoordinates/
├── ...existing structure...
├── common-1.20.1/            # Gradle subproject (legacyforge Vanilla mode)
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/justcoordinates/
│       │   └── CoordinatesHudRenderer.java  # Same as 1.21.1 version or with version-specific changes
│       └── resources/assets/justcoordinates/lang/
├── forge-base/               # Gradle subproject
│   ├── build.gradle
│   └── src/main/java/com/justcoordinates/forge/
│       └── JustCoordinatesForge.java
├── forge-1.20.1/             # Gradle subproject
│   ├── build.gradle
│   └── src/main/resources/
│       └── META-INF/mods.toml
├── fabric-1.20.1/            # If also supporting Fabric 1.20.1
│   ├── build.gradle
│   └── src/main/resources/
│       └── fabric.mod.json
└── neoforge-1.20.1/          # If also supporting NeoForge 1.20.1
    ├── build.gradle
    └── src/main/resources/
        └── META-INF/neoforge.mods.toml
```

### Notes for Forge Support

- **Java 17 constraint**: MC 1.20.x requires Java 17. common-1.20.1 must be compiled with Java 17 (common-shared must also be Java 17 compatible)
- **MC 1.20.4 is not supported by legacyforge**: Decision needed on whether to use ForgeGradle or only support 1.20.1
- **IGuiOverlay signature differences**: Forge (`ForgeGui, GuiGraphics, float, int, int`) vs NeoForge (`GuiGraphics, DeltaTracker`) — Absorbed in each loader's entry point; the pattern of passing only `GuiGraphics` to the common renderer is effective
- **Forge production JAR requires SRG reobf**: `reobfJar` task is needed after the `jar` task
