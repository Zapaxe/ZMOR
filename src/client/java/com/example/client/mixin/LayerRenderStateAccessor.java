package com.example.client.mixin;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.function.Supplier;

import org.joml.Vector3fc;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public interface LayerRenderStateAccessor {
    @Accessor("quads")
    List<BakedQuad> getQuads();

    @Accessor("particleIcon")
    TextureAtlasSprite getParticleIcon();

    @Accessor("particleIcon")
    void setParticleIcon(TextureAtlasSprite icon);

    @Accessor("extents")
    Supplier<Vector3fc[]> getExtents();

    @Accessor("extents")
    void setExtents(Supplier<Vector3fc[]> extents);
}
