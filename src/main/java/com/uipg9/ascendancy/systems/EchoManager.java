package com.uipg9.ascendancy.systems;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.data.PlayerDataManager;
import com.uipg9.ascendancy.network.AscendancyNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Echo System - Armored zombies guard legacy chests
 * v2.5 - Replayability Expansion
 * 
 * When a player ascends, their armor items are saved as IDs. When they approach their
 * legacy chest in a future life, "The Echo" spawns - a zombie wearing
 * their old armor. Defeating it grants 25% Soul XP bonus.
 */
public class EchoManager {
    
    // File storage for echo data
    private static final String ECHO_DATA_FILE = "ascendancy_echoes.dat";
    
    // Tracking spawned echoes to avoid duplicates
    private static final Set<String> spawnedEchoSites = new HashSet<>();
    
    // Track active echo entities for kill detection
    private static final Map<UUID, EchoData> activeEchoes = new HashMap<>();
    
    // Echo spawn distance from player
    private static final int ECHO_SPAWN_DISTANCE = 30;
    
    /**
     * Data for a legacy site where an echo can spawn
     */
    public record LegacySiteData(
        String playerUUID,
        int x, int y, int z,
        int ascensionNumber,
        List<String> armorItemIds,
        boolean echoDefeated
    ) {
        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putString("playerId", playerUUID);
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            tag.putInt("ascension", ascensionNumber);
            tag.putBoolean("defeated", echoDefeated);
            
            ListTag armorList = new ListTag();
            for (String itemId : armorItemIds) {
                armorList.add(StringTag.valueOf(itemId));
            }
            tag.put("armor", armorList);
            return tag;
        }
        
        public static LegacySiteData fromNbt(CompoundTag tag) {
            String playerId = tag.getStringOr("playerId", "");
            int x = tag.getIntOr("x", 0);
            int y = tag.getIntOr("y", 0);
            int z = tag.getIntOr("z", 0);
            int ascension = tag.getIntOr("ascension", 0);
            boolean defeated = tag.getBooleanOr("defeated", false);
            
            List<String> armor = new ArrayList<>();
            ListTag armorList = tag.getListOrEmpty("armor");
            for (int i = 0; i < armorList.size(); i++) {
                armor.add(armorList.getString(i).orElse("minecraft:air"));
            }
            
            return new LegacySiteData(playerId, x, y, z, ascension, armor, defeated);
        }
        
        public BlockPos getPos() {
            return new BlockPos(x, y, z);
        }
    }
    
    /**
     * Data for tracking an active Echo entity
     */
    private record EchoData(String playerUUID, BlockPos sitePos, int soulXPBonus) {}
    
    /**
     * Register a legacy site when player ascends
     * Called from AscensionManager after creating the chest
     */
    public static void registerLegacySite(ServerPlayer player, BlockPos chestPos) {
        ServerLevel level = (ServerLevel) player.level();
        
        // Collect current armor IDs
        List<String> armorIds = new ArrayList<>();
        armorIds.add(getItemId(player.getItemBySlot(EquipmentSlot.HEAD)));
        armorIds.add(getItemId(player.getItemBySlot(EquipmentSlot.CHEST)));
        armorIds.add(getItemId(player.getItemBySlot(EquipmentSlot.LEGS)));
        armorIds.add(getItemId(player.getItemBySlot(EquipmentSlot.FEET)));
        
        // Only register if they have SOME armor
        boolean hasArmor = armorIds.stream().anyMatch(id -> !id.equals("minecraft:air"));
        if (!hasArmor) {
            AscendancyMod.LOGGER.info("Player {} has no armor, skipping Echo registration", player.getName().getString());
            return;
        }
        
        int ascensionCount = PlayerDataManager.getAscensionCount(player);
        LegacySiteData siteData = new LegacySiteData(
            player.getUUID().toString(), 
            chestPos.getX(), chestPos.getY(), chestPos.getZ(),
            ascensionCount, armorIds, false
        );
        
        // Save to file
        saveLegacySite(level, siteData);
        
        AscendancyMod.LOGGER.info("Registered Echo site for {} at {} (Ascension #{})", 
            player.getName().getString(), chestPos, ascensionCount);
    }
    
    private static String getItemId(ItemStack stack) {
        if (stack.isEmpty()) return "minecraft:air";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
    
    private static ItemStack getItemFromId(String id) {
        if (id.equals("minecraft:air")) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(id));
        return new ItemStack(item);
    }
    
    /**
     * Called every player tick to check if they're near a legacy site
     */
    public static void tickPlayerProximity(ServerPlayer player) {
        // Only check every 2 seconds (40 ticks)
        if (player.tickCount % 40 != 0) return;
        
        ServerLevel level = (ServerLevel) player.level();
        List<LegacySiteData> sites = loadAllLegacySites(level);
        
        for (LegacySiteData site : sites) {
            // Skip defeated sites
            if (site.echoDefeated()) continue;
            
            String siteKey = site.playerUUID() + "_" + site.ascensionNumber();
            if (spawnedEchoSites.contains(siteKey)) continue;
            
            // Check distance
            double distance = player.blockPosition().distSqr(site.getPos());
            if (distance <= ECHO_SPAWN_DISTANCE * ECHO_SPAWN_DISTANCE) {
                // Spawn the Echo!
                spawnEcho(level, player, site);
                spawnedEchoSites.add(siteKey);
            }
        }
    }
    
    /**
     * Spawn the Echo boss near the legacy chest
     */
    private static void spawnEcho(ServerLevel level, ServerPlayer player, LegacySiteData site) {
        // Find spawn position near chest
        BlockPos spawnPos = site.getPos().offset(
            level.random.nextInt(5) - 2,
            1,
            level.random.nextInt(5) - 2
        );
        
        // Calculate Soul XP bonus (25% of max for this ascension)
        int maxSoulXP = AscendancyMod.getMaxSoulXP(site.ascensionNumber());
        int bonusXP = maxSoulXP / 4;
        
        // Create the zombie
        var echo = EntityType.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (echo == null) return;
        
        echo.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        
        // Give it the saved armor
        if (site.armorItemIds().size() >= 4) {
            echo.setItemSlot(EquipmentSlot.HEAD, getItemFromId(site.armorItemIds().get(0)));
            echo.setItemSlot(EquipmentSlot.CHEST, getItemFromId(site.armorItemIds().get(1)));
            echo.setItemSlot(EquipmentSlot.LEGS, getItemFromId(site.armorItemIds().get(2)));
            echo.setItemSlot(EquipmentSlot.FEET, getItemFromId(site.armorItemIds().get(3)));
            
            // Set drop chances to 0 (they don't drop their armor)
            echo.setDropChance(EquipmentSlot.HEAD, 0f);
            echo.setDropChance(EquipmentSlot.CHEST, 0f);
            echo.setDropChance(EquipmentSlot.LEGS, 0f);
            echo.setDropChance(EquipmentSlot.FEET, 0f);
        }
        
        // Make it stronger
        var healthAttr = echo.getAttribute(Attributes.MAX_HEALTH);
        var attackAttr = echo.getAttribute(Attributes.ATTACK_DAMAGE);
        var speedAttr = echo.getAttribute(Attributes.MOVEMENT_SPEED);
        
        if (healthAttr != null) healthAttr.setBaseValue(40.0); // 20 hearts
        echo.setHealth(40f);
        if (attackAttr != null) attackAttr.setBaseValue(6.0);
        if (speedAttr != null) speedAttr.setBaseValue(0.28);
        
        // Set custom name
        echo.setCustomName(Component.literal("§5§lThe Echo"));
        echo.setCustomNameVisible(true);
        
        // Persist the entity
        echo.setPersistenceRequired();
        
        // Track for kill detection (in-memory tracking)
        level.addFreshEntity(echo);
        activeEchoes.put(echo.getUUID(), new EchoData(site.playerUUID(), site.getPos(), bonusXP));
        
        // Alert the player
        player.sendSystemMessage(Component.literal("§5§l⚠ You feel a presence from your past life..."));
        player.sendSystemMessage(Component.literal("§d§oThe Echo guards your legacy. Defeat it for §e+" + bonusXP + " Soul XP§d§o!"));
        
        AscendancyMod.LOGGER.info("Spawned Echo for {} at {} (bonus: {} Soul XP)", 
            player.getName().getString(), spawnPos, bonusXP);
    }
    
    /**
     * Check if a killed mob was an Echo - returns true if it was handled
     */
    public static boolean onMobKilled(ServerPlayer player, Entity killed) {
        // Check our tracking map first
        EchoData echoData = activeEchoes.remove(killed.getUUID());
        if (echoData != null) {
            // It's an Echo! Award bonus Soul XP
            int currentXP = PlayerDataManager.getSoulXP(player);
            PlayerDataManager.setSoulXP(player, currentXP + echoData.soulXPBonus());
            
            player.sendSystemMessage(Component.literal("§5§l✦ The Echo is vanquished! ✦"));
            player.sendSystemMessage(Component.literal("§e+" + echoData.soulXPBonus() + " Soul XP §7(Echo Bonus)"));
            
            // Sync to client
            AscendancyNetworking.syncToClient(player);
            
            // Mark as defeated in saved data
            ServerLevel level = (ServerLevel) player.level();
            markEchoDefeated(level, echoData.playerUUID(), echoData.sitePos());
            
            AscendancyMod.LOGGER.info("Player {} defeated Echo, awarded {} Soul XP", 
                player.getName().getString(), echoData.soulXPBonus());
            return true;
        }
        
        // Echo not in tracking map, return false
        return false;
    }
    
    // ==================== FILE STORAGE ====================
    
    private static Path getEchoDataPath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(ECHO_DATA_FILE);
    }
    
    private static void saveLegacySite(ServerLevel level, LegacySiteData site) {
        try {
            Path path = getEchoDataPath(level);
            CompoundTag root;
            
            if (Files.exists(path)) {
                root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            } else {
                root = new CompoundTag();
            }
            
            ListTag sites = root.getListOrEmpty("sites");
            sites.add(site.toNbt());
            root.put("sites", sites);
            
            NbtIo.writeCompressed(root, path);
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to save Echo data", e);
        }
    }
    
    private static List<LegacySiteData> loadAllLegacySites(ServerLevel level) {
        List<LegacySiteData> sites = new ArrayList<>();
        try {
            Path path = getEchoDataPath(level);
            if (!Files.exists(path)) return sites;
            
            CompoundTag root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            ListTag sitesList = root.getListOrEmpty("sites");
            
            for (int i = 0; i < sitesList.size(); i++) {
                sites.add(LegacySiteData.fromNbt(sitesList.getCompoundOrEmpty(i)));
            }
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to load Echo data", e);
        }
        return sites;
    }
    
    private static void markEchoDefeated(ServerLevel level, String playerUUID, BlockPos sitePos) {
        try {
            Path path = getEchoDataPath(level);
            if (!Files.exists(path)) return;
            
            CompoundTag root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            ListTag sites = root.getListOrEmpty("sites");
            
            for (int i = 0; i < sites.size(); i++) {
                CompoundTag siteTag = sites.getCompoundOrEmpty(i);
                if (siteTag.getStringOr("playerId", "").equals(playerUUID) &&
                    siteTag.getIntOr("x", 0) == sitePos.getX() &&
                    siteTag.getIntOr("z", 0) == sitePos.getZ()) {
                    siteTag.putBoolean("defeated", true);
                    break;
                }
            }
            
            root.put("sites", sites);
            NbtIo.writeCompressed(root, path);
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to update Echo data", e);
        }
    }
}
