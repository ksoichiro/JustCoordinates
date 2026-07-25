# Share Coordinates to Chat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let players broadcast their current coordinates to chat via a client-side command (`/justcoordinates share`, aliased `/jc share`) or an unbound keybinding, across all 19 MC versions × Fabric/NeoForge/Forge.

**Architecture:** The locale-independent message string is built once in `common/shared` (pure Java). Sending it is version-specific and isolated in a new `CoordinatesShare` class per `common/{version}`. `CoordinatesHudRenderer` gains a third `KeyMapping` following the existing two. Client-command registration is loader-specific and lives inline in each loader's existing entry class, next to the existing keybinding registration.

**Tech Stack:** Brigadier (bundled with MC), Fabric client command API (v1 for 1.16.5–1.18.2, v2 for 1.19.2+), Forge/NeoForge `RegisterClientCommandsEvent`. No new dependencies — the full Fabric API is already a dependency and ships the command modules.

**Spec:** `docs/superpowers/specs/2026-07-25-share-coordinates-to-chat-design.md`

## Global Constraints

- Chat message format is **locale-independent** and fixed: `X: 100, Y: 64, Z: -200`. Never build it from a translation key — other players read it.
- Coordinates use `Mth.floor()` of `mc.player.getX()/getY()/getZ()`, identical to what `CoordinatesHudRenderer.render` displays.
- `common/shared` compiles on every version's classpath → max language level **Java 16** (1.16.5). No records, no `var` in fields, no switch expressions.
- New keybinding `key.justcoordinates.share` defaults to **`GLFW.GLFW_KEY_UNKNOWN` (unbound)**, matching the existing "Open Settings" key. Never bind a default key: an accidental press sends chat visible to other players.
- New translation keys (en_us + ja_jp, ×19): `key.justcoordinates.share` = `"Share Coordinates"` / `"座標をチャットで共有"`.
- Command tree: root literal `justcoordinates` with child `share`; second literal `jc` registered as a Brigadier `redirect()` to the root node. The alias is best-effort (another mod may already own `/jc`); the canonical name must always work.
- **Forge 1.16.5 and 1.17.1 get the keybinding only** — `net.minecraftforge.client.event.RegisterClientCommandsEvent` does not exist in Forge 36.x/37.x (verified against `forge-1.16.5-36.2.42` and `forge-1.17.1-37.1.1`). Do not attempt a workaround; do not add the command there.
- No new runtime dependencies, no mod-metadata (`fabric.mod.json` / `mods.toml`) changes.
- Never touch rendering code — this feature must not change HUD output in any way.
- Commit messages: English, Conventional Commits. **Ask the user before every `git commit`** (user's global policy).
- No test infrastructure exists in this repo: verification = compile/build + `runClient` and Prism manual checks (same as the 2026-07-06 plan).
- Build command form: `./gradlew :fabric:build -Ptarget_mc_version=<v>` (also `:neoforge:build`, `:forge:build`) from the repo root.
- macOS dev-env constraints (known, not bugs): Forge FG7 `runClient` needs `-XstartOnFirstThread`; Forge 1.16.5/1.18.2 dev clients don't run — verify those built JARs via Prism.

### Verified API facts (do not re-derive)

Chat send, by version:

| Versions | Call |
|----------|------|
| 1.16.5, 1.17.1, 1.18.2 | `mc.player.chat(String)` |
| 1.19.2 | `mc.player.chatSigned(String, Component)` — pass `null` for the second argument (`sendChat` has the same signature but is `private` on `LocalPlayer` in 1.19.2) |
| 1.20.1, 1.21.1, 1.21.3–1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 | `mc.player.connection.sendChat(String)` |

Fabric client command API, by version:

| Versions | API |
|----------|-----|
| 1.16.5, 1.17.1, 1.18.2 | `net.fabricmc.fabric.api.client.command.v1.ClientCommandManager` — static `DISPATCHER` field, register at init |
| 1.19.2, 1.20.1, 1.21.1, 1.21.3–1.21.11 | `...command.v2.ClientCommandRegistrationCallback.EVENT` + `...command.v2.ClientCommandManager.literal` |
| 26.1, 26.1.1, 26.1.2, 26.2 | `...command.v2.ClientCommandRegistrationCallback.EVENT` + `...command.v2.ClientCommands.literal` (`ClientCommandManager` was removed in fabric-command-api-v2 3.x) |

`KeyMapping` constructor's 4th argument:

| Versions | 4th argument |
|----------|--------------|
| 1.16.5–1.21.8 | `"key.categories.justcoordinates"` (String) |
| 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 | the existing `CATEGORY` constant (`KeyMapping.Category`) |

---

### Task 1: Shared message formatter

**Files:**
- Create: `common/shared/src/main/java/com/justcoordinates/CoordinatesMessage.java`

**Interfaces:**
- Produces: `CoordinatesMessage.format(int x, int y, int z): String` → `"X: 100, Y: 64, Z: -200"`

- [ ] **Step 1: Write `CoordinatesMessage.java`**

```java
package com.justcoordinates;

public final class CoordinatesMessage {
    private CoordinatesMessage() {
    }

    /**
     * Builds the chat line for sharing coordinates. Deliberately not translated: the message is
     * sent to other players, whose language may differ from the sender's.
     */
    public static String format(int x, int y, int z) {
        return "X: " + x + ", Y: " + y + ", Z: " + z;
    }
}
```

- [ ] **Step 2: Verify it compiles on the oldest and newest versions**

Run:
```bash
./gradlew :fabric:build -Ptarget_mc_version=1.16.5
./gradlew :fabric:build -Ptarget_mc_version=26.2
```
Expected: BUILD SUCCESSFUL for both.

- [ ] **Step 3: Confirm the class actually lands in the jar**

`common/shared` is wired into platform jars via `srcDir`; a missing wire compiles fine but silently drops the class.

Run:
```bash
unzip -l fabric/26.2/build/libs/*.jar | grep CoordinatesMessage
```
Expected: one line showing `com/justcoordinates/CoordinatesMessage.class`.

- [ ] **Step 4: Ask the user, then commit**

```bash
git add common/shared/src/main/java/com/justcoordinates/CoordinatesMessage.java
git commit -m "feat: add locale-independent coordinates message formatter"
```

---

### Task 2: Version-specific chat sender (`CoordinatesShare` ×19)

**Files:**
- Create (×19): `common/{version}/src/main/java/com/justcoordinates/CoordinatesShare.java` for every version in
  `1.16.5, 1.17.1, 1.18.2, 1.19.2, 1.20.1, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2`

**Interfaces:**
- Consumes: `CoordinatesMessage.format(int, int, int)` from Task 1
- Produces: `CoordinatesShare.share(): void` — sends the current coordinates to chat; no-op when not in a world

- [ ] **Step 1: Write variant A for `1.16.5`, `1.17.1`, `1.18.2`**

```java
package com.justcoordinates;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class CoordinatesShare {
    private CoordinatesShare() {
    }

    public static void share() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        String message = CoordinatesMessage.format(
                Mth.floor(player.getX()),
                Mth.floor(player.getY()),
                Mth.floor(player.getZ()));
        player.chat(message);
    }
}
```

- [ ] **Step 2: Write variant B for `1.19.2` only**

`LocalPlayer.chat(String)` is gone in 1.19.2. The public entry point is `chatSigned(String, Component)`, whose second argument is the (unused) signed-chat preview component. `sendChat` has the same signature but is `private` in 1.19.2.

```java
package com.justcoordinates;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class CoordinatesShare {
    private CoordinatesShare() {
    }

    public static void share() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        String message = CoordinatesMessage.format(
                Mth.floor(player.getX()),
                Mth.floor(player.getY()),
                Mth.floor(player.getZ()));
        // 1.19.2 only: chatSigned's second argument is the (unused) signed-chat preview component.
        player.chatSigned(message, null);
    }
}
```

- [ ] **Step 3: Write variant C for `1.20.1`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`**

```java
package com.justcoordinates;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class CoordinatesShare {
    private CoordinatesShare() {
    }

    public static void share() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        String message = CoordinatesMessage.format(
                Mth.floor(player.getX()),
                Mth.floor(player.getY()),
                Mth.floor(player.getZ()));
        player.connection.sendChat(message);
    }
}
```

- [ ] **Step 4: Build one version per variant**

Run:
```bash
./gradlew :fabric:build -Ptarget_mc_version=1.16.5
./gradlew :fabric:build -Ptarget_mc_version=1.19.2
./gradlew :fabric:build -Ptarget_mc_version=1.20.1
./gradlew :fabric:build -Ptarget_mc_version=26.2
```
Expected: BUILD SUCCESSFUL for all four. A `cannot find symbol: method chat/sendChat` here means the version landed in the wrong variant group — fix the grouping, do not change the API call arbitrarily.

- [ ] **Step 5: Build the remaining 15 versions**

Use the `minecraft-mod:for-each-version` skill, or run `./gradlew :fabric:build -Ptarget_mc_version=<v>` for each remaining version.
Expected: BUILD SUCCESSFUL for all 19.

- [ ] **Step 6: Ask the user, then commit**

```bash
git add common/*/src/main/java/com/justcoordinates/CoordinatesShare.java
git commit -m "feat: add per-version chat sender for coordinates sharing"
```

---

### Task 3: Share keybinding + translations

**Files:**
- Modify (×19): `common/{version}/src/main/java/com/justcoordinates/CoordinatesHudRenderer.java`
- Modify (×19): `common/{version}/src/main/resources/assets/justcoordinates/lang/en_us.json`
- Modify (×19): `common/{version}/src/main/resources/assets/justcoordinates/lang/ja_jp.json`

**Interfaces:**
- Consumes: `CoordinatesShare.share()` from Task 2
- Produces: `CoordinatesHudRenderer.getShareKey(): KeyMapping` — the loader entry classes register it in Task 4

- [ ] **Step 1: Add `SHARE_KEY` — String-category variant, for `1.16.5`, `1.17.1`, `1.18.2`, `1.19.2`, `1.20.1`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`**

Insert directly after the existing `OPEN_CONFIG_KEY` field:

```java
    private static final KeyMapping SHARE_KEY = new KeyMapping(
            "key.justcoordinates.share",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.justcoordinates"
    );
```

- [ ] **Step 2: Add `SHARE_KEY` — Category-object variant, for `1.21.9`, `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`**

Insert directly after the existing `OPEN_CONFIG_KEY` field:

```java
    private static final KeyMapping SHARE_KEY = new KeyMapping(
            "key.justcoordinates.share",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );
```

- [ ] **Step 3: Add the accessor (all 19 files, identical)**

Insert directly after the existing `getOpenConfigKey()` method:

```java
    public static KeyMapping getShareKey() {
        return SHARE_KEY;
    }
```

- [ ] **Step 4: Handle the key in `handleTick()` (all 19 files, identical)**

Append inside `handleTick()`, after the existing `OPEN_CONFIG_KEY` loop:

```java
        while (SHARE_KEY.consumeClick()) {
            CoordinatesShare.share();
        }
```

Do not touch `render()` in any file.

- [ ] **Step 5: Add the translation key to all 19 `en_us.json`**

Add this entry (keep the file valid JSON — the previous last entry needs a trailing comma):

```json
  "key.justcoordinates.share": "Share Coordinates"
```

- [ ] **Step 6: Add the translation key to all 19 `ja_jp.json`**

```json
  "key.justcoordinates.share": "座標をチャットで共有"
```

- [ ] **Step 7: Build all 19 versions (Fabric is enough for a compile check)**

Run per version: `./gradlew :fabric:build -Ptarget_mc_version=<v>`
Expected: BUILD SUCCESSFUL ×19. JSON syntax errors surface as a resource-processing failure or malformed lang at runtime — if unsure, verify with `python3 -m json.tool <file> > /dev/null` on each edited JSON file.

- [ ] **Step 8: Ask the user, then commit**

```bash
git add common/*/src/main/java/com/justcoordinates/CoordinatesHudRenderer.java common/*/src/main/resources/assets/justcoordinates/lang/*.json
git commit -m "feat: add share-coordinates keybinding and translations"
```

---

### Task 4: Register the keybinding in every loader entry point

The key must be registered or it never appears in Controls and never fires.

**Files:**
- Modify (Fabric, 9): `fabric/base`, `fabric/1.16.5`, `fabric/1.17.1`, `fabric/1.18.2`, `fabric/1.19.2`, `fabric/26.1`, `fabric/26.1.1`, `fabric/26.1.2`, `fabric/26.2` → `src/main/java/com/justcoordinates/fabric/JustCoordinatesFabric.java`
- Modify (NeoForge, 11): `neoforge/base`, `neoforge/1.21.6`, `neoforge/1.21.7`, `neoforge/1.21.8`, `neoforge/1.21.9`, `neoforge/1.21.10`, `neoforge/1.21.11`, `neoforge/26.1`, `neoforge/26.1.1`, `neoforge/26.1.2`, `neoforge/26.2` → `src/main/java/com/justcoordinates/neoforge/JustCoordinatesNeoForge.java`
- Modify (Forge, 19): `forge/{1.16.5,1.17.1,1.18.2,1.19.2,1.20.1,1.21.1,1.21.3,1.21.4,1.21.5,1.21.6,1.21.7,1.21.8,1.21.9,1.21.10,1.21.11,26.1,26.1.1,26.1.2,26.2}/src/main/java/com/justcoordinates/forge/JustCoordinatesForge.java` — every Forge module. `forge/26.1` exists with full sources but is excluded from builds by `enabled_platforms` in `props/26.1.properties`; keep it in sync anyway so re-enabling Forge 26.1 later cannot silently ship without this feature. It cannot be compile-verified — verify by diffing its edited region against `forge/26.1.1` and `forge/26.1.2`.

**Interfaces:**
- Consumes: `CoordinatesHudRenderer.getShareKey()` from Task 3

- [ ] **Step 1: Fabric — `fabric/base`, `fabric/1.16.5`, `fabric/1.17.1`, `fabric/1.18.2`, `fabric/1.19.2`**

Add one line after the existing `getOpenConfigKey()` registration in `onInitializeClient()`:

```java
        KeyBindingHelper.registerKeyBinding(CoordinatesHudRenderer.getShareKey());
```

- [ ] **Step 2: Fabric — `fabric/26.1`, `fabric/26.1.1`, `fabric/26.1.2`, `fabric/26.2`**

These use the newer helper:

```java
        KeyMappingHelper.registerKeyMapping(CoordinatesHudRenderer.getShareKey());
```

- [ ] **Step 3: NeoForge — all 11 modules**

Add one line inside the existing `registerKeyMappings(RegisterKeyMappingsEvent event)` method:

```java
            event.register(CoordinatesHudRenderer.getShareKey());
```

- [ ] **Step 4: Forge — `1.16.5`, `1.17.1`, `1.18.2`**

These register through `ClientRegistry` in `onClientSetup`. Add:

```java
            ClientRegistry.registerKeyBinding(CoordinatesHudRenderer.getShareKey());
```

- [ ] **Step 5: Forge — `1.19.2`, `1.20.1`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`**

Add inside the existing `registerKeyMappings(RegisterKeyMappingsEvent event)` method:

```java
            event.register(CoordinatesHudRenderer.getShareKey());
```

- [ ] **Step 6: Forge — `1.21.10`, `1.21.11`, `26.1.1`, `26.1.2`, `26.2`**

These register in the constructor via the per-event bus. Add inside the existing `RegisterKeyMappingsEvent.BUS.addListener` lambda:

```java
            event.register(CoordinatesHudRenderer.getShareKey());
```

- [ ] **Step 7: Build every enabled platform for all 19 versions**

Use the `minecraft-mod:for-each-version` skill with `./gradlew clean build -Ptarget_mc_version=<v> -x test`, or run the three platform builds per version (skip `:neoforge` for 1.16.5–1.20.1, skip `:forge` for 26.1).
Expected: BUILD SUCCESSFUL for every enabled platform × version.

- [ ] **Step 8: Runtime check on one version**

Run: `./gradlew :fabric:runClient -Ptarget_mc_version=1.21.11`
In game: Options > Controls > Key Binds > "Just Coordinates" shows **three** entries, with "Share Coordinates" unbound. Bind it to a free key, enter a world, press it, and confirm the chat line matches the HUD numbers exactly.

- [ ] **Step 9: Ask the user, then commit**

```bash
git add fabric/*/src/main/java/com/justcoordinates/fabric/JustCoordinatesFabric.java neoforge/*/src/main/java/com/justcoordinates/neoforge/JustCoordinatesNeoForge.java forge/*/src/main/java/com/justcoordinates/forge/JustCoordinatesForge.java
git commit -m "feat: register share-coordinates keybinding on all loaders"
```

---

### Task 5: Fabric client command

**Files:**
- Modify (9): `fabric/{base,1.16.5,1.17.1,1.18.2,1.19.2,26.1,26.1.1,26.1.2,26.2}/src/main/java/com/justcoordinates/fabric/JustCoordinatesFabric.java`

**Interfaces:**
- Consumes: `CoordinatesShare.share()` from Task 2

- [ ] **Step 1: `fabric/1.16.5`, `fabric/1.17.1`, `fabric/1.18.2` (command-api-v1)**

Add imports:

```java
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
```

Add at the end of `onInitializeClient()`:

```java
        LiteralCommandNode<FabricClientCommandSource> shareRoot = ClientCommandManager.DISPATCHER.register(
                ClientCommandManager.literal("justcoordinates")
                        .then(ClientCommandManager.literal("share")
                                .executes(context -> {
                                    CoordinatesShare.share();
                                    return 1;
                                })));
        // Best-effort short alias: if another client-side mod already owns "/jc", only the alias
        // is lost, never "/justcoordinates share".
        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("jc").redirect(shareRoot));
```

Also add `import com.justcoordinates.CoordinatesShare;` next to the existing `com.justcoordinates.*` imports.

- [ ] **Step 2: `fabric/base` and `fabric/1.19.2` (command-api-v2 2.x)**

Add imports:

```java
import com.justcoordinates.CoordinatesShare;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
```

Add at the end of `onInitializeClient()`:

```java
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralCommandNode<FabricClientCommandSource> shareRoot = dispatcher.register(
                    ClientCommandManager.literal("justcoordinates")
                            .then(ClientCommandManager.literal("share")
                                    .executes(context -> {
                                        CoordinatesShare.share();
                                        return 1;
                                    })));
            // Best-effort short alias: if another client-side mod already owns "/jc", only the
            // alias is lost, never "/justcoordinates share".
            dispatcher.register(ClientCommandManager.literal("jc").redirect(shareRoot));
        });
```

- [ ] **Step 3: `fabric/26.1`, `fabric/26.1.1`, `fabric/26.1.2`, `fabric/26.2` (command-api-v2 3.x)**

Identical to Step 2 except the helper class: fabric-command-api-v2 3.x removed `ClientCommandManager` and replaced it with `ClientCommands`.

Add imports:

```java
import com.justcoordinates.CoordinatesShare;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
```

Add at the end of `onInitializeClient()`:

```java
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            LiteralCommandNode<FabricClientCommandSource> shareRoot = dispatcher.register(
                    ClientCommands.literal("justcoordinates")
                            .then(ClientCommands.literal("share")
                                    .executes(context -> {
                                        CoordinatesShare.share();
                                        return 1;
                                    })));
            // Best-effort short alias: if another client-side mod already owns "/jc", only the
            // alias is lost, never "/justcoordinates share".
            dispatcher.register(ClientCommands.literal("jc").redirect(shareRoot));
        });
```

- [ ] **Step 4: Build all 19 Fabric targets**

Run per version: `./gradlew :fabric:build -Ptarget_mc_version=<v>`
Expected: BUILD SUCCESSFUL ×19.

- [ ] **Step 5: Runtime check on three versions (one per API variant)**

Run each and, in a world, type `/justcoordinates share` and `/jc share`:
```bash
./gradlew :fabric:runClient -Ptarget_mc_version=1.16.5    # v1 API
./gradlew :fabric:runClient -Ptarget_mc_version=1.21.11   # v2 2.x
./gradlew :fabric:runClient -Ptarget_mc_version=26.2      # v2 3.x
```
Expected: both forms send the coordinates line; typing `/ju` + Tab completes the command; the numbers match the HUD.

- [ ] **Step 6: Ask the user, then commit**

```bash
git add fabric/*/src/main/java/com/justcoordinates/fabric/JustCoordinatesFabric.java
git commit -m "feat: add /justcoordinates share client command on Fabric"
```

---

### Task 6: Forge client command

`RegisterClientCommandsEvent` is fired on the Forge game bus, so the handler belongs in the game-bus (`Bus.FORGE`, or annotation without an explicit `bus`) class of each entry file — **not** the MOD-bus class.

**Files (17 — `forge/1.16.5` and `forge/1.17.1` are deliberately excluded):**
- Modify: `forge/{1.18.2,1.19.2,1.20.1,1.21.1,1.21.3,1.21.4,1.21.5,1.21.6,1.21.7,1.21.8,1.21.9,1.21.10,1.21.11,26.1,26.1.1,26.1.2,26.2}/src/main/java/com/justcoordinates/forge/JustCoordinatesForge.java`

`forge/26.1` is excluded from builds by `enabled_platforms` in `props/26.1.properties`, so it cannot be compile-verified; keep it in sync anyway and verify by diffing its edited region against `forge/26.1.1` and `forge/26.1.2`.

Target class per version (annotation style):

| Version | Class to add the handler to |
|---------|------------------------------|
| 1.18.2 | `ClientEvents` (`bus = Bus.FORGE`) |
| 1.19.2, 1.20.1, 1.21.1, 1.21.3 | `ClientTickHandler` (annotation without `bus`) |
| 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8 | `ClientEvents` (`bus = Bus.FORGE`) |
| 1.21.9 | `ClientEvents` (`bus = Bus.FORGE`) |

Versions `1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2` use the constructor + per-event `BUS` style instead (Step 2).

**Interfaces:**
- Consumes: `CoordinatesShare.share()` from Task 2

- [ ] **Step 1: Annotation style — `1.18.2`, `1.19.2`, `1.20.1`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`**

Add imports:

```java
import com.justcoordinates.CoordinatesShare;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
```

Add this method to the game-bus class named in the table above:

```java
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            LiteralCommandNode<CommandSourceStack> shareRoot = event.getDispatcher().register(
                    Commands.literal("justcoordinates")
                            .then(Commands.literal("share")
                                    .executes(context -> {
                                        CoordinatesShare.share();
                                        return 1;
                                    })));
            // Best-effort short alias: if another client-side mod already owns "/jc", only the
            // alias is lost, never "/justcoordinates share".
            event.getDispatcher().register(Commands.literal("jc").redirect(shareRoot));
        }
```

- [ ] **Step 2: Per-event BUS style — `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`**

Add the same imports as Step 1, then add this to the constructor, directly after the existing `RegisterKeyMappingsEvent.BUS.addListener(...)` block:

```java
        RegisterClientCommandsEvent.BUS.addListener(event -> {
            LiteralCommandNode<CommandSourceStack> shareRoot = event.getDispatcher().register(
                    Commands.literal("justcoordinates")
                            .then(Commands.literal("share")
                                    .executes(context -> {
                                        CoordinatesShare.share();
                                        return 1;
                                    })));
            // Best-effort short alias: if another client-side mod already owns "/jc", only the
            // alias is lost, never "/justcoordinates share".
            event.getDispatcher().register(Commands.literal("jc").redirect(shareRoot));
        });
```

This mirrors the existing `RegisterKeyMappingsEvent.BUS.addListener` call, which already registers a client-only event type from the constructor without breaking dedicated servers.

- [ ] **Step 3: Leave `forge/1.16.5` and `forge/1.17.1` untouched**

Confirm no `RegisterClientCommandsEvent` reference exists in either file:

```bash
grep -rn "RegisterClientCommandsEvent" forge/1.16.5 forge/1.17.1
```
Expected: no output.

- [ ] **Step 4: Build all 18 Forge targets**

Run per version: `./gradlew :forge:build -Ptarget_mc_version=<v>` for
`1.16.5, 1.17.1, 1.18.2, 1.19.2, 1.20.1, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1.1, 26.1.2, 26.2`
Expected: BUILD SUCCESSFUL ×18.

- [ ] **Step 5: Runtime check on one annotation-style and one BUS-style version**

macOS note: FG7 `runClient` needs `-XstartOnFirstThread`; if it is not configured for the module, test the built JAR in Prism instead.
```bash
./gradlew :forge:runClient -Ptarget_mc_version=1.21.8
./gradlew :forge:runClient -Ptarget_mc_version=1.21.11
```
Expected: `/justcoordinates share` and `/jc share` both send the coordinates line.

- [ ] **Step 6: Ask the user, then commit**

```bash
git add forge/*/src/main/java/com/justcoordinates/forge/JustCoordinatesForge.java
git commit -m "feat: add /justcoordinates share client command on Forge 1.18.2+"
```

---

### Task 7: NeoForge client command

`RegisterClientCommandsEvent` is a NeoForge game-bus event, so the handler goes in the class annotated `@EventBusSubscriber(modid = ..., value = Dist.CLIENT)` **without** `bus = EventBusSubscriber.Bus.MOD`.

**Files (11):**
- Modify: `neoforge/{base,1.21.6,1.21.7,1.21.8,1.21.9,1.21.10,1.21.11,26.1,26.1.1,26.1.2,26.2}/src/main/java/com/justcoordinates/neoforge/JustCoordinatesNeoForge.java`

Target class per module:

| Module | Class to add the handler to |
|--------|------------------------------|
| `neoforge/base` (covers 1.21.1–1.21.5) | `ClientTickHandler` (game bus) |
| `neoforge/1.21.6` … `neoforge/26.2` | `ClientEvents` (game bus — the class that already holds `onClientTick`) |

**Interfaces:**
- Consumes: `CoordinatesShare.share()` from Task 2

- [ ] **Step 1: Add imports (all 11 files)**

```java
import com.justcoordinates.CoordinatesShare;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
```

- [ ] **Step 2: Add the handler to the game-bus class (all 11 files)**

```java
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            LiteralCommandNode<CommandSourceStack> shareRoot = event.getDispatcher().register(
                    Commands.literal("justcoordinates")
                            .then(Commands.literal("share")
                                    .executes(context -> {
                                        CoordinatesShare.share();
                                        return 1;
                                    })));
            // Best-effort short alias: if another client-side mod already owns "/jc", only the
            // alias is lost, never "/justcoordinates share".
            event.getDispatcher().register(Commands.literal("jc").redirect(shareRoot));
        }
```

- [ ] **Step 3: Build all 14 NeoForge targets**

Run per version: `./gradlew :neoforge:build -Ptarget_mc_version=<v>` for
`1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2` (`neoforge/base` supplies the sources for the first four)
Expected: BUILD SUCCESSFUL for each.

- [ ] **Step 4: Runtime check**

```bash
./gradlew :neoforge:runClient -Ptarget_mc_version=1.21.11
```
Expected: `/justcoordinates share` and `/jc share` both send the coordinates line.

- [ ] **Step 5: Ask the user, then commit**

```bash
git add neoforge/*/src/main/java/com/justcoordinates/neoforge/JustCoordinatesNeoForge.java
git commit -m "feat: add /justcoordinates share client command on NeoForge"
```

---

### Task 8: Full verification and documentation

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `README.md`
- Modify: `docs/modrinth_description.md`
- Modify: `docs/curseforge_description.md`

- [ ] **Step 1: Build every platform × version**

Run `./gradlew clean build -Ptarget_mc_version=<v> -x test` for all 19 versions (the `minecraft-mod:for-each-version` skill automates this).
Expected: BUILD SUCCESSFUL for every enabled platform in each `props/{version}.properties`.

- [ ] **Step 2: Confirm the shared class is in the shipped jars**

```bash
unzip -l fabric/1.21.1/build/libs/*.jar | grep CoordinatesMessage
unzip -l forge/1.21.1/build/libs/*.jar | grep CoordinatesMessage
```
Expected: the class is listed in both. If it is missing, the module's `build.gradle` lacks the `common/shared` `srcDir` wiring.

- [ ] **Step 3: Manual verification matrix**

Check each of these and record the result:
- Fabric 26.2, Fabric 1.16.5, NeoForge 1.21.11, Forge 1.21.11: `/justcoordinates share` sends the line
- The same versions: `/jc share` sends the same line
- Tab completion offers `justcoordinates` after typing `/ju`
- The bound keybinding sends the same line, including while the HUD is hidden with `J`
- Negative coordinates render as `X: -100, Y: 64, Z: -200`
- Singleplayer: the line appears in own chat. Vanilla server without OP: the line is broadcast
- Forge 1.16.5 and 1.17.1 (via Prism, using the built JARs): the keybinding works, `/justcoordinates` is not offered, and no startup error appears
- Not in a world (title screen with the key bound): pressing the key does nothing, no crash

- [ ] **Step 4: Update `CHANGELOG.md`**

Add under `## [Unreleased]`:

```markdown
### Added

- Share your current coordinates in chat with the `/justcoordinates share` command (short form: `/jc share`) or an assignable "Share Coordinates" keybinding (unbound by default). The command runs client-side, so it needs no server permission
- Note: on Forge 1.16.5 and 1.17.1 the keybinding is the only way to share — those Forge versions have no client-command API
```

- [ ] **Step 5: Update `README.md`**

Add to the feature list near the existing "Toggle visibility with a keybind" bullet:

```markdown
- Share your coordinates in chat with `/justcoordinates share` (or `/jc share`), or an assignable keybind
```

And add a section after the existing "Toggle Keybind" section:

```markdown
## Sharing Coordinates

Run `/justcoordinates share` (short form: `/jc share`) to post your current coordinates to chat as
`X: 100, Y: 64, Z: -200`. The command is handled by the mod on your own client, so it works on any
server without permissions. You can also assign a key to "Share Coordinates" in
Options > Controls > Key Binds under the "Just Coordinates" category (unbound by default).

On Forge 1.16.5 and 1.17.1 only the keybinding is available — those Forge versions have no
client-command API.
```

- [ ] **Step 6: Update the store descriptions**

Add this bullet to both `docs/modrinth_description.md` and `docs/curseforge_description.md`, next to the existing "Toggle Keybind" bullet:

```markdown
- **Share to Chat**: Run `/justcoordinates share` (or `/jc share`), or assign a "Share Coordinates" key — no server permission needed
```

- [ ] **Step 7: Ask the user, then commit**

```bash
git add CHANGELOG.md README.md docs/modrinth_description.md docs/curseforge_description.md
git commit -m "docs: document coordinates sharing feature"
```

---

## Self-Review Notes

**Spec coverage**

| Spec item | Task |
|-----------|------|
| Locale-independent message format | Task 1 |
| Coordinates match the HUD (`Mth.floor`) | Task 2 |
| Chat send per version (3 variants) | Task 2 |
| Keybinding, unbound by default | Task 3 (definition) + Task 4 (registration) |
| Translation keys en_us/ja_jp | Task 3 |
| Works while the HUD is hidden | Task 3 — `handleTick` is independent of `visible`; verified in Task 8 |
| `/justcoordinates share` + `/jc` alias, Fabric | Task 5 |
| Same, Forge 1.18.2+ | Task 6 |
| Same, NeoForge | Task 7 |
| Forge 1.16.5/1.17.1 keybinding-only | Task 6 Step 3 + Task 8 Step 3 |
| `mc.player == null` guard | Task 2 |
| No local confirmation message, no rate limiting | Not implemented anywhere — intentional |
| Build + manual verification | Task 8 |

**Type consistency**

- `CoordinatesMessage.format(int, int, int): String` — defined Task 1, used only in Task 2
- `CoordinatesShare.share(): void` — defined Task 2, used in Tasks 3, 5, 6, 7
- `CoordinatesHudRenderer.getShareKey(): KeyMapping` — defined Task 3, used in Task 4
- `SHARE_KEY` is private; all external access goes through `getShareKey()`, matching the existing two keys

**Known non-blocking risks**

- `/jc` may be shadowed by another client-side mod; the canonical command still works (spec-accepted)
- Forge 26.1 has no module (`enabled_platforms=fabric,neoforge`), so Forge task counts are 18, not 19

## Amendment (2026-07-25, pre-merge review)

The `/jc` alias planned above was removed before merge. A client-side command claims the whole
literal it registers: once `/jc` matches, the client command layer handles the input and never
forwards it to the server. On a server whose plugin owns `/jc`, every `/jc ...` command would die
client-side with a parse error, with no way to disable it. `/justcoordinates share` remains the
only command; there is no alias.
