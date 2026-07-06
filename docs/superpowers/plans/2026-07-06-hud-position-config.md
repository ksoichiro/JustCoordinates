# HUD Position Configuration (GUI) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users move the coordinates HUD to one of 6 preset positions via a self-built in-game config screen, persisted to `config/justcoordinates.json`, across all 19 MC versions × Fabric/NeoForge/Forge.

**Architecture:** Version-independent logic (position enum, JSON persistence) lives once in `common/shared` (pure Java + Gson). The config screen and renderer changes follow the existing copy-per-version convention in `common/{version}`. Loader entry points inject the config dir, register a new "open settings" key, and register loader-native config screen factories.

**Tech Stack:** Vanilla MC widgets only (`Screen`, `Button`, `CycleButton`), Gson (bundled with MC), log4j-api for logging. ModMenu as `compileOnly` (Fabric, build-time only).

**Spec:** `docs/superpowers/specs/2026-07-06-hud-position-config-design.md`

## Global Constraints

- No new **runtime** mod dependencies. ModMenu is `compileOnly` only.
- `common/shared` compiles on every version's classpath → max language level **Java 16** (1.16.5), Gson floor **2.8.0** (no `JsonParser.parseString`), logging via `org.apache.logging.log4j` (if any version's classpath lacks it, fall back to `java.util.logging` — decided at Task 1 compile check).
- Default config (`TOP_LEFT`) must render **pixel-identical** to the current release (MARGIN=2, PADDING=2, colors unchanged).
- Config file: `config/justcoordinates.json`, keys `schema_version` (=1) and `position` (lowercase enum name, e.g. `"top_left"`).
- `BOTTOM_CENTER` lifts 40px above the bottom edge (hotbar clearance); all other positions use MARGIN=2.
- New translation keys (en_us + ja_jp): `justcoordinates.config.title`, `justcoordinates.config.position`, `justcoordinates.position.{top_left,top_center,top_right,bottom_left,bottom_center,bottom_right}`, `key.justcoordinates.open_config`.
- Errors never block startup: corrupt/missing config → warn log + defaults; save failure → error log only.
- Commit messages: English, Conventional Commits. **Ask the user before every `git commit`** (user's global policy).
- Builds: `./gradlew :fabric:build -Ptarget_mc_version=<v>` etc. from repo root. No test infrastructure exists in this repo; verification = compile + `runClient` / Prism manual checks.
- macOS dev-env constraints (known, not bugs): Forge FG7 `runClient` needs `-XstartOnFirstThread`; Forge 1.16.5/1.18.2 dev clients don't run — verify built JARs via Prism.

---

### Task 1: Shared config model (`HudPosition`, `HudConfig`)

**Files:**
- Create: `common/shared/src/main/java/com/justcoordinates/HudPosition.java`
- Create: `common/shared/src/main/java/com/justcoordinates/HudConfig.java`

**Interfaces:**
- Produces: `HudPosition` enum — `DEFAULT`, `getSerializedName(): String`, `getTranslationKey(): String`, `fromSerializedName(String): HudPosition` (null if unknown), `next(): HudPosition`, `resolveX(int screenWidth, int hudWidth, int margin): int`, `resolveY(int screenHeight, int hudHeight, int margin): int`
- Produces: `HudConfig` — `getPosition(): HudPosition`, `setPosition(HudPosition)`, `load(Path configDir)`, `save()` (uses the path remembered from `load`)

- [ ] **Step 1: Write `HudPosition.java`**

```java
package com.justcoordinates;

public enum HudPosition {
    TOP_LEFT("top_left", Horizontal.LEFT, true),
    TOP_CENTER("top_center", Horizontal.CENTER, true),
    TOP_RIGHT("top_right", Horizontal.RIGHT, true),
    BOTTOM_LEFT("bottom_left", Horizontal.LEFT, false),
    BOTTOM_CENTER("bottom_center", Horizontal.CENTER, false),
    BOTTOM_RIGHT("bottom_right", Horizontal.RIGHT, false);

    public static final HudPosition DEFAULT = TOP_LEFT;

    // Keeps the HUD clear of the hotbar and experience bar when bottom-centered.
    private static final int BOTTOM_CENTER_LIFT = 40;

    private enum Horizontal { LEFT, CENTER, RIGHT }

    private final String serializedName;
    private final Horizontal horizontal;
    private final boolean top;

    HudPosition(String serializedName, Horizontal horizontal, boolean top) {
        this.serializedName = serializedName;
        this.horizontal = horizontal;
        this.top = top;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public String getTranslationKey() {
        return "justcoordinates.position." + serializedName;
    }

    public static HudPosition fromSerializedName(String name) {
        for (HudPosition position : values()) {
            if (position.serializedName.equals(name)) {
                return position;
            }
        }
        return null;
    }

    public HudPosition next() {
        HudPosition[] positions = values();
        return positions[(ordinal() + 1) % positions.length];
    }

    public int resolveX(int screenWidth, int hudWidth, int margin) {
        switch (horizontal) {
            case CENTER:
                return (screenWidth - hudWidth) / 2;
            case RIGHT:
                return screenWidth - margin - hudWidth;
            default:
                return margin;
        }
    }

    public int resolveY(int screenHeight, int hudHeight, int margin) {
        if (top) {
            return margin;
        }
        if (this == BOTTOM_CENTER) {
            return screenHeight - BOTTOM_CENTER_LIFT - hudHeight;
        }
        return screenHeight - margin - hudHeight;
    }
}
```

- [ ] **Step 2: Write `HudConfig.java`**

```java
package com.justcoordinates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HudConfig {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String FILE_NAME = "justcoordinates.json";
    private static final Logger LOGGER = LogManager.getLogger(JustCoordinates.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configFile;
    private static HudPosition position = HudPosition.DEFAULT;

    private HudConfig() {
    }

    public static HudPosition getPosition() {
        return position;
    }

    public static void setPosition(HudPosition value) {
        position = value == null ? HudPosition.DEFAULT : value;
    }

    public static void load(Path configDir) {
        configFile = configDir.resolve(FILE_NAME);
        if (!Files.exists(configFile)) {
            return; // keep defaults; the file is first created on save
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                LOGGER.warn("{} is empty; using defaults", FILE_NAME);
                return;
            }
            if (json.has("position")) {
                String name = json.get("position").getAsString();
                HudPosition parsed = HudPosition.fromSerializedName(name);
                if (parsed == null) {
                    LOGGER.warn("Unknown position '{}' in {}; using default", name, FILE_NAME);
                    parsed = HudPosition.DEFAULT;
                }
                position = parsed;
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Failed to read {}; using defaults", FILE_NAME, e);
            position = HudPosition.DEFAULT;
        }
    }

    public static void save() {
        if (configFile == null) {
            LOGGER.warn("Config was never loaded; skipping save");
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("schema_version", CURRENT_SCHEMA_VERSION);
        json.addProperty("position", position.getSerializedName());
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save {}", FILE_NAME, e);
        }
    }
}
```

- [ ] **Step 3: Compile spot-check on oldest / newest / mid versions**

Run (each from repo root):
```bash
./gradlew :fabric:build -Ptarget_mc_version=1.16.5 -x test
./gradlew :fabric:build -Ptarget_mc_version=1.21.11 -x test
./gradlew :fabric:build -Ptarget_mc_version=26.2 -x test
```
Expected: BUILD SUCCESSFUL ×3. This proves Java-16 compatibility, Gson availability, and log4j-api presence on all classpath generations.
If 26.x fails on `org.apache.logging.log4j`: replace the two logger lines with `java.util.logging.Logger.getLogger(JustCoordinates.MOD_ID)` and `LOGGER.warning(...)` / `LOGGER.severe(...)` (JUL is always present), then re-run all three builds.

- [ ] **Step 4: Commit (ask user first)**

```bash
git add common/shared/src/main/java/com/justcoordinates/HudPosition.java common/shared/src/main/java/com/justcoordinates/HudConfig.java
git commit -m "feat: add shared HUD position config model"
```

---

### Task 2: Load config at client init (all loader entry points)

**Files:**
- Modify: `fabric/base/src/main/java/com/justcoordinates/fabric/JustCoordinatesFabric.java`
- Modify: `fabric/{1.16.5,1.17.1,1.18.2,1.19.2,26.1,26.1.1,26.1.2,26.2}/src/main/java/com/justcoordinates/fabric/JustCoordinatesFabric.java` (8 files)
- Modify: `neoforge/base/src/main/java/com/justcoordinates/neoforge/JustCoordinatesNeoForge.java`
- Modify: `neoforge/{1.21.6,1.21.7,1.21.8,1.21.9,1.21.10,1.21.11,26.1,26.1.1,26.1.2,26.2}/src/main/java/com/justcoordinates/neoforge/JustCoordinatesNeoForge.java` (10 files)
- Modify: `forge/{all 19 versions}/src/main/java/com/justcoordinates/forge/JustCoordinatesForge.java`

**Interfaces:**
- Consumes: `HudConfig.load(Path)` from Task 1

- [ ] **Step 1: Fabric — add load call as first line of `onInitializeClient()`** (all 9 Fabric entry files)

```java
import com.justcoordinates.HudConfig;
import net.fabricmc.loader.api.FabricLoader;
// ...
    @Override
    public void onInitializeClient() {
        HudConfig.load(FabricLoader.getInstance().getConfigDir());
        // ... existing registrations unchanged
    }
```

- [ ] **Step 2: Forge — add a constructor** (all 19 Forge entry files)

```java
import com.justcoordinates.HudConfig;
import net.minecraftforge.fml.loading.FMLPaths;
// ...
    public JustCoordinatesForge() {
        HudConfig.load(FMLPaths.CONFIGDIR.get());
    }
```

- [ ] **Step 3: NeoForge — add a constructor** (all 11 NeoForge entry files)

```java
import com.justcoordinates.HudConfig;
import net.neoforged.fml.loading.FMLPaths;
// ...
    public JustCoordinatesNeoForge() {
        HudConfig.load(FMLPaths.CONFIGDIR.get());
    }
```

- [ ] **Step 4: Compile spot-check**

```bash
./gradlew :fabric:build :forge:build -Ptarget_mc_version=1.16.5 -x test
./gradlew :fabric:build :neoforge:build :forge:build -Ptarget_mc_version=1.21.11 -x test
./gradlew :fabric:build :neoforge:build :forge:build -Ptarget_mc_version=26.2 -x test
```
Expected: BUILD SUCCESSFUL ×3.

- [ ] **Step 5: Runtime check (config file loading)**

```bash
echo '{"schema_version":1,"position":"bottom_right"}' > fabric/26.2/run/config/justcoordinates.json  # adjust run dir if different
./gradlew :fabric:runClient -Ptarget_mc_version=26.2
```
Expected: game starts; no errors in log mentioning justcoordinates. (HUD still renders top-left — anchors come in Task 3.)
Also verify a corrupt file logs a warning and boots:
```bash
echo 'not json' > <run dir>/config/justcoordinates.json
```
Expected: warn log `Failed to read justcoordinates.json; using defaults`, game boots.

- [ ] **Step 6: Commit (ask user first)**

```bash
git add fabric neoforge forge
git commit -m "feat: load HUD config at client init on all loaders"
```

---

### Task 3: Anchor-aware rendering (`CoordinatesHudRenderer` ×19)

**Files:**
- Modify: `common/{all 19 versions}/src/main/java/com/justcoordinates/CoordinatesHudRenderer.java`

**Interfaces:**
- Consumes: `HudConfig.getPosition()`, `HudPosition.resolveX/resolveY` from Task 1

- [ ] **Step 1: Replace the fixed-origin block in `render()`** — archetype for 1.21.1–1.21.11 (GuiGraphics):

Replace:
```java
        int bgX1 = MARGIN;
        int bgY1 = MARGIN;
        int bgX2 = MARGIN + PADDING + textWidth + PADDING;
        int bgY2 = MARGIN + PADDING + textHeight + PADDING;

        guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, BACKGROUND_COLOR);
        guiGraphics.drawString(mc.font, text,
                MARGIN + PADDING, MARGIN + PADDING, TEXT_COLOR);
```
with:
```java
        int hudWidth = PADDING + textWidth + PADDING;
        int hudHeight = PADDING + textHeight + PADDING;
        HudPosition position = HudConfig.getPosition();
        int bgX1 = position.resolveX(guiGraphics.guiWidth(), hudWidth, MARGIN);
        int bgY1 = position.resolveY(guiGraphics.guiHeight(), hudHeight, MARGIN);

        guiGraphics.fill(bgX1, bgY1, bgX1 + hudWidth, bgY1 + hudHeight, BACKGROUND_COLOR);
        guiGraphics.drawString(mc.font, text,
                bgX1 + PADDING, bgY1 + PADDING, TEXT_COLOR);
```
(No new imports needed — `HudConfig`/`HudPosition` share the package.)

- [ ] **Step 2: Apply the same change to the other version families** with these substitutions (final draw calls keep each file's existing method, only origin args change):

| Versions | Screen size source | Draw call to keep |
|----------|--------------------|-------------------|
| 1.16.5–1.18.2 | `mc.getWindow().getGuiScaledWidth()` / `getGuiScaledHeight()` | `GuiComponent.fill(poseStack, ...)` + `mc.font.drawShadow(poseStack, text, bgX1 + PADDING, bgY1 + PADDING, TEXT_COLOR)` |
| 1.19.2 | `mc.getWindow().getGuiScaledWidth()` / `getGuiScaledHeight()` | same as above |
| 1.20.1 | `guiGraphics.guiWidth()` / `guiHeight()` | `guiGraphics.fill(...)` + `guiGraphics.drawString(...)` |
| 1.21.1–1.21.11 | `guiGraphics.guiWidth()` / `guiHeight()` | as in Step 1 |
| 26.x (GuiGraphicsExtractor) | `guiGraphics.guiWidth()` / `guiHeight()` if present; if the method doesn't compile, check the class for the renamed accessor (open the version's loom-cached source); fallback `mc.getWindow().getGuiScaledWidth()` | `guiGraphics.fill(...)` + `guiGraphics.text(...)` |

Note: `mc.font.drawShadow(poseStack, Component, ...)` takes float x/y on 1.16.5–1.19.2 — passing ints is fine (implicit widening).

- [ ] **Step 3: Build all 19 versions**

```bash
for v in 1.16.5 1.17.1 1.18.2 1.19.2 1.20.1 1.21.1 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10 1.21.11 26.1 26.1.1 26.1.2 26.2; do ./gradlew build -Ptarget_mc_version=$v -x test || break; done
```
Expected: BUILD SUCCESSFUL for every version.

- [ ] **Step 4: Runtime check — hand-edit JSON to each of the 6 positions**

```bash
./gradlew :fabric:runClient -Ptarget_mc_version=1.21.11
```
With `<run dir>/config/justcoordinates.json` set to each `position` value in turn, verify: HUD renders at the expected corner/center; `bottom_center` clears the hotbar; with the file **deleted**, HUD is at top-left exactly as the previous release.

- [ ] **Step 5: Commit (ask user first)**

```bash
git add common
git commit -m "feat: render HUD at configured anchor position"
```

---

### Task 4: `ConfigScreen` + translations (×19)

**Files:**
- Create: `common/{all 19 versions}/src/main/java/com/justcoordinates/ConfigScreen.java`
- Modify: `common/{all 19 versions}/src/main/resources/assets/justcoordinates/lang/en_us.json` and `ja_jp.json`

**Interfaces:**
- Consumes: `HudConfig.getPosition/setPosition/save`, `HudPosition.values()/next()/getTranslationKey()` from Task 1
- Produces: `ConfigScreen(Screen parent)` — constructor used by Tasks 5–7 (`new ConfigScreen(null)` from keybinding; `ConfigScreen::new` from factories)

- [ ] **Step 1: Archetype C — 1.20.1 and 1.21.1–1.21.11** (12 versions; also the starting point for Archetype D; complete file):

```java
package com.justcoordinates;

import java.util.Arrays;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("justcoordinates.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(CycleButton.builder(
                        (HudPosition value) -> Component.translatable(value.getTranslationKey()))
                .withValues(Arrays.asList(HudPosition.values()))
                .withInitialValue(HudConfig.getPosition())
                .create(width / 2 - 100, height / 2 - 24, 200, 20,
                        Component.translatable("justcoordinates.config.position"),
                        (button, value) -> HudConfig.setPosition(value)));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height / 2 + 4, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        HudConfig.save();
        minecraft.setScreen(parent);
    }
}
```
1.20.1 difference: `Screen.render` does not draw the background itself — add `renderBackground(guiGraphics);` as the first line of `render`. If any 1.21.x version's `renderBackground`/`render` signature differs at compile, follow that version's vanilla screens (loom-cached source).

- [ ] **Step 2: Archetype B — 1.17.1, 1.18.2, 1.19.2** (complete file for 1.17.1/1.18.2; for 1.19.2 replace every `new TranslatableComponent(X)` with `Component.translatable(X)` and drop that import):

```java
package com.justcoordinates;

import java.util.Arrays;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(new TranslatableComponent("justcoordinates.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(CycleButton.builder(
                        (HudPosition value) -> (Component) new TranslatableComponent(value.getTranslationKey()))
                .withValues(Arrays.asList(HudPosition.values()))
                .withInitialValue(HudConfig.getPosition())
                .create(width / 2 - 100, height / 2 - 24, 200, 20,
                        new TranslatableComponent("justcoordinates.config.position"),
                        (button, value) -> HudConfig.setPosition(value)));
        addRenderableWidget(new Button(width / 2 - 100, height / 2 + 4, 200, 20,
                CommonComponents.GUI_DONE, button -> onClose()));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, title, width / 2, height / 2 - 60, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        HudConfig.save();
        minecraft.setScreen(parent);
    }
}
```

- [ ] **Step 3: Archetype A — 1.16.5** (no `CycleButton`; complete file):

```java
package com.justcoordinates;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(new TranslatableComponent("justcoordinates.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addButton(new Button(width / 2 - 100, height / 2 - 24, 200, 20, positionLabel(),
                button -> {
                    HudConfig.setPosition(HudConfig.getPosition().next());
                    button.setMessage(positionLabel());
                }));
        addButton(new Button(width / 2 - 100, height / 2 + 4, 200, 20, CommonComponents.GUI_DONE,
                button -> onClose()));
    }

    private Component positionLabel() {
        return new TranslatableComponent("justcoordinates.config.position")
                .append(": ")
                .append(new TranslatableComponent(HudConfig.getPosition().getTranslationKey()));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, title, width / 2, height / 2 - 60, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        HudConfig.save();
        minecraft.setScreen(parent);
    }
}
```
Note: 1.16.5 `Screen.onClose()` exists and is called on ESC. If `minecraft` is package-private there, use `this.minecraft` (it is protected — fine).

- [ ] **Step 4: Archetype D — 26.x (26.1, 26.1.1, 26.1.2, 26.2)** — start from Archetype C, then apply the same renames the existing 26.x `CoordinatesHudRenderer` shows relative to 1.21.11 (e.g. `GuiGraphics` → `GuiGraphicsExtractor`, `drawString` → `text`). For the centered title use:

```java
        int titleWidth = font.width(title);
        guiGraphics.text(font, title, (width - titleWidth) / 2, height / 2 - 60, 0xFFFFFFFF);
```
If `CycleButton` no longer exists or its builder moved: fall back to the Archetype A plain-`Button` cycling pattern (with `Component.translatable`). Resolve exact names against the version's loom-cached vanilla source; compile is the arbiter.

- [ ] **Step 5: Add translation keys** to `en_us.json` and `ja_jp.json` in all 19 `common/{version}` resources (merge into existing JSON; keep existing keys):

en_us.json additions:
```json
{
  "justcoordinates.config.title": "Just Coordinates",
  "justcoordinates.config.position": "Position",
  "justcoordinates.position.top_left": "Top Left",
  "justcoordinates.position.top_center": "Top Center",
  "justcoordinates.position.top_right": "Top Right",
  "justcoordinates.position.bottom_left": "Bottom Left",
  "justcoordinates.position.bottom_center": "Bottom Center",
  "justcoordinates.position.bottom_right": "Bottom Right",
  "key.justcoordinates.open_config": "Open Settings"
}
```
ja_jp.json additions:
```json
{
  "justcoordinates.config.title": "Just Coordinates",
  "justcoordinates.config.position": "表示位置",
  "justcoordinates.position.top_left": "左上",
  "justcoordinates.position.top_center": "上中央",
  "justcoordinates.position.top_right": "右上",
  "justcoordinates.position.bottom_left": "左下",
  "justcoordinates.position.bottom_center": "下中央",
  "justcoordinates.position.bottom_right": "右下",
  "key.justcoordinates.open_config": "設定画面を開く"
}
```
(If a version has no `ja_jp.json`, create it with the full set of existing en keys' ja equivalents already used by sibling versions.)

- [ ] **Step 6: Build all 19 versions** (same loop command as Task 3 Step 3). Expected: BUILD SUCCESSFUL ×19.

- [ ] **Step 7: Commit (ask user first)**

```bash
git add common
git commit -m "feat: add self-built config screen and translations"
```

---

### Task 5: "Open settings" keybinding (renderer + all entries)

**Files:**
- Modify: `common/{all 19 versions}/src/main/java/com/justcoordinates/CoordinatesHudRenderer.java`
- Modify: all 39 loader entry files (same set as Task 2)

**Interfaces:**
- Consumes: `ConfigScreen(Screen parent)` from Task 4
- Produces: `CoordinatesHudRenderer.getOpenConfigKey(): KeyMapping` — registered by entries

- [ ] **Step 1: Add the key + handling to each renderer** (mirror that file's `TOGGLE_KEY` declaration style — category arg differs by version; key code is `GLFW.GLFW_KEY_UNKNOWN` = unbound):

```java
    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.justcoordinates.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            /* same category argument as TOGGLE_KEY in this file */
    );

    public static KeyMapping getOpenConfigKey() {
        return OPEN_CONFIG_KEY;
    }

    public static void handleTick() {
        while (TOGGLE_KEY.consumeClick()) {
            visible = !visible;
        }
        while (OPEN_CONFIG_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new ConfigScreen(null));
        }
    }
```

- [ ] **Step 2: Register the key in every entry, one line next to the existing toggle-key registration, in that file's existing style:**
- Fabric pre-26.x: `KeyBindingHelper.registerKeyBinding(CoordinatesHudRenderer.getOpenConfigKey());`
- Fabric 26.x: `KeyMappingHelper.registerKeyMapping(CoordinatesHudRenderer.getOpenConfigKey());`
- Forge 1.19.2+/NeoForge: `event.register(CoordinatesHudRenderer.getOpenConfigKey());` inside the existing `RegisterKeyMappingsEvent` handler
- Forge 1.16.5–1.18.2: duplicate the exact call used for the toggle key in that file (e.g. `ClientRegistry.registerKeyBinding(...)`)

- [ ] **Step 3: Build all 19 versions** (loop command from Task 3 Step 3). Expected: BUILD SUCCESSFUL ×19.

- [ ] **Step 4: Runtime check**

```bash
./gradlew :fabric:runClient -Ptarget_mc_version=1.21.11
```
In Options → Controls, both "Toggle Coordinates" and "Open Settings" appear under Just Coordinates; assign a key (e.g. K) to Open Settings; in-world press K → screen opens with translated labels; cycle position → HUD behind the screen moves immediately; Done → `config/justcoordinates.json` written; restart runClient → position retained.

- [ ] **Step 5: Commit (ask user first)**

```bash
git add common fabric neoforge forge
git commit -m "feat: add keybinding to open the config screen"
```

---

### Task 6: Forge/NeoForge native config-screen registration

**Files:**
- Modify: `forge/{all 19 versions}/.../JustCoordinatesForge.java`
- Modify: `neoforge/base` + `neoforge/{10 versions}/.../JustCoordinatesNeoForge.java`

**Interfaces:**
- Consumes: `ConfigScreen(Screen parent)` from Task 4

Note: all factory lambdas below use untyped parameters on purpose — `(minecraftOrContainer, parent) -> new ConfigScreen(parent)` compiles against either `(Minecraft, Screen)` or `(ModContainer, Screen)` functional signatures, absorbing the mid-1.21.x NeoForge signature change.

- [ ] **Step 1: Forge — add to the constructor created in Task 2:**

1.19.2–1.21.x and 26.x (expected API; compile verifies — if a 26.x version dropped `ConfigScreenHandler`, search that Forge version's sources for `ConfigScreen` factory/extension classes and adapt):
```java
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
// in constructor:
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));
```
1.17.1 / 1.18.2:
```java
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModLoadingContext;
// in constructor:
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((minecraft, parent) -> new ConfigScreen(parent)));
```
1.16.5:
```java
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
// in constructor:
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, parent) -> new ConfigScreen(parent));
```
(Import `com.justcoordinates.ConfigScreen` in each.)

- [ ] **Step 2: NeoForge — switch the constructor to receive `ModContainer` and register:**

```java
import com.justcoordinates.ConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
// ...
    public JustCoordinatesNeoForge(ModContainer container) {
        HudConfig.load(FMLPaths.CONFIGDIR.get());
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (minecraftOrContainer, parent) -> new ConfigScreen(parent));
    }
```
If a version fails to compile (package or signature moved), check that NeoForge version's `IConfigScreenFactory` source and adapt only that version's entry.

- [ ] **Step 3: Build all Forge + NeoForge versions** (loop from Task 3 Step 3 covers them). Expected: BUILD SUCCESSFUL ×19.

- [ ] **Step 4: Runtime check**

```bash
./gradlew :neoforge:runClient -Ptarget_mc_version=1.21.11
```
Mods → Just Coordinates → Config button opens the screen; Done returns to the Mods screen (parent restored).
Forge on macOS: verify via built JAR in Prism for one modern version (e.g. 1.21.11) — Mods list shows Config button.

- [ ] **Step 5: Commit (ask user first)**

```bash
git add forge neoforge
git commit -m "feat: register config screen with Forge/NeoForge mod list"
```

---

### Task 7: Fabric ModMenu integration (compileOnly)

**Files:**
- Modify: `fabric/build.gradle` (repo + conditional dependency)
- Modify: `props/{version}.properties` (add `modmenu_version` where available)
- Create: `JustCoordinatesModMenu.java` in each Fabric source root (base + per-version roots)
- Modify: `fabric/{version}` `fabric.mod.json` files (add `modmenu` entrypoint)

**Interfaces:**
- Consumes: `ConfigScreen(Screen parent)` from Task 4

- [ ] **Step 1: Research ModMenu artifact versions.** For each MC version 1.16.5–26.2, find the matching ModMenu version (Modrinth: https://modrinth.com/mod/modmenu/versions, or TerraformersMC maven). Record results in the props files as `modmenu_version=<ver>`. **Skip any MC version with no ModMenu release** (its Fabric build simply keeps only the keybinding entry). Note the sandbox network allowlist may need `maven.terraformersmc.com` added when Gradle first resolves it.

- [ ] **Step 2: Determine Fabric source roots per version.** Run `grep -n "srcDir" fabric/build.gradle` to see which versions compile `fabric/base` vs their own root; place `JustCoordinatesModMenu` accordingly (base once for the versions that share it, plus one copy per own-root version that has ModMenu).

- [ ] **Step 3: Add build config to `fabric/build.gradle`** (follow the file's existing per-version conditional style):

```gradle
repositories {
    maven { url = 'https://maven.terraformersmc.com/releases' }
}
dependencies {
    if (project.hasProperty('modmenu_version')) {   // adapt to how props/*.properties values are exposed in this build
        modCompileOnly "com.terraformersmc:modmenu:${modmenu_version}"   // plain compileOnly on 26.x (no remapping)
    }
}
```

- [ ] **Step 4: Create the ModMenu entry class** (1.17.1+ package shown; for 1.16.5 replace both imports with `io.github.prospector.modmenu.api.*`):

```java
package com.justcoordinates.fabric;

import com.justcoordinates.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class JustCoordinatesModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
```

- [ ] **Step 5: Add the entrypoint to each applicable `fabric.mod.json`:**

```json
    "entrypoints": {
        "client": ["..."],
        "modmenu": ["com.justcoordinates.fabric.JustCoordinatesModMenu"]
    }
```

- [ ] **Step 6: Build all Fabric versions** (loop from Task 3 Step 3). Expected: BUILD SUCCESSFUL for every version (including those without `modmenu_version`).

- [ ] **Step 7: Functional check (one version).** Temporarily add `modLocalRuntime "com.terraformersmc:modmenu:<ver for 1.21.11>"` to the fabric deps, `./gradlew :fabric:runClient -Ptarget_mc_version=1.21.11`, verify Mods → Just Coordinates shows the config button and it opens `ConfigScreen`. Remove the temporary line afterwards (`git diff` must show no leftover).

- [ ] **Step 8: Commit (ask user first)**

```bash
git add fabric props
git commit -m "feat: add optional ModMenu config screen integration"
```

---

### Task 8: Full verification + docs

**Files:**
- Possibly modify: `README.md`, `docs/modrinth_description.md`, `docs/curseforge_description.md`

- [ ] **Step 1: Full build matrix** — run the Task 3 Step 3 loop over all 19 versions once more from clean:

```bash
./gradlew clean
for v in 1.16.5 1.17.1 1.18.2 1.19.2 1.20.1 1.21.1 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10 1.21.11 26.1 26.1.1 26.1.2 26.2; do ./gradlew build -Ptarget_mc_version=$v -x test || break; done
```
Expected: 19× BUILD SUCCESSFUL.

- [ ] **Step 2: Manual verification checklist** (from the spec; runClient on Fabric 26.2 / 1.21.11 / 1.16.5, NeoForge 1.21.11; Forge via Prism where needed):
  - GUI opens from all three entries (key, Forge/NeoForge Config button, ModMenu)
  - All 6 positions render correctly incl. bottom-center hotbar clearance
  - Position survives restart; JSON file created only after first save
  - Corrupt JSON boots with warn + defaults
  - No config file → HUD identical to previous release (top-left)
  - Toggle key (J) still works; debug screen (F3) and hideGui (F1) still suppress the HUD

- [ ] **Step 3: Docs check.** `grep -in "config\|position" README.md docs/modrinth_description.md docs/curseforge_description.md` — if they claim "no configuration" or similar, draft updates describing the new position setting and confirm wording with the user before editing.

- [ ] **Step 4: Commit docs if changed (ask user first)**

```bash
git add README.md docs
git commit -m "docs: describe HUD position setting"
```

---

## Self-Review Notes

- Spec coverage: model/persistence (T1), load wiring (T2), anchors incl. BOTTOM_CENTER lift (T3), GUI + i18n (T4), keybinding entry (T5), Forge/NeoForge native entry (T6), ModMenu (T7), verification + docs (T8). Out-of-scope items (visible persistence, offsets, drag) intentionally absent.
- Known-unknowns are confined to explicit compile-verified steps (26.x screen API names, Forge 26.x factory class, NeoForge signature, ModMenu availability) with concrete fallbacks that never lose the keybinding entry.
- Type consistency: `ConfigScreen(Screen parent)`, `getOpenConfigKey()`, `HudConfig.load(Path)/save()`, `HudPosition.resolveX/Y(int,int,int)` used identically across tasks.
