package com.uipg9.ascendancy.client.gui;

import com.uipg9.ascendancy.client.AscendancyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Cinematic loading screen shown during ascension.
 * Creates the illusion of entering a new world.
 * 
 * Hides the actual teleportation and sky-drop from the player.
 */
@Environment(EnvType.CLIENT)
public class AscensionLoadingScreen extends Screen {
    
    // Timing constants (in ticks)
    private static final int FADE_IN_DURATION = 30;      // 1.5 seconds
    private static final int MESSAGE_DURATION = 80;       // 4 seconds for messages
    private static final int TOTAL_DURATION = 200;        // 10 seconds total
    
    // Animation state
    private int ticksOpen = 0;
    private float fadeAlpha = 0f;
    private int currentMessage = 0;
    
    // Mystical messages during ascension
    private static final String[] MESSAGES = {
        "§d✦ Your soul transcends... ✦",
        "§5The old world fades away...",
        "§d✦ A new realm awaits... ✦",
        "§5Reality shifts around you...",
        "§6§l✦ AWAKENING ✦"
    };
    
    // Particle state
    private float particleTime = 0f;
    
    public AscensionLoadingScreen() {
        super(Component.literal("Ascending..."));
    }
    
    @Override
    protected void init() {
        super.init();
        ticksOpen = 0;
        fadeAlpha = 0f;
        currentMessage = 0;
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksOpen++;
        
        // Update current message
        int messageInterval = MESSAGE_DURATION / MESSAGES.length;
        currentMessage = Math.min(ticksOpen / (messageInterval + 8), MESSAGES.length - 1);
        
        // Close screen after duration
        if (ticksOpen >= TOTAL_DURATION) {
            AscendancyClient.ascensionLoadingComplete = true;
            this.minecraft.setScreen(null);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Calculate fade
        float tickProgress = ticksOpen + partialTick;
        
        if (tickProgress < FADE_IN_DURATION) {
            fadeAlpha = tickProgress / FADE_IN_DURATION;
        } else if (tickProgress > TOTAL_DURATION - FADE_IN_DURATION) {
            fadeAlpha = (TOTAL_DURATION - tickProgress) / FADE_IN_DURATION;
        } else {
            fadeAlpha = 1.0f;
        }
        fadeAlpha = Mth.clamp(fadeAlpha, 0f, 1f);
        
        // Solid black background
        int bgAlpha = (int)(fadeAlpha * 255);
        graphics.fill(0, 0, this.width, this.height, (bgAlpha << 24));
        
        if (fadeAlpha < 0.3f) return;
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Animated particle effect (fake stars/souls)
        particleTime += partialTick * 0.05f;
        renderParticles(graphics, centerX, centerY);
        
        // Main message
        String message = MESSAGES[currentMessage];
        int messageAlpha = (int)(fadeAlpha * 255);
        
        // Pulsing effect on final message
        if (currentMessage == MESSAGES.length - 1) {
            float pulse = (float)(Math.sin(tickProgress * 0.2) * 0.3 + 0.7);
            messageAlpha = (int)(pulse * 255);
        }
        
        // Draw message with shadow
        int textWidth = this.font.width(message);
        graphics.drawString(this.font, message, centerX - textWidth / 2, centerY - 10, 
            (messageAlpha << 24) | 0xFFFFFF, true);
        
        // Subtitle with ascension count
        int ascensions = AscendancyClient.ascensionCount + 1;
        String subtitle = "§7Ascension #" + ascensions;
        int subWidth = this.font.width(subtitle);
        int subAlpha = (int)(fadeAlpha * 180);
        graphics.drawString(this.font, subtitle, centerX - subWidth / 2, centerY + 20,
            (subAlpha << 24) | 0xAAAAAA, false);
        
        // Progress bar at bottom
        renderProgressBar(graphics, tickProgress);
        
        // Decorative corners
        renderCorners(graphics);
    }
    
    private void renderParticles(GuiGraphics graphics, int centerX, int centerY) {
        // Draw floating soul particles
        int particleCount = 30;
        int alpha = (int)(fadeAlpha * 100);
        
        for (int i = 0; i < particleCount; i++) {
            float angle = (particleTime + i * 0.5f) * 0.3f;
            float radius = 50 + (float)Math.sin(particleTime * 0.5f + i) * 30 + i * 3;
            
            int px = centerX + (int)(Math.cos(angle) * radius);
            int py = centerY + (int)(Math.sin(angle) * radius * 0.5f); // Elliptical
            
            // Particle size varies
            int size = 1 + (i % 3);
            
            // Color varies between gold and purple
            int color = (i % 2 == 0) ? 0xFFD700 : 0xAA55FF;
            graphics.fill(px - size, py - size, px + size, py + size, (alpha << 24) | color);
        }
    }
    
    private void renderProgressBar(GuiGraphics graphics, float tickProgress) {
        int barWidth = 200;
        int barHeight = 4;
        int barX = (this.width - barWidth) / 2;
        int barY = this.height - 50;
        
        float progress = tickProgress / TOTAL_DURATION;
        int fillWidth = (int)(barWidth * progress);
        
        int alpha = (int)(fadeAlpha * 150);
        
        // Background
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, (alpha << 24) | 0x222222);
        
        // Fill
        if (fillWidth > 0) {
            graphics.fill(barX, barY, barX + fillWidth, barY + barHeight, (alpha << 24) | 0xFFD700);
        }
    }
    
    private void renderCorners(GuiGraphics graphics) {
        int alpha = (int)(fadeAlpha * 100);
        int color = (alpha << 24) | 0xFFD700;
        int size = 20;
        int thickness = 2;
        
        // Top-left
        graphics.fill(10, 10, 10 + size, 10 + thickness, color);
        graphics.fill(10, 10, 10 + thickness, 10 + size, color);
        
        // Top-right
        graphics.fill(this.width - 10 - size, 10, this.width - 10, 10 + thickness, color);
        graphics.fill(this.width - 10 - thickness, 10, this.width - 10, 10 + size, color);
        
        // Bottom-left
        graphics.fill(10, this.height - 10 - thickness, 10 + size, this.height - 10, color);
        graphics.fill(10, this.height - 10 - size, 10 + thickness, this.height - 10, color);
        
        // Bottom-right
        graphics.fill(this.width - 10 - size, this.height - 10 - thickness, this.width - 10, this.height - 10, color);
        graphics.fill(this.width - 10 - thickness, this.height - 10 - size, this.width - 10, this.height - 10, color);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Can't escape the ascension!
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
