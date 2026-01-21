package com.uipg9.ascendancy.logic;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.data.PlayerDataManager;
import com.uipg9.ascendancy.network.AscendancyNetworking;
import com.uipg9.ascendancy.systems.ConstellationManager;
import com.uipg9.ascendancy.systems.EchoManager;
import com.uipg9.ascendancy.systems.HeirloomManager;
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
import net.minecraft.tags.StructureTags;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Ascension process - A New World Awaits!
 * 
 * v2.5 - The Replayability Expansion:
 * - Echo boss guards legacy chests
 * - Heirloom items evolve across lives
 * - Constellation system for major perks
 * 
 * v2.4 - The Mystery Update:
 * - Player awakens mysteriously in a new world
 * - Blindness fades to reveal morning in a village
 * - No memory of how they got there - pure mystery!
 * - World resets to dawn with clear skies
 * 
 * Key features:
 * - Player keeps ONE chosen item (amount based on Keeper upgrade)
 * - Mysterious village spawn with blindness fade
 * - World reset: clear weather, dawn (time=0)
 * - Legacy chest preserves old items at departure site
 */
public class AscensionManager {
    
    // Minimum distance from origin to prevent close spawns
    private static final int MIN_DISTANCE_FROM_ORIGIN = 10_000;
    
    // How far to travel per ascension
    private static final int TELEPORT_DISTANCE = 50_000;
    
    // Random Z variance
    private static final int RANDOM_Z_RANGE = 10_000;
    
    // Mysterious awakening settings
    private static final int BLINDNESS_DURATION = 160; // 8 seconds (ticks) - slowly fades
    private static final int NIGHT_VISION_DURATION = 600; // 30 seconds after awakening
    
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
                
                // Process as Heirloom - adds lore and special properties!
                keptItem = HeirloomManager.processHeirloom(keptItem, player, currentAscensionCount + 1);
                
                AscendancyMod.LOGGER.info("Player keeping heirloom {} x{}", keptItem.getDisplayName().getString(), actualAmount);
            }
        }
        
        // 2. Collect ALL items before wiping (for legacy chest)
        List<ItemStack> allItems = collectAllItems(player);
        
        // 3. CREATE LEGACY SITE at old location + Register Echo spawn point
        BlockPos chestPos = createLegacySite(level, oldPos, allItems);
        EchoManager.registerLegacySite(player, chestPos);
        
        // 4. Clear constellation for new life selection
        ConstellationManager.clearConstellation(player);
        
        // 5. WIPE PLAYER completely - NO keep inventory!
        wipePlayer(player);
        
        // 6. Return kept item to first slot
        if (!keptItem.isEmpty()) {
            player.getInventory().setItem(0, keptItem);
        }
        
        // 7. CALCULATE NEW POSITION (village spawn)
        BlockPos newSpawn = calculateVillageSpawnLocation(level, oldPos);
        
        // 7. TELEPORT to village ground level and set as spawn
        player.teleportTo(newSpawn.getX() + 0.5, newSpawn.getY(), newSpawn.getZ() + 0.5);
        
        // Note: With reduced distance (50k blocks), lag should be minimal now
        // Player will respawn at world spawn if they die, but that's acceptable
        
        // 8. APPLY AWAKENING EFFECTS - The Mystery Experience
        applyAwakeningEffects(player);
        
        // 9. RESET WORLD PARAMETERS - New morning!
        level.setDayTime(0L); // Dawn - a new day begins
        level.setWeatherParameters(24000, 0, false, false); // Clear weather for a full day
        
        // 10. RESET FOR ASCENSION (handles count increment and prestige points)
        PlayerDataManager.resetForAscension(player);
        
        // 11. SYNC DATA - no chat notification during loading screen
        AscendancyNetworking.syncToClient(player);
        
        // Send subtle notification (player sees loading screen)
        player.displayClientMessage(Component.literal("§6§l✦ You awaken in a new world... ✦"), true);
        
        AscendancyMod.LOGGER.info("Player {} reborn at {}. Ascension #{}", 
            player.getName().getString(), newSpawn, PlayerDataManager.getAscensionCount(player));
    }
    
    /**
     * Apply awakening effects - blindness fades to reveal morning
     */
    private static void applyAwakeningEffects(ServerPlayer player) {
        // Blindness - mysterious awakening (fades to reveal new world)
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
            BLINDNESS_DURATION + NIGHT_VISION_DURATION,
            0,
            false,
            false,
            true
        ));
        
        // Regeneration - feel refreshed
        player.addEffect(new MobEffectInstance(
            MobEffects.REGENERATION,
            200, // 10 seconds
            1, // Level 2
            false,
            true, // Show particles (golden sparkles)
            true
        ));
        
        // Saturation - well fed feeling
        player.addEffect(new MobEffectInstance(
            MobEffects.SATURATION,
            100, // 5 seconds
            0,
            false,
            false,
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
     * Calculate a village spawn location for the mysterious awakening
     * Player spawns on the ground in a new location far away
     * Attempts to find an actual village structure!
     */
    private static BlockPos calculateVillageSpawnLocation(ServerLevel level, BlockPos oldPos) {
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
        
        BlockPos basePos = new BlockPos(targetX, 64, targetZ);
        
        // Try to find a village near the target location using structure tags
        BlockPos villagePos = level.findNearestMapStructure(
            StructureTags.VILLAGE,
            basePos,
            100, // Search radius in chunks (100 = 1600 blocks)
            false
        );
        
        BlockPos spawnPos;
        if (villagePos != null) {
            // Found a village! Spawn there
            int villageX = villagePos.getX();
            int villageZ = villagePos.getZ();
            AscendancyMod.LOGGER.info("Found village near target at X={}, Z={}", villageX, villageZ);
            
            // Get ground height at village location
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, villageX, villageZ);
            spawnPos = new BlockPos(villageX, Math.max(groundY, 64), villageZ);
        } else {
            // No village found, use safe ground spawn
            AscendancyMod.LOGGER.info("No village found, using safe ground spawn at X={}, Z={}", targetX, targetZ);
            
            // Force load the chunk to get accurate heightmap
            level.getChunk(targetX >> 4, targetZ >> 4);
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
            
            // Safety checks for spawn height
            if (groundY < 60) {
                groundY = 64; // Sea level as safe fallback
            }
            
            spawnPos = new BlockPos(targetX, groundY, targetZ);
        }
        
        // Make sure we're not spawning inside a block - find air
        int searchUp = 0;
        while (!level.getBlockState(spawnPos).isAir() && searchUp < 50) {
            spawnPos = spawnPos.above();
            searchUp++;
        }
        // Also check the block above for headroom
        while (!level.getBlockState(spawnPos.above()).isAir() && searchUp < 50) {
            spawnPos = spawnPos.above();
            searchUp++;
        }
        
        AscendancyMod.LOGGER.info("Safe spawn location: X={}, Z={}, Y={}", spawnPos.getX(), spawnPos.getZ(), spawnPos.getY());
        return spawnPos;
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
     * Also stores location for Echo boss spawning
     * @return The position of the chest for Echo registration
     */
    private static BlockPos createLegacySite(ServerLevel level, BlockPos pos, List<ItemStack> items) {
        // Find a safe surface position
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        BlockPos chestPos = new BlockPos(pos.getX(), surfaceY, pos.getZ());
        
        // Clear space above
        level.setBlock(chestPos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(chestPos.above().above(), Blocks.AIR.defaultBlockState(), 3);
        
        // Place double chest - ensure proper facing
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.TYPE, ChestType.LEFT)
            .setValue(ChestBlock.FACING, Direction.NORTH), 3);
        level.setBlock(chestPos.east(), Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.TYPE, ChestType.RIGHT)
            .setValue(ChestBlock.FACING, Direction.NORTH), 3);
        
        // Schedule item filling for next tick (ensures block entity exists)
        level.getServer().execute(() -> {
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                int slot = 0;
                for (ItemStack item : items) {
                    if (slot >= 54) break; // Double chest has 54 slots
                    chest.setItem(slot++, item);
                }
                chest.setChanged();
                AscendancyMod.LOGGER.info("Filled legacy chest with {} items at {}", slot, chestPos);
            } else {
                AscendancyMod.LOGGER.warn("Failed to get chest block entity at {}", chestPos);
            }
        });
        
        // Place a glowstone marker on top of chest for visibility
        level.setBlock(chestPos.above(), Blocks.GLOWSTONE.defaultBlockState(), 3);
        
        AscendancyMod.LOGGER.info("Created legacy site at {}", chestPos);
        return chestPos;
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
