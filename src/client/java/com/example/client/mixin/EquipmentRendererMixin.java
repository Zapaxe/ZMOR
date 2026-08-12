package com.example.client.mixin;

import com.example.ModConfig;
import com.example.client.LocalPlayerRenderStateAccessor;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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

        if (state instanceof LocalPlayerRenderStateAccessor accessor) {
            if (accessor.zmor$isLocalPlayer()) {
                isLocalPlayer = true;
            } else {
                isOtherPlayer = true;
            }
        }

        if (client.player != null && state instanceof AvatarRenderState avatarState) {
            if (avatarState.id == client.player.getId()) {
                isLocalPlayer = true;
                isOtherPlayer = false;
            } else if (!isLocalPlayer) {
                isOtherPlayer = true;
            }
        }

        if (isOtherPlayer && state instanceof LocalPlayerRenderStateAccessor accessor) {
            java.util.UUID peerUuid = accessor.zmor$getPlayerUuid();
            if (peerUuid != null) {
                Identifier remoteTexture = com.example.client.RemoteTextureManager.getRemoteTexture(peerUuid, textureId.getPath());
                if (remoteTexture != null) {
                    return RenderTypes.armorCutoutNoCull(remoteTexture);
                }
            }
        }

        Identifier itemRegId = stack != null && !stack.isEmpty() ? BuiltInRegistries.ITEM.getKey(stack.getItem()) : null;
        String itemStrId = itemRegId != null ? itemRegId.toString() : null;
        String perItemPack = itemStrId != null ? ModConfig.getItemPack(itemStrId) : "default";

        boolean useCustom = true;
        if (isLocalPlayer) {
            useCustom = true;
        } else {
            if (ModConfig.isItemWhitelisted(stack)) {
                if (isOtherPlayer) {
                    useCustom = ModConfig.applyToOtherPlayers;
                } else {
                    useCustom = ModConfig.applyToMobsAndArmorStands;
                }
            } else {
                useCustom = true;
            }
        }

        if (!useCustom) {
            textureId = Identifier.fromNamespaceAndPath("zmor-vanilla", textureId.getPath());
        } else if (!perItemPack.equals("default")) {
            if (perItemPack.equals("vanilla")) {
                textureId = Identifier.fromNamespaceAndPath("zmor-vanilla", textureId.getPath());
            } else {
                String packNs = "zmor-pk-" + VanillaItemSpriteSource.sanitizePackId(perItemPack);
                textureId = Identifier.fromNamespaceAndPath(packNs, textureId.getPath());
            }
        }

        return RenderTypes.armorCutoutNoCull(textureId);
    }
}
