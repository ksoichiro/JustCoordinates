# Design: HUD Position Configuration (GUI)

## Summary

Let users move the coordinates HUD to one of 6 preset screen positions, configured through an in-game GUI. No new runtime mod dependencies; the config screen is self-built with vanilla widgets. Applies to all supported versions (1.16.5–26.2) and all loaders (Fabric, NeoForge, Forge).

## Requirements

- HUD position selectable from 6 presets: top-left (default), top-center, top-right, bottom-left, bottom-center, bottom-right
- Configured via GUI, not by hand-editing a file (persistence still uses a JSON file under `config/`, transparently)
- Entry points: keybinding (all loaders) + loader-native config buttons (Forge/NeoForge Mods screen, Fabric ModMenu when installed)
- Zero added runtime dependencies; ModMenu integration is `compileOnly` only
- Default behavior (top-left) renders pixel-identical to the current implementation

## Architecture

### common/shared — version-independent logic (new, single copy)

Pure Java + Gson (bundled with MC, all versions). Must compile at the oldest supported language level (1.16.5 = Java 16).

**`HudPosition` enum** — `TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_CENTER`, `BOTTOM_RIGHT`
- Lowercase serialized name (`"top_left"`) for JSON
- Translation key helper: `justcoordinates.position.<name>`

**`HudConfig`** — holds current values, loads/saves JSON
- `position` (default `TOP_LEFT`) held statically; read by renderer and config screen
- `load(Path configDir)` — called once at client init with the loader-provided config dir. Reads `justcoordinates.json`; if the file is missing, keep defaults (the file is first created on save, not at startup)
- `save(Path configDir)` — Gson pretty-printed write; called when the config screen closes
- Error handling: corrupt JSON / unknown values → warn log + defaults (never block startup); save failures → error log only
- Includes `schema_version: 1` for future compatibility

```json
{
  "schema_version": 1,
  "position": "top_left"
}
```

### common/{version} — per-version copies (current project convention)

**`CoordinatesHudRenderer`** (modified ×19)
- Compute the draw origin from `HudConfig.position`:
  - right-aligned: `x = screenW - MARGIN - hudW`; bottom-aligned: `y = screenH - MARGIN - hudH`; centered: `x = (screenW - hudW) / 2`
- `BOTTOM_CENTER` special case: a 2px margin would overlap the hotbar/exp bar, so lift it by a fixed offset (~40px) above the bottom edge. All other positions use the uniform `MARGIN = 2`
- Screen size: `guiGraphics.guiWidth()/guiHeight()` on 1.20.1+; `mc.getWindow().getGuiScaledWidth()/getGuiScaledHeight()` on 1.16.5–1.19.2
- MARGIN / PADDING / colors unchanged

**`ConfigScreen`** (new ×19, ~60–80 lines each)
- Single `Screen` with: centered title, one position cycle button, Done button

```
+------------------------------+
|      Just Coordinates        |   <- title
|                              |
|   Position: < Top Center >   |   <- CycleButton (cycles 6 presets)
|                              |
|           [Done]             |
+------------------------------+
```

- Position button: vanilla `CycleButton` on 1.17+; 1.16.5 has no `CycleButton`, use a plain `Button` that swaps its label
- Immediate effect: pressing the button updates `HudConfig.position` right away (acts as a live preview when opened in-game)
- Save on `onClose()` (Done and ESC both) — no cancel concept; closing commits (single setting)
- Constructor takes a `parent` Screen; closing returns to it (needed for Mods-screen / ModMenu flows; keybinding flow passes `null` to return to the game)

Expected per-version diffs (absorbed by the existing copy-per-version convention):

| Range | Main differences |
|-------|------------------|
| 1.16.5 | No `CycleButton`; widget constructors |
| 1.17.1–1.19.2 | `CycleButton` available; `Button` via constructor |
| 1.20.1–1.21.x | `Button.builder` / `addRenderableWidget`; background render API shifts |
| 26.x | Class renames (`Identifier` etc.) |

### Loader integration

**Keybinding (universal entry, all loaders/versions)**
- New `KeyMapping` "open settings", default **unbound** (avoids conflicts; users assign in Controls)
- Registered through the same path as the existing toggle key; handled in `handleTick()` → `mc.setScreen(new ConfigScreen(null))`

**Config dir injection + load**
- Client init calls `HudConfig.load(configDir)` once
- Fabric: `FabricLoader.getInstance().getConfigDir()` (fabric/base plus the per-version entry sources that exist for 1.16.5–1.19.2 and 26.x)
- NeoForge / Forge: `FMLPaths.CONFIGDIR.get()` (exists on all supported Forge versions incl. 1.16.5)

**Loader-native config screen registration**

| Loader | Registration | Notes |
|--------|--------------|-------|
| Forge 1.16.5 | `ExtensionPoint.CONFIGGUIFACTORY` | |
| Forge 1.17.1/1.18.2 | `ConfigGuiHandler.ConfigGuiFactory` | |
| Forge 1.19.2–1.21.x | `ConfigScreenHandler.ConfigScreenFactory` | exact 26.x form verified during implementation |
| NeoForge 1.21.1+ | `IConfigScreenFactory` extension point | signature changed mid-1.21.x ((Minecraft, Screen) → (ModContainer, Screen)); absorbed per version |
| Fabric | ModMenu entrypoint `"modmenu"` + `ModMenuApi` impl | shows only when ModMenu installed; `compileOnly` dep, no runtime dependency |

ModMenu specifics:
- Add `modmenu_version` to `props/{version}.properties`; add TerraformersMC maven to the build (build-time only)
- 1.16.5 uses the old package `io.github.prospector.modmenu.api`; 1.17+ uses `com.terraformersmc.modmenu.api`
- ModMenu availability for 26.x verified during implementation. If any version lacks ModMenu (or a registration API), the keybinding entry still works everywhere — guaranteed fallback

### i18n (common/{version}/lang, en_us + ja_jp, ×19)

- `justcoordinates.config.title` — "Just Coordinates"
- `justcoordinates.config.position` — "Position" / "表示位置"
- `justcoordinates.position.top_left` … `bottom_right` (6 keys)
- `key.justcoordinates.open_config` — "Open Settings" / "設定画面を開く"

## Out of Scope

- Persisting the `visible` toggle state (stays runtime-only; easy to add to the config later)
- Pixel offsets / free drag-and-drop placement (the enum + JSON schema leave room to add offsets later)
- HUD content/format changes

## Error Handling

- Missing config file → defaults, no file created until first save
- Corrupt/unknown JSON values → warn log, defaults, startup proceeds
- Save I/O failure → error log, in-memory value stays active for the session

## Testing / Verification

1. Build all versions × all loaders (`buildAll` / for-each-version)
2. `runClient` spot checks: Fabric 26.2, 1.21.11, 1.16.5; NeoForge 1.21.11. Forge on macOS follows known constraints (FG7 needs `-XstartOnFirstThread`; 1.16.5/1.18.2 verified via Prism-launched JAR)
3. Manual checks:
   - GUI opens from all three entry points (keybinding, Forge/NeoForge Config button, ModMenu)
   - Cycling positions moves the HUD immediately (all 6 presets, incl. bottom-center hotbar lift)
   - Position survives restart (JSON written on close)
   - Corrupt JSON still boots with defaults
   - Default config renders identical to the previous release

## Risks

- Forge 26.x config-screen registration API shape unconfirmed
- NeoForge `IConfigScreenFactory` signature change point within 1.21.x
- ModMenu artifact availability/package names across 19 versions (esp. 26.x)
- All of the above degrade gracefully: worst case, that loader/version keeps only the keybinding entry
