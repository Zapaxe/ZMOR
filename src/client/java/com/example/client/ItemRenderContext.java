package com.example.client;

import net.minecraft.client.Minecraft;

public final class ItemRenderContext {
    private static final ThreadLocal<Boolean> IS_LOCAL_PLAYER = ThreadLocal.withInitial(() -> true);

    public static boolean isLocalPlayer() {
        return IS_LOCAL_PLAYER.get();
    }

    public static void setLocalPlayer(boolean isLocal) {
        IS_LOCAL_PLAYER.set(isLocal);
    }

    public static void cleanup() {
        IS_LOCAL_PLAYER.remove();
    }

    private ItemRenderContext() {}
}
