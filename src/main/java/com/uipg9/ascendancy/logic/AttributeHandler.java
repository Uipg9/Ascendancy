package com.uipg9.ascendancy.logic;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.data.PlayerDataManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Handles permanent attribute modifications from Ascendancy upgrades.
 * Uses Mojang Official Mappings for 1.21.11
 * 
 * Upgrades:
 * - Vitality: +2 Hearts (4.0 HP) per level
 * - Haste: +10% Mining Speed per level  
 * - Swiftness: +5% Movement Speed per level
 * - Titan's Reach: +1.0 Block Reach per level
 */
public class AttributeHandler {
    
    // Unique identifiers for each attribute modifier
    private static final Identifier HEALTH_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "vitality");
    private static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "swiftness");
    private static final Identifier REACH_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "titans_reach");
    private static final Identifier MINING_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "haste");
    
    // Upgrade values per level
    private static final double HEALTH_PER_LEVEL = 4.0;  // +2 hearts = +4 HP
    private static final double SPEED_PER_LEVEL = 0.05;  // +5% movement speed
    private static final double REACH_PER_LEVEL = 1.0;   // +1 block reach
    private static final double MINING_PER_LEVEL = 0.10; // +10% mining speed
    
    /**
     * Apply all upgrades from player data
     */
    public static void applyUpgrades(ServerPlayer player) {
        int healthLevel = PlayerDataManager.getHealthLevel(player);
        int speedLevel = PlayerDataManager.getSpeedLevel(player);
        int reachLevel = PlayerDataManager.getReachLevel(player);
        int miningLevel = PlayerDataManager.getMiningLevel(player);
        
        applyUpgrades(player, healthLevel, speedLevel, reachLevel, miningLevel);
    }
    
    /**
     * Apply upgrades with specific levels
     */
    public static void applyUpgrades(ServerPlayer player, int healthLvl, int speedLvl, int reachLvl, int miningLvl) {
        // 1. Max Health (Vitality)
        applyModifier(
            player.getAttribute(Attributes.MAX_HEALTH),
            HEALTH_ID,
            healthLvl * HEALTH_PER_LEVEL,
            AttributeModifier.Operation.ADD_VALUE
        );
        
        // 2. Movement Speed (Swiftness)
        applyModifier(
            player.getAttribute(Attributes.MOVEMENT_SPEED),
            SPEED_ID,
            speedLvl * SPEED_PER_LEVEL,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        
        // 3. Block Interaction Range (Titan's Reach)
        applyModifier(
            player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE),
            REACH_ID,
            reachLvl * REACH_PER_LEVEL,
            AttributeModifier.Operation.ADD_VALUE
        );
        
        // 4. Block Break Speed (Haste)
        applyModifier(
            player.getAttribute(Attributes.BLOCK_BREAK_SPEED),
            MINING_ID,
            miningLvl * MINING_PER_LEVEL,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        
        // Ensure player health is updated if max health changed
        if (healthLvl > 0 && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        
        AscendancyMod.LOGGER.debug("Applied upgrades to {}: H{} S{} R{} M{}", 
            player.getName().getString(), healthLvl, speedLvl, reachLvl, miningLvl);
    }
    
    /**
     * Apply a single modifier to an attribute
     */
    private static void applyModifier(AttributeInstance attribute, Identifier id, 
                                       double value, AttributeModifier.Operation operation) {
        if (attribute == null) {
            AscendancyMod.LOGGER.warn("Attribute instance is null for {}", id);
            return;
        }
        
        // Remove existing modifier if present
        attribute.removeModifier(id);
        
        // Add new modifier if value > 0
        if (value > 0) {
            attribute.addPermanentModifier(new AttributeModifier(
                id,
                value,
                operation
            ));
        }
    }
    
    /**
     * Purchase and apply a health upgrade
     */
    public static boolean purchaseVitality(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getHealthLevel(player);
        if (currentLevel >= PlayerDataManager.MAX_UPGRADE_LEVEL) return false;
        
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setHealthLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    /**
     * Purchase and apply a speed upgrade
     */
    public static boolean purchaseSwiftness(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getSpeedLevel(player);
        if (currentLevel >= PlayerDataManager.MAX_UPGRADE_LEVEL) return false;
        
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setSpeedLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    /**
     * Purchase and apply a reach upgrade
     */
    public static boolean purchaseReach(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getReachLevel(player);
        if (currentLevel >= PlayerDataManager.MAX_UPGRADE_LEVEL) return false;
        
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setReachLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    /**
     * Purchase and apply a mining speed upgrade
     */
    public static boolean purchaseHaste(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getMiningLevel(player);
        if (currentLevel >= PlayerDataManager.MAX_UPGRADE_LEVEL) return false;
        
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setMiningLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
}
