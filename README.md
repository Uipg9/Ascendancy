# Ascendancy

A Vanilla+ RPG Prestige mod for Minecraft 1.21.11 (Fabric)

## Concept: "The Long Journey"

A seamless prestige system that rewards players for simply playing the game. It allows players to "Ascend" (reset) their world progress in exchange for permanent, intrinsic power upgrades, without adding new items or blocks that clutter the game.

## The Cycle

1. **The Life**: Play normal Minecraft. Gather XP, explore, and survive to fill your "Soul Bar."
2. **The Ascension**: Trigger a ritual that wipes your inventory/progress but saves your "Soul."
3. **The Travel**: You are transported 1,000,000 blocks away to a fresh Village at dawn.
4. **The Evolution**: You wake up with Ascension Points to spend on permanent Attribute Modifiers.

## Features

### HUD & Progression
- Vertical progress bar in the bottom-right corner
- Fades out when empty, glows gold when full
- Tutorial mode: First ascension fills 5x faster

### The Prestige Event
- Keep ONE item (first hotbar slot) as an "Heirloom"
- Legacy chest placed at old location with golden beacon marker
- Complete wipe: Inventory, Ender Chest, Advancements, Experience
- Teleport 1,000,000 blocks to a village
- World time reset to dawn, weather cleared

### Permanent Upgrades
| Upgrade | Effect |
|---------|--------|
| **Vitality** | +2 Hearts per level |
| **Haste** | +10% Mining Speed per level |
| **Swiftness** | +5% Movement Speed per level |
| **Titan's Reach** | +1.0 Block Reach per level |

## Controls

- **P** - Open Ascension Menu (buy upgrades, trigger ascension)

## Installation

1. Install Fabric Loader 0.16.9+ for Minecraft 1.21.11
2. Install Fabric API
3. Drop `ascendancy-x.x.x.jar` into your mods folder

## Building from Source

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.16.9+
- Fabric API 0.104.0+
- Java 21

## License

MIT License - See LICENSE file
