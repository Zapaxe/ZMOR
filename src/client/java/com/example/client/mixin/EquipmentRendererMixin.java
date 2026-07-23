package com.example.client.mixin;

import com.example.ModConfig;
import com.example.client.LocalPlayerRenderStateAccessor;
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

        boolean useCustom = true;
        if (!ModConfig.shouldApplyTo(stack)) {
            useCustom = false;
        } else if (isOtherPlayer) {
            if (!ModConfig.applyToOtherPlayers) {
                useCustom = false;
            }
        } else if (!isLocalPlayer) {
            if (!ModConfig.applyToMobsAndArmorStands) {
                useCustom = false;
            }
        }

        if (!useCustom) {
            textureId = Identifier.fromNamespaceAndPath("zmor-vanilla", textureId.getPath());
        }

        return RenderTypes.armorCutoutNoCull(textureId);
    }
}
