package com.uipg9.ascendancy.systems;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.data.PlayerDataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Ancestral Bond - Carry pets across ascensions
 * v2.5 - Replayability Expansion
 * 
 * Bond with your companion so deeply that they follow you
 * into your next life. Limited by prestige level.
 * 
 * Simplified implementation: stores basic pet type and name,
 * recreates fresh pet in next life.
 */
public class AncestralBondManager {
    
    private static final String BOND_FILE = "ascendancy_bonds.dat";
    
    // Stored pet data per player - simplified to just store type and name
    private static final Map<UUID, List<PetData>> storedPets = new HashMap<>();
    
    /**
     * Pet data record (simplified)
     */
    public record PetData(
        String entityType,
        String customName,
        int bondStrength  // How many ascensions this pet has survived
    ) {}
    
    /**
     * Get max pets player can bring to next life (based on prestige)
     */
    public static int getMaxBondedPets(ServerPlayer player) {
        int prestige = PlayerDataManager.getPrestigePoints(player);
        return Math.min(3, prestige / 5);  // 1 pet at 5 prestige, 2 at 10, 3 at 15
    }
    
    /**
     * Store a tamed pet for transfer to next life
     */
    public static boolean storePet(ServerPlayer player, TamableAnimal pet) {
        UUID playerId = player.getUUID();
        List<PetData> pets = storedPets.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        int maxPets = getMaxBondedPets(player);
        if (pets.size() >= maxPets) {
            player.sendSystemMessage(Component.literal("§cYou can only bond with " + maxPets + " pets at your prestige level!"));
            return false;
        }
        
        // Check if this pet is tame and belongs to player
        if (!pet.isTame()) {
            player.sendSystemMessage(Component.literal("§cYou can only bond with your own tamed pets!"));
            return false;
        }
        
        // Get entity type
        String entityTypeId = EntityType.getKey(pet.getType()).toString();
        
        // Get custom name if any
        String customName = pet.hasCustomName() ? pet.getCustomName().getString() : null;
        
        PetData petData = new PetData(entityTypeId, customName, 1);
        pets.add(petData);
        
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§d§l✦ ANCESTRAL BOND FORMED ✦"));
        String petName = customName != null ? customName : pet.getType().getDescription().getString();
        player.sendSystemMessage(Component.literal("§5" + petName + " §7will follow you into your next life."));
        player.sendSystemMessage(Component.literal("§7Bond Strength: §d" + petData.bondStrength()));
        
        saveData(player);
        return true;
    }
    
    /**
     * Restore bonded pets after ascension
     */
    public static void restorePets(ServerPlayer player) {
        UUID playerId = player.getUUID();
        List<PetData> pets = storedPets.get(playerId);
        
        if (pets == null || pets.isEmpty()) return;
        
        ServerLevel level = (ServerLevel) player.level();
        Vec3 pos = player.position();
        int restored = 0;
        
        List<PetData> updatedPets = new ArrayList<>();
        
        for (PetData petData : pets) {
            try {
                // Parse entity type using Identifier (1.21.11 API)
                Identifier entityTypeId = Identifier.tryParse(petData.entityType());
                if (entityTypeId == null) continue;
                
                Optional<EntityType<?>> entityTypeOpt = EntityType.byString(entityTypeId.toString());
                if (entityTypeOpt.isEmpty()) continue;
                
                EntityType<?> entityType = entityTypeOpt.get();
                Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);
                
                if (entity instanceof TamableAnimal tamable) {
                    // Tame to player
                    tamable.tame(player);
                    
                    // Set position near player
                    double angle = Math.random() * Math.PI * 2;
                    double x = pos.x + Math.cos(angle) * 2;
                    double z = pos.z + Math.sin(angle) * 2;
                    tamable.setPos(x, pos.y, z);
                    
                    // Restore custom name
                    if (petData.customName() != null) {
                        tamable.setCustomName(Component.literal(petData.customName()));
                    }
                    
                    level.addFreshEntity(tamable);
                    restored++;
                    
                    // Update bond strength
                    updatedPets.add(new PetData(
                        petData.entityType(),
                        petData.customName(),
                        petData.bondStrength() + 1
                    ));
                    
                    AscendancyMod.LOGGER.info("Restored bonded pet {} for player {}", 
                        petData.entityType(), player.getName().getString());
                }
            } catch (Exception e) {
                AscendancyMod.LOGGER.error("Failed to restore pet: {}", petData.entityType(), e);
            }
        }
        
        // Update stored pets with increased bond strength
        storedPets.put(playerId, updatedPets);
        
        if (restored > 0) {
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("§d§l✦ BONDS RESTORED ✦"));
            player.sendSystemMessage(Component.literal("§5" + restored + " §7companion(s) followed you into this new life!"));
        }
        
        saveData(player);
    }
    
    /**
     * Clear all bonds (if player wants to release pets)
     */
    public static void clearBonds(ServerPlayer player) {
        storedPets.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("§7All ancestral bonds have been released."));
        saveData(player);
    }
    
    /**
     * Get number of bonded pets
     */
    public static int getBondedPetCount(ServerPlayer player) {
        return storedPets.getOrDefault(player.getUUID(), new ArrayList<>()).size();
    }
    
    /**
     * Get list of bonded pet names
     */
    public static List<String> getBondedPetNames(ServerPlayer player) {
        List<PetData> pets = storedPets.getOrDefault(player.getUUID(), new ArrayList<>());
        List<String> names = new ArrayList<>();
        for (PetData pet : pets) {
            names.add(pet.customName() != null ? pet.customName() : "Unknown Pet");
        }
        return names;
    }
    
    /**
     * Load data on player join
     */
    public static void loadData(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        
        try {
            Path path = getBondDataPath(level);
            if (Files.exists(path)) {
                CompoundTag root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                CompoundTag playerData = root.getCompoundOrEmpty(playerId.toString());
                
                ListTag petList = playerData.getListOrEmpty("pets");
                List<PetData> pets = new ArrayList<>();
                
                for (int i = 0; i < petList.size(); i++) {
                    CompoundTag petTag = petList.getCompoundOrEmpty(i);
                    String entityType = petTag.getStringOr("entityType", "minecraft:wolf");
                    String customName = petTag.contains("customName") ? petTag.getStringOr("customName", null) : null;
                    int bondStrength = petTag.getIntOr("bondStrength", 1);
                    
                    pets.add(new PetData(entityType, customName, bondStrength));
                }
                
                storedPets.put(playerId, pets);
                
                AscendancyMod.LOGGER.info("Loaded {} bonded pets for {}", pets.size(), player.getName().getString());
            }
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to load bond data", e);
        }
    }
    
    /**
     * Save data
     */
    public static void saveData(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        
        try {
            Path path = getBondDataPath(level);
            CompoundTag root;
            
            if (Files.exists(path)) {
                root = NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            } else {
                root = new CompoundTag();
            }
            
            CompoundTag playerData = new CompoundTag();
            List<PetData> pets = storedPets.getOrDefault(playerId, new ArrayList<>());
            
            ListTag petList = new ListTag();
            for (PetData pet : pets) {
                CompoundTag petTag = new CompoundTag();
                petTag.putString("entityType", pet.entityType());
                if (pet.customName() != null) {
                    petTag.putString("customName", pet.customName());
                }
                petTag.putInt("bondStrength", pet.bondStrength());
                petList.add(petTag);
            }
            
            playerData.put("pets", petList);
            root.put(playerId.toString(), playerData);
            
            NbtIo.writeCompressed(root, path);
        } catch (IOException e) {
            AscendancyMod.LOGGER.error("Failed to save bond data", e);
        }
    }
    
    private static Path getBondDataPath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(BOND_FILE);
    }
}
