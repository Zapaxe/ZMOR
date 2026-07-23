package com.example.client;

import java.util.UUID;

public interface LocalPlayerRenderStateAccessor {
    boolean zmor$isLocalPlayer();
    void zmor$setLocalPlayer(boolean isLocalPlayer);

    UUID zmor$getPlayerUuid();
    void zmor$setPlayerUuid(UUID uuid);
}
