package com.example.client;

import com.example.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;

    private Button otherPlayersButton;
    private Button mobsButton;
    private Button itemFramesButton;
    private Button syncPeersButton;
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
        int buttonHeight = 18;
        int buttonX = startX + panelWidth - buttonWidth - 12;

        int summaryY = 24;
        int section1Y = summaryY + 20;
        int rowSpacing = 24;

        // Section 1: Visibility Scopes Buttons (4 rows)
        this.otherPlayersButton = this.addRenderableWidget(
                Button.builder(getToggleComponent(ModConfig.applyToOtherPlayers), button -> {
                    ModConfig.applyToOtherPlayers = !ModConfig.applyToOtherPlayers;
                    button.setMessage(getToggleComponent(ModConfig.applyToOtherPlayers));
                    ClientNetworking.broadcastLocalManifest();
                })
                .bounds(buttonX, section1Y + 14, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("When enabled, your active resource pack textures also appear on other players.")))
                .build()
        );

        this.mobsButton = this.addRenderableWidget(
                Button.builder(getToggleComponent(ModConfig.applyToMobsAndArmorStands), button -> {
                    ModConfig.applyToMobsAndArmorStands = !ModConfig.applyToMobsAndArmorStands;
                    button.setMessage(getToggleComponent(ModConfig.applyToMobsAndArmorStands));
                })
                .bounds(buttonX, section1Y + 14 + rowSpacing, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("When enabled, custom armor and textures apply to mobs and armor stands.")))
                .build()
        );

        this.itemFramesButton = this.addRenderableWidget(
                Button.builder(getToggleComponent(ModConfig.applyToItemFrames), button -> {
                    ModConfig.applyToItemFrames = !ModConfig.applyToItemFrames;
                    button.setMessage(getToggleComponent(ModConfig.applyToItemFrames));
                })
                .bounds(buttonX, section1Y + 14 + rowSpacing * 2, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("When enabled, custom item textures apply to items mounted in item frames.")))
                .build()
        );

        String experimentalTooltip = "§e§lExperimental Feature§r\nCurrently works on LAN worlds and servers with ZMOR installed. We are working on making it work on third-party servers as well!\n\nWhen disabled, stops downloading textures from peers and displays standard vanilla textures on them.";

        this.syncPeersButton = this.addRenderableWidget(
                Button.builder(getToggleComponent(ModConfig.syncPeerTextures), button -> {
                    ModConfig.syncPeerTextures = !ModConfig.syncPeerTextures;
                    button.setMessage(getToggleComponent(ModConfig.syncPeerTextures));
                    if (!ModConfig.syncPeerTextures) {
                        RemoteTextureManager.clear();
                    } else {
                        ClientNetworking.broadcastLocalManifest();
                    }
                })
                .bounds(buttonX, section1Y + 14 + rowSpacing * 3, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal(experimentalTooltip)))
                .build()
        );

        // Section 2: Resources & Filters Buttons (3 rows)
        int section2Y = section1Y + 116;

        // Row 1: Item Filter Whitelist
        this.filterButton = this.addRenderableWidget(
                Button.builder(Component.literal("Manage Items..."), button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(new ItemFilterScreen(this));
                })
                .bounds(buttonX, section2Y + 14, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Configure the whitelist of items to isolate with vanilla textures for other entities.")))
                .build()
        );

        // Row 2: Main Override Pack
        this.mainOverrideButton = this.addRenderableWidget(
                Button.builder(Component.literal("Select Pack..."), button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(new MainOverridePackScreen(this));
                })
                .bounds(buttonX, section2Y + 14 + rowSpacing, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Select the primary resource pack supplying your custom models and textures.")))
                .build()
        );

        // Row 3: Fallback Base Pack
        this.basePackButton = this.addRenderableWidget(
                Button.builder(Component.literal("Select Pack..."), button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(new BaseResourcePackScreen(this));
                })
                .bounds(buttonX, section2Y + 14 + rowSpacing * 2, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Select the fallback base pack supplying standard textures for non-local items.")))
                .build()
        );

        // Bottom Action Buttons - Symmetrically centered pair
        int bottomBtnWidth = 120;
        int bottomSpacing = 12;
        int totalBottomWidth = bottomBtnWidth * 2 + bottomSpacing;
        int bottomStartX = (this.width - totalBottomWidth) / 2;
        int bottomY = this.height - 22;

        this.addRenderableWidget(
                Button.builder(Component.literal("Reset Defaults"), button -> {
                    ModConfig.applyToOtherPlayers = false;
                    ModConfig.applyToMobsAndArmorStands = false;
                    ModConfig.applyToItemFrames = false;
                    ModConfig.syncPeerTextures = true;
                    ModConfig.filteredItems = new ArrayList<>();
                    ModConfig.mainOverridePackId = "top";
                    ModConfig.baseResourcePackId = "vanilla";
                    ModConfig.save();
                    ClientNetworking.broadcastLocalManifest();
                    this.rebuildWidgets();
                })
                .bounds(bottomStartX, bottomY, bottomBtnWidth, 18)
                .tooltip(Tooltip.create(Component.literal("Reset all configuration settings back to their default state.")))
                .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Done"), button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(this.parent);
                })
                .bounds(bottomStartX + bottomBtnWidth + bottomSpacing, bottomY, bottomBtnWidth, 18)
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
        int buttonWidth = 110;
        int buttonX = startX + panelWidth - buttonWidth - 12;

        // Title and Subtitle with clean typography
        graphics.drawCenteredString(this.font, "§f§lZap's Model Only Resources §7(ZMOR)", this.width / 2, 4, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "§7Configure selective custom resource pack texture rendering", this.width / 2, 14, 0xFFAAAAAA);

        // Top Summary Bar
        int summaryY = 24;
        graphics.fill(startX, summaryY, startX + panelWidth, summaryY + 16, 0x40000000);
        graphics.renderOutline(startX, summaryY, panelWidth, 16, 0x20FFFFFF);

        String overrideDisplay = ModConfig.mainOverridePackId.equals("top") ? "Top Pack" : ModConfig.mainOverridePackId;
        if (overrideDisplay.length() > 14) overrideDisplay = overrideDisplay.substring(0, 12) + "..";

        String baseDisplay = ModConfig.baseResourcePackId.equals("vanilla") ? "Vanilla" : ModConfig.baseResourcePackId;
        if (baseDisplay.length() > 14) baseDisplay = baseDisplay.substring(0, 12) + "..";

        int filterCount = ModConfig.filteredItems.size();

        graphics.drawString(this.font, "§7Override: §e" + overrideDisplay, startX + 8, summaryY + 4, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Base: §e" + baseDisplay, startX + 138, summaryY + 4, 0xFFFFFFFF);
        String countStr = "§7Whitelist: §b" + filterCount;
        graphics.drawString(this.font, countStr, startX + panelWidth - this.font.width(countStr) - 8, summaryY + 4, 0xFFFFFFFF);

        // Section 1: Visibility Scopes (4 rows)
        int section1Y = summaryY + 20;
        int section1Height = 112;
        graphics.fill(startX, section1Y, startX + panelWidth, section1Y + section1Height, 0x30000000);
        graphics.renderOutline(startX, section1Y, panelWidth, section1Height, 0x20FFFFFF);

        graphics.drawString(this.font, "§f§lMultiplayer & Visibility Scopes", startX + 10, section1Y + 4, 0xFFFFFFFF);

        int rowSpacing = 24;
        // Row 1
        graphics.drawString(this.font, "Other Players", startX + 12, section1Y + 15, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Show your custom textures on other players", startX + 12, section1Y + 25, 0xFFAAAAAA);

        // Row 2
        graphics.drawString(this.font, "Mobs & Armor Stands", startX + 12, section1Y + 15 + rowSpacing, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Show custom armor on mobs & armor stands", startX + 12, section1Y + 25 + rowSpacing, 0xFFAAAAAA);

        // Row 3
        graphics.drawString(this.font, "Item Frames", startX + 12, section1Y + 15 + rowSpacing * 2, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Show custom item textures in item frames", startX + 12, section1Y + 25 + rowSpacing * 2, 0xFFAAAAAA);

        // Row 4
        int row4Y = section1Y + 15 + rowSpacing * 3;
        graphics.drawString(this.font, "Sync Peer Packs §e[?]", startX + 12, row4Y, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Download & view custom packs from other players", startX + 12, row4Y + 10, 0xFFAAAAAA);

        // Section 2: Resources & Filters (3 rows)
        int section2Y = section1Y + 116;
        int section2Height = 88;
        graphics.fill(startX, section2Y, startX + panelWidth, section2Y + section2Height, 0x30000000);
        graphics.renderOutline(startX, section2Y, panelWidth, section2Height, 0x20FFFFFF);

        graphics.drawString(this.font, "§f§lResource Overrides & Whitelist", startX + 10, section2Y + 4, 0xFFFFFFFF);

        // Row 1
        graphics.drawString(this.font, "Item Whitelist Filter", startX + 12, section2Y + 15, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7" + (filterCount == 0 ? "No items whitelisted (Shows custom for all)" : filterCount + " item" + (filterCount == 1 ? "" : "s") + " isolated"), startX + 12, section2Y + 25, 0xFFAAAAAA);

        // Row 2
        graphics.drawString(this.font, "Main Override Pack", startX + 12, section2Y + 15 + rowSpacing, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Primary custom textures source", startX + 12, section2Y + 25 + rowSpacing, 0xFFAAAAAA);

        // Row 3
        graphics.drawString(this.font, "Fallback Base Pack", startX + 12, section2Y + 15 + rowSpacing * 2, 0xFFFFFFFF);
        graphics.drawString(this.font, "§7Fallback texture provider for peers/mobs", startX + 12, section2Y + 25 + rowSpacing * 2, 0xFFAAAAAA);

        // Interactive tooltip when hovering over "Sync Peer Packs" text/label
        if (mouseX >= startX + 10 && mouseX <= buttonX - 10 && mouseY >= row4Y - 2 && mouseY <= row4Y + 22) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§e§lExperimental Feature"),
                    Component.literal("§7Currently works on LAN worlds and servers with ZMOR installed."),
                    Component.literal("§7We are working on making it work on third-party servers as well!"),
                    Component.literal(""),
                    Component.literal("§8When disabled, stops downloading textures from peers and displays standard vanilla textures on them.")
            ), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(this.parent);
    }
}
