package com.example.client.mixin;

import com.example.client.VanillaItemSpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SpriteSourceList.class)
public class SpriteSourceListMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpriteSourceListMixin.class);

    @Inject(method = "load", at = @At("RETURN"))
    private static void onLoad(ResourceManager resourceManager, Identifier atlasId,
                                CallbackInfoReturnable<SpriteSourceList> cir) {
        if (!atlasId.getNamespace().equals("minecraft") || !atlasId.getPath().equals("items")) return;

        SpriteSourceList list = cir.getReturnValue();
        if (list != null) {
            List<SpriteSource> sources = ((SpriteSourceListAccessor) list).getSources();
            int n = sources.size();
            sources.add(new VanillaItemSpriteSource());
            LOGGER.info("[zmor] injected VanillaItemSpriteSource (sources: {} -> {})", n, n + 1);
        }
    }
}
