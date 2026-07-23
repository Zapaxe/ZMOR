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
import java.util.List;

public class ModConfig {
    public static boolean applyToOtherPlayers = false;
    public static boolean applyToMobsAndArmorStands = false;
    public static boolean applyToItemFrames = false;
    public static List<String> filteredItems = new ArrayList<>();
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
                    filteredItems = data.filteredItems != null ? data.filteredItems : new ArrayList<>();
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
        data.filteredItems = filteredItems;
        data.baseResourcePackId = baseResourcePackId;
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            ExampleMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static boolean shouldApplyTo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (filteredItems.isEmpty()) {
            return true;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && filteredItems.contains(id.toString());
    }

    private static class ConfigData {
        boolean applyToOtherPlayers = false;
        boolean applyToMobsAndArmorStands = false;
        boolean applyToItemFrames = false;
        List<String> filteredItems = new ArrayList<>();
        String baseResourcePackId = "vanilla";
    }
}

