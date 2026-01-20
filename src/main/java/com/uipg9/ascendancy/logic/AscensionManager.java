package com.uipg9.ascendancy.logic;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.data.PlayerDataManager;
import com.uipg9.ascendancy.network.AscendancyNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Ascension process - A New World Awaits!
 * 
 * v2.2 - The Rebirth Update:
 * - Player descends from the heavens with protective effects
 * - Blindness fades to reveal a brand new world
 * - World resets to dawn with clear skies
 * - Everything feels like starting fresh
 * 
 * Key features:
 * - Player keeps ONE chosen item (amount based on Keeper upgrade)
 * - Sky spawn with Feather Falling + Blindness effects
 * - World reset: clear weather, dawn (time=0)
 * - Legacy chest preserves old items at departure site
 */
public class AscensionManager {
    
    // Minimum distance from origin to prevent close spawns
    private static final int MIN_DISTANCE_FROM_ORIGIN = 500_000;
    
    // How far to travel per ascension
    private static final int TELEPORT_DISTANCE = 1_000_000;
    
    // Random Z variance
    private static final int RANDOM_Z_RANGE = 100_000;
    
    // Sky spawn settings
    private static final int SKY_SPAWN_HEIGHT = 300; // Spawn high in the sky
    private static final int FEATHER_FALLING_DURATION = 600; // 30 seconds (ticks)
    private static final int BLINDNESS_DURATION = 200; // 10 seconds (ticks)
    private static final int SLOW_FALLING_DURATION = 600; // 30 seconds
    private static final int RESISTANCE_DURATION = 600; // 30 seconds for fall protection
    
    /**
     * Perform ascension with a chosen item to keep.
     * @param player The player ascending
     * @param keepSlot The inventory slot of the item to keep (-1 for none)
     */
    public static void performAscensionWithItem(ServerPlayer player, int keepSlot) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos oldPos = player.blockPosition();
        
        // Get ascension count BEFORE incrementing for reward calculation
        int currentAscensionCount = PlayerDataManager.getAscensionCount(player);
        int prestigeReward = AscendancyMod.getPrestigeReward(currentAscensionCount);
        
        AscendancyMod.LOGGER.info("Player {} beginning rebirth from {} (Ascension #{}, +{} pts)", 
            player.getName().getString(), oldPos, currentAscensionCount + 1, prestigeReward);
        
        // 1. SAVE CHOSEN ITEM (with amount limit based on Keeper level)
        ItemStack keptItem = ItemStack.EMPTY;
        if (keepSlot >= 0 && keepSlot < player.getInventory().getContainerSize()) {
            ItemStack original = player.getInventory().getItem(keepSlot);
            if (!original.isEmpty()) {
                int keepAmount = getKeepAmount(player);
                int actualAmount = Math.min(keepAmount, original.getCount());
                keptItem = original.copyWithCount(actualAmount);
                AscendancyMod.LOGGER.info("Player keeping {} x{}", keptItem.getDisplayName().getString(), actualAmount);
            }
        }
        
        // 2. Collect ALL items before wiping (for legacy chest)
        List<ItemStack> allItems = collectAllItems(player);
        
        // 3. CREATE LEGACY SITE at old location
        createLegacySite(level, oldPos, allItems);
        
        // 4. WIPE PLAYER completely - NO keep inventory!
        wipePlayer(player);
        
        // 5. Return kept item to first slot
        if (!keptItem.isEmpty()) {
            player.getInventory().setItem(0, keptItem);
        }
        
        // 6. CALCULATE NEW POSITION (sky spawn)
        BlockPos newSpawn = calculateSkySpawnLocation(level, oldPos);
        
        // 7. TELEPORT to sky position
        player.teleportTo(newSpawn.getX() + 0.5, newSpawn.getY(), newSpawn.getZ() + 0.5);
        
        // 8. APPLY DESCENT EFFECTS - The Rebirth Experience
        applyDescentEffects(player);
        
        // 9. RESET WORLD PARAMETERS - New World feeling
        level.setDayTime(0L); // Dawn - a new day begins
        level.setWeatherParameters(24000, 0, false, false); // Clear weather for a full day
        
        // 10. RESET FOR ASCENSION (handles count increment and prestige points)
        PlayerDataManager.resetForAscension(player);
        
        // 11. SYNC DATA - no chat notification during loading screen
        AscendancyNetworking.syncToClient(player);
        
        // Send subtle notification (player sees loading screen)
        player.displayClientMessage(Component.literal("§6§l✦ A new world awaits... ✦"), true);
        
        AscendancyMod.LOGGER.info("Player {} reborn at {}. Ascension #{}", 
            player.getName().getString(), newSpawn, PlayerDataManager.getAscensionCount(player));
    }
    
    /**
     * Apply protective effects for the descent from the sky
     */
    private static void applyDescentEffects(ServerPlayer player) {
        // Slow Falling - gentle descent
        player.addEffect(new MobEffectInstance(
            MobEffects.SLOW_FALLING, 
            SLOW_FALLING_DURATION, 
            0, // Level 1
            false, // Not ambient
            false, // Hide particles (more immersive)
            true   // Show icon
        ));
        
        // Resistance - protection during descent
        player.addEffect(new MobEffectInstance(
            MobEffects.RESISTANCE, 
            RESISTANCE_DURATION, 
            4, // Level 5 (immunity basically)
            false,
            false,
            true
        ));
        
        // Blindness - mysterious rebirth (fades to reveal new world)
        player.addEffect(new MobEffectInstance(
            MobEffects.BLINDNESS, 
            BLINDNESS_DURATION, 
            0, // Level 1
            false,
            false, // No particles
            false  // Hide icon for immersion
        ));
        
        // Night Vision after blindness - see the beautiful new world
        player.addEffect(new MobEffectInstance(
            MobEffects.NIGHT_VISION, 
            SLOW_FALLING_DURATION + 200, // Extra time after landing
            0,
            false,
            false,
            true
        ));
        
        // Regeneration - feel powerful
        player.addEffect(new MobEffectInstance(
            MobEffects.REGENERATION,
            SLOW_FALLING_DURATION,
            1, // Level 2
            false,
            true, // Show particles (golden sparkles)
            true
        ));
        
        // Glowing - you are special (brief)
        player.addEffect(new MobEffectInstance(
            MobEffects.GLOWING,
            100, // 5 seconds
            0,
            false,
            true,
            false
        ));
    }
    
    /**
     * Legacy method - ascend without choosing (keeps nothing)
     */
    public static void performAscension(ServerPlayer player) {
        performAscensionWithItem(player, -1);
    }
    
    /**
     * Get the amount of items a player can keep based on Keeper upgrade
     */
    private static int getKeepAmount(ServerPlayer player) {
        int keeperLevel = PlayerDataManager.getKeeperLevel(player);
        return 1 + keeperLevel; // Base 1, +1 per level
    }
    
    /**
     * Calculate a sky spawn location for the rebirth experience
     * Player spawns high in the sky, far from their previous location
     */
    private static BlockPos calculateSkySpawnLocation(ServerLevel level, BlockPos oldPos) {
        int randomZ = level.random.nextInt(RANDOM_Z_RANGE * 2) - RANDOM_Z_RANGE;
        
        // Calculate current distance from origin
        double currentDistanceFromOrigin = Math.sqrt(oldPos.getX() * (double)oldPos.getX() + oldPos.getZ() * (double)oldPos.getZ());
        
        int targetX, targetZ;
        
        if (currentDistanceFromOrigin < MIN_DISTANCE_FROM_ORIGIN) {
            // Force spawn to be at minimum distance + teleport distance
            targetX = MIN_DISTANCE_FROM_ORIGIN + TELEPORT_DISTANCE;
            targetZ = randomZ;
        } else {
            // Continue in positive X direction with Z variance
            targetX = oldPos.getX() + TELEPORT_DISTANCE;
            targetZ = oldPos.getZ() + randomZ;
        }
        
        // Verify we won't end up near origin
        double newDistFromOrigin = Math.sqrt(targetX * (double)targetX + targetZ * (double)targetZ);
        if (newDistFromOrigin < MIN_DISTANCE_FROM_ORIGIN) {
            targetX = MIN_DISTANCE_FROM_ORIGIN + 100_000 + level.random.nextInt(100_000);
            targetZ = randomZ;
        }
        
        // Sky spawn - high above the world
        AscendancyMod.LOGGER.info("Sky spawn location: X={}, Z={}, Y={}", targetX, targetZ, SKY_SPAWN_HEIGHT);
        return new BlockPos(targetX, SKY_SPAWN_HEIGHT, targetZ);
    }
    
    /**
     * Collect all items from player inventory and ender chest
     */
    private static List<ItemStack> collectAllItems(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();
        
        // Main inventory (includes hotbar, inventory, armor, offhand via getContainerSize)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        
        // Ender chest
        for (int i = 0; i < player.getEnderChestInventory().getContainerSize(); i++) {
            ItemStack stack = player.getEnderChestInventory().getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        
        return items;
    }
    
    /**
     * Create the legacy site with a double chest containing items
     */
    private static void createLegacySite(ServerLevel level, BlockPos pos, List<ItemStack> items) {
        // Find a safe surface position
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        BlockPos chestPos = new BlockPos(pos.getX(), surfaceY, pos.getZ());
        
        // Clear space above
        level.setBlock(chestPos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(chestPos.above().above(), Blocks.AIR.defaultBlockState(), 3);
        
        // Place double chest
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.TYPE, ChestType.LEFT)
            .setValue(ChestBlock.FACING, Direction.NORTH), 3);
        level.setBlock(chestPos.east(), Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.TYPE, ChestType.RIGHT)
            .setValue(ChestBlock.FACING, Direction.NORTH), 3);
        
        // Fill chest with items
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            int slot = 0;
            for (ItemStack item : items) {
                if (slot >= 54) break; // Double chest has 54 slots
                chest.setItem(slot++, item);
            }
        }
        
        // Place a glowstone marker on top of chest for visibility
        level.setBlock(chestPos.above(), Blocks.GLOWSTONE.defaultBlockState(), 3);
        
        AscendancyMod.LOGGER.info("Created legacy site at {}", chestPos);
    }
    
    /**
     * Wipe all player progress - NO keepInventory!
     */
    private static void wipePlayer(ServerPlayer player) {
        // Clear entire inventory (includes armor and offhand via clearContent)
        player.getInventory().clearContent();
        
        // Clear ender chest
        player.getEnderChestInventory().clearContent();
        
        // Clear status effects
        player.removeAllEffects();
        
        // Restore health and hunger
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        
        // Reset vanilla experience (we have our own Soul XP system)
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        
        // Revoke all advancements
        revokeAllAdvancements(player);
    }
    
    /**
     * Revoke all advancements to reset progress
     */
    private static void revokeAllAdvancements(ServerPlayer player) {
        var tracker = player.getAdvancements();
        MinecraftServer server = player.level().getServer();
        
        if (server != null) {
            server.getAdvancements().getAllAdvancements().forEach(advancement -> {
                var progress = tracker.getOrStartProgress(advancement);
                if (progress.isDone()) {
                    for (String criterion : progress.getCompletedCriteria()) {
                        tracker.revoke(advancement, criterion);
                    }
                }
            });
        }
    }
}
