package com.example.client;

import com.example.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class ConfigScreen extends Screen {
    private final Screen parent;

    private Button otherPlayersButton;
    private Button mobsButton;
    private Button itemFramesButton;
    private Button filterButton;
    private Button mainOverrideButton;
    private Button basePackButton;

    public ConfigScreen(Screen parent) {
        super(Component.literal("ZMOR Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = 380;
        int startX = (this.width - panelWidth) / 2;
        int buttonWidth = 110;
        int buttonHeight = 20;
        int buttonX = startX + panelWidth - buttonWidth - 12;

        int summaryY = 30;
        int section1Y = summaryY + 24;
        int rowSpacing = 24;

        // Section 1: Visibility Scopes Buttons
        this.otherPlayersButton = this.addRenderableWidget(
                Button.builder(getToggleComponent(ModConfig.applyToOtherPlayers), button -> {
                    ModConfig.applyToOtherPlayers = !ModConfig.applyToOtherPlayers;
                    button.setMessage(getToggleComponent(ModConfig.applyToOtherPlayers));
                })
                .bounds(buttonX, section1Y + 16, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("When enabled, custom textures also appear on other multiplayer peers.")))
                .build()
        );

        this.mobsButton = this.addRenderableWidget(
                Button.builder(getToggleComponent(ModConfig.applyToMobsAndArmorStands), button -> {
                    ModConfig.applyToMobsAndArmorStands = !ModConfig.applyToMobsAndArmorStands;
                    button.setMessage(getToggleComponent(ModConfig.applyToMobsAndArmorStands));
                })
                .bounds(buttonX, section1Y + 16 + rowSpacing, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("When enabled, custom armor and textures apply to mobs and armor stands.")))
                .build()
        );

        this.itemFramesButton = this.addRenderableWidget(
                Button.builder(getToggleComponent(ModConfig.applyToItemFrames), button -> {
                    ModConfig.applyToItemFrames = !ModConfig.applyToItemFrames;
                    button.setMessage(getToggleComponent(ModConfig.applyToItemFrames));
                })
                .bounds(buttonX, section1Y + 16 + rowSpacing * 2, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("When enabled, custom item textures apply to items mounted in item frames.")))
                .build()
        );

        // Section 2: Resources & Filters Buttons (3 rows)
        int section2Y = section1Y + 94;

        // Row 1: Item Filter Whitelist
        this.filterButton = this.addRenderableWidget(
                Button.builder(Component.literal("Manage Items..."), button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(new ItemFilterScreen(this));
                })
                .bounds(buttonX, section2Y + 16, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Configure the whitelist of items to isolate with vanilla textures for other entities.")))
                .build()
        );

        // Row 2: Main Override Pack
        this.mainOverrideButton = this.addRenderableWidget(
                Button.builder(Component.literal("Select Pack..."), button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(new MainOverridePackScreen(this));
                })
                .bounds(buttonX, section2Y + 16 + rowSpacing, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Select the primary resource pack supplying your custom models and textures.")))
                .build()
        );

        // Row 3: Fallback Base Pack
        this.basePackButton = this.addRenderableWidget(
                Button.builder(Component.literal("Select Pack..."), button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(new BaseResourcePackScreen(this));
                })
                .bounds(buttonX, section2Y + 16 + rowSpacing * 2, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Select the fallback base pack supplying standard textures for non-local items.")))
                .build()
        );

        // Bottom Action Buttons - Symmetrically centered pair
        int bottomBtnWidth = 120;
        int bottomSpacing = 12;
        int totalBottomWidth = bottomBtnWidth * 2 + bottomSpacing;
        int bottomStartX = (this.width - totalBottomWidth) / 2;
        int bottomY = this.height - 24;

        this.addRenderableWidget(
                Button.builder(Component.literal("Reset Defaults"), button -> {
                    ModConfig.applyToOtherPlayers = false;
                    ModConfig.applyToMobsAndArmorStands = false;
                    ModConfig.applyToItemFrames = false;
                    ModConfig.filteredItems = new ArrayList<>();
                    ModConfig.mainOverridePackId = "top";
                    ModConfig.baseResourcePackId = "vanilla";
                    ModConfig.save();
                    this.rebuildWidgets();
                })
                .bounds(bottomStartX, bottomY, bottomBtnWidth, 20)
                .tooltip(Tooltip.create(Component.literal("Reset all configuration settings back to their default state.")))
                .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Done"), button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(this.parent);
                })
                .bounds(bottomStartX + bottomBtnWidth + bottomSpacing, bottomY, bottomBtnWidth, 20)
                .build()
        );
    }

    private Component getToggleComponent(boolean state) {
        return state ? Component.literal("§a✔ Enabled") : Component.literal("§c✖ Disabled");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int panelWidth = 380;
        int startX = (this.width - panelWidth) / 2;

        // Title and Subtitle with clean typography
        graphics.drawCenteredString(this.font, "§f§lZap's Model Only Resources §7(ZMOR)", this.width / 2, 6, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "§7Configure selective custom resource pack texture rendering", this.width / 2, 17, 0xFFAAAAAA);

        // Top Summary Bar
        int summaryY = 30;
        graphics.fill(startX, summaryY, startX + panelWidth, summaryY + 18, 0x40000000);
        graphics.renderOutline(startX, summaryY, panelWidth, 18, 0x20FFFFFF);

        String overrideDisplay = ModConfig.mainOverridePackId.equals("top") ? "Top Pack" : ModConfig.mainOverridePackId;
        if (overrideDisplay.length() > 14) overrideDisplay = overrideDisplay.substring(0, 12) + "..";

        String baseDisplay = ModConfig.baseResourcePackId.equals("vanilla") ? "Vanilla" : ModConfig.baseResourcePackId;
        if (baseDisplay.length() > 14) baseDisplay = baseDisplay.substring(0, 12) + "..";

        int filterCount = ModConfig.filteredItems.size();

        graphics.drawString(this.font, "§7Override: §e" + overrideDisplay, startX + 8, summaryY + 5, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Base: §e" + baseDisplay, startX + 138, summaryY + 5, 0xFFFFFFFF);
        String countStr = "§7Whitelist: §b" + filterCount;
        graphics.drawString(this.font, countStr, startX + panelWidth - this.font.width(countStr) - 8, summaryY + 5, 0xFFFFFFFF);

        // Section 1: Visibility Scopes
        int section1Y = summaryY + 24;
        int section1Height = 88;
        graphics.fill(startX, section1Y, startX + panelWidth, section1Y + section1Height, 0x30000000);
        graphics.renderOutline(startX, section1Y, panelWidth, section1Height, 0x20FFFFFF);

        graphics.drawString(this.font, "§f§lEntity Visibility Scopes", startX + 10, section1Y + 5, 0xFFFFFFFF);

        int rowSpacing = 24;
        // Row 1
        graphics.drawString(this.font, "Other Players", startX + 12, section1Y + 18, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Show custom textures on multiplayer peers", startX + 12, section1Y + 28, 0xFFAAAAAA);

        // Row 2
        graphics.drawString(this.font, "Mobs & Armor Stands", startX + 12, section1Y + 18 + rowSpacing, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Show custom armor on mobs & armor stands", startX + 12, section1Y + 28 + rowSpacing, 0xFFAAAAAA);

        // Row 3
        graphics.drawString(this.font, "Item Frames", startX + 12, section1Y + 18 + rowSpacing * 2, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Show custom item textures in item frames", startX + 12, section1Y + 28 + rowSpacing * 2, 0xFFAAAAAA);

        // Section 2: Resources & Filters
        int section2Y = section1Y + 94;
        int section2Height = 88;
        graphics.fill(startX, section2Y, startX + panelWidth, section2Y + section2Height, 0x30000000);
        graphics.renderOutline(startX, section2Y, panelWidth, section2Height, 0x20FFFFFF);

        graphics.drawString(this.font, "§f§lResource Overrides & Whitelist", startX + 10, section2Y + 5, 0xFFFFFFFF);

        // Row 1
        graphics.drawString(this.font, "Item Whitelist Filter", startX + 12, section2Y + 18, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7" + (filterCount == 0 ? "No items whitelisted (Shows custom for all)" : filterCount + " item" + (filterCount == 1 ? "" : "s") + " isolated"), startX + 12, section2Y + 28, 0xFFAAAAAA);

        // Row 2
        graphics.drawString(this.font, "Main Override Pack", startX + 12, section2Y + 18 + rowSpacing, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Primary custom textures source", startX + 12, section2Y + 28 + rowSpacing, 0xFFAAAAAA);

        // Row 3
        graphics.drawString(this.font, "Fallback Base Pack", startX + 12, section2Y + 18 + rowSpacing * 2, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Fallback texture provider for peers/mobs", startX + 12, section2Y + 28 + rowSpacing * 2, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(this.parent);
    }
}
