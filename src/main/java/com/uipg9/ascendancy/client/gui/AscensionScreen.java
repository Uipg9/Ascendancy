package com.uipg9.ascendancy.client.gui;

import com.uipg9.ascendancy.client.AscendancyClient;
import com.uipg9.ascendancy.network.AscendancyNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * The Ascension Screen - view stats, purchase upgrades, ascend, and access guide.
 * Features infinite upgrades with scaling costs, 8 upgrade categories, sound effects.
 */
@Environment(EnvType.CLIENT)
public class AscensionScreen extends Screen {
    
    // Colors
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFAAAAAA;
    private static final int COLOR_DARK_BG = 0xD0101010;
    private static final int COLOR_PANEL_BG = 0xF0181818;
    private static final int COLOR_BORDER = 0xFF404040;
    private static final int COLOR_GREEN = 0xFF55FF55;
    private static final int COLOR_RED = 0xFFFF5555;
    private static final int COLOR_CYAN = 0xFF55FFFF;
    private static final int COLOR_PURPLE = 0xFFFF55FF;
    
    // Layout
    private int centerX;
    private int centerY;
    private int panelWidth = 380;
    private int panelHeight = 300;
    
    // Animation
    private float animProgress = 0;
    private boolean animationComplete = false;
    private long animationStartTime = 0;
    private static final long ANIMATION_DELAY_MS = 500; // 0.5 seconds before buttons enabled
    
    // Category tabs
    private int selectedCategory = 0;
    private static final String[] CATEGORIES = {"Combat", "Utility", "Special"};
    
    public AscensionScreen() {
        super(Component.translatable("gui.ascendancy.shop_title"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Track animation start time if not already set
        if (animationStartTime == 0) {
            animationStartTime = System.currentTimeMillis();
        }
        
        centerX = this.width / 2;
        centerY = this.height / 2;
        
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;
        
        // Guide button (top-right, book icon)
        Button guideBtn = Button.builder(
            Component.literal("📖 Guide"),
            button -> {
                playClickSound();
                this.minecraft.setScreen(new GuideScreen());
            }
        ).bounds(panelX + panelWidth - 70, panelY + 8, 60, 16).build();
        guideBtn.active = animationComplete; // Disabled during animation
        this.addRenderableWidget(guideBtn);
        
        // Category tabs
        int tabX = panelX + 10;
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int cat = i;
            Button tabBtn = Button.builder(
                Component.literal(CATEGORIES[i]),
                b -> {
                    playClickSound();
                    selectedCategory = cat;
                    rebuildWidgets();
                }
            ).bounds(tabX + (i * 75), panelY + 45, 70, 18).build();
            tabBtn.active = animationComplete; // Disabled during animation
            this.addRenderableWidget(tabBtn);
        }
        
        // Upgrade buttons based on category
        int startY = panelY + 75;
        int spacing = 34;
        
        switch (selectedCategory) {
            case 0 -> { // Combat
                addUpgradeButton(startY, "§c❤ Vitality", "+2 Hearts", AscendancyClient.healthLevel, 
                    AscendancyNetworking.PurchaseUpgradePayload.VITALITY);
                addUpgradeButton(startY + spacing, "§c⚔ Might", "+5% Damage", AscendancyClient.damageLevel,
                    AscendancyNetworking.PurchaseUpgradePayload.DAMAGE);
                addUpgradeButton(startY + spacing * 2, "§c🛡 Resilience", "+1 Armor", AscendancyClient.defenseLevel,
                    AscendancyNetworking.PurchaseUpgradePayload.DEFENSE);
            }
            case 1 -> { // Utility
                addUpgradeButton(startY, "§a⚡ Swiftness", "+3% Speed", AscendancyClient.speedLevel,
                    AscendancyNetworking.PurchaseUpgradePayload.SWIFTNESS);
                addUpgradeButton(startY + spacing, "§a✋ Titan's Reach", "+0.5 Reach", AscendancyClient.reachLevel,
                    AscendancyNetworking.PurchaseUpgradePayload.REACH);
                addUpgradeButton(startY + spacing * 2, "§a⛏ Haste", "+8% Mining", AscendancyClient.miningLevel,
                    AscendancyNetworking.PurchaseUpgradePayload.HASTE);
            }
            case 2 -> { // Special
                addUpgradeButton(startY, "§d🍀 Fortune", "+5% Luck", AscendancyClient.luckLevel,
                    AscendancyNetworking.PurchaseUpgradePayload.LUCK);
                addUpgradeButton(startY + spacing, "§d✦ Wisdom", "+10% Soul XP", AscendancyClient.wisdomLevel,
                    AscendancyNetworking.PurchaseUpgradePayload.WISDOM);
                addUpgradeButton(startY + spacing * 2, "§d📦 Keeper", "+1 Keep Amt", AscendancyClient.keeperLevel,
                    AscendancyNetworking.PurchaseUpgradePayload.KEEPER);
            }
        }
        
        // Ascend button (bottom, only if ready) - NOW OPENS ITEM SELECTION
        if (AscendancyClient.canAscend()) {
            Button ascendBtn = Button.builder(
                Component.literal("✦ ASCEND ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                button -> {
                    playAscendSound();
                    this.minecraft.setScreen(new ItemSelectionScreen());
                }
            ).bounds(centerX - 70, panelY + panelHeight - 40, 140, 28).build();
            ascendBtn.active = animationComplete; // Disabled during animation
            this.addRenderableWidget(ascendBtn);
        }
    }
    
    private void addUpgradeButton(int y, String name, String effect, int currentLevel, int upgradeType) {
        int panelX = centerX - panelWidth / 2;
        int cost = AscendancyClient.getUpgradeCost(currentLevel);
        boolean canAfford = AscendancyClient.canAfford(currentLevel);
        
        String buttonText = cost + " pts";
        
        Button button = Button.builder(
            Component.literal(buttonText),
            btn -> {
                if (canAfford) {
                    playPurchaseSound();
                    AscendancyNetworking.sendPurchaseRequest(upgradeType);
                    // Delay rebuild to let server sync
                    Minecraft.getInstance().execute(() -> this.rebuildWidgets());
                }
            }
        ).bounds(panelX + panelWidth - 80, y + 5, 65, 20).build();
        
        // Only active if animation complete AND can afford
        button.active = animationComplete && canAfford;
        this.addRenderableWidget(button);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Animation
        animProgress = Math.min(1.0f, animProgress + delta * 0.1f);
        float ease = 1.0f - (float)Math.pow(1.0f - animProgress, 3);
        
        // Check if animation delay has passed
        boolean wasAnimationComplete = animationComplete;
        animationComplete = (System.currentTimeMillis() - animationStartTime) > ANIMATION_DELAY_MS;
        
        // Enable/disable all widgets based on animation state
        if (!wasAnimationComplete && animationComplete) {
            // Animation just finished - enable all widgets
            for (var child : this.children()) {
                if (child instanceof Button btn) {
                    btn.active = true;
                }
            }
            rebuildWidgets(); // Rebuild to apply correct active states based on affordability
        }
        
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;
        
        // Dark background overlay
        graphics.fill(0, 0, this.width, this.height, COLOR_DARK_BG);
        
        // Main panel with animation
        int animOffset = (int)((1.0f - ease) * 20);
        int renderY = panelY + animOffset;
        int alpha = (int)(ease * 255);
        
        // Panel background
        graphics.fill(panelX, renderY, panelX + panelWidth, renderY + panelHeight, COLOR_PANEL_BG);
        graphics.renderOutline(panelX, renderY, panelWidth, panelHeight, COLOR_BORDER);
        
        // Gold accent line
        graphics.fill(panelX + 1, renderY + 1, panelX + panelWidth - 1, renderY + 3, COLOR_GOLD);
        
        // Title
        Component title = Component.literal("✦ Ascension ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        graphics.drawCenteredString(this.font, title, centerX, renderY + 10, COLOR_GOLD);
        
        // Stats section
        int statsY = renderY + 28;
        drawStatsSection(graphics, panelX, statsY);
        
        // Upgrade section header
        graphics.drawString(this.font, 
            Component.literal("§7§nUpgrades (" + CATEGORIES[selectedCategory] + ")"),
            panelX + 10, renderY + 65, COLOR_GRAY, false);
        
        // Upgrade labels
        int startY = renderY + 75;
        int spacing = 34;
        
        switch (selectedCategory) {
            case 0 -> {
                drawUpgradeRow(graphics, panelX + 15, startY, "§c❤ Vitality", "+2 Hearts/lvl", AscendancyClient.healthLevel);
                drawUpgradeRow(graphics, panelX + 15, startY + spacing, "§c⚔ Might", "+5% Damage/lvl", AscendancyClient.damageLevel);
                drawUpgradeRow(graphics, panelX + 15, startY + spacing * 2, "§c🛡 Resilience", "+1 Armor/lvl", AscendancyClient.defenseLevel);
            }
            case 1 -> {
                drawUpgradeRow(graphics, panelX + 15, startY, "§a⚡ Swiftness", "+3% Speed/lvl", AscendancyClient.speedLevel);
                drawUpgradeRow(graphics, panelX + 15, startY + spacing, "§a✋ Titan's Reach", "+0.5 Reach/lvl", AscendancyClient.reachLevel);
                drawUpgradeRow(graphics, panelX + 15, startY + spacing * 2, "§a⛏ Haste", "+8% Mining/lvl", AscendancyClient.miningLevel);
            }
            case 2 -> {
                drawUpgradeRow(graphics, panelX + 15, startY, "§d🍀 Fortune", "+5% Luck/lvl", AscendancyClient.luckLevel);
                drawUpgradeRow(graphics, panelX + 15, startY + spacing, "§d✦ Wisdom", "+10% Soul XP/lvl", AscendancyClient.wisdomLevel);
                drawUpgradeRow(graphics, panelX + 15, startY + spacing * 2, "§d📦 Keeper", "+1 Keep Amount", AscendancyClient.keeperLevel);
            }
        }
        
        // Soul progress bar at bottom
        drawSoulProgressBar(graphics, panelX, renderY + panelHeight - 65);
        
        // Draw widgets
        super.render(graphics, mouseX, mouseY, delta);
    }
    
    private void drawStatsSection(GuiGraphics graphics, int panelX, int y) {
        // Prestige Points
        graphics.drawString(this.font,
            Component.literal("§ePrestige Points: §f" + AscendancyClient.prestigePoints),
            panelX + 10, y, COLOR_WHITE, true);
        
        // Ascension Count
        graphics.drawString(this.font,
            Component.literal("§7Ascensions: §f" + AscendancyClient.ascensionCount),
            panelX + 180, y, COLOR_GRAY, true);
        
        // Next reward preview
        int nextReward = 5 + AscendancyClient.ascensionCount;
        graphics.drawString(this.font,
            Component.literal("§8Next Ascend: +" + nextReward + " pts"),
            panelX + 280, y, 0xFF666666, true);
    }
    
    private void drawUpgradeRow(GuiGraphics graphics, int x, int y, String name, String effect, int level) {
        // Name
        graphics.drawString(this.font, name, x, y + 3, COLOR_WHITE, false);
        
        // Level
        String levelStr = "Lv." + level;
        graphics.drawString(this.font, levelStr, x + 100, y + 3, COLOR_CYAN, true);
        
        // Effect
        graphics.drawString(this.font, "§8" + effect, x, y + 15, 0xFF555555, false);
        
        // Cost for next level
        int cost = AscendancyClient.getUpgradeCost(level);
        boolean canAfford = AscendancyClient.canAfford(level);
        int costColor = canAfford ? COLOR_GREEN : COLOR_RED;
        graphics.drawString(this.font, "§7Next: §f" + cost + " pts", x + 140, y + 15, costColor, false);
    }
    
    private void drawSoulProgressBar(GuiGraphics graphics, int panelX, int y) {
        int barX = panelX + 20;
        int barWidth = panelWidth - 40;
        int barHeight = 16;
        
        float progress = AscendancyClient.getSoulProgress();
        int percent = (int)(progress * 100);
        
        // Background
        graphics.fill(barX, y, barX + barWidth, y + barHeight, 0xFF0A0A15);
        graphics.renderOutline(barX, y, barWidth, barHeight, 0xFF333355);
        
        // Filled portion
        int fillWidth = (int)(barWidth * progress);
        if (fillWidth > 0) {
            int fillColor = AscendancyClient.canAscend() ? COLOR_GREEN : COLOR_GOLD;
            graphics.fill(barX + 1, y + 1, barX + fillWidth - 1, y + barHeight - 1, fillColor);
        }
        
        // Text
        String text = "Soul Energy: " + percent + "% (" + AscendancyClient.soulXP + "/" + AscendancyClient.maxSoulXP + ")";
        graphics.drawCenteredString(this.font, text, centerX, y + 4, COLOR_WHITE);
        
        // Ready message
        if (AscendancyClient.canAscend()) {
            graphics.drawCenteredString(this.font, 
                Component.literal("§a§l✦ Ready to Ascend! ✦"),
                centerX, y + barHeight + 5, COLOR_GREEN);
        }
    }
    
    // Sound effects
    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );
    }
    
    private void playPurchaseSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f, 0.8f)
        );
    }
    
    private void playAscendSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f, 0.5f)
        );
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.TOTEM_USE, 0.5f, 1.5f)
        );
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
