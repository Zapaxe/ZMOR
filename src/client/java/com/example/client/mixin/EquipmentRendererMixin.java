package com.example.client.mixin;

import com.example.ModConfig;
import com.example.client.LocalPlayerRenderStateAccessor;
import com.example.client.RemoteTextureManager;
import com.example.client.VanillaItemSpriteSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentRendererMixin {

    @Redirect(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private <S> RenderType redirectArmorCutout(
            Identifier textureId,
            EquipmentClientInfo.LayerType layerType,
            ResourceKey<EquipmentAsset> equipmentAssetKey,
            Model<? super S> model,
            S state,
            ItemStack stack,
            PoseStack matrices,
            SubmitNodeCollector renderCommandQueue,
            int light,
            Identifier playerTexture,
            int overlay,
            int color
    ) {
        boolean isLocalPlayer = false;
        boolean isOtherPlayer = false;
        Minecraft client = Minecraft.getInstance();
        UUID peerUuid = null;

        if (state instanceof LocalPlayerRenderStateAccessor accessor) {
            if (accessor.zmor$isLocalPlayer()) {
                isLocalPlayer = true;
            } else {
                isOtherPlayer = true;
            }
            if (accessor.zmor$getPlayerUuid() != null) {
                peerUuid = accessor.zmor$getPlayerUuid();
            }
        }

        if (client.player != null && state instanceof AvatarRenderState avatarState) {
            if (avatarState.id == client.player.getId()) {
                isLocalPlayer = true;
                isOtherPlayer = false;
            } else {
                isOtherPlayer = true;
                if (peerUuid == null && client.level != null) {
                    Entity ent = client.level.getEntity(avatarState.id);
                    if (ent != null) {
                        peerUuid = ent.getUUID();
                    }
                }
            }
        }

        Identifier itemRegId = stack != null && !stack.isEmpty() ? BuiltInRegistries.ITEM.getKey(stack.getItem()) : null;
        String itemStrId = itemRegId != null ? itemRegId.toString() : null;
        String perItemPack = itemStrId != null ? ModConfig.getItemPack(itemStrId) : "default";

        // 1. LOCAL PLAYER RENDERING
        if (isLocalPlayer) {
            textureId = zmor$resolveEffectiveTexture(textureId, perItemPack);
            return RenderTypes.armorCutoutNoCull(textureId);
        }

        // 2. OTHER PLAYERS RENDERING
        if (isOtherPlayer) {
            // A. If Peer Sync is enabled, try rendering peer's custom pack texture
            if (ModConfig.syncPeerTextures && peerUuid != null) {
                Identifier remoteTexture = RemoteTextureManager.getRemoteTextureForItem(peerUuid, itemStrId, textureId.getPath(), layerType);
                if (remoteTexture != null) {
                    return RenderTypes.armorCutoutNoCull(remoteTexture);
                }
            }

            // B. If Peer Sync is disabled or peer has no custom texture:
            boolean useCustom = ModConfig.applyToOtherPlayers;
            if (useCustom && !ModConfig.filteredItems.isEmpty()) {
                useCustom = ModConfig.isItemWhitelisted(stack);
            }

            if (!useCustom) {
                textureId = Identifier.fromNamespaceAndPath("zmor-vanilla", textureId.getPath());
            } else {
                textureId = zmor$resolveEffectiveTexture(textureId, perItemPack);
            }
            return RenderTypes.armorCutoutNoCull(textureId);
        }

        // 3. MOBS & ARMOR STANDS RENDERING
        boolean useCustomForMobs = ModConfig.applyToMobsAndArmorStands;
        if (useCustomForMobs && !ModConfig.filteredItems.isEmpty()) {
            useCustomForMobs = ModConfig.isItemWhitelisted(stack);
        }

        if (!useCustomForMobs) {
            textureId = Identifier.fromNamespaceAndPath("zmor-vanilla", textureId.getPath());
        } else {
            textureId = zmor$resolveEffectiveTexture(textureId, perItemPack);
        }

        return RenderTypes.armorCutoutNoCull(textureId);
    }

    @Unique
    private Identifier zmor$resolveEffectiveTexture(Identifier baseTextureId, String perItemPack) {
        if (!perItemPack.equals("default")) {
            if (perItemPack.equals("vanilla")) {
                return Identifier.fromNamespaceAndPath("zmor-vanilla", baseTextureId.getPath());
            } else {
                String packNs = "zmor-pk-" + VanillaItemSpriteSource.sanitizePackId(perItemPack);
                return Identifier.fromNamespaceAndPath(packNs, baseTextureId.getPath());
            }
        }
        if (!ModConfig.mainOverridePackId.equals("top")) {
            if (ModConfig.mainOverridePackId.equals("vanilla")) {
                return Identifier.fromNamespaceAndPath("zmor-vanilla", baseTextureId.getPath());
            } else {
                String packNs = "zmor-pk-" + VanillaItemSpriteSource.sanitizePackId(ModConfig.mainOverridePackId);
                return Identifier.fromNamespaceAndPath(packNs, baseTextureId.getPath());
            }
        }
        return baseTextureId;
    }
}
