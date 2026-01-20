package com.uipg9.ascendancy;

import com.uipg9.ascendancy.data.AscendancyAttachments;
import com.uipg9.ascendancy.data.PlayerDataManager;
import com.uipg9.ascendancy.logic.AttributeHandler;
import com.uipg9.ascendancy.network.AscendancyNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ascendancy Mod - A Vanilla+ RPG Prestige System
 * Version 2.2 - The Soul Harvester Update
 * 
 * Core loop: Play → Gather Soul XP → Ascend → Reset → Get Permanent Upgrades
 * 
 * SOUL XP SOURCES (Independent from vanilla XP!):
 * - Killing mobs (monsters, animals, bosses)
 * - Mining valuable ores
 * - Smelting items (via networking events)
 * - Crafting (future)
 * 
 * INFINITE PROGRESSION:
 * - No max upgrade level - just increasing costs
 * - Prestige rewards scale with ascension count
 * - Soul XP requirements scale with ascension count
 */
public class AscendancyMod implements ModInitializer {
    public static final String MOD_ID = "ascendancy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // ==================== PROGRESSION CONSTANTS ====================
    
    public static final int BASE_SOUL_XP = 100;
    public static final float SOUL_XP_SCALING = 0.5f;
    public static final int MAX_SOUL_XP_CAP = 10000;
    public static final float TUTORIAL_XP_MULTIPLIER = 10.0f;
    public static final int BASE_PRESTIGE_REWARD = 5;
    public static final int PRESTIGE_BONUS_PER_TIER = 1;
    public static final int BASE_UPGRADE_COST = 1;
    
    // Soul XP from kills
    public static final int SOUL_XP_PER_MONSTER = 5;
    public static final int SOUL_XP_PER_ANIMAL = 1;
    public static final int SOUL_XP_PER_BOSS = 50;
    
    // Soul XP from mining ores
    public static final int SOUL_XP_COAL = 1;
    public static final int SOUL_XP_IRON = 2;
    public static final int SOUL_XP_COPPER = 1;
    public static final int SOUL_XP_GOLD = 3;
    public static final int SOUL_XP_REDSTONE = 2;
    public static final int SOUL_XP_LAPIS = 2;
    public static final int SOUL_XP_DIAMOND = 8;
    public static final int SOUL_XP_EMERALD = 10;
    public static final int SOUL_XP_ANCIENT_DEBRIS = 15;
    public static final int SOUL_XP_QUARTZ = 2;
    
    // Soul XP from smelting
    public static final int SOUL_XP_SMELT_ORE = 2;
    public static final int SOUL_XP_SMELT_FOOD = 1;
    public static final int SOUL_XP_SMELT_OTHER = 1;
    
    // ==================== HELPER METHODS ====================
    
    public static int getMaxSoulXP(int ascensionCount) {
        int required = (int)(BASE_SOUL_XP * (1.0f + ascensionCount * SOUL_XP_SCALING));
        return Math.min(required, MAX_SOUL_XP_CAP);
    }
    
    public static int getPrestigeReward(int ascensionCount) {
        return BASE_PRESTIGE_REWARD + (ascensionCount * PRESTIGE_BONUS_PER_TIER);
    }
    
    public static int getUpgradeCost(int currentLevel) {
        if (currentLevel == 0) return 1;
        return (int) Math.ceil(BASE_UPGRADE_COST * Math.pow(1.3, currentLevel));
    }
    
    public static float getXPMultiplier(int ascensionCount) {
        if (ascensionCount == 0) return TUTORIAL_XP_MULTIPLIER;
        return 1.0f;
    }
    
    @Override
    public void onInitialize() {
        LOGGER.info("§6✦ Ascendancy v2.2 initializing... Your soul awaits. ✦");
        
        AscendancyAttachments.register();
        AscendancyNetworking.registerServerPackets();
        
        // Player join - welcome and sync
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            AttributeHandler.applyUpgrades(player);
            
            if (PlayerDataManager.getAscensionCount(player) == 0 && 
                PlayerDataManager.getSoulXP(player) == 0) {
                player.sendSystemMessage(Component.literal("§6§l✦ Welcome to Ascendancy! ✦"));
                player.sendSystemMessage(Component.literal("§7Your soul is untethered. §eGather Soul Energy§7 through:"));
                player.sendSystemMessage(Component.literal("§7  • §cSlaying monsters §7and creatures"));
                player.sendSystemMessage(Component.literal("§7  • §bMining precious ores"));
                player.sendSystemMessage(Component.literal("§7  • §6Smelting materials"));
                player.sendSystemMessage(Component.literal("§e→ Press §6[P]§e to open the Ascension menu!"));
            }
            
            AscendancyNetworking.syncToClient(player);
        });
        
        // Respawn - reapply upgrades
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            AttributeHandler.applyUpgrades(newPlayer);
            AscendancyNetworking.syncToClient(newPlayer);
        });
        
        // MOB KILLS - Soul XP from combat
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            Entity attacker = damageSource.getEntity();
            if (attacker instanceof ServerPlayer player) {
                onMobKill(player, entity);
            }
        });
        
        // ORE MINING - Soul XP from mining
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                onBlockMined(serverPlayer, state, pos);
            }
        });
        
        LOGGER.info("§a✦ Ascendancy initialized successfully! ✦");
    }
    
    /**
     * Handle mob kills - Award Soul XP for combat
     */
    private void onMobKill(ServerPlayer player, Entity killed) {
        int baseSoulXP;
        if (killed instanceof Monster) {
            String entityType = killed.getType().toShortString();
            if (entityType.contains("dragon") || entityType.contains("wither") || entityType.contains("elder_guardian")) {
                baseSoulXP = SOUL_XP_PER_BOSS;
            } else {
                baseSoulXP = SOUL_XP_PER_MONSTER;
            }
        } else if (killed instanceof Animal) {
            baseSoulXP = SOUL_XP_PER_ANIMAL;
        } else {
            baseSoulXP = 2;
        }
        
        awardSoulXP(player, baseSoulXP, "§c⚔");
    }
    
    /**
     * Handle block mining - Award Soul XP for ores
     */
    private void onBlockMined(ServerPlayer player, BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        
        int baseSoulXP = 0;
        
        // Check for ores
        if (path.contains("coal_ore")) baseSoulXP = SOUL_XP_COAL;
        else if (path.contains("iron_ore") || path.contains("deepslate_iron")) baseSoulXP = SOUL_XP_IRON;
        else if (path.contains("copper_ore")) baseSoulXP = SOUL_XP_COPPER;
        else if (path.contains("gold_ore") || path.contains("nether_gold")) baseSoulXP = SOUL_XP_GOLD;
        else if (path.contains("redstone_ore")) baseSoulXP = SOUL_XP_REDSTONE;
        else if (path.contains("lapis_ore")) baseSoulXP = SOUL_XP_LAPIS;
        else if (path.contains("diamond_ore")) baseSoulXP = SOUL_XP_DIAMOND;
        else if (path.contains("emerald_ore")) baseSoulXP = SOUL_XP_EMERALD;
        else if (path.contains("ancient_debris")) baseSoulXP = SOUL_XP_ANCIENT_DEBRIS;
        else if (path.contains("nether_quartz_ore") || path.contains("quartz_ore")) baseSoulXP = SOUL_XP_QUARTZ;
        
        if (baseSoulXP > 0) {
            awardSoulXP(player, baseSoulXP, "§b⛏");
        }
    }
    
    /**
     * Award Soul XP from smelting (called from networking)
     */
    public static void onItemSmelted(ServerPlayer player, String itemId) {
        int baseSoulXP;
        
        // Determine XP based on what was smelted
        if (itemId.contains("_ore") || itemId.contains("raw_")) {
            baseSoulXP = SOUL_XP_SMELT_ORE;
        } else if (itemId.contains("_beef") || itemId.contains("_pork") || 
                   itemId.contains("_chicken") || itemId.contains("_mutton") ||
                   itemId.contains("_rabbit") || itemId.contains("_cod") ||
                   itemId.contains("_salmon") || itemId.contains("potato") ||
                   itemId.contains("kelp")) {
            baseSoulXP = SOUL_XP_SMELT_FOOD;
        } else {
            baseSoulXP = SOUL_XP_SMELT_OTHER;
        }
        
        awardSoulXPStatic(player, baseSoulXP, "§6🔥");
    }
    
    /**
     * Central method to award Soul XP with all multipliers
     */
    private void awardSoulXP(ServerPlayer player, int baseSoulXP, String icon) {
        awardSoulXPStatic(player, baseSoulXP, icon);
    }
    
    /**
     * Static version for external callers
     */
    public static void awardSoulXPStatic(ServerPlayer player, int baseSoulXP, String icon) {
        int ascensionCount = PlayerDataManager.getAscensionCount(player);
        int maxSoulXP = getMaxSoulXP(ascensionCount);
        int currentSoulXP = PlayerDataManager.getSoulXP(player);
        
        if (currentSoulXP >= maxSoulXP) return;
        
        // Apply multipliers
        float multiplier = getXPMultiplier(ascensionCount);
        int wisdomLevel = PlayerDataManager.getWisdomLevel(player);
        float wisdomBonus = 1.0f + (wisdomLevel * 0.10f);
        
        int soulXPGain = (int)(baseSoulXP * multiplier * wisdomBonus);
        int newSoulXP = Math.min(currentSoulXP + soulXPGain, maxSoulXP);
        
        PlayerDataManager.setSoulXP(player, newSoulXP);
        AscendancyNetworking.syncToClient(player);
        
        // Notify when ready
        if (newSoulXP >= maxSoulXP && !PlayerDataManager.hasBeenNotified(player)) {
            player.displayClientMessage(Component.literal("§6§l✦ Your soul is ready! Press [P] to Ascend! ✦"), true);
            PlayerDataManager.setNotified(player, true);
        }
    }
}
