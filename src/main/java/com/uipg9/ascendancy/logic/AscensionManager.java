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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Ascension process.
 * Uses Mojang Official Mappings for 1.21.11
 */
public class AscensionManager {
    
    // Distance to teleport on ascension
    private static final int TELEPORT_DISTANCE = 1_000_000;
    private static final int RANDOM_Z_RANGE = 50_000;
    
    // Points awarded per ascension
    private static final int POINTS_PER_ASCENSION = 3;
    
    /**
     * Perform the full ascension ritual
     */
    public static void performAscension(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos oldPos = player.blockPosition();
        
        AscendancyMod.LOGGER.info("Player {} is ascending from {}", player.getName().getString(), oldPos);
        
        // 1. SAVE HEIRLOOM (Slot 0)
        ItemStack heirloom = player.getInventory().getItem(0).copy();
        
        // 2. Collect all items before wiping
        List<ItemStack> allItems = collectAllItems(player);
        
        // 3. CREATE LEGACY SITE
        createLegacySite(level, oldPos, allItems);
        
        // 4. WIPE PLAYER
        wipePlayer(player);
        
        // 5. Return heirloom
        player.getInventory().setItem(0, heirloom);
        
        // 6. CALCULATE NEW POSITION
        int randomZ = level.random.nextInt(RANDOM_Z_RANGE * 2) - RANDOM_Z_RANGE;
        BlockPos targetZone = oldPos.offset(TELEPORT_DISTANCE, 0, randomZ);
        
        // 7. FIND SAFE SPAWN
        BlockPos newSpawn = findSafeLocation(level, targetZone);
        
        // 8. TELEPORT (use the simple teleport that works)
        player.teleportTo(newSpawn.getX() + 0.5, newSpawn.getY() + 1, newSpawn.getZ() + 0.5);
        
        // 9. RESET WORLD PARAMETERS
        level.setDayTime(0L); // Dawn
        level.setWeatherParameters(6000, 0, false, false); // Clear weather
        
        // 10. INCREMENT ASCENSION COUNT & GRANT POINTS
        PlayerDataManager.incrementAscensionCount(player);
        PlayerDataManager.addPrestigePoints(player, POINTS_PER_ASCENSION);
        PlayerDataManager.resetForAscension(player);
        
        // 11. SYNC DATA & NOTIFY
        AscendancyNetworking.syncToClient(player);
        player.displayClientMessage(Component.translatable("gui.ascendancy.ascension_complete"), true);
        
        AscendancyMod.LOGGER.info("Player {} ascended to {}. Ascension #{}", 
            player.getName().getString(), newSpawn, PlayerDataManager.getAscensionCount(player));
    }
    
    /**
     * Collect all items from player inventory and ender chest
     */
    private static List<ItemStack> collectAllItems(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();
        
        // Main inventory (skip slot 0 - heirloom)
        for (int i = 1; i < player.getInventory().getContainerSize(); i++) {
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
     * Wipe all player progress
     */
    private static void wipePlayer(ServerPlayer player) {
        // Clear inventory
        player.getInventory().clearContent();
        
        // Clear ender chest
        player.getEnderChestInventory().clearContent();
        
        // Clear status effects
        player.removeAllEffects();
        
        // Restore health and hunger
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        
        // Reset experience (but keep attributes!)
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
    
    /**
     * Find a safe location near the target
     */
    private static BlockPos findSafeLocation(ServerLevel level, BlockPos target) {
        // Find surface level at target
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, target.getX(), target.getZ());
        return new BlockPos(target.getX(), y, target.getZ());
    }
}
