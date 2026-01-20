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
    public static int prestigePoints = 0;
    public static int ascensionCount = 0;
    public static int healthLevel = 0;
    public static int speedLevel = 0;
    public static int reachLevel = 0;
    public static int miningLevel = 0;
    
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
                    // Open ascension screen
                    client.setScreen(new AscensionScreen());
                }
            }
        });
        
        AscendancyMod.LOGGER.info("Ascendancy client initialized!");
    }
    
    /**
     * Update client-side data from server sync
     */
    public static void updateData(int soulXP, int prestigePoints, int ascensionCount,
                                   int healthLevel, int speedLevel, int reachLevel, int miningLevel) {
        AscendancyClient.soulXP = soulXP;
        AscendancyClient.prestigePoints = prestigePoints;
        AscendancyClient.ascensionCount = ascensionCount;
        AscendancyClient.healthLevel = healthLevel;
        AscendancyClient.speedLevel = speedLevel;
        AscendancyClient.reachLevel = reachLevel;
        AscendancyClient.miningLevel = miningLevel;
    }
    
    /**
     * Get the current soul progress as a float 0-1
     */
    public static float getSoulProgress() {
        return (float) soulXP / AscendancyMod.MAX_SOUL_XP;
    }
    
    /**
     * Check if the player is ready to ascend
     */
    public static boolean canAscend() {
        return soulXP >= AscendancyMod.MAX_SOUL_XP;
    }
}
