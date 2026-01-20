package com.uipg9.ascendancy.client.gui;

import com.uipg9.ascendancy.client.AscendancyClient;
import com.uipg9.ascendancy.network.AscendancyNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Item selection screen shown before ascending.
 * Player chooses ONE item/stack to keep (amount based on upgrade level).
 */
@Environment(EnvType.CLIENT)
public class ItemSelectionScreen extends Screen {
    
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 9;
    private static final int COLOR_SELECTED = 0xFF00FF00;
    private static final int COLOR_SLOT_BG = 0xFF222222;
    private static final int COLOR_SLOT_BORDER = 0xFF444444;
    
    private int selectedSlot = -1;
    private List<SlotInfo> slots = new ArrayList<>();
    
    private int panelX, panelY, panelWidth, panelHeight;
    
    public ItemSelectionScreen() {
        super(Component.literal("Choose an Item to Keep"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        panelWidth = 200;
        panelHeight = 220;
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        
        // Collect all non-empty items
        slots.clear();
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                slots.add(new SlotInfo(i, stack.copy()));
            }
        }
        
        // Confirm button
        this.addRenderableWidget(Button.builder(
            Component.literal("§a✦ ASCEND WITH ITEM ✦"),
            btn -> {
                if (selectedSlot >= 0) {
                    playAscendSound();
                    AscendancyNetworking.sendAscendWithItemRequest(selectedSlot);
                    this.minecraft.setScreen(null);
                }
            }
        ).bounds(panelX + 15, panelY + panelHeight - 55, panelWidth - 30, 20).build());
        
        // Ascend without item button
        this.addRenderableWidget(Button.builder(
            Component.literal("§7Ascend Empty-Handed"),
            btn -> {
                playAscendSound();
                AscendancyNetworking.sendAscendWithItemRequest(-1);
                this.minecraft.setScreen(null);
            }
        ).bounds(panelX + 15, panelY + panelHeight - 30, panelWidth - 30, 20).build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Background
        graphics.fill(0, 0, this.width, this.height, 0xD0101010);
        
        // Panel
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF0181818);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFF606060);
        
        // Gold accent
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 3, 0xFFFFD700);
        
        // Title
        graphics.drawCenteredString(this.font, 
            Component.literal("§6§l✦ Choose Your Heirloom ✦"),
            this.width / 2, panelY + 10, 0xFFFFD700);
        
        // Info text
        int keepAmount = getKeepAmount();
        graphics.drawCenteredString(this.font,
            Component.literal("§7You may keep up to §e" + keepAmount + "§7 of one item"),
            this.width / 2, panelY + 25, 0xFFAAAAAA);
        
        // Draw item grid
        int gridX = panelX + 10;
        int gridY = panelY + 45;
        int slotsWide = Math.min(SLOTS_PER_ROW, slots.size());
        int gridWidth = slotsWide * SLOT_SIZE;
        
        for (int i = 0; i < slots.size(); i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;
            int x = gridX + col * SLOT_SIZE;
            int y = gridY + row * SLOT_SIZE;
            
            SlotInfo slot = slots.get(i);
            
            // Slot background
            boolean isSelected = (selectedSlot == slot.inventorySlot);
            int bgColor = isSelected ? 0xFF115511 : COLOR_SLOT_BG;
            graphics.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, bgColor);
            
            // Border
            int borderColor = isSelected ? COLOR_SELECTED : COLOR_SLOT_BORDER;
            graphics.renderOutline(x, y, SLOT_SIZE - 1, SLOT_SIZE - 1, borderColor);
            
            // Item
            graphics.renderItem(slot.stack, x + 1, y + 1);
            graphics.renderItemDecorations(this.font, slot.stack, x + 1, y + 1);
        }
        
        // Selected item info
        if (selectedSlot >= 0) {
            SlotInfo selected = findSlot(selectedSlot);
            if (selected != null) {
                int infoY = panelY + 130;
                graphics.drawCenteredString(this.font,
                    Component.literal("§eSelected: §f" + selected.stack.getHoverName().getString()),
                    this.width / 2, infoY, 0xFFFFFFFF);
                
                int actualKeep = Math.min(keepAmount, selected.stack.getCount());
                graphics.drawCenteredString(this.font,
                    Component.literal("§7Will keep: §a" + actualKeep + "§7 / " + selected.stack.getCount()),
                    this.width / 2, infoY + 12, 0xFFAAAAAA);
            }
        } else {
            graphics.drawCenteredString(this.font,
                Component.literal("§8Click an item to select it"),
                this.width / 2, panelY + 130, 0xFF666666);
        }
        
        super.render(graphics, mouseX, mouseY, delta);
    }
    
    // Handle mouse clicks on the item grid
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int gridX = panelX + 10;
            int gridY = panelY + 45;
            
            for (int i = 0; i < slots.size(); i++) {
                int row = i / SLOTS_PER_ROW;
                int col = i % SLOTS_PER_ROW;
                int x = gridX + col * SLOT_SIZE;
                int y = gridY + row * SLOT_SIZE;
                
                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    selectedSlot = slots.get(i).inventorySlot;
                    playClickSound();
                    return true;
                }
            }
        }
        // Call the parent method without passing arguments since it's a screen method
        return false;
    }
    
    private SlotInfo findSlot(int inventorySlot) {
        for (SlotInfo slot : slots) {
            if (slot.inventorySlot == inventorySlot) return slot;
        }
        return null;
    }
    
    /**
     * Get max amount to keep based on Keeper upgrade level
     */
    private int getKeepAmount() {
        // Base: 1, +1 per Keeper level
        return 1 + AscendancyClient.keeperLevel;
    }
    
    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );
    }
    
    private void playAscendSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f, 0.5f)
        );
    }
    
    @Override
    public boolean isPauseScreen() {
        return true; // Pause while choosing
    }
    
    private record SlotInfo(int inventorySlot, ItemStack stack) {}
}
