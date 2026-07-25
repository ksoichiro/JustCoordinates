# Design: Share Coordinates to Chat

## Summary

Let players broadcast their current coordinates as a normal chat message, either by a
client-side command (`/justcoordinates share`, aliased as `/jc share`) or by a keybinding.
Client-side commands are intercepted by the mod before reaching the server, so no server
permission (OP level) is required — sending the result is an ordinary chat message.
Applies to all supported versions (1.16.5–26.2) and all loaders, with one documented gap:
Forge 1.16.5 / 1.17.1 have no client-command API, so those two get the keybinding only.

## Requirements

- Send the player's current coordinates to chat so other players can read them
- Two entry points: a client command and a keybinding
- No server-side permission, no server-side mod, no new runtime dependency
- Message must be readable regardless of the recipient's language (locale-independent text)
- Coordinates must match what the HUD shows
- Works whether the HUD is currently visible or hidden

## Message Format

Locale-independent fixed string, built from the same floored integers the HUD renders:

```
X: 100, Y: 64, Z: -200
```

Rationale: the message is read by *other* players. A translated message ("座標: ...") would
be generated in the sender's language and could be unreadable to the recipients, so the
shared text deliberately does not use a translation key. Keybinding names and any local
feedback still use translation keys as usual.

## Entry Points

### Client command

- Canonical: `/justcoordinates share`
- Alias: `/jc` registered via Brigadier `redirect()` to the canonical root, so `/jc share` works
- Rationale for a root + subcommand shape: future additions (`/justcoordinates toggle`, etc.)
  extend the existing tree instead of adding unrelated top-level commands
- Collision behavior: Brigadier merges same-named literals in one dispatcher. If another
  client-side mod already registers `/jc`, the alias may lose (first registration wins), but
  `/justcoordinates share` always works. The alias is best-effort; the canonical name is the
  guarantee
- Tab completion works for client commands on all three loaders, so the long canonical name
  stays practical to type

### Keybinding

- New `KeyMapping` "Share Coordinates" in the existing `justcoordinates` category
- Default: **unbound** (`GLFW_KEY_UNKNOWN`), matching the existing "Open Settings" key.
  Sending chat is visible to other players, so an accidental default-key conflict would have
  a social side effect; users opt in by assigning a key in Controls

## Architecture

### common/shared — version-independent (new, single copy)

**`CoordinatesMessage`** — pure function, no MC API:

- `String format(int x, int y, int z)` → `"X: 100, Y: 64, Z: -200"`

Must compile at the oldest supported language level (1.16.5 = Java 16).

### common/{version} — per-version copies (×19)

**`CoordinatesShare`** (new) — isolates the chat-send API differences:

- `share()`: get `Minecraft.getInstance()`, return early if `mc.player == null`, floor
  `getX()/getY()/getZ()`, build via `CoordinatesMessage.format`, send to chat

| Range | Send call |
|-------|-----------|
| 1.16.5–1.18.2 | `mc.player.chat(msg)` |
| 1.19.2 | `LocalPlayer` / `ClientPacketListener` send API — exact signature confirmed per version during implementation |
| 1.20.1–26.2 | `mc.player.connection.sendChat(msg)` |

**`CoordinatesHudRenderer`** (modified ×19)

- Add `SHARE_KEY` (unbound) alongside the existing `TOGGLE_KEY` / `OPEN_CONFIG_KEY`
- Add `getShareKey()` accessor, following the existing pattern
- In `handleTick()`, `while (SHARE_KEY.consumeClick()) CoordinatesShare.share();`

Rendering code is untouched.

### Loader integration

Command registration is the only loader-specific part. Each loader registers the same tree:
root literal `justcoordinates` with a `share` child that calls `CoordinatesShare.share()`,
plus a `jc` literal redirecting to the root.

| Loader / version | Registration API |
|------------------|------------------|
| Fabric 1.16.5, 1.17.1, 1.18.2 | `ClientCommandManager.DISPATCHER` (fabric-command-api-v1) |
| Fabric 1.19.2–26.2 | `ClientCommandRegistrationCallback` (fabric-command-api-v2) |
| NeoForge (all supported) | `RegisterClientCommandsEvent` |
| Forge 1.18.2–26.2 | `RegisterClientCommandsEvent` |
| Forge 1.16.5, 1.17.1 | **not available** — keybinding only |

Verified by inspecting the cached artifacts: `RegisterClientCommandsEvent` is present in
forge-1.18.2-40.1.73 and absent from forge-1.17.1-37.1.1 and forge-1.16.5-36.2.42.
Fabric's client command API is present in both v1 (`net.fabricmc.fabric.api.client.command.v1`)
and v2, and the mod already depends on the full Fabric API.

Per the existing project convention, client-only registrations on Forge/NeoForge live in the
`*ForgeClient` / `*NeoForgeClient` classes, never in the `@Mod` entry class.

Keybinding registration reuses the existing per-loader path (`KeyBindingHelper` on Fabric,
`RegisterKeyMappingsEvent` on Forge/NeoForge) — one added `register` call next to the
existing two keys.

### i18n (common/{version}/lang, en_us + ja_jp, ×19)

- `key.justcoordinates.share` — "Share Coordinates" / "座標をチャットで共有"

The chat message itself is not translated (see Message Format).

## Error Handling

- `mc.player == null` (not in a world) → `share()` returns without sending
- Client commands are only dispatchable while in a world, so the command path cannot run
  outside one; the null guard covers the keybinding path and any future callers
- The command's execute always reports success (`return 1`); the mod prints no local
  confirmation because the sent chat line is itself the feedback
- No client-side rate limiting: repeated use is subject to the server's normal chat spam
  handling, same as manually typing the coordinates

## Out of Scope

- Configurable message format
- Including the dimension in the message
- Copy-to-clipboard, waypoint sharing, or clickable/teleport messages
- Client-side cooldown

## Testing / Verification

1. Build all versions × all loaders (`buildAll` / for-each-version)
2. `runClient` spot checks: Fabric 26.2, 1.21.11, 1.16.5; NeoForge 1.21.11.
   Forge on macOS follows the known constraints (FG7 needs `-XstartOnFirstThread`;
   1.16.5/1.18.2 verified via a Prism-launched JAR)
3. Manual checks:
   - `/justcoordinates share` sends the message; `/jc share` does the same
   - Tab completion suggests the command
   - The keybinding, once assigned in Controls, sends the same message
   - Coordinates in chat match the HUD exactly, including negative values
   - Works while the HUD is hidden (J)
   - Works in singleplayer (message appears in own chat) and on a vanilla server without OP
   - Forge 1.16.5 / 1.17.1: keybinding works, command absent, no startup error

## Risks

- 1.19.2 chat-send signature differs from neighbours (signed chat transition); confirmed
  per version at implementation time. Worst case that single version needs its own call shape,
  which the per-version `CoordinatesShare` copy already accommodates
- `/jc` alias may be shadowed by another client-side mod; canonical command unaffected

- Fabric client-command API split (v1 vs v2) boundary is assumed at 1.19.2; if 1.19.2 still
  needs v1, only that version's registration class changes
- Wide but mechanical touch surface (19 renderers, ~38 loader entry points, 38 lang files);
  the risk is omission rather than complexity, so the build-all pass is the guard

## Amendment (2026-07-25, pre-merge review)

The `/jc` alias described above was removed before merge. A client-side command claims the
whole literal it registers: once `/jc` matches, the client command layer handles the input and
never forwards it to the server. On a server whose plugin owns `/jc`, every `/jc ...` command
would die client-side with a parse error, with no way to disable it. `/justcoordinates share`
remains the only command; there is no alias.
