# Punchy — First-Person Animation Mod

A Fabric mod for Minecraft **1.21.1** that overhauls first-person item animations.
Includes an in-game config screen, per-item blacklist with glob/regex support, and an automatic update checker.

---

## Features

- **Smooth first-person animations** for held items
- **Update checker** — polls the Modrinth API on launch and shows a one-time popup when a newer version is available
- **In-game config screen** via [ModMenu](https://modrinth.com/mod/modmenu)
  - Two-column item grid (enabled vs disabled)
  - Search, sort by name / mod / category
  - Group separators for easy navigation
  - Per-item click-to-toggle or bulk blacklist rules
- **Flexible blacklist** — supports exact IDs, whole mods, glob wildcards, and raw regex

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. *(Optional)* Install [ModMenu](https://modrinth.com/mod/modmenu) to access the config screen in-game
4. Drop the Punchy JAR into your `mods/` folder

---

## Configuration

Open the config via **ModMenu → Punchy → Config**, or edit `config/punchy.json` directly.

### Blacklist syntax

| Pattern | Matches |
|---|---|
| `cobblemon` | All items from the `cobblemon` mod |
| `minecraft:stick` | Exactly `minecraft:stick` |
| `cobblemon:*_pokemon` | All Cobblemon items whose path ends with `_pokemon` |
| `*_leggings` | Any item (any mod) whose path ends with `_leggings` |
| `cobblemon:.*` | Raw regex — all Cobblemon items |

Rules are stored as a list in `config/punchy.json` under `itemBlacklist`.

### Config screen buttons

| Button | Action |
|---|---|
| **Add Rule / Regex** | Opens the rule editor with syntax help |
| **Add All** | Adds one `modid:.*` wildcard per mod in the enabled column |
| **Remove All** | Clears the entire blacklist |
| **View Rules** | Opens the rules manager (scroll, remove per-rule, clear all) |

---

## Building from source

Requires **JDK 21**.

```bash
git clone https://github.com/Kewz4/mod-sample
cd mod-sample
./gradlew :fabric:build
# Output: fabric/build/libs/punchy-fabric-1.21.1-<version>.jar
```

---

## Project structure

```
fabric/src/main/java/com/punchy/
├── PunchyConfig.java          # Config load/save + blacklist pattern compiler
├── UpdateChecker.java         # Async Modrinth update check
├── ItemFetcher.java           # Reads all registered items from the game registry
├── client/
│   ├── PunchyConfigScreen.java     # Main config UI (two-column item grid)
│   ├── PunchyRulesScreen.java      # Blacklist rules viewer/manager
│   ├── PunchyRegexInputScreen.java # Rule input dialog with syntax guide
│   └── PunchyUpdateScreen.java     # Update notification popup
└── mixin/
    ├── MixinTitleScreen.java   # Injects update button + popup on the title screen
    └── MixinGameRenderer.java  # Cancels world blur when config screens are open
```

---

## Compatibility

- Minecraft **1.21.1**
- Fabric Loader ≥ 0.16.9
- Java 21

---

## License

MIT
