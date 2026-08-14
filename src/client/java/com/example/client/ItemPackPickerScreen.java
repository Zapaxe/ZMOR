package com.example.client;

import com.example.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ItemPackPickerScreen extends Screen {
    private final Screen parent;
    private final String itemStrId;
    private final ItemStack itemStack;
    private final String itemDisplayName;

    private final List<PackEntry> allPacks = new ArrayList<>();
    private List<PackEntry> filteredPacks = new ArrayList<>();
    private EditBox searchBox;
    private int page = 0;
    private static final int PACKS_PER_PAGE = 4;

    private Button prevButton;
    private Button nextButton;

    public static class PackEntry {
        public final String id;
        public final String title;
        public final String description;

        public PackEntry(String id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }
    }

    public ItemPackPickerScreen(Screen parent, String itemStrId) {
        super(Component.literal("Select Resource Pack"));
        this.parent = parent;
        this.itemStrId = itemStrId;

        // Resolve item stack & name
        Identifier id = Identifier.tryParse(itemStrId);
        ItemStack stack = ItemStack.EMPTY;
        String name = itemStrId;
        if (id != null) {
            Item item = BuiltInRegistries.ITEM.get(id)
                    .map(net.minecraft.core.Holder.Reference::value)
                    .orElse(Items.AIR);
            if (item != Items.AIR) {
                stack = new ItemStack(item);
                name = Component.translatable(item.getDescriptionId()).getString();
            }
        }
        this.itemStack = stack;
        this.itemDisplayName = name;

        // 1. Default (Global Setting)
        allPacks.add(new PackEntry("default", "Default (Global Override)", "Inherits the global Main Override Pack configuration"));

        // 2. Built-in Vanilla
        allPacks.add(new PackEntry("vanilla", "Default Vanilla", "Built-in standard Minecraft textures"));

        // 3. User Texture Packs
        PackRepository repo = Minecraft.getInstance().getResourcePackRepository();
        if (repo != null) {
            for (Pack pack : repo.getSelectedPacks()) {
                if (isUserTexturePack(pack)) {
                    allPacks.add(new PackEntry(
                            pack.getId(),
                            pack.getTitle().getString(),
                            pack.getDescription().getString()
                    ));
                }
            }
        }
        this.filteredPacks = new ArrayList<>(this.allPacks);
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
        int cardWidth = 340;
        int startX = (this.width - cardWidth) / 2;

        // Search Box
        this.searchBox = new EditBox(this.font, startX, 42, cardWidth, 18, Component.literal("Search packs..."));
        this.searchBox.setHint(Component.literal("§8Search resource packs..."));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        // Bottom Controls - Symmetrically centered trio
        int btnSpacing = 8;
        int prevNextWidth = 70;
        int doneWidth = 100;
        int totalBottomWidth = prevNextWidth * 2 + doneWidth + btnSpacing * 2;
        int bottomStartX = (this.width - totalBottomWidth) / 2;
        int bottomY = this.height - 28;

        this.prevButton = this.addRenderableWidget(Button.builder(
                Component.literal("◀ Prev"),
                button -> {
                    if (page > 0) {
                        page--;
                        rebuildWidgets();
                    }
                }
        ).bounds(bottomStartX, bottomY, prevNextWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(this.parent);
                }
        ).bounds(bottomStartX + prevNextWidth + btnSpacing, bottomY, doneWidth, 20).build());

        this.nextButton = this.addRenderableWidget(Button.builder(
                Component.literal("Next ▶"),
                button -> {
                    if ((page + 1) * PACKS_PER_PAGE < filteredPacks.size()) {
                        page++;
                        rebuildWidgets();
                    }
                }
        ).bounds(bottomStartX + prevNextWidth + doneWidth + btnSpacing * 2, bottomY, prevNextWidth, 20).build());

        // Card action buttons
        int startY = 68;
        int cardSpacing = 36;
        int startIdx = page * PACKS_PER_PAGE;
        String currentPack = ModConfig.getItemPack(itemStrId);

        for (int i = 0; i < PACKS_PER_PAGE; i++) {
            int currentIdx = startIdx + i;
            if (currentIdx >= filteredPacks.size()) break;

            PackEntry pack = filteredPacks.get(currentIdx);
            String packId = pack.id;
            boolean isSelected = currentPack.equals(packId);

            int cardY = startY + i * cardSpacing;

            if (!isSelected) {
                this.addRenderableWidget(Button.builder(
                        Component.literal("Select"),
                        button -> {
                            ModConfig.setItemPack(itemStrId, packId);
                            ModConfig.save();
                            Minecraft.getInstance().reloadResourcePacks();
                            rebuildWidgets();
                        }
                ).bounds(startX + cardWidth - 62, cardY + 7, 54, 18)
                .tooltip(Tooltip.create(Component.literal("Load " + itemDisplayName + " textures from this pack")))
                .build());
            }
        }

        updateNavState();
    }

    private void onSearchChanged(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        this.filteredPacks = allPacks.stream()
                .filter(p -> {
                    if (q.isEmpty()) return true;
                    return p.title.toLowerCase(Locale.ROOT).contains(q)
                            || p.id.toLowerCase(Locale.ROOT).contains(q);
                })
                .collect(Collectors.toList());
        this.page = 0;
        rebuildWidgets();
    }

    private void updateNavState() {
        if (this.prevButton != null) {
            this.prevButton.active = page > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = (page + 1) * PACKS_PER_PAGE < filteredPacks.size();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int cardWidth = 340;
        int startX = (this.width - cardWidth) / 2;

        // Render item icon & header
        if (!itemStack.isEmpty()) {
            graphics.fakeItem(itemStack, startX, 8);
        }
        graphics.text(this.font, "§f§lTexture Source for §e" + itemDisplayName, startX + 22, 10, 0xFFFFFFFF);
        graphics.centeredText(this.font, "§7Choose which resource pack supplies textures for this item", this.width / 2, 24, 0xFFAAAAAA);

        // Pack Cards
        int startY = 68;
        int cardHeight = 32;
        int cardSpacing = 36;
        int startIdx = page * PACKS_PER_PAGE;
        String currentPack = ModConfig.getItemPack(itemStrId);

        for (int i = 0; i < PACKS_PER_PAGE; i++) {
            int currentIdx = startIdx + i;
            if (currentIdx >= filteredPacks.size()) break;

            PackEntry pack = filteredPacks.get(currentIdx);
            String packId = pack.id;
            boolean isSelected = currentPack.equals(packId);

            int cardY = startY + i * cardSpacing;
            boolean isHovered = mouseX >= startX && mouseX <= startX + cardWidth &&
                                mouseY >= cardY && mouseY <= cardY + cardHeight;

            int bgColor = isSelected ? 0x3500E676 : (isHovered ? 0x35FFFFFF : 0x25000000);
            graphics.fill(startX, cardY, startX + cardWidth, cardY + cardHeight, bgColor);

            int outlineColor = isSelected ? 0xFF00E676 : (isHovered ? 0x60FFFFFF : 0x20FFFFFF);
            graphics.outline(startX, cardY, cardWidth, cardHeight, outlineColor);

            String title = pack.title;
            if (title.length() > 30) {
                title = title.substring(0, 28) + "...";
            }
            graphics.text(this.font, (isSelected ? "§a§l" : "§f§l") + title, startX + 8, cardY + 6, 0xFFFFFFFF);

            String desc = pack.description.replaceAll("\n", " ");
            if (desc.isEmpty() || desc.equals(title)) {
                desc = "ID: " + packId;
            }
            if (desc.length() > 46) {
                desc = desc.substring(0, 44) + "...";
            }
            graphics.text(this.font, "§7" + desc, startX + 8, cardY + 18, 0xFFAAAAAA);

            if (isSelected) {
                int badgeWidth = 66;
                int badgeX = startX + cardWidth - badgeWidth - 6;
                int badgeY = cardY + 8;
                graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 16, 0x4000E676);
                graphics.outline(badgeX, badgeY, badgeWidth, 16, 0xFF00E676);
                graphics.centeredText(this.font, "§a✔ ACTIVE", badgeX + badgeWidth / 2, badgeY + 4, 0xFFFFFFFF);
            }
        }

        if (filteredPacks.isEmpty()) {
            graphics.centeredText(this.font, "§7No matching resource packs found", this.width / 2, startY + 40, 0xFFAAAAAA);
        }

        // Page Indicator
        int totalPages = Math.max(1, (filteredPacks.size() + PACKS_PER_PAGE - 1) / PACKS_PER_PAGE);
        String pageStr = String.format("§7Page §f%d§7/§f%d §8(%d packs)", page + 1, totalPages, filteredPacks.size());
        graphics.centeredText(this.font, pageStr, this.width / 2, this.height - 40, 0xFF888888);
    }

    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(this.parent);
    }
}
