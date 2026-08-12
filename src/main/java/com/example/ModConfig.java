package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    public static boolean applyToOtherPlayers = false;
    public static boolean applyToMobsAndArmorStands = false;
    public static boolean applyToItemFrames = false;
    public static boolean syncPeerTextures = true;
    public static List<String> filteredItems = new ArrayList<>();
    public static Map<String, String> itemPackOverrides = new HashMap<>();
    public static String mainOverridePackId = "top";
    public static String baseResourcePackId = "vanilla";

    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "zmor.json"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    applyToOtherPlayers = data.applyToOtherPlayers;
                    applyToMobsAndArmorStands = data.applyToMobsAndArmorStands;
                    applyToItemFrames = data.applyToItemFrames;
                    syncPeerTextures = data.syncPeerTextures;
                    filteredItems = data.filteredItems != null ? data.filteredItems : new ArrayList<>();
                    itemPackOverrides = data.itemPackOverrides != null ? data.itemPackOverrides : new HashMap<>();
                    mainOverridePackId = data.mainOverridePackId != null ? data.mainOverridePackId : "top";
                    baseResourcePackId = data.baseResourcePackId != null ? data.baseResourcePackId : "vanilla";
                }
            } catch (IOException e) {
                ExampleMod.LOGGER.error("Failed to load config", e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.applyToOtherPlayers = applyToOtherPlayers;
        data.applyToMobsAndArmorStands = applyToMobsAndArmorStands;
        data.applyToItemFrames = applyToItemFrames;
        data.syncPeerTextures = syncPeerTextures;
        data.filteredItems = filteredItems;
        data.itemPackOverrides = itemPackOverrides;
        data.mainOverridePackId = mainOverridePackId;
        data.baseResourcePackId = baseResourcePackId;
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            ExampleMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static boolean isItemWhitelisted(ItemStack stack) {
        if (stack == null || stack.isEmpty() || filteredItems == null || filteredItems.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && filteredItems.contains(id.toString());
    }

    public static boolean shouldApplyTo(ItemStack stack) {
        return isItemWhitelisted(stack);
    }

    public static String getItemPack(String itemStrId) {
        if (itemStrId == null || itemPackOverrides == null) return "default";
        return itemPackOverrides.getOrDefault(itemStrId, "default");
    }

    public static void setItemPack(String itemStrId, String packId) {
        if (itemStrId == null) return;
        if (itemPackOverrides == null) itemPackOverrides = new HashMap<>();
        if (packId == null || packId.equals("default") || packId.isEmpty()) {
            itemPackOverrides.remove(itemStrId);
        } else {
            itemPackOverrides.put(itemStrId, packId);
        }
    }

    private static class ConfigData {
        boolean applyToOtherPlayers = false;
        boolean applyToMobsAndArmorStands = false;
        boolean applyToItemFrames = false;
        boolean syncPeerTextures = true;
        List<String> filteredItems = new ArrayList<>();
        Map<String, String> itemPackOverrides = new HashMap<>();
        String mainOverridePackId = "top";
        String baseResourcePackId = "vanilla";
    }
}
