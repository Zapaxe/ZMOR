package com.example.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
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
    
    // Maps "playerUUID:textureKey" (or "playerUUID:item:itemStrId") -> registered Identifier
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
            
            // Security guard: Cap max texture dimensions to 512x512
            if (nativeImage.getWidth() > 512 || nativeImage.getHeight() > 512) {
                LOGGER.warn("[zmor] Rejected remote texture from {}: dimensions {}x{} exceed 512x512 limit",
                        playerUuid, nativeImage.getWidth(), nativeImage.getHeight());
                return null;
            }

            DynamicTexture dynamicTexture = new DynamicTexture(() -> "zmor-remote-" + textureKey, nativeImage);
            
            // Create identifier e.g. zmor-remote:uuid/textureKey
            String sanitizeKey = textureKey.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            String sanitizeUuid = playerUuid.toString().toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            Identifier identifier = Identifier.fromNamespaceAndPath("zmor-remote", sanitizeUuid + "/" + sanitizeKey);

            // Register with Minecraft TextureManager & upload texture buffer to GPU on the render thread
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.getTextureManager() != null) {
                client.execute(() -> {
                    dynamicTexture.upload();
                    client.getTextureManager().register(identifier, dynamicTexture);
                });
            }

            // Put main key
            String mainMapKey = playerUuid + ":" + textureKey;
            REMOTE_TEXTURES.put(mainMapKey, identifier);

            // If it's an alternate armor path, register alias
            if (!textureKey.startsWith("item:")) {
                String altKey = LocalTextureExtractor.toAlternateArmorPath(textureKey);
                if (altKey != null) {
                    REMOTE_TEXTURES.put(playerUuid + ":" + altKey, identifier);
                }
            }

            LOGGER.info("[zmor] Registered remote texture {} ({}) for player {}", identifier, textureKey, playerUuid);
            return identifier;
        } catch (IOException e) {
            LOGGER.error("[zmor] Failed to decode remote texture PNG for player {}", playerUuid, e);
            return null;
        }
    }

    public static Identifier getRemoteTextureForItem(UUID playerUuid, String itemStrId, String fallbackPath, EquipmentClientInfo.LayerType layerType) {
        if (playerUuid == null) return null;

        // 1. Check per-item specific texture override first! (e.g. peerUuid + ":item:minecraft:diamond_chestplate")
        if (itemStrId != null) {
            Identifier itemSpecific = REMOTE_TEXTURES.get(playerUuid + ":item:" + itemStrId);
            if (itemSpecific != null) {
                return itemSpecific;
            }
        }

        // 2. Fall back to layer-aware generic material texture
        return getRemoteTexture(playerUuid, fallbackPath, layerType);
    }

    public static Identifier getRemoteTexture(UUID playerUuid, String texturePath, EquipmentClientInfo.LayerType layerType) {
        if (playerUuid == null || texturePath == null) return null;
        
        String prefix = playerUuid + ":";
        
        // 1. Direct exact path match
        Identifier id = REMOTE_TEXTURES.get(prefix + texturePath);
        if (id != null) return id;

        // 2. Extract material name (e.g. "diamond", "netherite", "iron", "leather", "gold")
        String matName = extractMaterialName(texturePath);
        if (matName == null || matName.isEmpty()) {
            return null;
        }

        // 3. Strict Layer-Aware Match (Prevent layer 1 vs layer 2 cross-contamination)
        if (layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS) {
            // Layer 2 ONLY for this exact material (Leggings)
            id = REMOTE_TEXTURES.get(prefix + "textures/entity/equipment/humanoid_leggings/" + matName + ".png");
            if (id != null) return id;
            id = REMOTE_TEXTURES.get(prefix + "textures/models/armor/" + matName + "_layer_2.png");
            if (id != null) return id;
        } else if (layerType == EquipmentClientInfo.LayerType.HUMANOID) {
            // Layer 1 ONLY for this exact material (Helmet, Chestplate, Boots)
            id = REMOTE_TEXTURES.get(prefix + "textures/entity/equipment/humanoid/" + matName + ".png");
            if (id != null) return id;
            id = REMOTE_TEXTURES.get(prefix + "textures/models/armor/" + matName + "_layer_1.png");
            if (id != null) return id;
        } else if (layerType == EquipmentClientInfo.LayerType.WINGS) {
            id = REMOTE_TEXTURES.get(prefix + "textures/entity/equipment/humanoid/elytra.png");
            if (id != null) return id;
            id = REMOTE_TEXTURES.get(prefix + "textures/entity/elytra.png");
            if (id != null) return id;
        }

        return null;
    }

    public static Identifier getRemoteTexture(UUID playerUuid, String textureKey) {
        return getRemoteTexture(playerUuid, textureKey, EquipmentClientInfo.LayerType.HUMANOID);
    }

    public static String extractMaterialName(String path) {
        if (path == null) return null;
        String clean = path;
        if (clean.endsWith(".png")) {
            clean = clean.substring(0, clean.length() - 4);
        }
        if (clean.endsWith("_layer_1") || clean.endsWith("_layer_2")) {
            clean = clean.substring(0, clean.length() - 8);
        }
        if (clean.endsWith("_overlay")) {
            clean = clean.substring(0, clean.length() - 8);
        }
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        return clean.isEmpty() ? null : clean;
    }

    public static void setRemoteHash(UUID playerUuid, String textureKey, String hash) {
        if (playerUuid != null && textureKey != null && hash != null) {
            REMOTE_HASHES.put(playerUuid + ":" + textureKey, hash);
            if (!textureKey.startsWith("item:")) {
                String alt = LocalTextureExtractor.toAlternateArmorPath(textureKey);
                if (alt != null) {
                    REMOTE_HASHES.put(playerUuid + ":" + alt, hash);
                }
            }
        }
    }

    public static String getRemoteHash(UUID playerUuid, String textureKey) {
        if (playerUuid == null || textureKey == null) return null;
        String hash = REMOTE_HASHES.get(playerUuid + ":" + textureKey);
        if (hash != null) return hash;

        if (!textureKey.startsWith("item:")) {
            String alt = LocalTextureExtractor.toAlternateArmorPath(textureKey);
            if (alt != null) {
                return REMOTE_HASHES.get(playerUuid + ":" + alt);
            }
        }
        return null;
    }
    
    public static void clear() {
        REMOTE_TEXTURES.clear();
        REMOTE_HASHES.clear();
    }
}
