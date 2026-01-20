package com.uipg9.ascendancy.logic;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.data.PlayerDataManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Handles permanent attribute modifications from Ascendancy upgrades.
 * Uses Mojang Official Mappings for 1.21.11
 * 
 * INFINITE PROGRESSION - No caps! Costs scale exponentially.
 * 
 * Original Upgrades:
 * - Vitality: +2 Hearts (4.0 HP) per level
 * - Swiftness: +5% Movement Speed per level
 * - Titan's Reach: +0.5 Block Reach per level
 * - Haste: +10% Mining Speed per level
 * 
 * New Upgrades:
 * - Fortune's Favor: +5% Luck per level
 * - Might: +5% Attack Damage per level
 * - Resilience: +4% Armor per level (decreasing returns)
 * - Wisdom: +10% Soul XP gain per level (handled in tick logic)
 */
public class AttributeHandler {
    
    // Unique identifiers for each attribute modifier
    private static final Identifier HEALTH_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "vitality");
    private static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "swiftness");
    private static final Identifier REACH_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "titans_reach");
    private static final Identifier MINING_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "haste");
    private static final Identifier LUCK_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "fortune");
    private static final Identifier DAMAGE_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "might");
    private static final Identifier ARMOR_ID = Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "resilience");
    
    // Upgrade values per level (balanced for infinite progression)
    private static final double HEALTH_PER_LEVEL = 4.0;   // +2 hearts = +4 HP
    private static final double SPEED_PER_LEVEL = 0.03;   // +3% movement speed (reduced for balance)
    private static final double REACH_PER_LEVEL = 0.5;    // +0.5 block reach (reduced for balance)
    private static final double MINING_PER_LEVEL = 0.08;  // +8% mining speed
    private static final double LUCK_PER_LEVEL = 0.05;    // +5% luck
    private static final double DAMAGE_PER_LEVEL = 0.05;  // +5% attack damage
    private static final double ARMOR_PER_LEVEL = 1.0;    // +1 armor point
    
    /**
     * Apply all upgrades from player data
     */
    public static void applyUpgrades(ServerPlayer player) {
        // Original upgrades
        applyModifier(
            player.getAttribute(Attributes.MAX_HEALTH),
            HEALTH_ID,
            PlayerDataManager.getHealthLevel(player) * HEALTH_PER_LEVEL,
            AttributeModifier.Operation.ADD_VALUE
        );
        
        applyModifier(
            player.getAttribute(Attributes.MOVEMENT_SPEED),
            SPEED_ID,
            PlayerDataManager.getSpeedLevel(player) * SPEED_PER_LEVEL,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        
        applyModifier(
            player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE),
            REACH_ID,
            PlayerDataManager.getReachLevel(player) * REACH_PER_LEVEL,
            AttributeModifier.Operation.ADD_VALUE
        );
        
        applyModifier(
            player.getAttribute(Attributes.BLOCK_BREAK_SPEED),
            MINING_ID,
            PlayerDataManager.getMiningLevel(player) * MINING_PER_LEVEL,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        
        // New upgrades
        applyModifier(
            player.getAttribute(Attributes.LUCK),
            LUCK_ID,
            PlayerDataManager.getLuckLevel(player) * LUCK_PER_LEVEL,
            AttributeModifier.Operation.ADD_VALUE
        );
        
        applyModifier(
            player.getAttribute(Attributes.ATTACK_DAMAGE),
            DAMAGE_ID,
            PlayerDataManager.getDamageLevel(player) * DAMAGE_PER_LEVEL,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        
        applyModifier(
            player.getAttribute(Attributes.ARMOR),
            ARMOR_ID,
            PlayerDataManager.getDefenseLevel(player) * ARMOR_PER_LEVEL,
            AttributeModifier.Operation.ADD_VALUE
        );
        
        // Experience (Wisdom) is handled in tickSoulXP via PlayerDataManager.getExperienceLevel()
        
        // Ensure player health is updated if max health changed
        int healthLevel = PlayerDataManager.getHealthLevel(player);
        if (healthLevel > 0 && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        
        AscendancyMod.LOGGER.debug("Applied upgrades to {}", player.getName().getString());
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
        
        attribute.removeModifier(id);
        
        if (value > 0) {
            attribute.addPermanentModifier(new AttributeModifier(id, value, operation));
        }
    }
    
    // ==================== PURCHASE METHODS (NO CAPS!) ====================
    
    public static boolean purchaseVitality(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getHealthLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setHealthLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    public static boolean purchaseSwiftness(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getSpeedLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setSpeedLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    public static boolean purchaseReach(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getReachLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setReachLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    public static boolean purchaseHaste(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getMiningLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setMiningLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    public static boolean purchaseLuck(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getLuckLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setLuckLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    public static boolean purchaseDamage(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getDamageLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setDamageLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    public static boolean purchaseDefense(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getDefenseLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setDefenseLevel(player, currentLevel + 1);
            applyUpgrades(player);
            return true;
        }
        return false;
    }
    
    public static boolean purchaseExperience(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getExperienceLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setExperienceLevel(player, currentLevel + 1);
            // No attribute modification - handled in tickSoulXP
            return true;
        }
        return false;
    }
    
    /**
     * Keeper upgrade: +1 item count to keep on ascension per level
     * Base keeps 1 item, each level adds 1 more (e.g. level 5 = keep 6 items)
     */
    public static boolean purchaseKeeper(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getKeeperLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setKeeperLevel(player, currentLevel + 1);
            // No attribute modification - handled in AscensionManager
            return true;
        }
        return false;
    }
    
    /**
     * Wisdom upgrade: +10% Soul XP gain per level
     * Now handled in onMobKill event since we don't track vanilla XP anymore
     */
    public static boolean purchaseWisdom(ServerPlayer player) {
        int currentLevel = PlayerDataManager.getWisdomLevel(player);
        int cost = PlayerDataManager.getUpgradeCost(currentLevel);
        if (PlayerDataManager.spendPrestigePoints(player, cost)) {
            PlayerDataManager.setWisdomLevel(player, currentLevel + 1);
            // No attribute modification - handled in onMobKill
            return true;
        }
        return false;
    }
}
