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
 * v2.4 - VERTICAL bar on the LEFT CENTER of the screen
 * Now with floating +XP popups when gaining Soul XP!
 * 
 * ALWAYS VISIBLE - shows player's ascension progress
 */
@Environment(EnvType.CLIENT)
public class AscensionHud {
    
    // Bar dimensions - VERTICAL orientation
    private static final int BAR_WIDTH = 8;    // Thin bar
    private static final int BAR_HEIGHT = 80;  // Tall bar
    private static final int MARGIN_LEFT = 8;  // Slight margin from screen edge
    
    // Colors (ARGB)
    private static final int COLOR_BACKGROUND = 0xDD000000;      // Almost opaque black
    private static final int COLOR_BORDER = 0xFF555555;          // Medium gray border
    private static final int COLOR_BORDER_GLOW = 0xFFAA8800;     // Gold border when ready
    private static final int COLOR_BAR_EMPTY = 0xFF1A1A2E;       // Dark purple-ish
    private static final int COLOR_BAR_FILL = 0xFFFFD700;        // Gold
    private static final int COLOR_BAR_GLOW = 0xFFFFFF00;        // Bright yellow
    private static final int COLOR_TEXT = 0xFFFFFFFF;            // White
    private static final int COLOR_READY = 0xFF00FF00;           // Green
    private static final int COLOR_GOLD_TEXT = 0xFFFFD700;       // Gold text
    
    // Animation state
    private static float smoothProgress = 0f;
    private static float glowPulse = 0f;
    
    /**
     * Main render method called by HudRenderCallback
     */
    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        
        // Don't render while a screen is open (except inventory)
        if (client.screen != null && !(client.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) {
            return;
        }
        
        float targetProgress = AscendancyClient.getSoulProgress();
        
        // Smooth animation
        smoothProgress = Mth.lerp(0.15f, smoothProgress, targetProgress);
        
        // Update glow pulse
        glowPulse += deltaTracker.getGameTimeDeltaTicks() * 0.1f;
        
        // Position: LEFT CENTER of screen
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int x = MARGIN_LEFT;
        int y = (screenHeight / 2) - (BAR_HEIGHT / 2);
        
        // Draw the vertical soul bar (ALWAYS)
        drawSoulBar(graphics, x, y, smoothProgress);
        
        // Draw "SOUL" label above the bar (vertical)
        drawLabel(graphics, client, x, y);
        
        // Draw percentage below the bar
        drawPercentage(graphics, client, x, y);
        
        // Draw ready indicator when full
        if (AscendancyClient.canAscend()) {
            drawReadyIndicator(graphics, client, x, y);
        }
        
        // Draw XP popup when gaining XP
        drawXpPopup(graphics, client, x, y);
    }
    
    /**
     * Draw floating +XP popup
     */
    private static void drawXpPopup(GuiGraphics graphics, Minecraft client, int barX, int barY) {
        long elapsed = System.currentTimeMillis() - AscendancyClient.popupStartTime;
        if (elapsed > AscendancyClient.POPUP_DURATION || AscendancyClient.xpGainedPopup <= 0) {
            return;
        }
        
        // Calculate fade and float animation
        float progress = (float) elapsed / AscendancyClient.POPUP_DURATION;
        float alpha = 1.0f - (progress * progress); // Fade out faster at end
        float floatOffset = progress * 30f; // Float upward
        
        if (alpha < 0.05f) return;
        
        // Draw the popup
        String text = "§a+" + AscendancyClient.xpGainedPopup + " XP";
        int textWidth = client.font.width(text.replace("§a", "")); // Remove color code for width
        
        int textX = barX + BAR_WIDTH + 8;
        int textY = (int)(barY + (BAR_HEIGHT / 2) - 4 - floatOffset);
        
        int alphaInt = (int)(alpha * 255);
        int color = (alphaInt << 24) | 0x55FF55; // Green with alpha
        
        // Background for better readability
        int bgAlpha = (int)(alpha * 160);
        graphics.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 10, (bgAlpha << 24));
        
        // Draw text
        graphics.drawString(client.font, "+" + AscendancyClient.xpGainedPopup + " XP", textX, textY, color, true);
    }
    
    /**
     * Draw the VERTICAL soul bar
     * Fills from BOTTOM to TOP (like filling a container)
     */
    private static void drawSoulBar(GuiGraphics graphics, int x, int y, float progress) {
        int barX = x;
        int barY = y;
        
        // Background panel (slightly larger for padding)
        graphics.fill(barX - 3, barY - 3, barX + BAR_WIDTH + 3, barY + BAR_HEIGHT + 3, COLOR_BACKGROUND);
        
        // Border (glows gold when ready)
        int borderColor = AscendancyClient.canAscend() ? 
            lerpColor(COLOR_BORDER, COLOR_BORDER_GLOW, (float)(Math.sin(glowPulse) * 0.5 + 0.5)) : 
            COLOR_BORDER;
        graphics.renderOutline(barX - 3, barY - 3, BAR_WIDTH + 6, BAR_HEIGHT + 6, borderColor);
        
        // Empty bar background
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, COLOR_BAR_EMPTY);
        
        // Filled portion (from BOTTOM to TOP)
        int fillHeight = (int)(BAR_HEIGHT * progress);
        if (fillHeight > 0) {
            int fillStartY = barY + BAR_HEIGHT - fillHeight;
            
            // Glow effect when full
            int fillColor = COLOR_BAR_FILL;
            if (AscendancyClient.canAscend()) {
                float pulse = (float)(Math.sin(glowPulse) * 0.5 + 0.5);
                fillColor = lerpColor(COLOR_BAR_FILL, COLOR_BAR_GLOW, pulse);
            }
            
            graphics.fill(barX, fillStartY, barX + BAR_WIDTH, barY + BAR_HEIGHT, fillColor);
            
            // Shine effect at the left edge of the fill
            int shineColor = 0x40FFFFFF;
            graphics.fill(barX, fillStartY, barX + 2, barY + BAR_HEIGHT, shineColor);
            
            // Top edge highlight (where the liquid level is)
            if (progress < 1.0f) {
                graphics.fill(barX, fillStartY, barX + BAR_WIDTH, fillStartY + 1, 0xFFFFFFFF);
            }
        }
        
        // Inner border for depth
        graphics.renderOutline(barX - 1, barY - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, 0xFF333333);
        
        // Decorative corner marks
        drawCornerMarks(graphics, barX - 3, barY - 3, BAR_WIDTH + 6, BAR_HEIGHT + 6);
    }
    
    /**
     * Draw decorative corner marks
     */
    private static void drawCornerMarks(GuiGraphics graphics, int x, int y, int width, int height) {
        int cornerColor = 0xFFAA8800; // Gold
        int cornerLength = 4;
        
        // Top-left
        graphics.fill(x, y, x + cornerLength, y + 1, cornerColor);
        graphics.fill(x, y, x + 1, y + cornerLength, cornerColor);
        
        // Top-right
        graphics.fill(x + width - cornerLength, y, x + width, y + 1, cornerColor);
        graphics.fill(x + width - 1, y, x + width, y + cornerLength, cornerColor);
        
        // Bottom-left
        graphics.fill(x, y + height - 1, x + cornerLength, y + height, cornerColor);
        graphics.fill(x, y + height - cornerLength, x + 1, y + height, cornerColor);
        
        // Bottom-right
        graphics.fill(x + width - cornerLength, y + height - 1, x + width, y + height, cornerColor);
        graphics.fill(x + width - 1, y + height - cornerLength, x + width, y + height, cornerColor);
    }
    
    /**
     * Draw the "SOUL" label above the bar
     */
    private static void drawLabel(GuiGraphics graphics, Minecraft client, int barX, int barY) {
        String text = "✦";
        int textWidth = client.font.width(text);
        int textX = barX + (BAR_WIDTH / 2) - (textWidth / 2);
        int textY = barY - 12;
        
        graphics.drawString(client.font, text, textX, textY, COLOR_GOLD_TEXT, true);
    }
    
    /**
     * Draw percentage below the bar
     */
    private static void drawPercentage(GuiGraphics graphics, Minecraft client, int barX, int barY) {
        int percent = (int)(AscendancyClient.getSoulProgress() * 100);
        String text = percent + "%";
        int textWidth = client.font.width(text);
        int textX = barX + (BAR_WIDTH / 2) - (textWidth / 2);
        int textY = barY + BAR_HEIGHT + 5;
        
        int color = AscendancyClient.canAscend() ? COLOR_READY : COLOR_TEXT;
        graphics.drawString(client.font, text, textX, textY, color, true);
    }
    
    /**
     * Draw indicator when ready to ascend
     */
    private static void drawReadyIndicator(GuiGraphics graphics, Minecraft client, int barX, int barY) {
        String text = "[P]";
        int textWidth = client.font.width(text);
        
        // Pulsing alpha
        float pulse = (float)(Math.sin(glowPulse * 2) * 0.3 + 0.7);
        int alpha = (int)(pulse * 255);
        int color = (alpha << 24) | 0x00FF00;
        
        // Draw below the percentage
        int textX = barX + (BAR_WIDTH / 2) - (textWidth / 2);
        int textY = barY + BAR_HEIGHT + 16;
        
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
