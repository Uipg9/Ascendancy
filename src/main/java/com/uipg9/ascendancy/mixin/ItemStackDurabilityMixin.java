package com.uipg9.ascendancy.mixin;

import com.uipg9.ascendancy.systems.HeirloomManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Mixin to prevent Timeworn heirloom items from breaking
 * v2.5 - Heirloom System
 * 
 * In 1.21.11, hurtAndBreak signature is:
 * hurtAndBreak(int damage, ServerLevel level, ServerPlayer player, Consumer<Item> onBreak)
 * Returns void
 */
@Mixin(ItemStack.class)
public class ItemStackDurabilityMixin {
    
    @Inject(
        method = "hurtAndBreak",
        at = @At("HEAD"),
        cancellable = true
    )
    private void ascendancy$preventTimewornBreaking(
        int damage, 
        ServerLevel level,
        ServerPlayer player, 
        Consumer<net.minecraft.world.item.Item> onBreak,
        CallbackInfo ci
    ) {
        ItemStack self = (ItemStack)(Object)this;
        
        // If timeworn, prevent ALL durability damage
        if (HeirloomManager.shouldPreventBreaking(self)) {
            // Cancel the method to prevent any durability loss
            ci.cancel();
        }
    }
}
