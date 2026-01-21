package com.uipg9.ascendancy.client.gui;

import com.uipg9.ascendancy.network.AscendancyNetworking;
import com.uipg9.ascendancy.systems.ConstellationManager.Constellation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Constellation Selection Screen - Choose a major perk for this life
 * v2.5 - Replayability Expansion
 */
@Environment(EnvType.CLIENT)
public class ConstellationSelectionScreen extends Screen {
    
    // Colors
    private static final int COLOR_BG = 0xE0080818;
    private static final int COLOR_PANEL = 0xF0101030;
    private static final int COLOR_BORDER = 0xFF3366FF;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_PURPLE = 0xFFAA55FF;
    
    // Selected constellation
    private Constellation selectedConstellation = Constellation.NONE;
    
    // Layout
    private int centerX;
    private int centerY;
    private int panelWidth = 360;
    private int panelHeight = 280;
    
    public ConstellationSelectionScreen() {
        super(Component.literal("Choose Your Constellation"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        centerX = this.width / 2;
        centerY = this.height / 2;
        
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;
        
        // Create constellation buttons
        int buttonY = panelY + 70;
        int buttonSpacing = 48;
        
        // Get the 4 actual constellations (skip NONE)
        Constellation[] constellations = {
            Constellation.STAR_OF_DEEP,
            Constellation.STAR_OF_WIND,
            Constellation.STAR_OF_BEAST,
            Constellation.STAR_OF_SEA
        };
        
        for (int i = 0; i < constellations.length; i++) {
            final Constellation constellation = constellations[i];
            int y = buttonY + (i * buttonSpacing);
            
            Button btn = Button.builder(
                Component.literal(getConstellationIcon(constellation) + " " + constellation.getDisplayName()),
                button -> {
                    playClickSound();
                    selectedConstellation = constellation;
                    rebuildWidgets(); // Refresh to show selection
                }
            ).bounds(panelX + 30, y, 200, 20).build();
            this.addRenderableWidget(btn);
        }
        
        // Skip button (choose no constellation)
        Button skipBtn = Button.builder(
            Component.literal("§7Skip (No Constellation)"),
            button -> {
                playClickSound();
                confirmSelection(Constellation.NONE);
            }
        ).bounds(panelX + 30, buttonY + (4 * buttonSpacing), 140, 20).build();
        this.addRenderableWidget(skipBtn);
        
        // Confirm button
        Button confirmBtn = Button.builder(
            Component.literal("§a✓ Confirm Selection"),
            button -> {
                playClickSound();
                confirmSelection(selectedConstellation);
            }
        ).bounds(panelX + panelWidth - 150, panelY + panelHeight - 35, 120, 20).build();
        confirmBtn.active = selectedConstellation != Constellation.NONE;
        this.addRenderableWidget(confirmBtn);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Render dark starry background
        renderBackground(graphics, mouseX, mouseY, delta);
        
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;
        
        // Main panel
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL);
        drawBorder(graphics, panelX, panelY, panelWidth, panelHeight, COLOR_BORDER);
        
        // Title
        graphics.drawCenteredString(font, "§d§l✦ Choose Your Constellation ✦", centerX, panelY + 15, COLOR_WHITE);
        graphics.drawCenteredString(font, "§7A constellation guides you through this life", centerX, panelY + 30, 0xFFAAAAAA);
        graphics.drawCenteredString(font, "§8(Lost on Ascension)", centerX, panelY + 42, 0xFF666666);
        
        // Render selection indicator and description
        if (selectedConstellation != Constellation.NONE) {
            int descY = panelY + panelHeight - 70;
            graphics.drawCenteredString(font, "§e§lSelected: " + selectedConstellation.getDisplayName(), centerX, descY, COLOR_GOLD);
            graphics.drawCenteredString(font, selectedConstellation.getDescription(), centerX, descY + 15, COLOR_WHITE);
        }
        
        // Draw constellation descriptions on the right side
        int buttonY = panelY + 70;
        int buttonSpacing = 48;
        Constellation[] constellations = {
            Constellation.STAR_OF_DEEP,
            Constellation.STAR_OF_WIND,
            Constellation.STAR_OF_BEAST,
            Constellation.STAR_OF_SEA
        };
        
        for (int i = 0; i < constellations.length; i++) {
            Constellation c = constellations[i];
            int y = buttonY + (i * buttonSpacing);
            
            // Description text to the right of button
            String desc = c.getDescription().replace("§b", "").replace("§a", "").replace("§6", "").replace("§3", "");
            graphics.drawString(font, "§7" + desc, panelX + 240, y + 6, 0xFFAAAAAA);
            
            // Selection indicator
            if (selectedConstellation == c) {
                graphics.drawString(font, "§e►", panelX + 15, y + 6, COLOR_GOLD);
            }
        }
        
        // Draw decorative stars
        drawStars(graphics, panelX, panelY);
        
        super.render(graphics, mouseX, mouseY, delta);
    }
    
    private void drawStars(GuiGraphics graphics, int panelX, int panelY) {
        // Simple star decorations
        long time = System.currentTimeMillis();
        float twinkle = (float) Math.sin(time / 500.0) * 0.3f + 0.7f;
        
        int starColor = ((int)(255 * twinkle) << 24) | 0xFFFFFF;
        
        // Draw a few stars around the panel
        graphics.drawString(font, "✦", panelX + 5, panelY + 5, starColor);
        graphics.drawString(font, "✦", panelX + panelWidth - 15, panelY + 5, starColor);
        graphics.drawString(font, "⭐", panelX + panelWidth / 2, panelY + panelHeight - 95, starColor);
    }
    
    private String getConstellationIcon(Constellation c) {
        return switch (c) {
            case STAR_OF_DEEP -> "§b⬇";
            case STAR_OF_WIND -> "§a↑";
            case STAR_OF_BEAST -> "§6🐎";
            case STAR_OF_SEA -> "§3~";
            default -> "§7○";
        };
    }
    
    private void confirmSelection(Constellation constellation) {
        // Send to server
        ClientPlayNetworking.send(new AscendancyNetworking.SelectConstellationPayload(constellation.ordinal()));
        
        // Play sound and close
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2f, 1.0f)
        );
        
        this.onClose();
    }
    
    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
    
    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
