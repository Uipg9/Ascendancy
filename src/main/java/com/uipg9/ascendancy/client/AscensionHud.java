package com.uipg9.ascendancy.client;

import com.uipg9.ascendancy.AscendancyMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Renders the Soul Energy HUD element.
 * Uses Mojang Official Mappings for 1.21.11
 * 
 * Visual: A vertical progress bar in the bottom-right corner.
 * Behavior: Fades out when empty, glows gold when full.
 */
@Environment(EnvType.CLIENT)
public class AscensionHud {
    
    // Bar dimensions
    private static final int BAR_WIDTH = 8;
    private static final int BAR_HEIGHT = 60;
    private static final int MARGIN_RIGHT = 20;
    private static final int MARGIN_BOTTOM = 80;
    
    // Colors (ARGB)
    private static final int COLOR_BACKGROUND = 0x80000000;      // Semi-transparent black
    private static final int COLOR_BORDER = 0xFF333333;          // Dark gray border
    private static final int COLOR_BAR_EMPTY = 0xFF1A1A1A;       // Very dark gray
    private static final int COLOR_BAR_FILL = 0xFFFFD700;        // Gold
    private static final int COLOR_BAR_GLOW = 0xFFFFFF00;        // Bright yellow
    
    // Animation state
    private static float smoothProgress = 0f;
    private static float glowPulse = 0f;
    
    /**
     * Main render method called by HudRenderCallback
     */
    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        
        float targetProgress = AscendancyClient.getSoulProgress();
        
        // Smooth animation
        smoothProgress = Mth.lerp(0.1f, smoothProgress, targetProgress);
        
        // Don't render if empty and not animating
        if (smoothProgress < 0.001f && targetProgress < 0.001f) return;
        
        // Calculate alpha based on progress (fade in as bar fills)
        float alpha = Math.max(0.3f, smoothProgress);
        
        // Glow pulse when full
        if (AscendancyClient.canAscend()) {
            glowPulse += deltaTracker.getGameTimeDeltaTicks() * 0.1f;
            alpha = 1.0f;
        }
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        
        // Position: bottom-right corner
        int x = screenWidth - MARGIN_RIGHT - BAR_WIDTH;
        int y = screenHeight - MARGIN_BOTTOM - BAR_HEIGHT;
        
        // Draw the soul bar
        drawSoulBar(graphics, x, y, smoothProgress, alpha);
        
        // Draw ready indicator when full
        if (AscendancyClient.canAscend()) {
            drawReadyIndicator(graphics, client, x, y);
        }
    }
    
    /**
     * Draw the vertical soul bar
     */
    private static void drawSoulBar(GuiGraphics graphics, int x, int y, float progress, float alpha) {
        int alphaInt = (int)(alpha * 255) << 24;
        
        // Background with alpha
        int bgColor = (alphaInt & 0xFF000000) | (COLOR_BACKGROUND & 0x00FFFFFF);
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, bgColor);
        
        // Border
        int borderColor = (alphaInt & 0xFF000000) | (COLOR_BORDER & 0x00FFFFFF);
        graphics.renderOutline(x - 1, y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, borderColor);
        
        // Empty bar background
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOR_BAR_EMPTY);
        
        // Filled portion (from bottom up)
        int fillHeight = (int)(BAR_HEIGHT * progress);
        if (fillHeight > 0) {
            int fillY = y + BAR_HEIGHT - fillHeight;
            
            // Glow effect when full
            int fillColor = COLOR_BAR_FILL;
            if (AscendancyClient.canAscend()) {
                float pulse = (float)(Math.sin(glowPulse) * 0.5 + 0.5);
                fillColor = lerpColor(COLOR_BAR_FILL, COLOR_BAR_GLOW, pulse);
            }
            
            graphics.fill(x, fillY, x + BAR_WIDTH, y + BAR_HEIGHT, fillColor);
            
            // Shine effect at the top of the fill
            int shineColor = 0x40FFFFFF;
            graphics.fill(x, fillY, x + BAR_WIDTH, fillY + 2, shineColor);
        }
    }
    
    /**
     * Draw indicator when ready to ascend
     */
    private static void drawReadyIndicator(GuiGraphics graphics, Minecraft client, int barX, int barY) {
        String text = "⬆ P";
        int textWidth = client.font.width(text);
        
        // Pulsing alpha
        float pulse = (float)(Math.sin(glowPulse * 2) * 0.3 + 0.7);
        int alpha = (int)(pulse * 255);
        int color = (alpha << 24) | 0xFFD700;
        
        // Draw above the bar
        int textX = barX + (BAR_WIDTH / 2) - (textWidth / 2);
        int textY = barY - 12;
        
        graphics.drawString(client.font, text, textX, textY, color, true);
    }
    
    /**
     * Linear interpolation between two colors
     */
    private static int lerpColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
