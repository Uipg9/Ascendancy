package com.uipg9.ascendancy;

import com.uipg9.ascendancy.data.AscendancyAttachments;
import com.uipg9.ascendancy.data.PlayerDataManager;
import com.uipg9.ascendancy.logic.AscensionManager;
import com.uipg9.ascendancy.logic.AttributeHandler;
import com.uipg9.ascendancy.network.AscendancyNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ascendancy Mod - A Vanilla+ RPG Prestige System
 * 
 * Core loop: Play → Gather Soul XP → Ascend → Reset → Get Permanent Upgrades
 */
public class AscendancyMod implements ModInitializer {
    public static final String MOD_ID = "ascendancy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // XP multiplier for Ascension 0 (tutorial mode - fills 5x faster)
    public static final float TUTORIAL_XP_MULTIPLIER = 5.0f;
    
    // Soul XP required to fill the bar
    public static final int MAX_SOUL_XP = 1000;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Ascendancy initializing... Your soul awaits.");
        
        // Register data attachments (Fabric API persistent data)
        AscendancyAttachments.register();
        
        // Register networking packets
        AscendancyNetworking.registerServerPackets();
        
        // Player join event - show tutorial message & apply upgrades
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            
            // Apply saved attribute upgrades
            AttributeHandler.applyUpgrades(player);
            
            // First join message
            if (PlayerDataManager.getAscensionCount(player) == 0 && 
                PlayerDataManager.getSoulXP(player) == 0) {
                player.sendSystemMessage(Component.translatable("gui.ascendancy.first_join"));
            }
            
            // Sync data to client
            AscendancyNetworking.syncToClient(player);
        });
        
        // Player respawn event - reapply upgrades
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            AttributeHandler.applyUpgrades(newPlayer);
            AscendancyNetworking.syncToClient(newPlayer);
        });
        
        // Server tick - accumulate Soul XP from experience
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickSoulXP(player);
            }
        });
        
        LOGGER.info("Ascendancy initialized successfully!");
    }
    
    /**
     * Tick the soul XP accumulation for a player.
     * Soul XP is gained passively based on the player's total experience.
     */
    private void tickSoulXP(ServerPlayer player) {
        // Only tick every 20 ticks (1 second)
        if (player.tickCount % 20 != 0) return;
        
        int currentXP = player.totalExperience;
        int lastKnownXP = PlayerDataManager.getLastKnownXP(player);
        
        if (currentXP > lastKnownXP) {
            int xpGained = currentXP - lastKnownXP;
            
            // Apply tutorial multiplier if ascension 0
            float multiplier = PlayerDataManager.getAscensionCount(player) == 0 
                ? TUTORIAL_XP_MULTIPLIER : 1.0f;
            
            int soulXPGain = (int)(xpGained * multiplier * 0.5f); // 50% of XP becomes Soul XP
            
            int newSoulXP = Math.min(
                PlayerDataManager.getSoulXP(player) + soulXPGain,
                MAX_SOUL_XP
            );
            
            PlayerDataManager.setSoulXP(player, newSoulXP);
            PlayerDataManager.setLastKnownXP(player, currentXP);
            
            // Sync to client for HUD update
            AscendancyNetworking.syncToClient(player);
            
            // Check if ready to ascend
            if (newSoulXP >= MAX_SOUL_XP && !PlayerDataManager.hasBeenNotified(player)) {
                player.displayClientMessage(Component.translatable("gui.ascendancy.ascension_ready"), true);
                PlayerDataManager.setNotified(player, true);
            }
        }
    }
}
