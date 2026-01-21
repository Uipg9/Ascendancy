package com.uipg9.ascendancy.systems;

import com.uipg9.ascendancy.AscendancyMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Heirloom System - Items evolve across ascensions
 * v2.5 - Replayability Expansion
 * 
 * Items kept across ascensions gain lore and special properties:
 * - Track how many ages they've survived
 * - Add flavor text about their history
 * - Visual upgrades (gold names, enchant glow)
 * - "Timeworn" state after many ages (becomes immortal but weaker)
 */
public class HeirloomManager {
    
    // NBT keys for heirloom data
    private static final String KEY_HEIRLOOM = "ascendancy_heirloom";
    private static final String KEY_AGE = "heirloom_age";
    private static final String KEY_ORIGINAL_OWNER = "original_owner";
    private static final String KEY_TIMEWORN = "is_timeworn";
    
    // Age thresholds for effects
    private static final int AGE_FOR_GOLD_NAME = 5;
    private static final int AGE_FOR_GLOW = 10;
    private static final int AGE_FOR_TIMEWORN = 25;
    
    /**
     * Process an item being kept through ascension
     * @return The modified item with heirloom properties
     */
    public static ItemStack processHeirloom(ItemStack item, ServerPlayer player, int newAscensionNumber) {
        if (item.isEmpty()) return item;
        
        // Get or create heirloom data
        CompoundTag heirloomData = getHeirloomData(item);
        
        // Increment age
        int currentAge = heirloomData.getIntOr(KEY_AGE, 0);
        int newAge = currentAge + 1;
        heirloomData.putInt(KEY_AGE, newAge);
        
        // Set original owner if first time
        if (!heirloomData.contains(KEY_ORIGINAL_OWNER)) {
            heirloomData.putString(KEY_ORIGINAL_OWNER, player.getName().getString());
        }
        
        // Check for timeworn state
        if (newAge >= AGE_FOR_TIMEWORN && !heirloomData.getBooleanOr(KEY_TIMEWORN, false)) {
            heirloomData.putBoolean(KEY_TIMEWORN, true);
            player.sendSystemMessage(Component.literal("§5§l✦ Your heirloom has become Timeworn! ✦"));
            player.sendSystemMessage(Component.literal("§7It will never break, but its power has faded..."));
        }
        
        // Save back to item
        setHeirloomData(item, heirloomData);
        
        // Update visual properties
        updateHeirloomLore(item, heirloomData);
        applyAgeEffects(item, newAge);
        
        AscendancyMod.LOGGER.info("Processed heirloom {} for {}, now age {}", 
            item.getDisplayName().getString(), player.getName().getString(), newAge);
        
        return item;
    }
    
    /**
     * Get the age of an heirloom item
     */
    public static int getHeirloomAge(ItemStack item) {
        CompoundTag data = getHeirloomData(item);
        return data.getIntOr(KEY_AGE, 0);
    }
    
    /**
     * Check if item is a recognized heirloom
     */
    public static boolean isHeirloom(ItemStack item) {
        return getHeirloomAge(item) > 0;
    }
    
    /**
     * Check if item has reached timeworn state
     */
    public static boolean isTimeworn(ItemStack item) {
        return getHeirloomData(item).getBooleanOr(KEY_TIMEWORN, false);
    }
    
    /**
     * Get the original owner's name
     */
    public static String getOriginalOwner(ItemStack item) {
        return getHeirloomData(item).getStringOr(KEY_ORIGINAL_OWNER, "Unknown");
    }
    
    // ==================== INTERNAL METHODS ====================
    
    private static CompoundTag getHeirloomData(ItemStack item) {
        CustomData customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(KEY_HEIRLOOM)) {
                return tag.getCompoundOrEmpty(KEY_HEIRLOOM);
            }
        }
        return new CompoundTag();
    }
    
    private static void setHeirloomData(ItemStack item, CompoundTag heirloomData) {
        CustomData customData = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.put(KEY_HEIRLOOM, heirloomData);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    private static void updateHeirloomLore(ItemStack item, CompoundTag heirloomData) {
        int age = heirloomData.getIntOr(KEY_AGE, 0);
        String owner = heirloomData.getStringOr(KEY_ORIGINAL_OWNER, "Unknown");
        boolean timeworn = heirloomData.getBooleanOr(KEY_TIMEWORN, false);
        
        List<Component> loreLines = new ArrayList<>();
        
        // Title based on age
        if (timeworn) {
            loreLines.add(Component.literal("§5§lTimeworn Relic"));
        } else if (age >= AGE_FOR_GLOW) {
            loreLines.add(Component.literal("§6§lLegendary Heirloom"));
        } else if (age >= AGE_FOR_GOLD_NAME) {
            loreLines.add(Component.literal("§eAncient Heirloom"));
        } else if (age >= 3) {
            loreLines.add(Component.literal("§bFamily Heirloom"));
        } else {
            loreLines.add(Component.literal("§7Heirloom"));
        }
        
        // Age line
        loreLines.add(Component.literal("§8Survived " + age + " age" + (age == 1 ? "" : "s")));
        
        // Origin line
        loreLines.add(Component.literal("§8Originally owned by §7" + owner));
        
        // Flavor text based on age
        String flavorText = getFlavorText(age, timeworn);
        if (flavorText != null) {
            loreLines.add(Component.literal(""));
            loreLines.add(Component.literal("§o§7\"" + flavorText + "\""));
        }
        
        // Timeworn warning
        if (timeworn) {
            loreLines.add(Component.literal(""));
            loreLines.add(Component.literal("§5Unbreakable but weakened by time"));
        }
        
        // Apply lore
        item.set(DataComponents.LORE, new ItemLore(loreLines));
    }
    
    private static void applyAgeEffects(ItemStack item, int age) {
        // Gold name at age 5+
        if (age >= AGE_FOR_GOLD_NAME) {
            Component baseName = item.getItem().getName(item);
            item.set(DataComponents.CUSTOM_NAME, 
                baseName.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
        }
        
        // Enchant glint at age 10+ (even without enchants)
        if (age >= AGE_FOR_GLOW) {
            item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
    }
    
    private static String getFlavorText(int age, boolean timeworn) {
        if (timeworn) {
            return "Time has worn its edge, but it endures.";
        }
        
        return switch (age) {
            case 1 -> "Passed down through one life.";
            case 2 -> "Two souls have held this item.";
            case 3 -> "A family heirloom in the making.";
            case 4 -> "Stories cling to its surface.";
            case 5 -> "Generations have known its weight.";
            case 6 -> "The metal remembers.";
            case 7 -> "It has seen the world change.";
            case 8 -> "Older than most kingdoms.";
            case 9 -> "Legends speak of this item.";
            case 10 -> "Time itself respects its legacy.";
            default -> {
                if (age > 10 && age < AGE_FOR_TIMEWORN) {
                    yield "Beyond ancient. Beyond memory.";
                }
                yield null;
            }
        };
    }
    
    /**
     * Handle durability for timeworn items - they shouldn't break
     * Call this from a damage handler to prevent breaking
     * @return true if item should be protected from breaking
     */
    public static boolean shouldPreventBreaking(ItemStack item) {
        return isTimeworn(item);
    }
}
