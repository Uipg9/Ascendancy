package com.uipg9.ascendancy.mixin;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.systems.ChronicleManager;
import com.uipg9.ascendancy.systems.ConstellationManager;
import com.uipg9.ascendancy.systems.EchoManager;
import com.uipg9.ascendancy.systems.SoulCravingManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to track player movement and tick new systems
 * v2.5 - Added Echo proximity, Constellation effects, depth tracking
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void ascendancy$onTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        
        // Walking Soul XP
        AscendancyMod.tickPlayerMovement(player);
        
        // Echo boss proximity check (spawn near legacy chest)
        EchoManager.tickPlayerProximity(player);
        
        // Constellation passive effects
        ConstellationManager.tickConstellationEffects(player);
        
        // v2.5 - Depth tracking for cravings
        SoulCravingManager.onDepthsTick(player);
        
        // v2.5 - Chronicle deepslate milestone
        if (player.getY() < 0 && player.tickCount % 200 == 0) {
            ChronicleManager.recordMilestone(player, "deepslate", "Descended into the deepslate caverns");
        }
    }
}
