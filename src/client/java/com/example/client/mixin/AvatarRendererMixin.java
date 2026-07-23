package com.example.client.mixin;

import com.example.client.LocalPlayerRenderStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void onExtractRenderState(Avatar player, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            boolean isLocal = player.getUUID().equals(client.player.getUUID());
            LocalPlayerRenderStateAccessor accessor = (LocalPlayerRenderStateAccessor) state;
            accessor.zmor$setLocalPlayer(isLocal);
            accessor.zmor$setPlayerUuid(player.getUUID());
        }
    }
}
