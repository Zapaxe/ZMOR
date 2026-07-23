package com.example.client.mixin;

import com.example.ModConfig;
import com.example.client.ItemRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

    @Inject(method = "updateForLiving", at = @At("HEAD"))
    private void onUpdateForLiving(ItemStackRenderState state, ItemStack stack,
                                    ItemDisplayContext displayContext, LivingEntity entity,
                                    CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || entity == null) {
            ItemRenderContext.setLocalPlayer(true);
            return;
        }
        if (entity.getUUID().equals(client.player.getUUID())) {
            ItemRenderContext.setLocalPlayer(true);
        } else {
            ItemRenderContext.setLocalPlayer(ModConfig.applyToOtherPlayers);
        }
    }

    @Inject(method = "updateForLiving", at = @At("RETURN"))
    private void afterUpdateForLiving(CallbackInfo ci) {
        ItemRenderContext.setLocalPlayer(true);
    }

    @Inject(method = "updateForNonLiving", at = @At("HEAD"))
    private void onUpdateForNonLiving(ItemStackRenderState state, ItemStack stack,
                                       ItemDisplayContext displayContext, Entity entity,
                                       CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            ItemRenderContext.setLocalPlayer(true);
            return;
        }
        if (entity instanceof ItemFrame) {
            ItemRenderContext.setLocalPlayer(ModConfig.applyToItemFrames);
        } else if (entity instanceof ArmorStand) {
            ItemRenderContext.setLocalPlayer(ModConfig.applyToMobsAndArmorStands);
        } else if (entity instanceof ItemEntity) {
            ItemRenderContext.setLocalPlayer(true);
        } else {
            ItemRenderContext.setLocalPlayer(ModConfig.applyToMobsAndArmorStands);
        }
    }

    @Inject(method = "updateForNonLiving", at = @At("RETURN"))
    private void afterUpdateForNonLiving(CallbackInfo ci) {
        ItemRenderContext.setLocalPlayer(true);
    }

    @Inject(method = "appendItemLayers", at = @At("RETURN"))
    private void afterAppendItemLayers(ItemStackRenderState state, ItemStack stack,
                                        ItemDisplayContext displayContext, Level level,
                                        ItemOwner itemOwner, int seed, CallbackInfo ci) {
        boolean shouldRemap = !ItemRenderContext.isLocalPlayer() || !ModConfig.shouldApplyTo(stack);
        if (shouldRemap) {
            remapToVanilla(state);
        }
    }

    private void remapToVanilla(ItemStackRenderState state) {
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_ITEMS);
        if (atlas == null) return;

        ItemStackRenderStateAccessor accessor = (ItemStackRenderStateAccessor) state;
        int layerCount = accessor.getActiveLayerCount();
        ItemStackRenderState.LayerRenderState[] layers = accessor.getLayers();

        for (int i = 0; i < layerCount; i++) {
            remapLayer(layers[i], atlas);
        }
    }

    private void remapLayer(ItemStackRenderState.LayerRenderState layer, TextureAtlas atlas) {
        LayerRenderStateAccessor layerAccessor = (LayerRenderStateAccessor) (Object) layer;
        List<BakedQuad> quads = layerAccessor.getQuads();
        if (quads == null || quads.isEmpty()) return;

        boolean anyRemapped = false;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            TextureAtlasSprite originalSprite = quad.sprite();
            if (originalSprite == null) continue;

            Identifier spriteId = originalSprite.contents().name();
            String namespace = spriteId.getNamespace();
            String path = spriteId.getPath();
            if (!namespace.equals("minecraft") || !path.startsWith("item/")) continue;

            Identifier vanillaId = Identifier.fromNamespaceAndPath("zmor-vanilla", path);
            TextureAtlasSprite vanillaSprite = atlas.getSprite(vanillaId);
            if (vanillaSprite == null || vanillaSprite == atlas.missingSprite()) continue;

            quads.set(i, remapQuad(quad, originalSprite, vanillaSprite));
            anyRemapped = true;
        }

        if (anyRemapped) {
            TextureAtlasSprite icon = layerAccessor.getParticleIcon();
            if (icon != null) {
                Identifier iconId = icon.contents().name();
                if (iconId.getNamespace().equals("minecraft") && iconId.getPath().startsWith("item/")) {
                    Identifier vanillaIconId = Identifier.fromNamespaceAndPath("zmor-vanilla", iconId.getPath());
                    TextureAtlasSprite vanillaIcon = atlas.getSprite(vanillaIconId);
                    if (vanillaIcon != null && vanillaIcon != atlas.missingSprite()) {
                        layerAccessor.setParticleIcon(vanillaIcon);
                    }
                }
            }
        }
    }

    private static float unpackU(long packed) {
        return Float.intBitsToFloat((int) (packed >> 32));
    }

    private static float unpackV(long packed) {
        return Float.intBitsToFloat((int) packed);
    }

    private static long packUV(float u, float v) {
        return ((long) Float.floatToRawIntBits(u) << 32) | (Float.floatToRawIntBits(v) & 0xFFFFFFFFL);
    }

    private BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite original, TextureAtlasSprite vanilla) {
        float u0 = original.getU0();
        float v0 = original.getV0();
        float u1 = original.getU1();
        float v1 = original.getV1();
        float u0v = vanilla.getU0();
        float v0v = vanilla.getV0();
        float u1v = vanilla.getU1();
        float v1v = vanilla.getV1();

        float uScale = (u1v - u0v) / (u1 - u0);
        float vScale = (v1v - v0v) / (v1 - v0);

        long[] newUVs = new long[4];
        for (int i = 0; i < 4; i++) {
            long packedUV = quad.packedUV(i);
            float oldU = unpackU(packedUV);
            float oldV = unpackV(packedUV);
            float newU = u0v + (oldU - u0) * uScale;
            float newV = v0v + (oldV - v0) * vScale;
            newUVs[i] = packUV(newU, newV);
        }

        return new BakedQuad(
            quad.position0(), quad.position1(), quad.position2(), quad.position3(),
            newUVs[0], newUVs[1], newUVs[2], newUVs[3],
            quad.tintIndex(), quad.direction(), vanilla,
            quad.shade(), quad.lightEmission()
        );
    }
}
