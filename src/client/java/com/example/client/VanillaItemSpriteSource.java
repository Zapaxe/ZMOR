package com.example.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;

public class VanillaItemSpriteSource implements SpriteSource {
    @Override
    public void run(ResourceManager resourceManager, Output output) {
        VanillaPackResources vanillaPack = Minecraft.getInstance().getVanillaPackResources();
        if (vanillaPack == null) return;

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

    @Override
    public MapCodec<? extends SpriteSource> codec() {
        return null;
    }
}
