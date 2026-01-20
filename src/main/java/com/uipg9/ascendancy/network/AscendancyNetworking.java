package com.uipg9.ascendancy.network;

import com.uipg9.ascendancy.AscendancyMod;
import com.uipg9.ascendancy.client.AscendancyClient;
import com.uipg9.ascendancy.data.PlayerDataManager;
import com.uipg9.ascendancy.logic.AscensionManager;
import com.uipg9.ascendancy.logic.AttributeHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles all networking between client and server for Ascendancy.
 * Uses Mojang Official Mappings for 1.21.11
 */
public class AscendancyNetworking {
    
    // ==================== PAYLOAD DEFINITIONS ====================
    
    /**
     * Server -> Client: Sync player data
     */
    public record SyncDataPayload(
        int soulXP, 
        int prestigePoints, 
        int ascensionCount,
        int healthLevel,
        int speedLevel,
        int reachLevel,
        int miningLevel
    ) implements CustomPacketPayload {
        public static final Type<SyncDataPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "sync_data"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncDataPayload::soulXP,
            ByteBufCodecs.INT, SyncDataPayload::prestigePoints,
            ByteBufCodecs.INT, SyncDataPayload::ascensionCount,
            ByteBufCodecs.INT, SyncDataPayload::healthLevel,
            ByteBufCodecs.INT, SyncDataPayload::speedLevel,
            ByteBufCodecs.INT, SyncDataPayload::reachLevel,
            ByteBufCodecs.INT, SyncDataPayload::miningLevel,
            SyncDataPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Client -> Server: Request to ascend
     */
    public record AscendRequestPayload() implements CustomPacketPayload {
        public static final Type<AscendRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "ascend_request"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, AscendRequestPayload> STREAM_CODEC = StreamCodec.unit(new AscendRequestPayload());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Client -> Server: Request to purchase an upgrade
     */
    public record PurchaseUpgradePayload(int upgradeType) implements CustomPacketPayload {
        public static final Type<PurchaseUpgradePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "purchase_upgrade"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseUpgradePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PurchaseUpgradePayload::upgradeType,
            PurchaseUpgradePayload::new
        );
        
        // Upgrade type constants
        public static final int VITALITY = 0;
        public static final int SWIFTNESS = 1;
        public static final int REACH = 2;
        public static final int HASTE = 3;
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // ==================== REGISTRATION ====================
    
    /**
     * Register server-side packet handlers
     */
    public static void registerServerPackets() {
        // Register payload types
        PayloadTypeRegistry.playS2C().register(SyncDataPayload.TYPE, SyncDataPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AscendRequestPayload.TYPE, AscendRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PurchaseUpgradePayload.TYPE, PurchaseUpgradePayload.STREAM_CODEC);
        
        // Handle ascend request
        ServerPlayNetworking.registerGlobalReceiver(AscendRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            player.level().getServer().execute(() -> {
                // Verify player can ascend
                if (PlayerDataManager.getSoulXP(player) >= AscendancyMod.MAX_SOUL_XP) {
                    AscensionManager.performAscension(player);
                }
            });
        });
        
        // Handle upgrade purchase
        ServerPlayNetworking.registerGlobalReceiver(PurchaseUpgradePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            player.level().getServer().execute(() -> {
                boolean success = switch (payload.upgradeType()) {
                    case PurchaseUpgradePayload.VITALITY -> AttributeHandler.purchaseVitality(player);
                    case PurchaseUpgradePayload.SWIFTNESS -> AttributeHandler.purchaseSwiftness(player);
                    case PurchaseUpgradePayload.REACH -> AttributeHandler.purchaseReach(player);
                    case PurchaseUpgradePayload.HASTE -> AttributeHandler.purchaseHaste(player);
                    default -> false;
                };
                
                // Sync updated data back to client
                if (success) {
                    syncToClient(player);
                }
            });
        });
        
        AscendancyMod.LOGGER.info("Registered server packets");
    }
    
    /**
     * Register client-side packet handlers
     */
    @Environment(EnvType.CLIENT)
    public static void registerClientPackets() {
        // Handle sync data from server
        ClientPlayNetworking.registerGlobalReceiver(SyncDataPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                AscendancyClient.updateData(
                    payload.soulXP(),
                    payload.prestigePoints(),
                    payload.ascensionCount(),
                    payload.healthLevel(),
                    payload.speedLevel(),
                    payload.reachLevel(),
                    payload.miningLevel()
                );
            });
        });
        
        AscendancyMod.LOGGER.info("Registered client packets");
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Sync all player data to client
     */
    public static void syncToClient(ServerPlayer player) {
        SyncDataPayload payload = new SyncDataPayload(
            PlayerDataManager.getSoulXP(player),
            PlayerDataManager.getPrestigePoints(player),
            PlayerDataManager.getAscensionCount(player),
            PlayerDataManager.getHealthLevel(player),
            PlayerDataManager.getSpeedLevel(player),
            PlayerDataManager.getReachLevel(player),
            PlayerDataManager.getMiningLevel(player)
        );
        
        ServerPlayNetworking.send(player, payload);
    }
    
    /**
     * Send ascension request to server (client-side)
     */
    @Environment(EnvType.CLIENT)
    public static void sendAscendRequest() {
        ClientPlayNetworking.send(new AscendRequestPayload());
    }
    
    /**
     * Send upgrade purchase request to server (client-side)
     */
    @Environment(EnvType.CLIENT)
    public static void sendPurchaseRequest(int upgradeType) {
        ClientPlayNetworking.send(new PurchaseUpgradePayload(upgradeType));
    }
}
