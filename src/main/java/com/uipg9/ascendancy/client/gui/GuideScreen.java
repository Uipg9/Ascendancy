package com.uipg9.ascendancy.client.gui;

import com.uipg9.ascendancy.client.AscendancyClient;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Guide Screen - Comprehensive documentation for Ascendancy mod
 * Features:
 * - Table of Contents
 * - Detailed explanations
 * - Simple summaries
 * - Scrollable content
 */
@Environment(EnvType.CLIENT)
public class GuideScreen extends Screen {
    
    // Colors
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFAAAAAA;
    private static final int COLOR_DARK_BG = 0xE0101010;
    private static final int COLOR_PANEL_BG = 0xF0181818;
    private static final int COLOR_BORDER = 0xFF404040;
    private static final int COLOR_HIGHLIGHT = 0xFF3A3A50;
    private static final int COLOR_SECTION = 0xFF40404F;
    
    // Layout
    private int centerX;
    private int centerY;
    private int panelWidth = 360;
    private int panelHeight = 280;
    private int panelX;
    private int panelY;
    
    // Scrolling
    private float scrollOffset = 0;
    private float targetScroll = 0;
    private static final int LINE_HEIGHT = 12;
    private static final int CONTENT_PADDING = 15;
    
    // Content
    private final List<GuideEntry> entries = new ArrayList<>();
    private int currentSection = 0;
    
    // Sections
    private static final String[] SECTIONS = {
        "Overview", "Getting Started", "Soul Energy", "Ascension", 
        "Upgrades", "Tips & Tricks", "Quick Reference"
    };
    
    public GuideScreen() {
        super(Component.literal("Ascendancy Guide"));
        buildContent();
    }
    
    private void buildContent() {
        entries.clear();
        
        switch (currentSection) {
            case 0 -> buildOverview();
            case 1 -> buildGettingStarted();
            case 2 -> buildSoulEnergy();
            case 3 -> buildAscension();
            case 4 -> buildUpgrades();
            case 5 -> buildTips();
            case 6 -> buildQuickReference();
        }
    }
    
    private void buildOverview() {
        addTitle("✦ Welcome to Ascendancy ✦");
        addSpacer();
        addText("A prestige system that rewards your gameplay!");
        addSpacer();
        addSubtitle("§eTL;DR (The Simple Version):");
        addText("1. Play the game normally → Gain Soul XP");
        addText("2. Fill the Soul bar → Press P to Ascend");
        addText("3. Get Prestige Points → Buy upgrades");
        addText("4. Repeat forever → Get stronger!");
        addSpacer();
        addSubtitle("§bThe Full Story:");
        addText("Ascendancy adds a persistent progression");
        addText("system that lets you earn permanent");
        addText("upgrades by 'ascending' - resetting your");
        addText("experience while gaining Prestige Points.");
        addSpacer();
        addText("These points buy upgrades that make you");
        addText("stronger, faster, and more efficient.");
        addText("The more you ascend, the more rewards!");
    }
    
    private void buildGettingStarted() {
        addTitle("Getting Started");
        addSpacer();
        addSubtitle("§eTL;DR:");
        addText("Do stuff → Watch Soul bar fill → Press P");
        addSpacer();
        addSubtitle("§bStep by Step:");
        addText("§f1. Look at the §eleft-center§f of screen");
        addText("   You'll see the vertical SOUL bar.");
        addText("   It fills as you earn Soul XP.");
        addSpacer();
        addText("§f2. Earn Soul XP by playing:");
        addText("   §c• Kill mobs and animals");
        addText("   §b• Mine ores");
        addText("   §6• Smelt items");
        addText("   §a• Harvest crops");
        addText("   §d• Just walk around!");
        addSpacer();
        addText("§f3. When the bar is full (100%):");
        addText("   Press §e[P]§f to open the menu");
        addText("   §6Choose an item to keep!§f");
        addText("   Click the §6ASCEND§f button!");
        addSpacer();
        addText("§f4. Buy upgrades with your points");
        addText("   Then do it all again!");
    }
    
    private void buildSoulEnergy() {
        addTitle("Soul Energy System");
        addSpacer();
        addSubtitle("§eTL;DR:");
        addText("Actions → Soul XP → Ascend when full!");
        addSpacer();
        addSubtitle("§bHow To Earn Soul XP:");
        addText("§c⚔ Combat:§f Kill mobs (5), animals (1), bosses (50)");
        addText("§b⛏ Mining:§f Ores! Diamond (8), Emerald (10), etc.");
        addText("§6🔥 Smelting:§f Ores (2), Food (1), Other (1)");
        addText("§a🌾 Farming:§f Harvest crops (1 per crop)");
        addText("§d👟 Walking:§f Every 100 blocks (1 XP)");
        addSpacer();
        addSubtitle("§dFirst Ascension Bonus:");
        addText("Your first ascension fills §a10x faster§f!");
        addText("Soul XP gains multiplied by 10!");
        addText("This lets you try the system quickly.");
        addSpacer();
        addSubtitle("§dScaling Requirements:");
        addText("Each ascension requires more Soul XP:");
        addText("  Ascension 0: 100 Soul XP (easy!)");
        addText("  Ascension 1: 150 Soul XP");
        addText("  Ascension 2: 200 Soul XP");
        addText("  And so on... (caps at 10,000)");
    }
    
    private void buildAscension() {
        addTitle("Ascending");
        addSpacer();
        addSubtitle("§eTL;DR:");
        addText("Reset everything, keep upgrades + 1 item!");
        addSpacer();
        addSubtitle("§bWhat Happens When You Ascend:");
        addText("§a✓§f All your permanent upgrades remain");
        addText("§a✓§f You gain Prestige Points");
        addText("§a✓§f You keep §eONE chosen item§f (Keeper upgrade!)");
        addText("§a✓§f You're reborn in a §dnew world§f far away");
        addText("§c✗§f Your inventory is §4WIPED§f (except kept item)");
        addText("§c✗§f Your Ender Chest is cleared");
        addText("§c✗§f Your advancements reset");
        addText("§c✗§f Soul Energy resets to zero");
        addSpacer();
        addSubtitle("§dPrestige Point Rewards:");
        addText("Base reward: §e5 points§f per ascension");
        addText("Bonus: §e+1 point§f per ascension tier");
        addText("  1st Ascension: 5 points");
        addText("  2nd Ascension: 6 points");
        addText("  3rd Ascension: 7 points");
        addText("  ...and so on!");
        addSpacer();
        addSubtitle("§6The Mysterious Awakening:");
        addText("You awaken in a strange new place:");
        addText("  §5• Blindness§f - Slowly fades away");
        addText("  §e• Night Vision§f - See your new home");
        addText("  §6• Morning§f - A fresh new day");
        addText("Old items stored in §6Legacy Chest§f!");
    }
    
    private void buildUpgrades() {
        addTitle("Upgrades (Infinite!)");
        addSpacer();
        addSubtitle("§eTL;DR:");
        addText("8 upgrades, no max level, costs scale");
        addSpacer();
        addSubtitle("§b⚔ Combat Category:");
        addText("§cVitality§f - +2 Hearts per level");
        addText("§cMight§f - +5% Attack Damage per level");
        addText("§cResilience§f - +1 Armor per level");
        addSpacer();
        addSubtitle("§b⚡ Utility Category:");
        addText("§aSwiftness§f - +3% Move Speed per level");
        addText("§aTitan's Reach§f - +0.5 Block Reach");
        addText("§aHaste§f - +8% Mining Speed per level");
        addSpacer();
        addSubtitle("§b✦ Special Category:");
        addText("§dFortune§f - +5% Luck per level");
        addText("§dWisdom§f - +10% Soul XP gain per level");
        addSpacer();
        addSubtitle("§6Upgrade Costs:");
        addText("Level 0→1: 1 point");
        addText("Level 1→2: 2 points");
        addText("Costs scale by §e1.3x§f each level!");
        addText("No cap - upgrade forever!");
    }
    
    private void buildTips() {
        addTitle("Tips & Tricks");
        addSpacer();
        addSubtitle("§e★ Getting Started Fast:");
        addText("• First ascension is 10x easier!");
        addText("• Mine ANY ore for quick Soul XP");
        addText("• Walking gives passive Soul XP!");
        addText("• Harvest crops for bonus XP!");
        addSpacer();
        addSubtitle("§e★ Optimal Strategy:");
        addText("• Get §dWisdom§f early for faster XP");
        addText("• Get §cKeeper§f to keep more items!");
        addText("• §aSwiftness§f = more walking XP!");
        addSpacer();
        addSubtitle("§e★ Don't Settle!");
        addText("Each ascension gives MORE points.");
        addText("Ascension 10+ gives 15+ points!");
        addText("Keep ascending for best rewards.");
        addSpacer();
        addSubtitle("§e★ Legacy Chests:");
        addText("Your old items are saved in a");
        addText("glowing chest at your old location!");
        addText("Return to collect past treasures.");
    }
    
    private void buildQuickReference() {
        addTitle("Quick Reference");
        addSpacer();
        addSubtitle("§eControls:");
        addText("§f[P]§7 - Open Ascension Menu");
        addSpacer();
        addSubtitle("§eHUD (Left-Center):");
        addText("§f[SOUL Bar]§7 - Vertical progress bar");
        addText("§f[Percentage]§7 - How close to ascend");
        addSpacer();
        addSubtitle("§eYour Current Stats:");
        addText("Ascension Count: §e" + AscendancyClient.ascensionCount);
        addText("Prestige Points: §e" + AscendancyClient.prestigePoints);
        addText("Total Earned: §e" + AscendancyClient.totalPrestigeEarned);
        addSpacer();
        addSubtitle("§eYour Upgrades:");
        addText("Vitality: §c" + AscendancyClient.healthLevel);
        addText("Swiftness: §a" + AscendancyClient.speedLevel);
        addText("Reach: §a" + AscendancyClient.reachLevel);
        addText("Haste: §a" + AscendancyClient.miningLevel);
        addText("Fortune: §d" + AscendancyClient.luckLevel);
        addText("Might: §c" + AscendancyClient.damageLevel);
        addText("Resilience: §c" + AscendancyClient.defenseLevel);
        addText("Wisdom: §d" + AscendancyClient.experienceLevel);
    }
    
    // Helper methods for content
    private void addTitle(String text) {
        entries.add(new GuideEntry(text, EntryType.TITLE));
    }
    
    private void addSubtitle(String text) {
        entries.add(new GuideEntry(text, EntryType.SUBTITLE));
    }
    
    private void addText(String text) {
        entries.add(new GuideEntry(text, EntryType.TEXT));
    }
    
    private void addSpacer() {
        entries.add(new GuideEntry("", EntryType.SPACER));
    }
    
    @Override
    protected void init() {
        super.init();
        
        centerX = this.width / 2;
        centerY = this.height / 2;
        panelX = centerX - panelWidth / 2;
        panelY = centerY - panelHeight / 2;
        
        // Section buttons (left side)
        int buttonY = panelY + 25;
        for (int i = 0; i < SECTIONS.length; i++) {
            final int section = i;
            Button btn = Button.builder(
                Component.literal(SECTIONS[i]),
                b -> {
                    playClickSound();
                    currentSection = section;
                    scrollOffset = 0;
                    targetScroll = 0;
                    buildContent();
                }
            ).bounds(panelX + 5, buttonY + (i * 22), 90, 18).build();
            this.addRenderableWidget(btn);
        }
        
        // Back button
        this.addRenderableWidget(Button.builder(
            Component.literal("← Back"),
            b -> {
                playClickSound();
                this.minecraft.setScreen(new AscensionScreen());
            }
        ).bounds(panelX + 5, panelY + panelHeight - 25, 60, 20).build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Smooth scrolling
        scrollOffset = Mth.lerp(0.2f, scrollOffset, targetScroll);
        
        // Dark overlay
        graphics.fill(0, 0, this.width, this.height, 0xD0000000);
        
        // Main panel
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL_BG);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, COLOR_BORDER);
        
        // Gold accent
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 3, COLOR_GOLD);
        
        // Title
        graphics.drawCenteredString(this.font, 
            Component.literal("§6§l✦ Ascendancy Guide ✦"), 
            centerX, panelY + 8, COLOR_GOLD);
        
        // Section divider
        graphics.fill(panelX + 100, panelY + 22, panelX + 101, panelY + panelHeight - 5, COLOR_BORDER);
        
        // Content area background
        int contentX = panelX + 105;
        int contentY = panelY + 25;
        int contentWidth = panelWidth - 115;
        int contentHeight = panelHeight - 35;
        
        graphics.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, 0xFF0A0A0A);
        graphics.renderOutline(contentX, contentY, contentWidth, contentHeight, 0xFF2A2A2A);
        
        // Enable scissor for content scrolling
        graphics.enableScissor(contentX + 2, contentY + 2, contentX + contentWidth - 2, contentY + contentHeight - 2);
        
        // Render content
        int y = contentY + 5 - (int)scrollOffset;
        for (GuideEntry entry : entries) {
            y = renderEntry(graphics, entry, contentX + 5, y, contentWidth - 15);
        }
        
        graphics.disableScissor();
        
        // Scroll indicators
        if (scrollOffset > 5) {
            graphics.drawCenteredString(this.font, "▲", contentX + contentWidth / 2, contentY + 3, 0x88FFFFFF);
        }
        int maxScroll = getMaxScroll(contentHeight);
        if (scrollOffset < maxScroll - 5) {
            graphics.drawCenteredString(this.font, "▼", contentX + contentWidth / 2, contentY + contentHeight - 10, 0x88FFFFFF);
        }
        
        // Draw widgets
        super.render(graphics, mouseX, mouseY, delta);
    }
    
    private int renderEntry(GuiGraphics graphics, GuideEntry entry, int x, int y, int maxWidth) {
        switch (entry.type) {
            case TITLE:
                graphics.drawString(this.font, "§6§l" + entry.text, x, y + 2, COLOR_GOLD, true);
                return y + LINE_HEIGHT + 6;
            case SUBTITLE:
                graphics.drawString(this.font, entry.text, x, y + 1, COLOR_WHITE, false);
                return y + LINE_HEIGHT + 2;
            case TEXT:
                graphics.drawString(this.font, "§7" + entry.text, x + 2, y, COLOR_GRAY, false);
                return y + LINE_HEIGHT;
            case SPACER:
                return y + 6;
        }
        return y + LINE_HEIGHT;
    }
    
    private int getMaxScroll(int contentHeight) {
        int totalHeight = 0;
        for (GuideEntry entry : entries) {
            totalHeight += switch (entry.type) {
                case TITLE -> LINE_HEIGHT + 6;
                case SUBTITLE -> LINE_HEIGHT + 2;
                case TEXT -> LINE_HEIGHT;
                case SPACER -> 6;
            };
        }
        return Math.max(0, totalHeight - contentHeight + 20);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentX = panelX + 105;
        int contentWidth = panelWidth - 115;
        int contentY = panelY + 25;
        int contentHeight = panelHeight - 35;
        
        if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
            mouseY >= contentY && mouseY <= contentY + contentHeight) {
            targetScroll -= verticalAmount * 20;
            targetScroll = Mth.clamp(targetScroll, 0, getMaxScroll(contentHeight));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
    
    // Entry type enum
    private enum EntryType {
        TITLE, SUBTITLE, TEXT, SPACER
    }
    
    // Guide entry record
    private record GuideEntry(String text, EntryType type) {}
}
