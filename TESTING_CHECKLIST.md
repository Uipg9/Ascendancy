# Ascendancy - Testing Checklist

## ✅ Pre-Test Setup
- [ ] Mod loads without crashes
- [ ] Console shows "Ascendancy initializing..." and "Ascendancy initialized successfully!"
- [ ] No mixin errors in the log

---

## 🎮 In-Game Testing

### 1. First Join Experience
- [ ] On first join, see welcome message: "Your soul is untethered..."
- [ ] HUD shows Soul XP bar (should be at 0%)

### 2. Soul XP Accumulation
- [ ] Mine ores or kill mobs to gain vanilla XP
- [ ] Soul XP bar fills up as you gain experience
- [ ] In Ascension 0 (first playthrough), bar fills 5x faster (tutorial mode)
- [ ] When Soul XP reaches 100%, see notification

### 3. Ascension Screen (Press P)
- [ ] Opens the Ascension screen when pressing P key
- [ ] Shows current Soul XP and Prestige Points
- [ ] Shows current Ascension count
- [ ] Displays upgrade grid with:
  - Health (❤️)
  - Speed (⚡)
  - Reach (🤚)
  - Mining (⛏️)

### 4. Upgrade System
- [ ] Each upgrade shows current level and cost
- [ ] Can purchase upgrades with Prestige Points
- [ ] Upgrades visually update (level number changes)
- [ ] Sound plays on successful purchase
- [ ] Cannot purchase when insufficient points

### 5. Ascend Button
- [ ] "Ascend" button shows cost in Soul XP
- [ ] Clicking Ascend when Soul XP bar is full:
  - Resets Soul XP to 0
  - Grants Prestige Points (based on Ascension tier)
  - Resets vanilla XP and level to 0
  - Increments Ascension count
- [ ] After Ascension, upgrade effects persist

### 6. Persistent Upgrades
- [ ] Health upgrade: Check max hearts increased
  - Level 1 = +1 heart, Level 10 = +10 hearts
- [ ] Speed upgrade: Movement speed increased
- [ ] Reach upgrade: Block reach distance increased
- [ ] Mining upgrade: Mining speed increased

### 7. Death & Respawn
- [ ] Die and respawn
- [ ] Check that:
  - Prestige Points persist
  - Ascension count persists
  - Upgrade levels persist
  - Soul XP resets to 0 (intentional)
  - Attribute bonuses re-apply after respawn

### 8. Save/Load Persistence
- [ ] Save and quit the world
- [ ] Re-enter the world
- [ ] All data should be preserved:
  - Prestige Points
  - Ascension count
  - Upgrade levels
  - Soul XP (current amount)

### 9. Commands (if implemented)
- [ ] `/ascendancy stats` - Shows player stats
- [ ] `/ascendancy reset` - Resets player data (OP only)
- [ ] `/ascendancy give <player> <points>` - Gives prestige points (OP only)

---

## 🐛 Bug Checks

### Common Issues to Watch For
- [ ] No crash when opening GUI
- [ ] No crash when purchasing upgrades
- [ ] No crash when ascending
- [ ] HUD renders correctly at different resolutions
- [ ] Keybind (P) doesn't conflict with other mods
- [ ] Data syncs properly in multiplayer

### Performance
- [ ] No significant FPS drops with mod enabled
- [ ] No memory leaks after extended play
- [ ] Server tick rate stays stable

---

## 📊 Expected Values

| Upgrade | Per Level | Max Level | Max Bonus |
|---------|-----------|-----------|-----------|
| Health | +1 heart | 10 | +10 hearts (30 total) |
| Speed | +5% | 10 | +50% speed |
| Reach | +0.5 blocks | 10 | +5 blocks reach |
| Mining | +10% | 10 | +100% mining speed |

### Upgrade Costs (Prestige Points)
| Level | Cost to Upgrade |
|-------|----------------|
| 0 → 1 | 1 |
| 1 → 2 | 2 |
| 2 → 3 | 3 |
| ... | ... |
| 9 → 10 | 10 |
| **Total** | **55** |

### Tutorial Mode
- First Ascension (Ascension 0): Soul XP fills 5x faster
- Goal: Let new players experience the full loop quickly

---

## 🎯 Test Complete When
- [ ] All core features work as expected
- [ ] No crashes during normal gameplay
- [ ] Data persists across saves and deaths
- [ ] Upgrades apply correct bonuses
- [ ] GUI is functional and readable

---

*Testing Checklist - Ascendancy v1.0.0*
