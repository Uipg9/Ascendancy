# Ascendancy v2.5.1 - Testing Checklist

## ✅ Pre-Test Setup
- [ ] Mod loads without crashes
- [ ] Console shows "Ascendancy v2.5.1 initializing..." and "Ascendancy initialized successfully!"
- [ ] No mixin errors in the log

---

## 🎮 Core Features Testing

### 1. First Join Experience
- [ ] On first join, see welcome message: "Your soul is untethered..."
- [ ] HUD shows Soul XP bar (should be at 0%)

### 2. Soul XP Accumulation
- [ ] Mine ores to gain Soul XP
- [ ] Kill mobs to gain Soul XP
- [ ] Harvest crops for Soul XP
- [ ] Walking awards Soul XP over distance
- [ ] In Ascension 0 (first playthrough), gains are 10x (tutorial mode)
- [ ] When Soul XP reaches max, notification appears

### 3. Ascension Screen (Press P)
- [ ] Opens the Ascension screen when pressing P key
- [ ] Shows current Soul XP and Prestige Points
- [ ] Shows current Ascension count
- [ ] Displays upgrade tabs: Combat, Utility, Special

### 4. Upgrade System
- [ ] Each upgrade shows current level and cost
- [ ] Can purchase upgrades with Prestige Points
- [ ] Upgrades visually update (level number changes)
- [ ] Sound plays on successful purchase
- [ ] Cannot purchase when insufficient points

### 5. Ascend Flow
- [ ] "Ascend" button only enabled when max Soul XP reached
- [ ] Item Selection Screen opens when clicking Ascend
- [ ] Can select ONE item to keep (with Keeper upgrade)
- [ ] Clicking Ascend starts cinematic loading screen

---

## 🆕 v2.5.0 - Replayability Expansion Testing

### 6. Legacy Chest & Echo Boss
- [ ] After ascending, legacy chest appears at old location
- [ ] Glowstone marks the chest location
- [ ] ALL previous inventory items are in the chest
- [ ] When approaching legacy chest, "The Echo" spawns (purple zombie)
- [ ] Echo wears your old armor from previous life
- [ ] Killing Echo gives bonus message and Soul XP
- [ ] Echo bonus is 25% of max Soul XP for that ascension

### 7. Constellation System
- [ ] After ascension cinematic, Constellation Selection Screen opens
- [ ] Can choose one of 4 constellations
- [ ] Skip option available (no constellation)
- [ ] **Star of the Deep**: Get Night Vision when below Y=0
- [ ] **Star of the Wind**: Take 80% less fall damage
- [ ] **Star of the Beast**: Mounted animals heal over time
- [ ] **Star of the Sea**: Never run out of air underwater

### 8. Heirloom System
- [ ] Item kept through ascension shows "Heirloom" lore
- [ ] Age counter tracks how many lives item survived
- [ ] After 5 ages, item gets gold name
- [ ] After 10 ages, item gets enchant glow
- [ ] After 25 ages, item becomes "Timeworn" (unbreakable)

---

## Original Testing (Still Valid)
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

## 🆕 v2.5.1 - Extended Replayability Features

### 9. Soul's Craving System
- [ ] After ascension, receive a random craving notification
- [ ] Craving types include: Bloodlust, Earth Hunger, Nature's Call, etc.
- [ ] Progress updates as you complete the craving's requirements
- [ ] On completion, see "Soul's Craving Satisfied!" message
- [ ] Completing craving grants +3 bonus Prestige on next ascension

### 10. Chronicle System
- [ ] Chronicle auto-records when entering Nether for first time
- [ ] Chronicle auto-records when entering End for first time
- [ ] Chronicle records first diamond mined
- [ ] Chronicle records boss kills (Dragon, Wither)
- [ ] Notification shows "[Chronicle: event description]"

### 11. Ascension Achievements
- [ ] Achievements persist forever (even across ascensions)
- [ ] Kill achievements: Slayer I/II/III (100/500/2000 kills)
- [ ] Mining achievements: Miner I/II (500/2000 ores)
- [ ] Travel achievements: Explorer I/II (10k/50k blocks)
- [ ] Boss achievements: Dragon Hunter, Wither Slayer
- [ ] Ascension achievements: First Steps, Veteran, Legend, Eternal
- [ ] Achievement unlock shows notification with reward description

### 12. Ancestral Bond System
- [ ] At 5+ Prestige, can bond with 1 tamed pet
- [ ] At 10+ Prestige, can bond with 2 pets
- [ ] At 15+ Prestige, can bond with 3 pets
- [ ] After ascension, bonded pets spawn near player
- [ ] Pets have custom names preserved
- [ ] Bond strength increases each ascension

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

### Achievement Bonuses
| Achievement | Reward |
|-------------|--------|
| Slayer I/II/III | +2%/3%/5% damage |
| Explorer I/II | +2%/3% speed |
| Miner I/II | +5%/10% mining speed |
| Dragon/Wither | -10% boss damage |
| First Steps | +5% Soul XP |
| Veteran/Legend | +10%/15% Soul XP |
| Eternal | +1 Prestige per ascension |

---

## 🎯 Test Complete When
- [ ] All core features work as expected
- [ ] All v2.5.0 replayability features work
- [ ] All v2.5.1 extended features work
- [ ] No crashes during normal gameplay
- [ ] Data persists across saves and deaths
- [ ] Upgrades apply correct bonuses
- [ ] GUI is functional and readable

---

*Testing Checklist - Ascendancy v2.5.1*
