package com.example.client.mixin;

import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(MultiPackResourceManager.class)
public interface MultiPackResourceManagerAccessor {
    @Accessor("packs")
    List<PackResources> getPacks();
}
