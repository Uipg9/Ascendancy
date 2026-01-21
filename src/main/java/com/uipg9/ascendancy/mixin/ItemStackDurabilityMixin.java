package com.uipg9.ascendancy.mixin;

import com.uipg9.ascendancy.systems.HeirloomManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * Mixin to prevent Timeworn heirloom items from breaking
 * v2.5 - Heirloom System
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
        LivingEntity entity, 
        Consumer<net.minecraft.world.item.Item> onBreak,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        ItemStack self = (ItemStack)(Object)this;
        
        // If timeworn, prevent ALL durability damage
        if (HeirloomManager.shouldPreventBreaking(self)) {
            // Return the unchanged item, preventing any durability loss
            cir.setReturnValue(self);
        }
    }
}
