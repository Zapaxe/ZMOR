package com.example.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteTextureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteTextureManager.class);
    
    // Maps "playerUUID:textureKey" -> registered Identifier
    private static final Map<String, Identifier> REMOTE_TEXTURES = new ConcurrentHashMap<>();
    
    // Maps "playerUUID:textureKey" -> SHA-256 hash string
    private static final Map<String, String> REMOTE_HASHES = new ConcurrentHashMap<>();

    public static Identifier registerRemoteTexture(UUID playerUuid, String textureKey, byte[] pngData) {
        if (playerUuid == null || textureKey == null || pngData == null || pngData.length == 0) {
            return null;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(pngData);
            NativeImage nativeImage = NativeImage.read(bais);
            
            // Security guard: Cap max texture dimensions to 256x256
            if (nativeImage.getWidth() > 256 || nativeImage.getHeight() > 256) {
                LOGGER.warn("[zmor] Rejected remote texture from {}: dimensions {}x{} exceed 256x256 limit",
                        playerUuid, nativeImage.getWidth(), nativeImage.getHeight());
                return null;
            }

            DynamicTexture dynamicTexture = new DynamicTexture(() -> "zmor-remote-" + textureKey, nativeImage);
            
            // Create identifier e.g. zmor-remote:uuid/textureKey
            String sanitizeKey = textureKey.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            String sanitizeUuid = playerUuid.toString().toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            Identifier identifier = Identifier.fromNamespaceAndPath("zmor-remote", sanitizeUuid + "/" + sanitizeKey);

            // Register with Minecraft TextureManager
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().getTextureManager().register(identifier, dynamicTexture);
            });

            String mapKey = playerUuid.toString() + ":" + textureKey;
            REMOTE_TEXTURES.put(mapKey, identifier);
            LOGGER.info("[zmor] Registered remote texture {} for player {}", identifier, playerUuid);
            
            return identifier;
        } catch (IOException e) {
            LOGGER.error("[zmor] Failed to decode remote texture PNG for player {}", playerUuid, e);
            return null;
        }
    }

    public static Identifier getRemoteTexture(UUID playerUuid, String textureKey) {
        if (playerUuid == null || textureKey == null) return null;
        return REMOTE_TEXTURES.get(playerUuid.toString() + ":" + textureKey);
    }

    public static void setRemoteHash(UUID playerUuid, String textureKey, String hash) {
        if (playerUuid != null && textureKey != null && hash != null) {
            REMOTE_HASHES.put(playerUuid.toString() + ":" + textureKey, hash);
        }
    }

    public static String getRemoteHash(UUID playerUuid, String textureKey) {
        if (playerUuid == null || textureKey == null) return null;
        return REMOTE_HASHES.get(playerUuid.toString() + ":" + textureKey);
    }
    
    public static void clear() {
        REMOTE_TEXTURES.clear();
        REMOTE_HASHES.clear();
    }
}
