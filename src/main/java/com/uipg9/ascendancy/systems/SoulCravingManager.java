package com.uipg9.ascendancy.systems;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.data.PlayerDataManager;
import com.uipg9.ascendancy.network.AscendancyNetworking;
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
 * Soul's Craving System - Random quest objectives for bonus prestige
 * v2.5 - Replayability Expansion
 * 
 * Each life, the player's soul "craves" something specific.
 * Completing the craving grants bonus prestige points on ascension.
 */
public class SoulCravingManager {
    
    private static final String CRAVING_FILE = "ascendancy_cravings.dat";
    
    // Track active cravings per player
    private static final Map<UUID, Craving> playerCravings = new HashMap<>();
    
    // Track progress per player
    private static final Map<UUID, Integer> cravingProgress = new HashMap<>();
    
    // Bonus prestige for completing craving
    private static final int CRAVING_BONUS_PRESTIGE = 3;
    
    /**
     * Types of cravings the soul can have
     */
    public enum CravingType {
        SLAY_MONSTERS("Bloodlust", "§c⚔ Slay %d monsters", "monsters slain"),
        MINE_ORES("Earth Hunger", "§b⛏ Mine %d ores", "ores mined"),
        HARVEST_CROPS("Nature's Call", "§a🌾 Harvest %d crops", "crops harvested"),
        TRAVEL_DISTANCE("Wanderlust", "§d🚶 Travel %d blocks", "blocks traveled"),
        KILL_UNDEAD("Purifier", "§e☀ Destroy %d undead", "undead destroyed"),
        MINE_DIAMONDS("Diamond Fever", "§b💎 Mine %d diamonds", "diamonds found"),
        EXPLORE_DEPTHS("Depths Caller", "§5⬇ Spend %d seconds below Y=0", "seconds in depths"),
        FISH_CATCH("Ocean's Bounty", "§3🎣 Catch %d fish", "fish caught");
        
        private final String name;
        private final String descriptionFormat;
        private final String progressLabel;
        
        CravingType(String name, String descriptionFormat, String progressLabel) {
            this.name = name;
            this.descriptionFormat = descriptionFormat;
            this.progressLabel = progressLabel;
        }
        
        public String getName() { return name; }
        public String getDescription(int target) { return String.format(descriptionFormat, target); }
        public String getProgressLabel() { return progressLabel; }
    }
    
    /**
     * A specific craving instance
     */
    public record Craving(
        CravingType type,
        int targetAmount,
        boolean completed
    ) {
        public String getDescription() {
            return type.getDescription(targetAmount);
        }
        
        public String getName() {
            return type.getName();
        }
    }
    
    /**
     * Generate a new random craving for a player's new life
     */
    public static void generateNewCraving(ServerPlayer player) {
        Random random = new Random();
        CravingType[] types = CravingType.values();
        CravingType type = types[random.nextInt(types.length)];
        
        // Scale target based on ascension count (harder each time)
        int ascensionCount = PlayerDataManager.getAscensionCount(player);
        int baseTarget = getBaseTarget(type);
        int scaledTarget = baseTarget + (ascensionCount * baseTarget / 4);
        
        Craving craving = new Craving(type, scaledTarget, false);
        playerCravings.put(player.getUUID(), craving);
        cravingProgress.put(player.getUUID(), 0);
        
        // Notify player
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§d§l✦ Your Soul Craves... ✦"));
        player.sendSystemMessage(Component.literal("§5" + craving.getName() + ": " + craving.getDescription()));
        player.sendSystemMessage(Component.literal("§7Complete this for §e+" + CRAVING_BONUS_PRESTIGE + " bonus Prestige§7 on Ascension!"));
        
        AscendancyMod.LOGGER.info("Generated craving for {}: {} (target: {})", 
            player.getName().getString(), type.name(), scaledTarget);
    }
    
    private static int getBaseTarget(CravingType type) {
        return switch (type) {
            case SLAY_MONSTERS -> 50;
            case MINE_ORES -> 30;
            case HARVEST_CROPS -> 40;
            case TRAVEL_DISTANCE -> 5000;
            case KILL_UNDEAD -> 25;
            case MINE_DIAMONDS -> 5;
            case EXPLORE_DEPTHS -> 300; // 5 minutes
            case FISH_CATCH -> 15;
        };
    }
    
    /**
     * Get a player's current craving
     */
    public static Craving getCraving(ServerPlayer player) {
        return playerCravings.get(player.getUUID());
    }
    
    /**
     * Get current progress toward craving
     */
    public static int getProgress(ServerPlayer player) {
        return cravingProgress.getOrDefault(player.getUUID(), 0);
    }
    
    /**
     * Check if craving is completed
     */
    public static boolean isCravingCompleted(ServerPlayer player) {
        Craving craving = getCraving(player);
        if (craving == null) return false;
        return craving.completed() || getProgress(player) >= craving.targetAmount();
    }
    
    /**
     * Add progress to the craving
     */
    public static void addProgress(ServerPlayer player, CravingType type, int amount) {
        Craving craving = getCraving(player);
        if (craving == null || craving.type() != type || craving.completed()) return;
        
        int current = cravingProgress.getOrDefault(player.getUUID(), 0);
        int newProgress = current + amount;
        cravingProgress.put(player.getUUID(), newProgress);
        
        // Check for completion
        if (newProgress >= craving.targetAmount() && current < craving.targetAmount()) {
            // Just completed!
            playerCravings.put(player.getUUID(), new Craving(craving.type(), craving.targetAmount(), true));
            
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("§d§l✦ Soul's Craving Satisfied! ✦"));
            player.sendSystemMessage(Component.literal("§a" + craving.getName() + " complete!"));
            player.sendSystemMessage(Component.literal("§e+" + CRAVING_BONUS_PRESTIGE + " bonus Prestige §7will be awarded on Ascension!"));
            
            AscendancyMod.LOGGER.info("Player {} completed craving: {}", 
                player.getName().getString(), type.name());
        }
    }
    
    /**
     * Get bonus prestige for completed craving (called during ascension)
     */
    public static int getBonusPrestige(ServerPlayer player) {
        if (isCravingCompleted(player)) {
            return CRAVING_BONUS_PRESTIGE;
        }
        return 0;
    }
    
    /**
     * Clear craving on ascension
     */
    public static void clearCraving(ServerPlayer player) {
        playerCravings.remove(player.getUUID());
        cravingProgress.remove(player.getUUID());
    }
    
    // ==================== PROGRESS TRACKING HOOKS ====================
    
    /**
     * Called when player kills a mob
     */
    public static void onMobKill(ServerPlayer player, boolean isUndead, boolean isMonster) {
        if (isUndead) {
            addProgress(player, CravingType.KILL_UNDEAD, 1);
        }
        if (isMonster) {
            addProgress(player, CravingType.SLAY_MONSTERS, 1);
        }
    }
    
    /**
     * Called when player mines an ore
     */
    public static void onOreMined(ServerPlayer player, boolean isDiamond) {
        addProgress(player, CravingType.MINE_ORES, 1);
        if (isDiamond) {
            addProgress(player, CravingType.MINE_DIAMONDS, 1);
        }
    }
    
    /**
     * Called when player harvests a crop
     */
    public static void onCropHarvested(ServerPlayer player) {
        addProgress(player, CravingType.HARVEST_CROPS, 1);
    }
    
    /**
     * Called periodically to track travel distance
     */
    public static void onTravel(ServerPlayer player, double distance) {
        addProgress(player, CravingType.TRAVEL_DISTANCE, (int) distance);
    }
    
    /**
     * Called periodically while player is below Y=0
     */
    public static void onDepthsTick(ServerPlayer player) {
        if (player.getY() < 0) {
            // Add 1 second every 20 ticks
            if (player.tickCount % 20 == 0) {
                addProgress(player, CravingType.EXPLORE_DEPTHS, 1);
            }
        }
    }
    
    /**
     * Called when player catches a fish
     */
    public static void onFishCaught(ServerPlayer player) {
        addProgress(player, CravingType.FISH_CATCH, 1);
    }
    
    /**
     * Get progress display string for UI
     */
    public static String getProgressDisplay(ServerPlayer player) {
        Craving craving = getCraving(player);
        if (craving == null) return "§7No craving active";
        
        int progress = getProgress(player);
        int target = craving.targetAmount();
        
        if (craving.completed()) {
            return "§a✓ " + craving.getName() + " §7(Complete!)";
        }
        
        float percent = (float) progress / target * 100;
        return String.format("§d%s: §f%d/%d §7(%.0f%%)", 
            craving.getName(), progress, target, percent);
    }
    
    // ==================== PERSISTENCE ====================
    
    /**
     * Load craving data for a player
     */
    public static void loadData(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        
        try {
            Path path = getCravingDataPath(level);
            if (Files.exists(path)) {
                CompoundTag root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                CompoundTag playerData = root.getCompoundOrEmpty(playerId.toString());
                
                if (playerData.contains("cravingType")) {
                    String typeName = playerData.getStringOr("cravingType", "SLAY_MONSTERS");
                    int targetAmount = playerData.getIntOr("targetAmount", 50);
                    boolean completed = playerData.getBooleanOr("completed", false);
                    int progress = playerData.getIntOr("progress", 0);
                    
                    try {
                        CravingType type = CravingType.valueOf(typeName);
                        playerCravings.put(playerId, new Craving(type, targetAmount, completed));
                        cravingProgress.put(playerId, progress);
                        AscendancyMod.LOGGER.info("Loaded craving for {}: {} ({}/{})", 
                            player.getName().getString(), typeName, progress, targetAmount);
                    } catch (IllegalArgumentException e) {
                        // Invalid craving type, ignore
                    }
                }
            }
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to load craving data", e);
        }
    }
    
    /**
     * Save craving data for a player
     */
    public static void saveData(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        
        try {
            Path path = getCravingDataPath(level);
            CompoundTag root;
            
            if (Files.exists(path)) {
                root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            } else {
                root = new CompoundTag();
            }
            
            CompoundTag playerData = new CompoundTag();
            Craving craving = playerCravings.get(playerId);
            
            if (craving != null) {
                playerData.putString("cravingType", craving.type().name());
                playerData.putInt("targetAmount", craving.targetAmount());
                playerData.putBoolean("completed", craving.completed());
                playerData.putInt("progress", cravingProgress.getOrDefault(playerId, 0));
            }
            
            root.put(playerId.toString(), playerData);
            NbtIo.writeCompressed(root, path);
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to save craving data", e);
        }
    }
    
    private static Path getCravingDataPath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(CRAVING_FILE);
    }
}
