package com.uipg9.ascendancy.client;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.client.gui.AscensionScreen;
import com.uipg9.ascendancy.network.AscendancyNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side initialization for Ascendancy.
 * Uses Mojang Official Mappings for 1.21.11
 */
@Environment(EnvType.CLIENT)
public class AscendancyClient implements ClientModInitializer {
    
    // Client-side data cache (synced from server)
    public static int soulXP = 0;
    public static int maxSoulXP = AscendancyMod.BASE_SOUL_XP;
    public static int prestigePoints = 0;
    public static int ascensionCount = 0;
    public static int totalPrestigeEarned = 0;
    
    // XP Popup tracking
    private static int lastSoulXP = 0;
    public static int xpGainedPopup = 0;
    public static long popupStartTime = 0;
    public static final long POPUP_DURATION = 2000; // 2 seconds in ms
    
    // Original upgrades
    public static int healthLevel = 0;
    public static int speedLevel = 0;
    public static int reachLevel = 0;
    public static int miningLevel = 0;
    
    // New upgrade categories
    public static int luckLevel = 0;
    public static int damageLevel = 0;
    public static int defenseLevel = 0;
    public static int experienceLevel = 0;
    
    // v2.1 upgrades
    public static int keeperLevel = 0;
    public static int wisdomLevel = 0;
    
    // v2.2 - Loading screen state
    public static boolean ascensionLoadingComplete = false;
    public static boolean ascensionInProgress = false;
    
    // Keybinding for ascension menu
    private static KeyMapping ascendKey;
    
    // Category registration - 1.21.11 requires Category object
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "main")
    );
    
    @Override
    public void onInitializeClient() {
        AscendancyMod.LOGGER.info("Ascendancy client initializing...");
        
        // Register keybinding
        ascendKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.ascendancy.ascend",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY
        ));
        
        // Register HUD renderer
        HudRenderCallback.EVENT.register(AscensionHud::render);
        
        // Register networking (client receivers)
        AscendancyNetworking.registerClientPackets();
        
        // Handle keybinding
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ascendKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreen(new AscensionScreen());
                }
            }
        });
        
        AscendancyMod.LOGGER.info("Ascendancy client initialized!");
    }
    
    /**
     * Update client-side data from server sync (15 fields for v2.1)
     */
    public static void updateData(int soulXP, int maxSoulXP, int prestigePoints, int ascensionCount,
                                   int totalPrestigeEarned, int healthLevel, int speedLevel, 
                                   int reachLevel, int miningLevel, int luckLevel,
                                   int damageLevel, int defenseLevel, int experienceLevel,
                                   int keeperLevel, int wisdomLevel) {
        // Track XP gains for popup
        int xpGained = soulXP - AscendancyClient.soulXP;
        if (xpGained > 0 && AscendancyClient.soulXP > 0) {
            // Add to current popup or start new one
            if (System.currentTimeMillis() - popupStartTime < POPUP_DURATION) {
                xpGainedPopup += xpGained; // Accumulate
            } else {
                xpGainedPopup = xpGained;
                popupStartTime = System.currentTimeMillis();
            }
        }
        
        AscendancyClient.soulXP = soulXP;
        AscendancyClient.maxSoulXP = maxSoulXP;
        AscendancyClient.prestigePoints = prestigePoints;
        AscendancyClient.ascensionCount = ascensionCount;
        AscendancyClient.totalPrestigeEarned = totalPrestigeEarned;
        AscendancyClient.healthLevel = healthLevel;
        AscendancyClient.speedLevel = speedLevel;
        AscendancyClient.reachLevel = reachLevel;
        AscendancyClient.miningLevel = miningLevel;
        AscendancyClient.luckLevel = luckLevel;
        AscendancyClient.damageLevel = damageLevel;
        AscendancyClient.defenseLevel = defenseLevel;
        AscendancyClient.experienceLevel = experienceLevel;
        AscendancyClient.keeperLevel = keeperLevel;
        AscendancyClient.wisdomLevel = wisdomLevel;
    }
    
    /**
     * Get the current soul progress as a float 0-1
     */
    public static float getSoulProgress() {
        if (maxSoulXP <= 0) return 0f;
        return (float) soulXP / maxSoulXP;
    }
    
    /**
     * Check if the player is ready to ascend
     */
    public static boolean canAscend() {
        return soulXP >= maxSoulXP && maxSoulXP > 0;
    }
    
    /**
     * Get the cost to upgrade a stat
     */
    public static int getUpgradeCost(int currentLevel) {
        return AscendancyMod.getUpgradeCost(currentLevel);
    }
    
    /**
     * Check if player can afford an upgrade
     */
    public static boolean canAfford(int currentLevel) {
        return prestigePoints >= getUpgradeCost(currentLevel);
    }
}
