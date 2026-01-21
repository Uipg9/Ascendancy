package com.uipg9.ascendancy.systems;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.data.PlayerDataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Ascension Achievements - Permanent unlockable buffs
 * v2.5 - Replayability Expansion
 * 
 * Complete achievements across ALL your lives to unlock permanent
 * passive bonuses. These persist forever, even across ascensions.
 */
public class AchievementManager {
    
    private static final String ACHIEVEMENT_FILE = "ascendancy_achievements.dat";
    
    // Cache of unlocked achievements per player
    private static final Map<UUID, Set<Achievement>> unlockedAchievements = new HashMap<>();
    
    // Progress tracking
    private static final Map<UUID, Map<Achievement, Integer>> achievementProgress = new HashMap<>();
    
    /**
     * Available achievements
     */
    public enum Achievement {
        // Combat Achievements
        SLAYER_I("Slayer I", "Kill 100 monsters total", 100, AchievementReward.DAMAGE_BOOST_1),
        SLAYER_II("Slayer II", "Kill 500 monsters total", 500, AchievementReward.DAMAGE_BOOST_2),
        SLAYER_III("Slayer III", "Kill 2000 monsters total", 2000, AchievementReward.DAMAGE_BOOST_3),
        
        DRAGON_HUNTER("Dragon Hunter", "Slay the Ender Dragon", 1, AchievementReward.DRAGON_RESISTANCE),
        WITHER_SLAYER("Wither Slayer", "Defeat the Wither", 1, AchievementReward.WITHER_RESISTANCE),
        
        // Exploration Achievements
        EXPLORER_I("Explorer I", "Travel 10,000 blocks total", 10000, AchievementReward.SPEED_BOOST_1),
        EXPLORER_II("Explorer II", "Travel 50,000 blocks total", 50000, AchievementReward.SPEED_BOOST_2),
        
        DIMENSION_HOPPER("Dimension Hopper", "Visit Nether and End in one life", 2, AchievementReward.PORTAL_SICKNESS_IMMUNITY),
        
        // Mining Achievements
        MINER_I("Miner I", "Mine 500 ores total", 500, AchievementReward.MINING_SPEED_1),
        MINER_II("Miner II", "Mine 2000 ores total", 2000, AchievementReward.MINING_SPEED_2),
        
        DIAMOND_COLLECTOR("Diamond Collector", "Mine 100 diamonds total", 100, AchievementReward.FORTUNE_TOUCH),
        
        // Ascension Achievements
        FIRST_ASCENSION("First Steps", "Complete your first ascension", 1, AchievementReward.SOUL_ATTUNEMENT),
        VETERAN("Veteran Soul", "Ascend 5 times", 5, AchievementReward.XP_BOOST_1),
        LEGEND("Legendary Soul", "Ascend 10 times", 10, AchievementReward.XP_BOOST_2),
        ETERNAL("Eternal Soul", "Ascend 25 times", 25, AchievementReward.ETERNAL_BLESSING),
        
        // Special Achievements
        ECHO_VANQUISHER("Echo Vanquisher", "Defeat 5 Echoes", 5, AchievementReward.ECHO_INSIGHT),
        CRAVING_MASTER("Craving Master", "Complete 10 Soul's Cravings", 10, AchievementReward.CRAVING_BONUS),
        HEIRLOOM_KEEPER("Heirloom Keeper", "Keep an item through 10 ascensions", 10, AchievementReward.KEEPER_BONUS);
        
        private final String name;
        private final String description;
        private final int target;
        private final AchievementReward reward;
        
        Achievement(String name, String description, int target, AchievementReward reward) {
            this.name = name;
            this.description = description;
            this.target = target;
            this.reward = reward;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getTarget() { return target; }
        public AchievementReward getReward() { return reward; }
    }
    
    /**
     * Achievement rewards (permanent passive effects)
     */
    public enum AchievementReward {
        DAMAGE_BOOST_1("+2% attack damage", "damage", 0.02f),
        DAMAGE_BOOST_2("+3% attack damage", "damage", 0.03f),
        DAMAGE_BOOST_3("+5% attack damage", "damage", 0.05f),
        
        SPEED_BOOST_1("+2% movement speed", "speed", 0.02f),
        SPEED_BOOST_2("+3% movement speed", "speed", 0.03f),
        
        MINING_SPEED_1("+5% mining speed", "mining", 0.05f),
        MINING_SPEED_2("+10% mining speed", "mining", 0.10f),
        
        DRAGON_RESISTANCE("-10% damage from dragons", "dragon_resist", 0.10f),
        WITHER_RESISTANCE("-10% wither damage", "wither_resist", 0.10f),
        PORTAL_SICKNESS_IMMUNITY("No nausea from portals", "portal_immunity", 1.0f),
        
        FORTUNE_TOUCH("+5% ore drop bonus", "fortune", 0.05f),
        
        SOUL_ATTUNEMENT("+5% Soul XP gain", "soul_xp", 0.05f),
        XP_BOOST_1("+10% Soul XP gain", "soul_xp", 0.10f),
        XP_BOOST_2("+15% Soul XP gain", "soul_xp", 0.15f),
        ETERNAL_BLESSING("+1 Prestige per ascension", "prestige", 1.0f),
        
        ECHO_INSIGHT("Echoes drop better loot", "echo_loot", 1.0f),
        CRAVING_BONUS("+1 bonus prestige from cravings", "craving", 1.0f),
        KEEPER_BONUS("Keep +1 item stack per ascension", "keeper", 1.0f);
        
        private final String description;
        private final String effectType;
        private final float value;
        
        AchievementReward(String description, String effectType, float value) {
            this.description = description;
            this.effectType = effectType;
            this.value = value;
        }
        
        public String getDescription() { return description; }
        public String getEffectType() { return effectType; }
        public float getValue() { return value; }
    }
    
    /**
     * Load achievements from file on player join
     */
    public static void loadAchievements(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        
        Set<Achievement> unlocked = new HashSet<>();
        Map<Achievement, Integer> progress = new HashMap<>();
        
        try {
            Path path = getAchievementDataPath(level);
            if (Files.exists(path)) {
                CompoundTag root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                CompoundTag playerData = root.getCompoundOrEmpty(playerId.toString());
                
                // Load unlocked achievements
                for (Achievement achievement : Achievement.values()) {
                    if (playerData.getBooleanOr("unlocked_" + achievement.name(), false)) {
                        unlocked.add(achievement);
                    }
                    progress.put(achievement, playerData.getIntOr("progress_" + achievement.name(), 0));
                }
            }
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to load achievements", e);
        }
        
        unlockedAchievements.put(playerId, unlocked);
        achievementProgress.put(playerId, progress);
        
        AscendancyMod.LOGGER.info("Loaded {} achievements for {}", unlocked.size(), player.getName().getString());
    }
    
    /**
     * Save achievements to file
     */
    public static void saveAchievements(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        
        try {
            Path path = getAchievementDataPath(level);
            CompoundTag root;
            
            if (Files.exists(path)) {
                root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            } else {
                root = new CompoundTag();
            }
            
            CompoundTag playerData = new CompoundTag();
            Set<Achievement> unlocked = unlockedAchievements.getOrDefault(playerId, new HashSet<>());
            Map<Achievement, Integer> progress = achievementProgress.getOrDefault(playerId, new HashMap<>());
            
            for (Achievement achievement : Achievement.values()) {
                playerData.putBoolean("unlocked_" + achievement.name(), unlocked.contains(achievement));
                playerData.putInt("progress_" + achievement.name(), progress.getOrDefault(achievement, 0));
            }
            
            root.put(playerId.toString(), playerData);
            NbtIo.writeCompressed(root, path);
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to save achievements", e);
        }
    }
    
    /**
     * Add progress toward an achievement
     */
    public static void addProgress(ServerPlayer player, Achievement achievement, int amount) {
        UUID playerId = player.getUUID();
        Set<Achievement> unlocked = unlockedAchievements.computeIfAbsent(playerId, k -> new HashSet<>());
        Map<Achievement, Integer> progress = achievementProgress.computeIfAbsent(playerId, k -> new HashMap<>());
        
        if (unlocked.contains(achievement)) return; // Already unlocked
        
        int current = progress.getOrDefault(achievement, 0);
        int newProgress = current + amount;
        progress.put(achievement, newProgress);
        
        // Check for unlock
        if (newProgress >= achievement.getTarget()) {
            unlockAchievement(player, achievement);
        }
    }
    
    /**
     * Set progress (for one-time achievements like boss kills)
     */
    public static void setProgress(ServerPlayer player, Achievement achievement, int value) {
        UUID playerId = player.getUUID();
        Map<Achievement, Integer> progress = achievementProgress.computeIfAbsent(playerId, k -> new HashMap<>());
        progress.put(achievement, value);
        
        if (value >= achievement.getTarget()) {
            unlockAchievement(player, achievement);
        }
    }
    
    /**
     * Unlock an achievement
     */
    private static void unlockAchievement(ServerPlayer player, Achievement achievement) {
        UUID playerId = player.getUUID();
        Set<Achievement> unlocked = unlockedAchievements.computeIfAbsent(playerId, k -> new HashSet<>());
        
        if (unlocked.contains(achievement)) return;
        
        unlocked.add(achievement);
        
        // Notify player
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l★ ACHIEVEMENT UNLOCKED ★"));
        player.sendSystemMessage(Component.literal("§e" + achievement.getName()));
        player.sendSystemMessage(Component.literal("§7" + achievement.getDescription()));
        player.sendSystemMessage(Component.literal("§aReward: " + achievement.getReward().getDescription()));
        
        // Save immediately
        saveAchievements(player);
        
        AscendancyMod.LOGGER.info("Player {} unlocked achievement: {}", 
            player.getName().getString(), achievement.name());
    }
    
    /**
     * Check if player has an achievement
     */
    public static boolean hasAchievement(ServerPlayer player, Achievement achievement) {
        return unlockedAchievements.getOrDefault(player.getUUID(), new HashSet<>()).contains(achievement);
    }
    
    /**
     * Get total bonus for a specific effect type
     */
    public static float getTotalBonus(ServerPlayer player, String effectType) {
        Set<Achievement> unlocked = unlockedAchievements.getOrDefault(player.getUUID(), new HashSet<>());
        float total = 0;
        
        for (Achievement achievement : unlocked) {
            if (achievement.getReward().getEffectType().equals(effectType)) {
                total += achievement.getReward().getValue();
            }
        }
        
        return total;
    }
    
    /**
     * Get count of unlocked achievements
     */
    public static int getUnlockedCount(ServerPlayer player) {
        return unlockedAchievements.getOrDefault(player.getUUID(), new HashSet<>()).size();
    }
    
    /**
     * Get total achievement count
     */
    public static int getTotalCount() {
        return Achievement.values().length;
    }
    
    /**
     * Get progress for specific achievement
     */
    public static int getProgress(ServerPlayer player, Achievement achievement) {
        return achievementProgress
            .getOrDefault(player.getUUID(), new HashMap<>())
            .getOrDefault(achievement, 0);
    }
    
    private static Path getAchievementDataPath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(ACHIEVEMENT_FILE);
    }
}
