package com.example.client;

import com.example.ModConfig;
import com.example.network.TextureManifestPayload;
import com.example.network.TextureRequestPayload;
import com.example.network.TextureResponsePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class ClientNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientNetworking.class);

    public static void init() {
        // Handle incoming texture manifest from other players
        ClientPlayNetworking.registerGlobalReceiver(TextureManifestPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            UUID peerUuid = payload.playerUuid();

            client.execute(() -> {
                if (client.player == null || peerUuid.equals(client.player.getUUID())) {
                    return;
                }

                // If peer sent an empty manifest (meaning they disabled sharing with others):
                if (payload.textures().isEmpty()) {
                    LOGGER.info("[zmor-client] Peer {} disabled sharing (empty manifest received)", peerUuid);
                    RemoteTextureManager.removePlayer(peerUuid);
                    return;
                }

                // If our client has peer texture sync disabled, ignore manifest:
                if (!ModConfig.syncPeerTextures) {
                    LOGGER.debug("[zmor-client] Peer texture sync is disabled; skipping manifest from {}", peerUuid);
                    return;
                }

                LOGGER.info("[zmor-client] Received texture manifest from peer {} with {} textures", peerUuid, payload.textures().size());

                for (Map.Entry<String, String> entry : payload.textures().entrySet()) {
                    String path = entry.getKey();
                    String hash = entry.getValue();

                    String cachedHash = RemoteTextureManager.getRemoteHash(peerUuid, path);
                    boolean hasTexture = false;
                    if (path.startsWith("item:")) {
                        String itemStrId = path.substring("item:".length());
                        hasTexture = RemoteTextureManager.getRemoteTextureForItem(peerUuid, itemStrId, null, null) != null;
                    } else {
                        hasTexture = RemoteTextureManager.getRemoteTexture(peerUuid, path) != null;
                    }

                    if (!hasTexture || cachedHash == null || !cachedHash.equals(hash)) {
                        LOGGER.info("[zmor-client] Requesting missing texture '{}' from peer {}", path, peerUuid);
                        ClientPlayNetworking.send(new TextureRequestPayload(client.player.getUUID(), peerUuid, path));
                    }
                }
            });
        });

        // Handle request from a peer for one of our custom textures
        ClientPlayNetworking.registerGlobalReceiver(TextureRequestPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();

            client.execute(() -> {
                if (client.player == null || !payload.targetPlayerUuid().equals(client.player.getUUID())) {
                    return;
                }

                // If sharing to other players is disabled, REJECT the request!
                if (!ModConfig.applyToOtherPlayers) {
                    LOGGER.info("[zmor-client] Refusing texture request from {} because 'Other Players' sharing is disabled", payload.requesterUuid());
                    return;
                }

                String path = payload.texturePath();
                LOGGER.info("[zmor-client] Peer {} requested texture '{}', reading and sending bytes", payload.requesterUuid(), path);
                
                byte[] bytes = null;
                if (path.startsWith("item:")) {
                    String itemStrId = path.substring("item:".length());
                    bytes = LocalTextureExtractor.getArmorItemTextureBytes(itemStrId);
                } else {
                    bytes = LocalTextureExtractor.getActiveTextureBytes(path);
                }

                if (bytes != null && bytes.length > 0) {
                    ClientPlayNetworking.send(new TextureResponsePayload(payload.requesterUuid(), client.player.getUUID(), path, bytes));
                } else {
                    LOGGER.warn("[zmor-client] Could not find bytes for requested texture '{}'", path);
                }
            });
        });

        // Handle incoming texture data from a peer
        ClientPlayNetworking.registerGlobalReceiver(TextureResponsePayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();

            client.execute(() -> {
                if (client.player == null) return;
                UUID me = client.player.getUUID();
                if (!payload.targetPlayerUuid().equals(me) && !payload.targetPlayerUuid().equals(new UUID(0, 0))) {
                    return;
                }

                // If peer texture sync is disabled, discard incoming texture data
                if (!ModConfig.syncPeerTextures) {
                    return;
                }

                LOGGER.info("[zmor-client] Received binary texture data for '{}' from peer {} ({} bytes)",
                        payload.texturePath(), payload.ownerUuid(), payload.pngData().length);

                RemoteTextureManager.registerRemoteTexture(payload.ownerUuid(), payload.texturePath(), payload.pngData());
                String hash = LocalTextureExtractor.computeHash(payload.pngData());
                if (hash != null) {
                    RemoteTextureManager.setRemoteHash(payload.ownerUuid(), payload.texturePath(), hash);
                }
            });
        });

        // On joining server/LAN world: scan and broadcast local manifest
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                LOGGER.info("[zmor-client] Connected to server, scanning and broadcasting local texture manifest...");
                broadcastLocalManifest();
            });
        });

        // On disconnect: clear cached remote textures
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("[zmor-client] Disconnected, clearing remote texture cache");
            RemoteTextureManager.clear();
        });
    }

    public static void broadcastLocalManifest() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;

        // If sharing is disabled, broadcast empty manifest so peers clear any existing custom textures
        if (!ModConfig.applyToOtherPlayers) {
            try {
                if (ClientPlayNetworking.canSend(TextureManifestPayload.TYPE)) {
                    ClientPlayNetworking.send(new TextureManifestPayload(client.player.getUUID(), Collections.emptyMap()));
                    LOGGER.info("[zmor-client] Sharing disabled: broadcasted empty manifest to clear peer caches");
                }
            } catch (Exception e) {
                LOGGER.error("[zmor-client] Failed to send empty manifest", e);
            }
            return;
        }

        Map<String, String> textures = LocalTextureExtractor.scanActiveTextures();
        if (!textures.isEmpty()) {
            try {
                if (ClientPlayNetworking.canSend(TextureManifestPayload.TYPE)) {
                    ClientPlayNetworking.send(new TextureManifestPayload(client.player.getUUID(), textures));
                    LOGGER.info("[zmor-client] Broadcasted texture manifest with {} textures to server/LAN", textures.size());
                }
            } catch (Exception e) {
                LOGGER.error("[zmor-client] Failed to send texture manifest", e);
            }
        }
    }
}
