package com.uipg9.ascendancy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Immutable data record for Ascendancy player stats.
 * Used with Fabric Data Attachment API for automatic persistence.
 */
public record AscendancyData(
    int soulXP,
    int prestigePoints,
    int ascensionCount,
    int lastKnownXP,
    boolean notified,
    int healthLevel,
    int speedLevel,
    int reachLevel,
    int miningLevel
) {
    
    // Default values for new players
    public static final AscendancyData DEFAULT = new AscendancyData(0, 0, 0, 0, false, 0, 0, 0, 0);
    
    // Max upgrade level for all stats
    public static final int MAX_UPGRADE_LEVEL = 10;
    
    /**
     * Codec for serialization/deserialization.
     * This is used by Fabric API to persist the data.
     */
    public static final Codec<AscendancyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("soul_xp").forGetter(AscendancyData::soulXP),
        Codec.INT.fieldOf("prestige_points").forGetter(AscendancyData::prestigePoints),
        Codec.INT.fieldOf("ascension_count").forGetter(AscendancyData::ascensionCount),
        Codec.INT.fieldOf("last_known_xp").forGetter(AscendancyData::lastKnownXP),
        Codec.BOOL.fieldOf("notified").forGetter(AscendancyData::notified),
        Codec.INT.fieldOf("health_level").forGetter(AscendancyData::healthLevel),
        Codec.INT.fieldOf("speed_level").forGetter(AscendancyData::speedLevel),
        Codec.INT.fieldOf("reach_level").forGetter(AscendancyData::reachLevel),
        Codec.INT.fieldOf("mining_level").forGetter(AscendancyData::miningLevel)
    ).apply(instance, AscendancyData::new));
    
    // Builder methods for immutable updates
    
    public AscendancyData withSoulXP(int value) {
        return new AscendancyData(value, prestigePoints, ascensionCount, lastKnownXP, notified, healthLevel, speedLevel, reachLevel, miningLevel);
    }
    
    public AscendancyData withPrestigePoints(int value) {
        return new AscendancyData(soulXP, value, ascensionCount, lastKnownXP, notified, healthLevel, speedLevel, reachLevel, miningLevel);
    }
    
    public AscendancyData withAscensionCount(int value) {
        return new AscendancyData(soulXP, prestigePoints, value, lastKnownXP, notified, healthLevel, speedLevel, reachLevel, miningLevel);
    }
    
    public AscendancyData withLastKnownXP(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, value, notified, healthLevel, speedLevel, reachLevel, miningLevel);
    }
    
    public AscendancyData withNotified(boolean value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, value, healthLevel, speedLevel, reachLevel, miningLevel);
    }
    
    public AscendancyData withHealthLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, Math.min(value, MAX_UPGRADE_LEVEL), speedLevel, reachLevel, miningLevel);
    }
    
    public AscendancyData withSpeedLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, healthLevel, Math.min(value, MAX_UPGRADE_LEVEL), reachLevel, miningLevel);
    }
    
    public AscendancyData withReachLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, healthLevel, speedLevel, Math.min(value, MAX_UPGRADE_LEVEL), miningLevel);
    }
    
    public AscendancyData withMiningLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, healthLevel, speedLevel, reachLevel, Math.min(value, MAX_UPGRADE_LEVEL));
    }
    
    /**
     * Reset data for a new ascension cycle.
     */
    public AscendancyData resetForAscension(int newLastKnownXP) {
        return new AscendancyData(0, prestigePoints, ascensionCount + 1, newLastKnownXP, false, healthLevel, speedLevel, reachLevel, miningLevel);
    }
    
    /**
     * Calculate upgrade cost for next level.
     */
    public static int getUpgradeCost(int currentLevel) {
        return currentLevel + 1;
    }
}
