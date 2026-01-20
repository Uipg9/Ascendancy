package com.uipg9.ascendancy.data;

import com.uipg9.ascendancy.AscendancyMod;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manages persistent player data for Ascendancy.
 * Uses Fabric Data Attachment API for automatic persistence.
 */
public class PlayerDataManager {
    
    // ==================== DATA ACCESS ====================
    
    public static AscendancyData getData(ServerPlayer player) {
        return player.getAttachedOrCreate(AscendancyAttachments.ASCENDANCY_DATA);
    }
    
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
    
    // ==================== PRESTIGE POINTS ====================
    
    public static int getPrestigePoints(ServerPlayer player) {
        return getData(player).prestigePoints();
    }
    
    public static void setPrestigePoints(ServerPlayer player, int value) {
        setData(player, getData(player).withPrestigePoints(value));
    }
    
    public static void addPrestigePoints(ServerPlayer player, int amount) {
        AscendancyData data = getData(player);
        setData(player, data.withPrestigePoints(data.prestigePoints() + amount)
                           .withTotalPrestigeEarned(data.totalPrestigeEarned() + amount));
    }
    
    public static boolean spendPrestigePoints(ServerPlayer player, int amount) {
        AscendancyData data = getData(player);
        if (data.prestigePoints() >= amount) {
            setData(player, data.withPrestigePoints(data.prestigePoints() - amount));
            return true;
        }
        return false;
    }
    
    // ==================== TOTAL PRESTIGE EARNED ====================
    
    public static int getTotalPrestigeEarned(ServerPlayer player) {
        return getData(player).totalPrestigeEarned();
    }
    
    // ==================== ASCENSION COUNT ====================
    
    public static int getAscensionCount(ServerPlayer player) {
        return getData(player).ascensionCount();
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
    
    // ==================== ORIGINAL UPGRADES ====================
    
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
    
    // ==================== NEW UPGRADES ====================
    
    public static int getLuckLevel(ServerPlayer player) {
        return getData(player).luckLevel();
    }
    
    public static void setLuckLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withLuckLevel(level));
    }
    
    public static int getDamageLevel(ServerPlayer player) {
        return getData(player).damageLevel();
    }
    
    public static void setDamageLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withDamageLevel(level));
    }
    
    public static int getDefenseLevel(ServerPlayer player) {
        return getData(player).defenseLevel();
    }
    
    public static void setDefenseLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withDefenseLevel(level));
    }
    
    public static int getExperienceLevel(ServerPlayer player) {
        return getData(player).experienceLevel();
    }
    
    public static void setExperienceLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withExperienceLevel(level));
    }
    
    // ==================== V2.1 UPGRADES ====================
    
    public static int getKeeperLevel(ServerPlayer player) {
        return getData(player).keeperLevel();
    }
    
    public static void setKeeperLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withKeeperLevel(level));
    }
    
    public static int getWisdomLevel(ServerPlayer player) {
        return getData(player).wisdomLevel();
    }
    
    public static void setWisdomLevel(ServerPlayer player, int level) {
        setData(player, getData(player).withWisdomLevel(level));
    }
    
    // ==================== UPGRADE COST (INFINITE SCALING) ====================
    
    public static int getUpgradeCost(int currentLevel) {
        return AscendancyMod.getUpgradeCost(currentLevel);
    }
    
    // ==================== ASCENSION ====================
    
    public static void resetForAscension(ServerPlayer player) {
        AscendancyData data = getData(player);
        int prestigeReward = AscendancyMod.getPrestigeReward(data.ascensionCount());
        setData(player, data.resetForAscension(player.totalExperience, prestigeReward));
    }
}
