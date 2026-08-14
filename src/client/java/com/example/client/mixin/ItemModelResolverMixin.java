package com.example.client.mixin;

import com.example.ModConfig;
import com.example.client.ItemRenderContext;
import com.example.client.ItemRenderContext.TargetType;
import com.example.client.VanillaItemSpriteSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
            ItemRenderContext.setTarget(TargetType.LOCAL_PLAYER);
            return;
        }
        if (entity.getUUID().equals(client.player.getUUID())) {
            ItemRenderContext.setTarget(TargetType.LOCAL_PLAYER);
        } else if (entity instanceof Player) {
            ItemRenderContext.setTarget(TargetType.OTHER_PLAYER);
        } else {
            ItemRenderContext.setTarget(TargetType.MOB_OR_ARMOR_STAND);
        }
    }

    @Inject(method = "updateForLiving", at = @At("RETURN"))
    private void afterUpdateForLiving(CallbackInfo ci) {
        ItemRenderContext.setTarget(TargetType.LOCAL_PLAYER);
    }

    @Inject(method = "updateForNonLiving", at = @At("HEAD"))
    private void onUpdateForNonLiving(ItemStackRenderState state, ItemStack stack,
                                       ItemDisplayContext displayContext, Entity entity,
                                       CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || entity == null) {
            ItemRenderContext.setTarget(TargetType.LOCAL_PLAYER);
            return;
        }
        if (entity instanceof ItemFrame) {
            ItemRenderContext.setTarget(TargetType.ITEM_FRAME);
        } else if (entity instanceof ArmorStand) {
            ItemRenderContext.setTarget(TargetType.MOB_OR_ARMOR_STAND);
        } else if (entity instanceof ItemEntity) {
            ItemRenderContext.setTarget(TargetType.WORLD_ITEM);
        } else {
            ItemRenderContext.setTarget(TargetType.MOB_OR_ARMOR_STAND);
        }
    }

    @Inject(method = "updateForNonLiving", at = @At("RETURN"))
    private void afterUpdateForNonLiving(CallbackInfo ci) {
        ItemRenderContext.setTarget(TargetType.LOCAL_PLAYER);
    }

    @Inject(method = "appendItemLayers", at = @At("RETURN"))
    private void afterAppendItemLayers(ItemStackRenderState state, ItemStack stack,
                                        ItemDisplayContext displayContext, Level level,
                                        ItemOwner itemOwner, int seed, CallbackInfo ci) {
        TargetType target = ItemRenderContext.getTarget();
        Identifier itemId = stack != null && !stack.isEmpty() ? BuiltInRegistries.ITEM.getKey(stack.getItem()) : null;
        String itemStrId = itemId != null ? itemId.toString() : null;
        String perItemPack = itemStrId != null ? ModConfig.getItemPack(itemStrId) : "default";

        if (target == TargetType.LOCAL_PLAYER || target == TargetType.WORLD_ITEM) {
            String targetNs = resolveEffectiveNamespace(perItemPack);
            if (targetNs != null) {
                remapToNamespace(state, targetNs);
            }
            return;
        }

        if (target == TargetType.OTHER_PLAYER && !ModConfig.applyToOtherPlayers) {
            remapToNamespace(state, "zmor-vanilla");
            return;
        }
        if (target == TargetType.MOB_OR_ARMOR_STAND && !ModConfig.applyToMobsAndArmorStands) {
            remapToNamespace(state, "zmor-vanilla");
            return;
        }
        if (target == TargetType.ITEM_FRAME && !ModConfig.applyToItemFrames) {
            remapToNamespace(state, "zmor-vanilla");
            return;
        }

        boolean allowCustom = switch (target) {
            case OTHER_PLAYER -> ModConfig.applyToOtherPlayers;
            case MOB_OR_ARMOR_STAND -> ModConfig.applyToMobsAndArmorStands;
            case ITEM_FRAME -> ModConfig.applyToItemFrames;
            default -> true;
        };

        if (allowCustom && !ModConfig.filteredItems.isEmpty()) {
            allowCustom = ModConfig.isItemWhitelisted(stack);
        }

        if (!allowCustom) {
            remapToNamespace(state, "zmor-vanilla");
        } else {
            String targetNs = resolveEffectiveNamespace(perItemPack);
            if (targetNs != null) {
                remapToNamespace(state, targetNs);
            }
        }
    }

    @Unique
    private String resolveEffectiveNamespace(String perItemPack) {
        if (!perItemPack.equals("default")) {
            if (perItemPack.equals("vanilla")) {
                return "zmor-vanilla";
            } else {
                return "zmor-pk-" + VanillaItemSpriteSource.sanitizePackId(perItemPack);
            }
        }
        if (!ModConfig.mainOverridePackId.equals("top")) {
            if (ModConfig.mainOverridePackId.equals("vanilla")) {
                return "zmor-vanilla";
            } else {
                return "zmor-pk-" + VanillaItemSpriteSource.sanitizePackId(ModConfig.mainOverridePackId);
            }
        }
        return null;
    }

    private void remapToNamespace(ItemStackRenderState state, String targetNamespace) {
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_ITEMS);
        if (atlas == null) return;

        ItemStackRenderStateAccessor accessor = (ItemStackRenderStateAccessor) state;
        int layerCount = accessor.getActiveLayerCount();
        ItemStackRenderState.LayerRenderState[] layers = accessor.getLayers();

        for (int i = 0; i < layerCount; i++) {
            remapLayer(layers[i], atlas, targetNamespace);
        }
    }

    private void remapLayer(ItemStackRenderState.LayerRenderState layer, TextureAtlas atlas, String targetNamespace) {
        LayerRenderStateAccessor layerAccessor = (LayerRenderStateAccessor) (Object) layer;
        List<BakedQuad> quads = layerAccessor.getQuads();
        if (quads == null || quads.isEmpty()) return;

        boolean anyRemapped = false;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            TextureAtlasSprite originalSprite = quad.sprite();
            if (originalSprite == null) continue;

            Identifier spriteId = originalSprite.contents().name();
            String path = spriteId.getPath();
            if (!path.startsWith("item/")) continue;

            Identifier customId = Identifier.fromNamespaceAndPath(targetNamespace, path);
            TextureAtlasSprite customSprite = atlas.getSprite(customId);
            if (customSprite == null || customSprite == atlas.missingSprite()) continue;

            quads.set(i, remapQuad(quad, originalSprite, customSprite));
            anyRemapped = true;
        }

        if (anyRemapped) {
            TextureAtlasSprite icon = layerAccessor.getParticleIcon();
            if (icon != null) {
                Identifier iconId = icon.contents().name();
                if (iconId.getPath().startsWith("item/")) {
                    Identifier customIconId = Identifier.fromNamespaceAndPath(targetNamespace, iconId.getPath());
                    TextureAtlasSprite customIcon = atlas.getSprite(customIconId);
                    if (customIcon != null && customIcon != atlas.missingSprite()) {
                        layerAccessor.setParticleIcon(customIcon);
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

    private BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite original, TextureAtlasSprite target) {
        float u0 = original.getU0();
        float v0 = original.getV0();
        float u1 = original.getU1();
        float v1 = original.getV1();
        float u0v = target.getU0();
        float v0v = target.getV0();
        float u1v = target.getU1();
        float v1v = target.getV1();

        float origDu = u1 - u0;
        float origDv = v1 - v0;
        float targetDu = u1v - u0v;
        float targetDv = v1v - v0v;

        if (Math.abs(origDu) < 1e-6f || Math.abs(origDv) < 1e-6f) {
            return quad;
        }

        long[] newUVs = new long[4];
        for (int i = 0; i < 4; i++) {
            long packedUV = quad.packedUV(i);
            float oldU = unpackU(packedUV);
            float oldV = unpackV(packedUV);

            // Normalized position [0.0 .. 1.0] within the sprite bounds
            float normU = (oldU - u0) / origDu;
            float normV = (oldV - v0) / origDv;

            // Clamping tightly prevents subpixel bleeding into neighboring atlas textures (e.g. grass/foliage)
            normU = Math.max(0.0f, Math.min(1.0f, normU));
            normV = Math.max(0.0f, Math.min(1.0f, normV));

            float newU = u0v + normU * targetDu;
            float newV = v0v + normV * targetDv;
            newUVs[i] = packUV(newU, newV);
        }

        return new BakedQuad(
            quad.position0(), quad.position1(), quad.position2(), quad.position3(),
            newUVs[0], newUVs[1], newUVs[2], newUVs[3],
            quad.tintIndex(), quad.direction(), target,
            quad.shade(), quad.lightEmission()
        );
    }
}
