package com.uipg9.ascendancy.mixin;

import com.uipg9.ascendancy.AscendancyMod;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to track player movement for walking Soul XP
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void ascendancy$onTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        AscendancyMod.tickPlayerMovement(player);
    }
}
