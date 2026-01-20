package com.uipg9.ascendancy.data;

import com.uipg9.ascendancy.AscendancyMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * Registers the Ascendancy data attachment using Fabric API.
 * This handles automatic persistence and death copying.
 */
public class AscendancyAttachments {
    
    /**
     * The attachment type for Ascendancy player data.
     * - persistent: Data is saved/loaded with the player file
     * - copyOnDeath: Data persists when player dies and respawns
     */
    public static final AttachmentType<AscendancyData> ASCENDANCY_DATA = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "player_data"),
        builder -> builder
            .persistent(AscendancyData.CODEC)
            .copyOnDeath()
            .initializer(() -> AscendancyData.DEFAULT)
    );
    
    /**
     * Call this during mod initialization to ensure the attachment is registered.
     */
    public static void register() {
        AscendancyMod.LOGGER.info("Ascendancy attachments registered.");
    }
}
