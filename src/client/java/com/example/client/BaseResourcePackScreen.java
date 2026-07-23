package com.example.client;

import com.example.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BaseResourcePackScreen extends Screen {
    private final Screen parent;
    private final List<Pack> selectedPacks = new ArrayList<>();
    private int page = 0;
    private static final int PACKS_PER_PAGE = 5;

    private Button prevButton;
    private Button nextButton;

    public BaseResourcePackScreen(Screen parent) {
        super(Component.literal("Configure Base Resources"));
        this.parent = parent;

        // Fetch selected resource packs from repository, filtering out internal mod packs
        PackRepository repo = Minecraft.getInstance().getResourcePackRepository();
        if (repo != null) {
            for (Pack pack : repo.getSelectedPacks()) {
                if (isUserTexturePack(pack)) {
                    selectedPacks.add(pack);
                }
            }
        }
    }

    private static boolean isUserTexturePack(Pack pack) {
        String id = pack.getId();
        String title = pack.getTitle().getString();
        
        if (id.startsWith("mod:") || id.startsWith("fabric:") || title.startsWith("Fabric Mod ")) {
            return false;
        }
        
        return id.equals("vanilla") || id.startsWith("file/") || id.startsWith("folder/") 
            || id.equals("programmer_art") || id.equals("high_contrast")
            || (!id.startsWith("mod") && !title.toLowerCase(Locale.ROOT).contains("fabric mod"));
    }

    @Override
    protected void init() {
        // Back Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(this.parent);
                }
        ).bounds(this.width / 2 - 75, this.height - 30, 150, 20).build());

        int buttonWidth = 260;
        int buttonHeight = 20;
        int startX = (this.width - buttonWidth) / 2;

        // Previous Page Button
        this.prevButton = this.addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> {
                    if (page > 0) {
                        page--;
                        updateButtons();
                    }
                }
        ).bounds(startX, 180, 40, 20).build());

        // Next Page Button
        this.nextButton = this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> {
                    if ((page + 1) * PACKS_PER_PAGE < selectedPacks.size()) {
                        page++;
                        updateButtons();
                    }
                }
        ).bounds(startX + buttonWidth - 40, 180, 40, 20).build());

        updateButtons();
    }

    private void updateButtons() {
        this.clearWidgets();
        
        // Re-add static widgets
        this.addRenderableWidget(this.prevButton);
        this.addRenderableWidget(this.nextButton);
        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(this.parent);
                }
        ).bounds(this.width / 2 - 75, this.height - 30, 150, 20).build());

        int buttonWidth = 260;
        int buttonHeight = 20;
        int spacing = 22;
        int startX = (this.width - buttonWidth) / 2;
        int startY = 60;

        int totalPacksCount = selectedPacks.size();

        // Determine list starting index
        int startIdx = page * PACKS_PER_PAGE;
        for (int i = 0; i < PACKS_PER_PAGE; i++) {
            int currentIdx = startIdx + i;
            if (currentIdx >= totalPacksCount) break;

            Pack pack = selectedPacks.get(currentIdx);
            String packId = pack.getId();
            String displayName = pack.getTitle().getString();

            boolean isSelected = ModConfig.baseResourcePackId.equals(packId);
            String label = displayName + (isSelected ? " (Selected)" : "");

            Button btn = Button.builder(
                    Component.literal(label),
                    button -> {
                        ModConfig.baseResourcePackId = packId;
                        ModConfig.save();
                        
                        // Reload client resources to apply changes!
                        Minecraft.getInstance().reloadResourcePacks();
                        
                        updateButtons();
                    }
            )
            .bounds(startX, startY + i * spacing, buttonWidth, buttonHeight)
            .build();

            // Disable clicking if already selected
            btn.active = !isSelected;
            this.addRenderableWidget(btn);
        }

        this.prevButton.active = page > 0;
        this.nextButton.active = (page + 1) * PACKS_PER_PAGE < totalPacksCount;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "Fallback textures will be loaded from the selected pack downwards.", this.width / 2, 34, 0xFFAAAAAA);

        int totalPacksCount = selectedPacks.size();
        String pageStr = String.format("Page %d/%d", page + 1, Math.max(1, (totalPacksCount + PACKS_PER_PAGE - 1) / PACKS_PER_PAGE));
        graphics.drawCenteredString(this.font, pageStr, this.width / 2, 185, 0xFF888888);
    }

    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(this.parent);
    }
}
