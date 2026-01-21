package com.uipg9.ascendancy.systems;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.data.PlayerDataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The Chronicle System - A procedural diary of your adventures
 * v2.5 - Replayability Expansion
 * 
 * Automatically records significant events in each life,
 * creating a persistent history across all ascensions.
 */
public class ChronicleManager {
    
    private static final String CHRONICLE_FILE = "ascendancy_chronicle.dat";
    
    // Current life events (cleared on ascension, then saved)
    private static final Map<UUID, List<ChronicleEntry>> currentLifeEntries = new HashMap<>();
    
    // Track milestones already recorded this life
    private static final Map<UUID, Set<String>> recordedMilestones = new HashMap<>();
    
    /**
     * A single chronicle entry
     */
    public record ChronicleEntry(
        String timestamp,
        int ascensionNumber,
        String eventType,
        String description
    ) {
        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putString("time", timestamp);
            tag.putInt("ascension", ascensionNumber);
            tag.putString("type", eventType);
            tag.putString("desc", description);
            return tag;
        }
        
        public static ChronicleEntry fromNbt(CompoundTag tag) {
            return new ChronicleEntry(
                tag.getStringOr("time", "Unknown"),
                tag.getIntOr("ascension", 0),
                tag.getStringOr("type", "event"),
                tag.getStringOr("desc", "...")
            );
        }
    }
    
    /**
     * Record an event in the chronicle
     */
    public static void recordEvent(ServerPlayer player, String eventType, String description) {
        UUID playerId = player.getUUID();
        currentLifeEntries.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        int ascension = PlayerDataManager.getAscensionCount(player);
        
        ChronicleEntry entry = new ChronicleEntry(timestamp, ascension, eventType, description);
        currentLifeEntries.get(playerId).add(entry);
        
        AscendancyMod.LOGGER.debug("Chronicle: {} - {}", eventType, description);
    }
    
    /**
     * Record a milestone (only once per life)
     */
    public static void recordMilestone(ServerPlayer player, String milestoneId, String description) {
        UUID playerId = player.getUUID();
        Set<String> recorded = recordedMilestones.computeIfAbsent(playerId, k -> new HashSet<>());
        
        if (!recorded.contains(milestoneId)) {
            recorded.add(milestoneId);
            recordEvent(player, "milestone", description);
            
            // Notify player
            player.sendSystemMessage(Component.literal("§8§o[Chronicle: " + description + "]"));
        }
    }
    
    /**
     * Called when a new life begins - start fresh chronicle for this life
     */
    public static void onNewLife(ServerPlayer player) {
        UUID playerId = player.getUUID();
        currentLifeEntries.put(playerId, new ArrayList<>());
        recordedMilestones.put(playerId, new HashSet<>());
        
        int ascension = PlayerDataManager.getAscensionCount(player);
        recordEvent(player, "birth", "A soul awakens in Age " + (ascension + 1));
    }
    
    /**
     * Called on ascension - save this life's chronicle and clear
     */
    public static void onAscension(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        int ascension = PlayerDataManager.getAscensionCount(player);
        
        // Record final entry
        recordEvent(player, "ascension", "The soul transcends, leaving this world behind");
        
        // Save to persistent file
        List<ChronicleEntry> lifeEntries = currentLifeEntries.getOrDefault(playerId, new ArrayList<>());
        saveChronicleEntries(level, playerId.toString(), ascension, lifeEntries);
        
        // Clear for next life
        currentLifeEntries.remove(playerId);
        recordedMilestones.remove(playerId);
    }
    
    // ==================== AUTOMATIC EVENT HOOKS ====================
    
    /**
     * First time entering the Nether
     */
    public static void onEnterNether(ServerPlayer player) {
        recordMilestone(player, "nether", "Stepped through a portal into the burning depths");
    }
    
    /**
     * First time entering the End
     */
    public static void onEnterEnd(ServerPlayer player) {
        recordMilestone(player, "end", "Gazed upon the void between worlds");
    }
    
    /**
     * First diamond found
     */
    public static void onFirstDiamond(ServerPlayer player) {
        recordMilestone(player, "diamond", "Unearthed a precious diamond");
    }
    
    /**
     * First death
     */
    public static void onDeath(ServerPlayer player, String cause) {
        recordMilestone(player, "death", "Fell to " + cause + ", but the soul endures");
    }
    
    /**
     * Dragon slain
     */
    public static void onDragonSlain(ServerPlayer player) {
        recordMilestone(player, "dragon", "Vanquished the Ender Dragon!");
    }
    
    /**
     * Wither slain
     */
    public static void onWitherSlain(ServerPlayer player) {
        recordMilestone(player, "wither", "Defeated the Wither in glorious combat");
    }
    
    /**
     * Echo defeated
     */
    public static void onEchoDefeated(ServerPlayer player) {
        recordEvent(player, "echo", "Confronted and defeated an Echo of a past life");
    }
    
    /**
     * Reached deep slate layer for first time
     */
    public static void onReachDeepslate(ServerPlayer player) {
        recordMilestone(player, "deepslate", "Descended into the deepslate caverns");
    }
    
    /**
     * Found ancient debris
     */
    public static void onFoundAncientDebris(ServerPlayer player) {
        recordMilestone(player, "debris", "Discovered ancient debris in the Nether");
    }
    
    /**
     * Tamed first animal
     */
    public static void onTameAnimal(ServerPlayer player, String animalType) {
        recordMilestone(player, "tame_" + animalType, "Befriended a " + animalType);
    }
    
    /**
     * Built a beacon
     */
    public static void onBeaconActivated(ServerPlayer player) {
        recordMilestone(player, "beacon", "Activated a beacon, its light piercing the sky");
    }
    
    // ==================== FILE STORAGE ====================
    
    private static Path getChronicleDataPath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(CHRONICLE_FILE);
    }
    
    private static void saveChronicleEntries(ServerLevel level, String playerId, int ascension, List<ChronicleEntry> entries) {
        try {
            Path path = getChronicleDataPath(level);
            CompoundTag root;
            
            if (Files.exists(path)) {
                root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            } else {
                root = new CompoundTag();
            }
            
            // Get or create player's chronicle
            CompoundTag playerChronicle = root.getCompoundOrEmpty(playerId);
            
            // Create life entry
            CompoundTag lifeTag = new CompoundTag();
            lifeTag.putInt("ascension", ascension);
            
            ListTag entriesList = new ListTag();
            for (ChronicleEntry entry : entries) {
                entriesList.add(entry.toNbt());
            }
            lifeTag.put("entries", entriesList);
            
            // Store under "life_X" key
            playerChronicle.put("life_" + ascension, lifeTag);
            root.put(playerId, playerChronicle);
            
            NbtIo.writeCompressed(root, path);
            AscendancyMod.LOGGER.info("Saved chronicle for {} (Age {}, {} entries)", 
                playerId, ascension, entries.size());
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to save chronicle", e);
        }
    }
    
    /**
     * Load all chronicle entries for a player (for viewing)
     */
    public static List<ChronicleEntry> loadAllEntries(ServerLevel level, String playerId) {
        List<ChronicleEntry> allEntries = new ArrayList<>();
        try {
            Path path = getChronicleDataPath(level);
            if (!Files.exists(path)) return allEntries;
            
            CompoundTag root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            CompoundTag playerChronicle = root.getCompoundOrEmpty(playerId);
            
            // Iterate through all lives
            for (String key : playerChronicle.keySet()) {
                if (key.startsWith("life_")) {
                    CompoundTag lifeTag = playerChronicle.getCompoundOrEmpty(key);
                    ListTag entriesList = lifeTag.getListOrEmpty("entries");
                    
                    for (int i = 0; i < entriesList.size(); i++) {
                        allEntries.add(ChronicleEntry.fromNbt(entriesList.getCompoundOrEmpty(i)));
                    }
                }
            }
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to load chronicle", e);
        }
        return allEntries;
    }
    
    /**
     * Get entry count for display
     */
    public static int getCurrentLifeEntryCount(ServerPlayer player) {
        return currentLifeEntries.getOrDefault(player.getUUID(), new ArrayList<>()).size();
    }
    
    /**
     * Save this life's chronicle to history (called during ascension)
     */
    public static void saveToHistory(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        int ascension = PlayerDataManager.getAscensionCount(player);
        
        // Record final entry
        recordEvent(player, "ascension", "The soul transcends, leaving this world behind");
        
        // Save to persistent file
        List<ChronicleEntry> lifeEntries = currentLifeEntries.getOrDefault(playerId, new ArrayList<>());
        saveChronicleEntries(level, playerId.toString(), ascension, lifeEntries);
        
        // Clear for next life
        currentLifeEntries.remove(playerId);
        recordedMilestones.remove(playerId);
    }
    
    /**
     * Load current life data on player join
     */
    public static void loadCurrentLife(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        
        try {
            Path path = level.getServer().getWorldPath(LevelResource.ROOT).resolve("ascendancy_chronicle_current.dat");
            if (Files.exists(path)) {
                CompoundTag root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                CompoundTag playerData = root.getCompoundOrEmpty(playerId.toString());
                
                // Load current life entries
                ListTag entriesList = playerData.getListOrEmpty("entries");
                List<ChronicleEntry> entries = new ArrayList<>();
                for (int i = 0; i < entriesList.size(); i++) {
                    entries.add(ChronicleEntry.fromNbt(entriesList.getCompoundOrEmpty(i)));
                }
                currentLifeEntries.put(playerId, entries);
                
                // Load recorded milestones
                ListTag milestonesList = playerData.getListOrEmpty("milestones");
                Set<String> milestones = new HashSet<>();
                for (int i = 0; i < milestonesList.size(); i++) {
                    milestones.add(milestonesList.getString(i).orElse(""));
                }
                recordedMilestones.put(playerId, milestones);
                
                AscendancyMod.LOGGER.info("Loaded {} chronicle entries for {}", entries.size(), player.getName().getString());
            }
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to load current chronicle", e);
        }
    }
    
    /**
     * Save current life data on player disconnect
     */
    public static void saveCurrentLife(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        
        try {
            Path path = level.getServer().getWorldPath(LevelResource.ROOT).resolve("ascendancy_chronicle_current.dat");
            CompoundTag root;
            
            if (Files.exists(path)) {
                root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            } else {
                root = new CompoundTag();
            }
            
            CompoundTag playerData = new CompoundTag();
            
            // Save current life entries
            List<ChronicleEntry> entries = currentLifeEntries.getOrDefault(playerId, new ArrayList<>());
            ListTag entriesList = new ListTag();
            for (ChronicleEntry entry : entries) {
                entriesList.add(entry.toNbt());
            }
            playerData.put("entries", entriesList);
            
            // Save recorded milestones
            Set<String> milestones = recordedMilestones.getOrDefault(playerId, new HashSet<>());
            ListTag milestonesList = new ListTag();
            for (String milestone : milestones) {
                milestonesList.add(net.minecraft.nbt.StringTag.valueOf(milestone));
            }
            playerData.put("milestones", milestonesList);
            
            root.put(playerId.toString(), playerData);
            NbtIo.writeCompressed(root, path);
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to save current chronicle", e);
        }
    }
}
