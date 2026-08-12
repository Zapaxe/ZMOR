package com.example.client;

public final class ItemRenderContext {
    public enum TargetType {
        LOCAL_PLAYER,
        OTHER_PLAYER,
        MOB_OR_ARMOR_STAND,
        ITEM_FRAME,
        WORLD_ITEM
    }

    private static final ThreadLocal<TargetType> CURRENT_TARGET = ThreadLocal.withInitial(() -> TargetType.LOCAL_PLAYER);

    public static TargetType getTarget() {
        return CURRENT_TARGET.get();
    }

    public static void setTarget(TargetType target) {
        CURRENT_TARGET.set(target != null ? target : TargetType.LOCAL_PLAYER);
    }

    public static boolean isLocalPlayer() {
        return CURRENT_TARGET.get() == TargetType.LOCAL_PLAYER;
    }

    public static void setLocalPlayer(boolean isLocal) {
        CURRENT_TARGET.set(isLocal ? TargetType.LOCAL_PLAYER : TargetType.OTHER_PLAYER);
    }

    public static void cleanup() {
        CURRENT_TARGET.remove();
    }

    private ItemRenderContext() {}
}
