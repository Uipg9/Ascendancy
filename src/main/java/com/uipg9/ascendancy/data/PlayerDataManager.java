package com.uipg9.ascendancy.data;

import net.minecraft.server.level.ServerPlayer;

/**
 * Manages persistent player data for Ascendancy.
 * Uses Fabric Data Attachment API for automatic persistence.
 */
public class PlayerDataManager {
    
    // ==================== DATA ACCESS ====================
    
    /**
     * Get the Ascendancy data for a player, creating default if not present.
     */
    public static AscendancyData getData(ServerPlayer player) {
        return player.getAttachedOrCreate(AscendancyAttachments.ASCENDANCY_DATA);
    }
    
    /**
     * Set the Ascendancy data for a player.
     */
    public static void setData(ServerPlayer player, AscendancyData data) {
        player.setAttached(AscendancyAttachments.ASCENDANCY_DATA, data);
    }
    
    // ==================== SOUL XP ====================
    
    public static int getSoulXP(ServerPlayer player) {
        return getData(player).soulXP();
    }
    
    public static void setSoulXP(ServerPlayer player, int value) {
        setData(player, getData(player).withSoulXP(value));
    }
    
    public static void addSoulXP(ServerPlayer player, int amount) {
        AscendancyData data = getData(player);
        setData(player, data.withSoulXP(data.soulXP() + amount));
    }
    
    // ==================== PRESTIGE POINTS ====================
    
    public static int getPrestigePoints(ServerPlayer player) {
        return getData(player).prestigePoints();
    }
    
    public static void setPrestigePoints(ServerPlayer player, int value) {
        setData(player, getData(player).withPrestigePoints(value));
    }
    
    public static void addPrestigePoints(ServerPlayer player, int amount) {
        AscendancyData data = getData(player);
        setData(player, data.withPrestigePoints(data.prestigePoints() + amount));
    }
    
    public static boolean spendPrestigePoints(ServerPlayer player, int amount) {
        AscendancyData data = getData(player);
        if (data.prestigePoints() >= amount) {
            setData(player, data.withPrestigePoints(data.prestigePoints() - amount));
            return true;
        }
        return false;
    }
    
    // ==================== ASCENSION COUNT ====================
    
    public static int getAscensionCount(ServerPlayer player) {
        return getData(player).ascensionCount();
    }
    
    public static void incrementAscensionCount(ServerPlayer player) {
        AscendancyData data = getData(player);
        setData(player, data.withAscensionCount(data.ascensionCount() + 1));
    }
    
    // ==================== LAST KNOWN XP ====================
    
    public static int getLastKnownXP(ServerPlayer player) {
        return getData(player).lastKnownXP();
    }
    
    public static void setLastKnownXP(ServerPlayer player, int value) {
        setData(player, getData(player).withLastKnownXP(value));
    }
    
    // ==================== NOTIFICATION FLAG ====================
    
    public static boolean hasBeenNotified(ServerPlayer player) {
        return getData(player).notified();
    }
    
    public static void setNotified(ServerPlayer player, boolean value) {
        setData(player, getData(player).withNotified(value));
    }
    
    // ==================== UPGRADE LEVELS ====================
    
    public static int getHealthLevel(ServerPlayer player) {
        return getData(player).healthLevel();
    }
    
    public static void setHealthLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withHealthLevel(level));
    }
    
    public static int getSpeedLevel(ServerPlayer player) {
        return getData(player).speedLevel();
    }
    
    public static void setSpeedLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withSpeedLevel(level));
    }
    
    public static int getReachLevel(ServerPlayer player) {
        return getData(player).reachLevel();
    }
    
    public static void setReachLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withReachLevel(level));
    }
    
    public static int getMiningLevel(ServerPlayer player) {
        return getData(player).miningLevel();
    }
    
    public static void setMiningLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withMiningLevel(level));
    }
    
    // ==================== MAX UPGRADE LEVEL ====================
    
    public static final int MAX_UPGRADE_LEVEL = AscendancyData.MAX_UPGRADE_LEVEL;
    
    /**
     * Calculate the cost to upgrade to the next level.
     */
    public static int getUpgradeCost(int currentLevel) {
        return AscendancyData.getUpgradeCost(currentLevel);
    }
    
    // ==================== ASCENSION ====================
    
    /**
     * Reset soul XP and notification for a new ascension cycle.
     */
    public static void resetForAscension(ServerPlayer player) {
        AscendancyData data = getData(player);
        setData(player, data.resetForAscension(player.totalExperience));
    }
}
