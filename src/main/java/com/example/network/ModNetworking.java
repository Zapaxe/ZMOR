package com.example.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModNetworking.class);
    
    // Cache manifests of active players on the server
    private static final Map<UUID, TextureManifestPayload> CACHED_MANIFESTS = new ConcurrentHashMap<>();

    public static void registerCommon() {
        // Register C2S and S2C payloads
        PayloadTypeRegistry.playC2S().register(TextureManifestPayload.TYPE, TextureManifestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TextureManifestPayload.TYPE, TextureManifestPayload.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(TextureRequestPayload.TYPE, TextureRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TextureRequestPayload.TYPE, TextureRequestPayload.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(TextureResponsePayload.TYPE, TextureResponsePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TextureResponsePayload.TYPE, TextureResponsePayload.STREAM_CODEC);

        // Server-side Relay Handlers
        ServerPlayNetworking.registerGlobalReceiver(TextureManifestPayload.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            context.server().execute(() -> {
                LOGGER.info("[zmor-server] Received texture manifest from {} with {} textures",
                        sender.getName().getString(), payload.textures().size());
                
                if (payload.textures().isEmpty()) {
                    CACHED_MANIFESTS.remove(sender.getUUID());
                } else {
                    CACHED_MANIFESTS.put(sender.getUUID(), payload);
                }

                // Relay manifest to all other connected players
                for (ServerPlayer peer : context.server().getPlayerList().getPlayers()) {
                    if (!peer.getUUID().equals(sender.getUUID())) {
                        ServerPlayNetworking.send(peer, payload);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TextureRequestPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer target = context.server().getPlayerList().getPlayer(payload.targetPlayerUuid());
                if (target != null) {
                    LOGGER.info("[zmor-server] Forwarding texture request for '{}' to {}", payload.texturePath(), target.getName().getString());
                    ServerPlayNetworking.send(target, payload);
                } else {
                    LOGGER.warn("[zmor-server] Target player {} not found for texture request", payload.targetPlayerUuid());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TextureResponsePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer recipient = context.server().getPlayerList().getPlayer(payload.targetPlayerUuid());
                if (recipient != null) {
                    LOGGER.info("[zmor-server] Forwarding texture response for '{}' to {}", payload.texturePath(), recipient.getName().getString());
                    ServerPlayNetworking.send(recipient, payload);
                }
            });
        });

        // When a new player joins the server/LAN world:
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer joiningPlayer = handler.getPlayer();
            server.execute(() -> {
                LOGGER.info("[zmor-server] Player {} joined. Sending {} cached manifests",
                        joiningPlayer.getName().getString(), CACHED_MANIFESTS.size());
                
                // Send all existing cached manifests to the new joining player
                for (Map.Entry<UUID, TextureManifestPayload> entry : CACHED_MANIFESTS.entrySet()) {
                    if (!entry.getKey().equals(joiningPlayer.getUUID())) {
                        ServerPlayNetworking.send(joiningPlayer, entry.getValue());
                    }
                }
            });
        });

        // When a player leaves:
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID leavingUuid = handler.getPlayer().getUUID();
            CACHED_MANIFESTS.remove(leavingUuid);
        });
    }
}
