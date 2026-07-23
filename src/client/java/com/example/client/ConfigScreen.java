package com.example.client;

import com.example.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Custom Resource Pack Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 220;
        int buttonHeight = 20;
        int spacing = 24;

        int startX = (this.width - buttonWidth) / 2;
        int startY = this.height / 2 - 65;

        // Button 1: Toggle other players
        this.addRenderableWidget(Button.builder(
                getOtherPlayersComponent(),
                button -> {
                    ModConfig.applyToOtherPlayers = !ModConfig.applyToOtherPlayers;
                    button.setMessage(getOtherPlayersComponent());
                }
        ).bounds(startX, startY, buttonWidth, buttonHeight).build());

        // Button 2: Toggle Mobs & Armor Stands
        this.addRenderableWidget(Button.builder(
                getMobsAndArmorStandsComponent(),
                button -> {
                    ModConfig.applyToMobsAndArmorStands = !ModConfig.applyToMobsAndArmorStands;
                    button.setMessage(getMobsAndArmorStandsComponent());
                }
        ).bounds(startX, startY + spacing, buttonWidth, buttonHeight).build());

        // Button 3: Toggle Item Frames
        this.addRenderableWidget(Button.builder(
                getItemFramesComponent(),
                button -> {
                    ModConfig.applyToItemFrames = !ModConfig.applyToItemFrames;
                    button.setMessage(getItemFramesComponent());
                }
        ).bounds(startX, startY + spacing * 2, buttonWidth, buttonHeight).build());

        // Button 4: Edit Item Filter List
        this.addRenderableWidget(Button.builder(
                getItemFilterComponent(),
                button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(new ItemFilterScreen(this));
                }
        ).bounds(startX, startY + spacing * 3, buttonWidth, buttonHeight).build());

        // Button 5: Configure Base Resources
        this.addRenderableWidget(Button.builder(
                Component.literal("Configure Base Resources..."),
                button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(new BaseResourcePackScreen(this));
                }
        ).bounds(startX, startY + spacing * 4, buttonWidth, buttonHeight).build());

        // Button 6: Done
        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(this.parent);
                }
        ).bounds(startX, startY + spacing * 5 + 10, buttonWidth, buttonHeight).build());
    }

    private Component getOtherPlayersComponent() {
        return Component.literal("Apply to Other Players: " + (ModConfig.applyToOtherPlayers ? "ON" : "OFF"));
    }

    private Component getMobsAndArmorStandsComponent() {
        return Component.literal("Apply to Mobs & Armor Stands: " + (ModConfig.applyToMobsAndArmorStands ? "ON" : "OFF"));
    }

    private Component getItemFramesComponent() {
        return Component.literal("Apply to Item Frames: " + (ModConfig.applyToItemFrames ? "ON" : "OFF"));
    }

    private Component getItemFilterComponent() {
        int count = ModConfig.filteredItems.size();
        return Component.literal("Edit Item Filter List (" + (count == 0 ? "All Items" : count + " Selected") + ")...");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        
        // Draw the title on top of the rendered background and widgets
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(this.parent);
    }
}
