package com.uipg9.ascendancy.mixin;

import com.uipg9.ascendancy.systems.ConstellationManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to modify damage for constellation effects
 * v2.5 - Star of the Wind fall damage reduction
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {
    
    @ModifyVariable(
        method = "actuallyHurt",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private float ascendancy$modifyDamage(float damage, DamageSource source) {
        LivingEntity self = (LivingEntity)(Object)this;
        
        // Only for server players
        if (!(self instanceof ServerPlayer player)) {
            return damage;
        }
        
        // Check if fall damage and has Star of the Wind
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            return ConstellationManager.modifyFallDamage(player, damage);
        }
        
        return damage;
    }
}
