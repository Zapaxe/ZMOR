package com.example.client;

import com.example.ModConfig;
import com.example.client.mixin.MultiPackResourceManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class LocalTextureExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTextureExtractor.class);

    public static final Map<String, String> ARMOR_ITEM_TO_TEXTURE = Map.ofEntries(
            Map.entry("minecraft:leather_helmet", "textures/entity/equipment/humanoid/leather.png"),
            Map.entry("minecraft:leather_chestplate", "textures/entity/equipment/humanoid/leather.png"),
            Map.entry("minecraft:leather_leggings", "textures/entity/equipment/humanoid_leggings/leather.png"),
            Map.entry("minecraft:leather_boots", "textures/entity/equipment/humanoid/leather.png"),

            Map.entry("minecraft:chainmail_helmet", "textures/entity/equipment/humanoid/chainmail.png"),
            Map.entry("minecraft:chainmail_chestplate", "textures/entity/equipment/humanoid/chainmail.png"),
            Map.entry("minecraft:chainmail_leggings", "textures/entity/equipment/humanoid_leggings/chainmail.png"),
            Map.entry("minecraft:chainmail_boots", "textures/entity/equipment/humanoid/chainmail.png"),

            Map.entry("minecraft:iron_helmet", "textures/entity/equipment/humanoid/iron.png"),
            Map.entry("minecraft:iron_chestplate", "textures/entity/equipment/humanoid/iron.png"),
            Map.entry("minecraft:iron_leggings", "textures/entity/equipment/humanoid_leggings/iron.png"),
            Map.entry("minecraft:iron_boots", "textures/entity/equipment/humanoid/iron.png"),

            Map.entry("minecraft:golden_helmet", "textures/entity/equipment/humanoid/gold.png"),
            Map.entry("minecraft:golden_chestplate", "textures/entity/equipment/humanoid/gold.png"),
            Map.entry("minecraft:golden_leggings", "textures/entity/equipment/humanoid_leggings/gold.png"),
            Map.entry("minecraft:golden_boots", "textures/entity/equipment/humanoid/gold.png"),

            Map.entry("minecraft:diamond_helmet", "textures/entity/equipment/humanoid/diamond.png"),
            Map.entry("minecraft:diamond_chestplate", "textures/entity/equipment/humanoid/diamond.png"),
            Map.entry("minecraft:diamond_leggings", "textures/entity/equipment/humanoid_leggings/diamond.png"),
            Map.entry("minecraft:diamond_boots", "textures/entity/equipment/humanoid/diamond.png"),

            Map.entry("minecraft:netherite_helmet", "textures/entity/equipment/humanoid/netherite.png"),
            Map.entry("minecraft:netherite_chestplate", "textures/entity/equipment/humanoid/netherite.png"),
            Map.entry("minecraft:netherite_leggings", "textures/entity/equipment/humanoid_leggings/netherite.png"),
            Map.entry("minecraft:netherite_boots", "textures/entity/equipment/humanoid/netherite.png"),

            Map.entry("minecraft:turtle_helmet", "textures/entity/equipment/humanoid/turtle_scute.png"),
            Map.entry("minecraft:elytra", "textures/entity/equipment/humanoid/elytra.png")
    );

    private static final List<String> COMMON_ARMOR_PATHS = List.of(
            "textures/models/armor/diamond_layer_1.png",
            "textures/models/armor/diamond_layer_2.png",
            "textures/models/armor/netherite_layer_1.png",
            "textures/models/armor/netherite_layer_2.png",
            "textures/models/armor/iron_layer_1.png",
            "textures/models/armor/iron_layer_2.png",
            "textures/models/armor/gold_layer_1.png",
            "textures/models/armor/gold_layer_2.png",
            "textures/models/armor/chainmail_layer_1.png",
            "textures/models/armor/chainmail_layer_2.png",
            "textures/models/armor/leather_layer_1.png",
            "textures/models/armor/leather_layer_2.png",
            "textures/models/armor/turtle_layer_1.png",
            "textures/entity/equipment/humanoid/diamond.png",
            "textures/entity/equipment/humanoid_leggings/diamond.png",
            "textures/entity/equipment/humanoid/netherite.png",
            "textures/entity/equipment/humanoid_leggings/netherite.png",
            "textures/entity/equipment/humanoid/iron.png",
            "textures/entity/equipment/humanoid_leggings/iron.png",
            "textures/entity/equipment/humanoid/gold.png",
            "textures/entity/equipment/humanoid_leggings/gold.png",
            "textures/entity/equipment/humanoid/chainmail.png",
            "textures/entity/equipment/humanoid_leggings/chainmail.png",
            "textures/entity/equipment/humanoid/leather.png",
            "textures/entity/equipment/humanoid_leggings/leather.png",
            "textures/entity/equipment/humanoid/turtle_scute.png",
            "textures/entity/equipment/humanoid/elytra.png"
    );

    public static byte[] getActiveTextureBytes(String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) return null;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getResourceManager() == null) return null;

        try {
            Identifier id = Identifier.tryParse(texturePath.contains(":") ? texturePath : "minecraft:" + texturePath);
            if (id != null) {
                Optional<Resource> res = client.getResourceManager().getResource(id);
                if (res.isPresent()) {
                    try (InputStream is = res.get().open()) {
                        return is.readAllBytes();
                    }
                }
            }

            // Fallback alternate path
            String alternatePath = toAlternateArmorPath(texturePath);
            if (alternatePath != null) {
                Identifier altId = Identifier.fromNamespaceAndPath("minecraft", alternatePath);
                Optional<Resource> altRes = client.getResourceManager().getResource(altId);
                if (altRes.isPresent()) {
                    try (InputStream is = altRes.get().open()) {
                        return is.readAllBytes();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[zmor] Error reading texture bytes for {}", texturePath, e);
        }
        return null;
    }

    public static byte[] getTextureBytesForPack(String packId, String texturePath) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return null;

        if (packId == null || packId.equals("default") || packId.equals("top")) {
            return getActiveTextureBytes(texturePath);
        }

        if (packId.equals("vanilla")) {
            VanillaPackResources vanilla = client.getVanillaPackResources();
            if (vanilla != null) {
                Identifier id = Identifier.fromNamespaceAndPath("minecraft", texturePath);
                IoSupplier<InputStream> sup = vanilla.getResource(PackType.CLIENT_RESOURCES, id);
                if (sup == null) {
                    String alt = toAlternateArmorPath(texturePath);
                    if (alt != null) sup = vanilla.getResource(PackType.CLIENT_RESOURCES, Identifier.fromNamespaceAndPath("minecraft", alt));
                }
                if (sup != null) {
                    try (InputStream is = sup.get()) {
                        return is.readAllBytes();
                    } catch (Exception ignored) {}
                }
            }
            return null;
        }

        // Custom resource pack
        PackResources pack = getPackResourcesById(packId);
        if (pack != null) {
            Identifier id = Identifier.fromNamespaceAndPath("minecraft", texturePath);
            IoSupplier<InputStream> sup = pack.getResource(PackType.CLIENT_RESOURCES, id);
            if (sup == null) {
                String alt = toAlternateArmorPath(texturePath);
                if (alt != null) sup = pack.getResource(PackType.CLIENT_RESOURCES, Identifier.fromNamespaceAndPath("minecraft", alt));
            }
            if (sup != null) {
                try (InputStream is = sup.get()) {
                    return is.readAllBytes();
                } catch (Exception ignored) {}
            }
        }

        return getActiveTextureBytes(texturePath);
    }

    public static byte[] getArmorItemTextureBytes(String itemStrId) {
        String baseTexture = ARMOR_ITEM_TO_TEXTURE.get(itemStrId);
        if (baseTexture == null) return null;

        String packId = ModConfig.getItemPack(itemStrId);
        return getTextureBytesForPack(packId, baseTexture);
    }

    public static PackResources getPackResourcesById(String targetId) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || targetId == null) return null;

        if ("vanilla".equals(targetId)) {
            return client.getVanillaPackResources();
        }

        ResourceManager rm = client.getResourceManager();
        if (rm instanceof MultiPackResourceManager multiPackManager) {
            List<PackResources> openPacks = ((MultiPackResourceManagerAccessor) multiPackManager).getPacks();
            if (openPacks != null) {
                for (PackResources pack : openPacks) {
                    if (pack.packId().equals(targetId) || VanillaItemSpriteSource.sanitizePackId(pack.packId()).equals(targetId)) {
                        return pack;
                    }
                }
            }
        }
        return null;
    }

    public static Map<String, String> scanActiveTextures() {
        Map<String, String> manifest = new HashMap<>();
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getResourceManager() == null) return manifest;

        // 1. Scan and register specific textures for each armor item with its configured pack
        for (Map.Entry<String, String> entry : ARMOR_ITEM_TO_TEXTURE.entrySet()) {
            String itemStrId = entry.getKey();
            byte[] bytes = getArmorItemTextureBytes(itemStrId);
            if (bytes != null && bytes.length > 0) {
                String hash = computeHash(bytes);
                if (hash != null) {
                    manifest.put("item:" + itemStrId, hash);
                }
            }
        }

        // 2. Scan all global active armor textures for legacy/general fallbacks
        for (String path : COMMON_ARMOR_PATHS) {
            byte[] bytes = getActiveTextureBytes(path);
            if (bytes != null && bytes.length > 0) {
                String hash = computeHash(bytes);
                if (hash != null) {
                    manifest.put(path, hash);
                }
            }
        }

        LOGGER.info("[zmor] Scanned {} active local custom textures to broadcast (including per-item configs)", manifest.size());
        return manifest;
    }

    public static String computeHash(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String toAlternateArmorPath(String path) {
        if (path == null) return null;
        if (path.contains("/humanoid_leggings/")) {
            String mat = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
            return "textures/models/armor/" + mat + "_layer_2.png";
        }
        if (path.contains("/humanoid/")) {
            String mat = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
            return "textures/models/armor/" + mat + "_layer_1.png";
        }
        if (path.contains("/armor/") && path.endsWith("_layer_1.png")) {
            String mat = path.substring(path.lastIndexOf('/') + 1, path.length() - "_layer_1.png".length());
            return "textures/entity/equipment/humanoid/" + mat + ".png";
        }
        if (path.contains("/armor/") && path.endsWith("_layer_2.png")) {
            String mat = path.substring(path.lastIndexOf('/') + 1, path.length() - "_layer_2.png".length());
            return "textures/entity/equipment/humanoid_leggings/" + mat + ".png";
        }
        if (path.contains("/horse/")) {
            String mat = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
            return "textures/entity/horse/armor/horse_armor_" + mat + ".png";
        }
        return null;
    }
}
