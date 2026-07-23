package com.example.client.mixin;

import com.example.client.LocalPlayerRenderStateAccessor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements LocalPlayerRenderStateAccessor {
    @Unique
    private boolean zmor$isLocalPlayer;
    @Unique
    private UUID zmor$playerUuid;

    @Override
    public boolean zmor$isLocalPlayer() {
        return zmor$isLocalPlayer;
    }

    @Override
    public void zmor$setLocalPlayer(boolean isLocalPlayer) {
        this.zmor$isLocalPlayer = isLocalPlayer;
    }

    @Override
    public UUID zmor$getPlayerUuid() {
        return zmor$playerUuid;
    }

    @Override
    public void zmor$setPlayerUuid(UUID uuid) {
        this.zmor$playerUuid = uuid;
    }
}
