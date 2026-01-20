package com.uipg9.ascendancy.client.gui;

import com.uipg9.ascendancy.client.AscendancyClient;
import com.uipg9.ascendancy.data.PlayerDataManager;
import com.uipg9.ascendancy.network.AscendancyNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Ascension Screen - allows players to view stats, purchase upgrades, and ascend.
 * Uses Mojang Official Mappings and pure DrawContext API per GUI_BEST_PRACTICES.md
 */
@Environment(EnvType.CLIENT)
public class AscensionScreen extends Screen {
    
    // Colors
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFAAAAAA;
    private static final int COLOR_DARK_BG = 0xC0101010;
    private static final int COLOR_PANEL_BG = 0xE0202020;
    private static final int COLOR_BORDER = 0xFF404040;
    
    // Layout
    private int centerX;
    private int centerY;
    private int panelWidth = 300;
    private int panelHeight = 250;
    
    public AscensionScreen() {
        super(Component.translatable("gui.ascendancy.shop_title"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        centerX = this.width / 2;
        centerY = this.height / 2;
        
        int startY = centerY - 60;
        int spacing = 28;
        
        // Upgrade buttons
        addUpgradeButton(startY, "Vitality", AscendancyClient.healthLevel, 
            AscendancyNetworking.PurchaseUpgradePayload.VITALITY);
        addUpgradeButton(startY + spacing, "Swiftness", AscendancyClient.speedLevel,
            AscendancyNetworking.PurchaseUpgradePayload.SWIFTNESS);
        addUpgradeButton(startY + spacing * 2, "Titan's Reach", AscendancyClient.reachLevel,
            AscendancyNetworking.PurchaseUpgradePayload.REACH);
        addUpgradeButton(startY + spacing * 3, "Haste", AscendancyClient.miningLevel,
            AscendancyNetworking.PurchaseUpgradePayload.HASTE);
        
        // Ascend button (only shown when ready)
        if (AscendancyClient.canAscend()) {
            this.addRenderableWidget(Button.builder(
                Component.literal("✦ ASCEND ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                button -> {
                    AscendancyNetworking.sendAscendRequest();
                    this.onClose();
                }
            ).bounds(centerX - 60, centerY + 80, 120, 25).build());
        }
    }
    
    private void addUpgradeButton(int y, String name, int currentLevel, int upgradeType) {
        boolean isMaxed = currentLevel >= PlayerDataManager.MAX_UPGRADE_LEVEL;
        int cost = isMaxed ? 0 : getUpgradeCost(currentLevel);
        boolean canAfford = AscendancyClient.prestigePoints >= cost && !isMaxed;
        
        String buttonText = isMaxed ? "MAX" : "Buy (" + cost + " pts)";
        
        Button button = Button.builder(
            Component.literal(buttonText),
            btn -> {
                if (!isMaxed) {
                    AscendancyNetworking.sendPurchaseRequest(upgradeType);
                    // Refresh screen
                    this.rebuildWidgets();
                }
            }
        ).bounds(centerX + 30, y, 80, 20).build();
        
        button.active = canAfford;
        this.addRenderableWidget(button);
    }
    
    private int getUpgradeCost(int currentLevel) {
        return currentLevel + 1;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Dark background
        graphics.fill(0, 0, this.width, this.height, COLOR_DARK_BG);
        
        // Main panel
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;
        
        // Panel background with border
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL_BG);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, COLOR_BORDER);
        
        // Gold accent line at top
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 3, COLOR_GOLD);
        
        // Title
        Component title = Component.literal("✦ Ascension Upgrades ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        graphics.drawCenteredString(this.font, title, centerX, panelY + 12, COLOR_GOLD);
        
        // Stats display
        int statsY = panelY + 30;
        graphics.drawString(this.font, 
            Component.literal("Prestige Points: " + AscendancyClient.prestigePoints).withStyle(ChatFormatting.YELLOW),
            panelX + 10, statsY, COLOR_WHITE, true);
        graphics.drawString(this.font,
            Component.literal("Ascension Count: " + AscendancyClient.ascensionCount).withStyle(ChatFormatting.GRAY),
            panelX + 10, statsY + 12, COLOR_GRAY, true);
        
        // Upgrade labels
        int startY = centerY - 60;
        int spacing = 28;
        
        drawUpgradeLabel(graphics, panelX + 10, startY, "Vitality", AscendancyClient.healthLevel, "+2♥");
        drawUpgradeLabel(graphics, panelX + 10, startY + spacing, "Swiftness", AscendancyClient.speedLevel, "+5%");
        drawUpgradeLabel(graphics, panelX + 10, startY + spacing * 2, "Titan's Reach", AscendancyClient.reachLevel, "+1m");
        drawUpgradeLabel(graphics, panelX + 10, startY + spacing * 3, "Haste", AscendancyClient.miningLevel, "+10%");
        
        // Soul bar progress
        if (!AscendancyClient.canAscend()) {
            int soulProgress = (int)(AscendancyClient.getSoulProgress() * 100);
            graphics.drawString(this.font,
                Component.literal("Soul Energy: " + soulProgress + "%").withStyle(ChatFormatting.AQUA),
                panelX + 10, centerY + 60, COLOR_WHITE, true);
        } else {
            graphics.drawCenteredString(this.font,
                Component.literal("Your soul is ready to transcend!").withStyle(ChatFormatting.GOLD),
                centerX, centerY + 60, COLOR_GOLD);
        }
        
        // Draw widgets (buttons)
        super.render(graphics, mouseX, mouseY, delta);
    }
    
    private void drawUpgradeLabel(GuiGraphics graphics, int x, int y, String name, int level, String effect) {
        boolean isMaxed = level >= PlayerDataManager.MAX_UPGRADE_LEVEL;
        String levelStr = isMaxed ? "MAX" : "Lv." + level;
        ChatFormatting color = isMaxed ? ChatFormatting.GREEN : ChatFormatting.WHITE;
        
        graphics.drawString(this.font,
            Component.literal(name + " [" + levelStr + "]").withStyle(color),
            x, y + 5, COLOR_WHITE, true);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
