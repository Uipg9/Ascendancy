package com.uipg9.ascendancy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Immutable data record for Ascendancy player stats.
 * Used with Fabric Data Attachment API for automatic persistence.
 * 
 * INFINITE PROGRESSION: No max upgrade level - costs scale exponentially!
 */
public record AscendancyData(
    int soulXP,
    int prestigePoints,
    int ascensionCount,
    int lastKnownXP,
    boolean notified,
    int totalPrestigeEarned,
    // Original upgrades
    int healthLevel,
    int speedLevel,
    int reachLevel,
    int miningLevel,
    // New upgrade categories
    int luckLevel,
    int damageLevel,
    int defenseLevel,
    int experienceLevel,
    // v2.1 upgrades
    int keeperLevel,  // Items to keep on ascension
    int wisdomLevel   // Soul XP multiplier
) {
    
    // Default values for new players
    public static final AscendancyData DEFAULT = new AscendancyData(
        0, 0, 0, 0, false, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0  // v2.1 defaults
    );
    
    /**
     * Codec for serialization/deserialization.
     * This is used by Fabric API to persist the data.
     * NOTE: RecordCodecBuilder has 16-field limit. We have 16 now - at the limit!
     */
    public static final Codec<AscendancyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("soul_xp").forGetter(AscendancyData::soulXP),
        Codec.INT.fieldOf("prestige_points").forGetter(AscendancyData::prestigePoints),
        Codec.INT.fieldOf("ascension_count").forGetter(AscendancyData::ascensionCount),
        Codec.INT.fieldOf("last_known_xp").forGetter(AscendancyData::lastKnownXP),
        Codec.BOOL.fieldOf("notified").forGetter(AscendancyData::notified),
        Codec.INT.optionalFieldOf("total_prestige_earned", 0).forGetter(AscendancyData::totalPrestigeEarned),
        Codec.INT.fieldOf("health_level").forGetter(AscendancyData::healthLevel),
        Codec.INT.fieldOf("speed_level").forGetter(AscendancyData::speedLevel),
        Codec.INT.fieldOf("reach_level").forGetter(AscendancyData::reachLevel),
        Codec.INT.fieldOf("mining_level").forGetter(AscendancyData::miningLevel),
        Codec.INT.optionalFieldOf("luck_level", 0).forGetter(AscendancyData::luckLevel),
        Codec.INT.optionalFieldOf("damage_level", 0).forGetter(AscendancyData::damageLevel),
        Codec.INT.optionalFieldOf("defense_level", 0).forGetter(AscendancyData::defenseLevel),
        Codec.INT.optionalFieldOf("experience_level", 0).forGetter(AscendancyData::experienceLevel),
        Codec.INT.optionalFieldOf("keeper_level", 0).forGetter(AscendancyData::keeperLevel),
        Codec.INT.optionalFieldOf("wisdom_level", 0).forGetter(AscendancyData::wisdomLevel)
    ).apply(instance, AscendancyData::new));
    
    // ==================== BUILDER METHODS ====================
    
    public AscendancyData withSoulXP(int value) {
        return new AscendancyData(value, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withPrestigePoints(int value) {
        return new AscendancyData(soulXP, value, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withAscensionCount(int value) {
        return new AscendancyData(soulXP, prestigePoints, value, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withLastKnownXP(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, value, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withNotified(boolean value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, value, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withTotalPrestigeEarned(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, value,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    // Original upgrades
    public AscendancyData withHealthLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            value, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withSpeedLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, value, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withReachLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, value, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withMiningLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, value, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    // New upgrades
    public AscendancyData withLuckLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, value, damageLevel, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withDamageLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, value, defenseLevel, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withDefenseLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, value, experienceLevel, keeperLevel, wisdomLevel);
    }
    
    public AscendancyData withExperienceLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, value, keeperLevel, wisdomLevel);
    }
    
    // v2.1 upgrades
    public AscendancyData withKeeperLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, value, wisdomLevel);
    }
    
    public AscendancyData withWisdomLevel(int value) {
        return new AscendancyData(soulXP, prestigePoints, ascensionCount, lastKnownXP, notified, totalPrestigeEarned,
            healthLevel, speedLevel, reachLevel, miningLevel, luckLevel, damageLevel, defenseLevel, experienceLevel, keeperLevel, value);
    }
    
    /**
     * Reset data for a new ascension cycle.
     * Keeps all upgrades, resets soul XP, increments count
     */
    public AscendancyData resetForAscension(int newLastKnownXP, int prestigeReward) {
        return new AscendancyData(
            0, // Soul XP resets
            prestigePoints + prestigeReward, // Add reward
            ascensionCount + 1, // Increment count
            newLastKnownXP,
            false, // Reset notification
            totalPrestigeEarned + prestigeReward, // Track total earned
            healthLevel, speedLevel, reachLevel, miningLevel,
            luckLevel, damageLevel, defenseLevel, experienceLevel,
            keeperLevel, wisdomLevel // v2.1 upgrades persist
        );
    }
}
