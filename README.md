# ✦ Ascendancy ✦

> *A Vanilla+ RPG Prestige mod for Minecraft 1.21.11 (Fabric)*

![Version](https://img.shields.io/badge/version-2.4.2-gold)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![Fabric](https://img.shields.io/badge/Fabric-0.18.4+-blue)

## 🌟 Concept: "The Eternal Journey"

Ascendancy is a seamless prestige system that rewards you for simply playing the game. **Ascend** to reset your world progress in exchange for permanent power upgrades - no new items or blocks, just pure vanilla+ progression.

> *"Every ending is a new beginning. Every death, a rebirth stronger than before."*

---

## ♻️ The Cycle

1. **⚔️ The Life** - Play Minecraft. Kill mobs, mine ores, farm crops, explore! Your actions fill the **Soul Bar**.
2. **✨ The Ascension** - When ready, choose ONE precious item to keep and trigger the ritual.
3. **🌌 The Rebirth** - Descend from the heavens into a brand new world, far from your origins.
4. **💪 The Evolution** - Spend **Prestige Points** on permanent upgrades that persist forever.

---

## 🎮 Features

### Soul XP System
Earn **Soul XP** (independent from vanilla XP) through:
| Activity | Soul XP |
|----------|---------|
| 🗡️ Kill Monsters | 5 |
| 🐄 Kill Animals | 1 |
| 🐉 Kill Bosses | 50 |
| 💎 Mine Diamond Ore | 8 |
| 🟢 Mine Emerald Ore | 10 |
| 🟡 Mine Gold Ore | 3 |
| ⚫ Mine Ancient Debris | 15 |
| 🔥 Smelt Ores | 2 |
| 🍖 Smelt Food | 1 |
| 🌾 Harvest Crops | 1 |
| 👟 Walk 100 blocks | 1 |

### The HUD
- **Vertical soul bar** on the left-center of screen
- Gold decorative frame with corner accents
- Pulsing glow effect when ready to ascend
- Fills from bottom to top like a mystical container

### The Mysterious Awakening
When you ascend:
- ⚠️ **EVERYTHING IS WIPED** (inventory, ender chest, advancements)
- 🎒 You keep **ONE chosen item** (more with Keeper upgrade!)
- 📦 Old items preserved in a **Legacy Chest** at your old location
- 🌅 World time resets to **dawn**
- ☀️ Weather becomes **clear**
- 👁️ You awaken in **darkness** that slowly fades
- 🌄 **Morning** reveals your new surroundings
- ✨ **Night Vision** helps you explore

### Permanent Upgrades
| Upgrade | Effect | Description |
|---------|--------|-------------|
| 💜 **Vitality** | +2 Hearts | More health to survive |
| ⛏️ **Haste** | +10% Mining Speed | Break blocks faster |
| 👟 **Swiftness** | +5% Movement Speed | Run faster (more walking XP!) |
| 🦾 **Titan's Reach** | +1 Block Reach | Interact from farther |
| 🍀 **Fortune's Favor** | +1 Luck | Better loot |
| ⚔️ **Warrior's Might** | +1 Attack Damage | Hit harder |
| 🛡️ **Guardian's Blessing** | +1 Armor | Take less damage |
| 📚 **Scholar's Gift** | +10% XP Gain | Level up faster |
| 🎒 **Keeper** | +1 Items Kept | Keep more items when ascending |
| 🧠 **Wisdom** | +10% Soul XP | Earn Soul XP faster |

---

## 🎯 Controls

| Key | Action |
|-----|--------|
| **P** | Open Ascension Menu |

---

## 📦 Installation

1. Install **Fabric Loader 0.18.4+** for Minecraft 1.21.11
2. Install **Fabric API 0.141.1+**
3. Drop `ascendancy-2.4.2.jar` into your `mods` folder
4. Launch and enjoy!

---

## 🔧 Building from Source

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

---

## 📋 Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.4+
- Fabric API 0.141.1+
- Java 21

---

## 📜 Changelog

### v2.4.2 - Stability Patch
- 🏘️ **Village Spawn Fixed**: Now actually finds and spawns you near villages!
- 🖱️ **Item Selection Fixed**: Click detection completely rewritten using proper button widgets
- ⏳ **Animation Polish**: Buttons disabled during menu open animation (0.5s delay)
- 🛡️ **Safer Spawning**: Uses `MOTION_BLOCKING_NO_LEAVES` heightmap for proper ground detection
- 🔧 **1.21.11 Compatibility**: Fixed API compatibility issues with mouse input system

### v2.4.1 - Bugfix Patch
- 🐛 **Fixed Spawn Location**: No longer spawn in void/underground - proper surface detection
- 🐛 **Fixed Item Selection**: Buttons now work properly when choosing items to keep
- 🛡️ **Safe Spawning**: Added minimum Y=64, block collision checks, and headroom verification

### v2.4.0 - The Mystery Update
- 🌙 **Mysterious Awakening**: No more falling from sky - awaken from darkness!
- ✨ **XP Popups**: Floating "+XP" notifications when gaining Soul XP!
- 🌅 **Morning Welcome**: Blindness fades to reveal a new morning
- 📖 **Improved Immersion**: The transition feels magical and mysterious
- 🔧 **Code Cleanup**: Organized codebase and documentation

### v2.3.0 - The Journey Update
- 🌾 **Crop Harvesting**: Earn Soul XP from harvesting mature crops!
- 👟 **Walking XP**: Earn 1 Soul XP for every 100 blocks walked!
- 📖 **Fixed Guide**: Corrected all misleading info about inventory/ascension
- 🔧 **Documentation**: Guide now accurately describes the rebirth experience
- ✨ **More Polish**: Updated welcome messages with all XP sources

### v2.2.0 - The Soul Harvester Update
- ✨ **New Soul XP Sources**: Earn Soul XP from mining ores and smelting items!
- 🌌 **Mysterious Awakening**: Awaken in a new place with dramatic effects
- 👁️ **Immersive Transition**: Blindness fades to reveal your new world
- 📊 **New HUD Position**: Vertical bar on left-center (avoids mod conflicts)
- 🎨 **Visual Polish**: Decorative corners, pulsing glow, smooth animations
- 📖 **Updated Messages**: Welcome tutorial mentions all Soul XP sources

### v2.1.0 - The Keeper Update
- 🎒 **Keeper Upgrade**: Keep more items when ascending
- 🧠 **Wisdom Upgrade**: Earn Soul XP faster
- 📦 **Item Selection**: Choose which item to keep before ascending
- 🏘️ **Village Spawn**: Spawn at villages for a safer start

### v2.0.0 - The Rebirth
- 💜 Independent Soul XP system (separate from vanilla XP)
- 📊 New vertical HUD design
- ⚔️ New upgrade categories (8 total)
- 🎮 Polished UI with animations

---

## 🤝 Contributing

Contributions welcome! Please feel free to submit a Pull Request.

---

## 📄 License

MIT License - See LICENSE file

---

<div align="center">

**Made with ❤️ by [uipg9](https://github.com/uipg9)**

*"Ascend beyond your limits."*

</div>
