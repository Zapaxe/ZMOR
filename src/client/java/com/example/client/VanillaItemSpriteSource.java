package com.example.client;

import com.example.ModConfig;
import com.example.client.mixin.MultiPackResourceManagerAccessor;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;

import java.util.List;
import java.util.Locale;

public class VanillaItemSpriteSource implements SpriteSource {

    public static String sanitizePackId(String packId) {
        if (packId == null) return "vanilla";
        return packId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        VanillaPackResources vanillaPack = Minecraft.getInstance().getVanillaPackResources();
        if (vanillaPack != null) {
            vanillaPack.listResources(PackType.CLIENT_RESOURCES, "minecraft", "textures/item",
                (id, supplier) -> {
                    String path = id.getPath();
                    if (path.startsWith("textures/item/") && path.endsWith(".png")) {
                        String spritePath = path.substring("textures/".length(), path.length() - ".png".length());
                        Identifier zmorId = Identifier.fromNamespaceAndPath("zmor-vanilla", spritePath);
                        output.add(zmorId, new Resource(vanillaPack, supplier));
                    }
                }
            );
        }

        PackResources basePack = getBasePackResources(resourceManager);
        if (basePack != null && basePack != vanillaPack) {
            basePack.listResources(PackType.CLIENT_RESOURCES, "minecraft", "textures/item",
                (id, supplier) -> {
                    String path = id.getPath();
                    if (path.startsWith("textures/item/") && path.endsWith(".png")) {
                        String spritePath = path.substring("textures/".length(), path.length() - ".png".length());
                        Identifier zmorId = Identifier.fromNamespaceAndPath("zmor-vanilla", spritePath);
                        output.add(zmorId, new Resource(basePack, supplier));
                    }
                }
            );
        }

        // Register custom pack sprites for per-item overrides
        if (resourceManager instanceof MultiPackResourceManager multiPackManager) {
            List<PackResources> openPacks = ((MultiPackResourceManagerAccessor) multiPackManager).getPacks();
            if (openPacks != null) {
                for (PackResources pack : openPacks) {
                    String packNs = "zmor-pk-" + sanitizePackId(pack.packId());
                    pack.listResources(PackType.CLIENT_RESOURCES, "minecraft", "textures/item",
                        (id, supplier) -> {
                            String path = id.getPath();
                            if (path.startsWith("textures/item/") && path.endsWith(".png")) {
                                String spritePath = path.substring("textures/".length(), path.length() - ".png".length());
                                Identifier customSpriteId = Identifier.fromNamespaceAndPath(packNs, spritePath);
                                output.add(customSpriteId, new Resource(pack, supplier));
                            }
                        }
                    );
                }
            }
        }
    }

    private PackResources getBasePackResources(ResourceManager resourceManager) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return null;

        String targetId = ModConfig.baseResourcePackId;
        if ("vanilla".equals(targetId) || targetId == null) {
            return client.getVanillaPackResources();
        }

        if (resourceManager instanceof MultiPackResourceManager multiPackManager) {
            List<PackResources> openPacks = ((MultiPackResourceManagerAccessor) multiPackManager).getPacks();
            if (openPacks != null) {
                for (PackResources pack : openPacks) {
                    if (pack.packId().equals(targetId)) {
                        return pack;
                    }
                }
            }
        }
        return client.getVanillaPackResources();
    }

    @Override
    public MapCodec<? extends SpriteSource> codec() {
        return null;
    }
}
