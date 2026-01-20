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
     * Server -> Client: Sync player data (15 fields now!)
     * Uses manual encoding because StreamCodec.composite only supports up to 12 fields.
     */
    public record SyncDataPayload(
        int soulXP,
        int maxSoulXP,
        int prestigePoints, 
        int ascensionCount,
        int totalPrestigeEarned,
        int healthLevel,
        int speedLevel,
        int reachLevel,
        int miningLevel,
        int luckLevel,
        int damageLevel,
        int defenseLevel,
        int experienceLevel,
        int keeperLevel,   // v2.1
        int wisdomLevel    // v2.1
    ) implements CustomPacketPayload {
        public static final Type<SyncDataPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "sync_data"));
        
        // Manual StreamCodec for 15 fields (composite only supports up to 12)
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SyncDataPayload decode(RegistryFriendlyByteBuf buf) {
                return new SyncDataPayload(
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt()
                );
            }
            
            @Override
            public void encode(RegistryFriendlyByteBuf buf, SyncDataPayload payload) {
                buf.writeInt(payload.soulXP);
                buf.writeInt(payload.maxSoulXP);
                buf.writeInt(payload.prestigePoints);
                buf.writeInt(payload.ascensionCount);
                buf.writeInt(payload.totalPrestigeEarned);
                buf.writeInt(payload.healthLevel);
                buf.writeInt(payload.speedLevel);
                buf.writeInt(payload.reachLevel);
                buf.writeInt(payload.miningLevel);
                buf.writeInt(payload.luckLevel);
                buf.writeInt(payload.damageLevel);
                buf.writeInt(payload.defenseLevel);
                buf.writeInt(payload.experienceLevel);
                buf.writeInt(payload.keeperLevel);
                buf.writeInt(payload.wisdomLevel);
            }
        };
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Client -> Server: Request to ascend (legacy - no item kept)
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
     * Client -> Server: Request to ascend WITH a chosen item
     */
    public record AscendWithItemPayload(int slotToKeep) implements CustomPacketPayload {
        public static final Type<AscendWithItemPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AscendancyMod.MOD_ID, "ascend_with_item"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, AscendWithItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, AscendWithItemPayload::slotToKeep,
            AscendWithItemPayload::new
        );
        
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
        public static final int LUCK = 4;
        public static final int DAMAGE = 5;
        public static final int DEFENSE = 6;
        public static final int EXPERIENCE = 7;
        public static final int KEEPER = 8;    // v2.1
        public static final int WISDOM = 9;    // v2.1
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // ==================== REGISTRATION ====================
    
    public static void registerServerPackets() {
        // Register payload types
        PayloadTypeRegistry.playS2C().register(SyncDataPayload.TYPE, SyncDataPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AscendRequestPayload.TYPE, AscendRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AscendWithItemPayload.TYPE, AscendWithItemPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PurchaseUpgradePayload.TYPE, PurchaseUpgradePayload.STREAM_CODEC);
        
        // Handle ascend request (legacy - keeps nothing)
        ServerPlayNetworking.registerGlobalReceiver(AscendRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            player.level().getServer().execute(() -> {
                int maxSoulXP = AscendancyMod.getMaxSoulXP(PlayerDataManager.getAscensionCount(player));
                if (PlayerDataManager.getSoulXP(player) >= maxSoulXP) {
                    AscensionManager.performAscension(player);
                }
            });
        });
        
        // Handle ascend with item request (v2.1)
        ServerPlayNetworking.registerGlobalReceiver(AscendWithItemPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            player.level().getServer().execute(() -> {
                int maxSoulXP = AscendancyMod.getMaxSoulXP(PlayerDataManager.getAscensionCount(player));
                if (PlayerDataManager.getSoulXP(player) >= maxSoulXP) {
                    AscensionManager.performAscensionWithItem(player, payload.slotToKeep());
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
                    case PurchaseUpgradePayload.LUCK -> AttributeHandler.purchaseLuck(player);
                    case PurchaseUpgradePayload.DAMAGE -> AttributeHandler.purchaseDamage(player);
                    case PurchaseUpgradePayload.DEFENSE -> AttributeHandler.purchaseDefense(player);
                    case PurchaseUpgradePayload.EXPERIENCE -> AttributeHandler.purchaseExperience(player);
                    case PurchaseUpgradePayload.KEEPER -> AttributeHandler.purchaseKeeper(player);
                    case PurchaseUpgradePayload.WISDOM -> AttributeHandler.purchaseWisdom(player);
                    default -> false;
                };
                
                if (success) {
                    syncToClient(player);
                }
            });
        });
        
        AscendancyMod.LOGGER.info("Registered server packets");
    }
    
    @Environment(EnvType.CLIENT)
    public static void registerClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(SyncDataPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                AscendancyClient.updateData(
                    payload.soulXP(),
                    payload.maxSoulXP(),
                    payload.prestigePoints(),
                    payload.ascensionCount(),
                    payload.totalPrestigeEarned(),
                    payload.healthLevel(),
                    payload.speedLevel(),
                    payload.reachLevel(),
                    payload.miningLevel(),
                    payload.luckLevel(),
                    payload.damageLevel(),
                    payload.defenseLevel(),
                    payload.experienceLevel(),
                    payload.keeperLevel(),
                    payload.wisdomLevel()
                );
            });
        });
        
        AscendancyMod.LOGGER.info("Registered client packets");
    }
    
    // ==================== UTILITY METHODS ====================
    
    public static void syncToClient(ServerPlayer player) {
        int ascensionCount = PlayerDataManager.getAscensionCount(player);
        SyncDataPayload payload = new SyncDataPayload(
            PlayerDataManager.getSoulXP(player),
            AscendancyMod.getMaxSoulXP(ascensionCount),
            PlayerDataManager.getPrestigePoints(player),
            ascensionCount,
            PlayerDataManager.getTotalPrestigeEarned(player),
            PlayerDataManager.getHealthLevel(player),
            PlayerDataManager.getSpeedLevel(player),
            PlayerDataManager.getReachLevel(player),
            PlayerDataManager.getMiningLevel(player),
            PlayerDataManager.getLuckLevel(player),
            PlayerDataManager.getDamageLevel(player),
            PlayerDataManager.getDefenseLevel(player),
            PlayerDataManager.getExperienceLevel(player),
            PlayerDataManager.getKeeperLevel(player),
            PlayerDataManager.getWisdomLevel(player)
        );
        
        ServerPlayNetworking.send(player, payload);
    }
    
    @Environment(EnvType.CLIENT)
    public static void sendAscendRequest() {
        ClientPlayNetworking.send(new AscendRequestPayload());
    }
    
    @Environment(EnvType.CLIENT)
    public static void sendAscendWithItemRequest(int slotToKeep) {
        ClientPlayNetworking.send(new AscendWithItemPayload(slotToKeep));
    }
    
    @Environment(EnvType.CLIENT)
    public static void sendPurchaseRequest(int upgradeType) {
        ClientPlayNetworking.send(new PurchaseUpgradePayload(upgradeType));
    }
}
