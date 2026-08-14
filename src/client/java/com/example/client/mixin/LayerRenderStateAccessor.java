package com.example.client.mixin;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public interface LayerRenderStateAccessor {
    @Accessor("quads")
    List<BakedQuad> getQuads();
}
