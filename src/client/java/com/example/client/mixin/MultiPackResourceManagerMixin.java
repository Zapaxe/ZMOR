package com.example.client.mixin;

import com.example.ModConfig;
import com.example.client.VanillaItemSpriteSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {

    @Unique
    private static final ThreadLocal<Boolean> zmor$fallbackGuard = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void redirectGetResource(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (zmor$fallbackGuard.get()) {
            return;
        }

        String namespace = id.getNamespace();

        // 1. Custom Per-Item Pack Lookups: zmor-pk-<sanitizedPackId>
        if (namespace.startsWith("zmor-pk-")) {
            String sanitizedPack = namespace.substring("zmor-pk-".length());
            Identifier originalId = Identifier.fromNamespaceAndPath("minecraft", id.getPath());
            PackResources targetPack = zmor$getPackResourcesBySanitizedId(sanitizedPack);
            if (targetPack != null) {
                IoSupplier<InputStream> supplier = targetPack.getResource(PackType.CLIENT_RESOURCES, originalId);
                if (supplier != null) {
                    cir.setReturnValue(Optional.of(new Resource(targetPack, supplier)));
                    return;
                }
                if (originalId.getPath().startsWith("textures/entity/equipment/")) {
                    Identifier oldId = toOldArmorPath(originalId);
                    if (oldId != null) {
                        IoSupplier<InputStream> oldSupplier = targetPack.getResource(PackType.CLIENT_RESOURCES, oldId);
                        if (oldSupplier != null) {
                            cir.setReturnValue(Optional.of(new Resource(targetPack, oldSupplier)));
                            return;
                        }
                    }
                }
            }
        }

        // 2. Default Fallback Base Lookups: zmor-vanilla
        if (namespace.equals("zmor-vanilla")) {
            Identifier originalId = Identifier.fromNamespaceAndPath("minecraft", id.getPath());

            // 1. Try configured base pack
            PackResources basePack = zmor$getBasePackResources();
            if (basePack != null) {
                IoSupplier<InputStream> supplier = basePack.getResource(PackType.CLIENT_RESOURCES, originalId);
                if (supplier != null) {
                    cir.setReturnValue(Optional.of(new Resource(basePack, supplier)));
                    return;
                }

                // Try old format if it's armor
                if (originalId.getPath().startsWith("textures/entity/equipment/")) {
                    Identifier oldId = toOldArmorPath(originalId);
                    if (oldId != null) {
                        IoSupplier<InputStream> oldSupplier = basePack.getResource(PackType.CLIENT_RESOURCES, oldId);
                        if (oldSupplier != null) {
                            cir.setReturnValue(Optional.of(new Resource(basePack, oldSupplier)));
                            return;
                        }
                    }
                }
            }

            // 2. Try vanilla pack fallback
            VanillaPackResources vanillaPack = Minecraft.getInstance().getVanillaPackResources();
            if (vanillaPack != null) {
                IoSupplier<InputStream> supplier = vanillaPack.getResource(PackType.CLIENT_RESOURCES, originalId);
                if (supplier != null) {
                    cir.setReturnValue(Optional.of(new Resource(vanillaPack, supplier)));
                    return;
                }

                if (originalId.getPath().startsWith("textures/entity/equipment/")) {
                    Identifier oldId = toOldArmorPath(originalId);
                    if (oldId != null) {
                        IoSupplier<InputStream> oldSupplier = vanillaPack.getResource(PackType.CLIENT_RESOURCES, oldId);
                        if (oldSupplier != null) {
                            cir.setReturnValue(Optional.of(new Resource(vanillaPack, oldSupplier)));
                            return;
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void fallbackOldArmorPaths(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (id.getNamespace().startsWith("zmor-")) {
            return;
        }
        if (!id.getPath().startsWith("textures/entity/equipment/")) {
            return;
        }
        Identifier oldId = toOldArmorPath(id);
        if (oldId == null) {
            return;
        }

        zmor$fallbackGuard.set(true);
        Optional<Resource> oldResource;
        try {
            oldResource = ((MultiPackResourceManager) (Object) this).getResource(oldId);
        } finally {
            zmor$fallbackGuard.set(false);
        }

        if (oldResource.isPresent()) {
            Optional<Resource> currentResource = cir.getReturnValue();
            if (currentResource.isEmpty() || oldResource.get().source() != currentResource.get().source()) {
                cir.setReturnValue(oldResource);
            }
        }
    }

    @Unique
    private PackResources zmor$getBasePackResources() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return null;

        String targetId = ModConfig.baseResourcePackId;
        if ("vanilla".equals(targetId) || targetId == null) {
            return client.getVanillaPackResources();
        }

        List<PackResources> openPacks = ((MultiPackResourceManagerAccessor) this).getPacks();
        if (openPacks != null) {
            for (PackResources pack : openPacks) {
                if (pack.packId().equals(targetId)) {
                    return pack;
                }
            }
        }
        return client.getVanillaPackResources();
    }

    @Unique
    private PackResources zmor$getPackResourcesBySanitizedId(String sanitized) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || sanitized == null) return null;

        if ("vanilla".equals(sanitized)) {
            return client.getVanillaPackResources();
        }

        List<PackResources> openPacks = ((MultiPackResourceManagerAccessor) this).getPacks();
        if (openPacks != null) {
            for (PackResources pack : openPacks) {
                if (VanillaItemSpriteSource.sanitizePackId(pack.packId()).equals(sanitized)) {
                    return pack;
                }
            }
        }
        return null;
    }

    @Unique
    private static Identifier toOldArmorPath(Identifier id) {
        String path = id.getPath();
        String namespace = id.getNamespace();
        if (path.contains("/humanoid_leggings/")) {
            String material = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
            if (material.isEmpty()) return null;
            return Identifier.fromNamespaceAndPath(namespace, "textures/models/armor/" + material + "_layer_2.png");
        }
        if (path.contains("/humanoid/")) {
            String material = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
            if (material.isEmpty()) return null;
            return Identifier.fromNamespaceAndPath(namespace, "textures/models/armor/" + material + "_layer_1.png");
        }
        if (path.contains("/horse/")) {
            String material = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
            if (material.isEmpty()) return null;
            return Identifier.fromNamespaceAndPath(namespace, "textures/entity/horse/armor/horse_armor_" + material + ".png");
        }
        return null;
    }
}
