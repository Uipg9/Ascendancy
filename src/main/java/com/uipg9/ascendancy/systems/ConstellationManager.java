package com.uipg9.ascendancy.systems;

import com.uipg9.ascendancy.AscendancyMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Constellation System - Major perks that define playstyle per run
 * v2.5 - Replayability Expansion
 * 
 * Players choose ONE constellation per life, giving powerful passive effects.
 * Constellations are lost on ascension but remembered for stats tracking.
 */
public class ConstellationManager {
    
    // Track active constellation per player (transient - cleared on ascension)
    private static final Map<UUID, Constellation> playerConstellations = new HashMap<>();
    
    /**
     * Available Constellations with their effects
     */
    public enum Constellation {
        NONE("None", "§7No constellation chosen", 0x888888),
        
        STAR_OF_DEEP(
            "Star of the Deep", 
            "§bNight Vision below Y=0",
            0x3366FF
        ),
        
        STAR_OF_WIND(
            "Star of the Wind", 
            "§a80% fall damage reduction",
            0x99FF99
        ),
        
        STAR_OF_BEAST(
            "Star of the Beast", 
            "§6Mounts heal slowly over time",
            0xFFAA00
        ),
        
        STAR_OF_SEA(
            "Star of the Sea", 
            "§3Infinite water breathing",
            0x00AAFF
        );
        
        private final String displayName;
        private final String description;
        private final int color;
        
        Constellation(String displayName, String description, int color) {
            this.displayName = displayName;
            this.description = description;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getColor() { return color; }
    }
    
    /**
     * Set a player's constellation for this life
     */
    public static void setConstellation(ServerPlayer player, Constellation constellation) {
        if (constellation == Constellation.NONE) {
            playerConstellations.remove(player.getUUID());
        } else {
            playerConstellations.put(player.getUUID(), constellation);
        }
        
        player.sendSystemMessage(Component.literal("§d§l✦ " + constellation.getDisplayName() + " §d§lchosen! ✦"));
        player.sendSystemMessage(Component.literal(constellation.getDescription()));
        
        AscendancyMod.LOGGER.info("Player {} chose constellation: {}", 
            player.getName().getString(), constellation.name());
    }
    
    /**
     * Get a player's active constellation
     */
    public static Constellation getConstellation(ServerPlayer player) {
        return playerConstellations.getOrDefault(player.getUUID(), Constellation.NONE);
    }
    
    /**
     * Clear constellation on ascension
     */
    public static void clearConstellation(ServerPlayer player) {
        Constellation old = playerConstellations.remove(player.getUUID());
        if (old != null && old != Constellation.NONE) {
            AscendancyMod.LOGGER.info("Cleared constellation {} for player {}", 
                old.name(), player.getName().getString());
        }
    }
    
    /**
     * Apply constellation effects every tick
     */
    public static void tickConstellationEffects(ServerPlayer player) {
        Constellation constellation = getConstellation(player);
        if (constellation == Constellation.NONE) return;
        
        switch (constellation) {
            case STAR_OF_DEEP -> tickDeep(player);
            case STAR_OF_WIND -> {} // Handled by damage event
            case STAR_OF_BEAST -> tickBeast(player);
            case STAR_OF_SEA -> tickSea(player);
            default -> {}
        }
    }
    
    /**
     * Star of the Deep - Night vision when below Y=0
     */
    private static void tickDeep(ServerPlayer player) {
        if (player.getY() < 0) {
            // Add/refresh night vision (5 seconds to avoid flickering)
            if (!player.hasEffect(MobEffects.NIGHT_VISION) || 
                player.getEffect(MobEffects.NIGHT_VISION).getDuration() < 100) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 
                    200, // 10 seconds
                    0, 
                    true, // ambient
                    false, // no particles
                    true // show icon
                ));
            }
        }
    }
    
    /**
     * Star of the Beast - Heal mounts over time
     */
    private static void tickBeast(ServerPlayer player) {
        // Check every 5 seconds
        if (player.tickCount % 100 != 0) return;
        
        // Heal any living entity mount (horses, pigs, camels, etc.)
        if (player.getVehicle() instanceof LivingEntity mount) {
            if (mount.getHealth() < mount.getMaxHealth()) {
                mount.heal(1.0f); // Half heart every 5 seconds
            }
        }
    }
    
    /**
     * Star of the Sea - Infinite water breathing
     */
    private static void tickSea(ServerPlayer player) {
        // Keep air at max when underwater
        if (player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply()) {
            player.setAirSupply(player.getMaxAirSupply());
        }
        
        // Also give water breathing effect for visual
        if (player.isUnderWater()) {
            if (!player.hasEffect(MobEffects.WATER_BREATHING) ||
                player.getEffect(MobEffects.WATER_BREATHING).getDuration() < 100) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.WATER_BREATHING,
                    200,
                    0,
                    true,
                    false,
                    true
                ));
            }
        }
    }
    
    /**
     * Calculate fall damage reduction for Star of the Wind
     * Call this from a damage handler
     */
    public static float modifyFallDamage(ServerPlayer player, float damage) {
        if (getConstellation(player) == Constellation.STAR_OF_WIND) {
            return damage * 0.2f; // 80% reduction
        }
        return damage;
    }
    
    /**
     * Check if player has a specific constellation
     */
    public static boolean hasConstellation(ServerPlayer player, Constellation constellation) {
        return getConstellation(player) == constellation;
    }
}
