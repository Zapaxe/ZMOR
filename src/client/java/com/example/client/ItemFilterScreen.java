package com.example.client;

import com.example.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ItemFilterScreen extends Screen {
    private final Screen parent;
    private EditBox searchBox;
    private List<Item> searchResults = new ArrayList<>();
    private final List<Item> allItems = new ArrayList<>();

    public enum Category {
        ALL("All"),
        ARMOR("Armor"),
        WEAPONS("Weapons"),
        TOOLS("Tools"),
        OTHER("Other");

        private final String label;
        Category(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private Category selectedCategory = Category.ALL;

    private int searchPage = 0;
    private int filterPage = 0;
    private static final int COLS = 12;
    private static final int ROWS = 6;
    private static final int ITEMS_PER_PAGE = COLS * ROWS; // 72 items per page
    private static final int TRACKED_ITEMS_PER_PAGE = 6;

    private Button armorPresetButton;
    private Button weaponsPresetButton;
    private Button toolsPresetButton;

    private Button searchPrevButton;
    private Button searchNextButton;
    private Button filterPrevButton;
    private Button filterNextButton;

    public ItemFilterScreen(Screen parent) {
        super(Component.literal("Item Whitelist Manager"));
        this.parent = parent;

        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                allItems.add(item);
            }
        }
    }

    @Override
    protected void init() {
        int leftW = 276;
        int rightW = 224;
        int gap = 16;
        int totalW = leftW + rightW + gap;
        int startX = (this.width - totalW) / 2;
        int leftX = startX;
        int rightX = startX + leftW + gap;
        int topY = 18;

        // Left Panel - Search Box (Width 256px)
        this.searchBox = new EditBox(this.font, leftX + 10, topY + 20, 256, 16, Component.literal("Search..."));
        this.searchBox.setResponder(text -> filterSearchResults());
        this.searchBox.setHint(Component.literal("§8Search all items by name or ID..."));
        this.addRenderableWidget(this.searchBox);

        // Left Panel - Presets Row (Toggleable Add/Remove buttons)
        int presetsY = topY + 188;
        this.armorPresetButton = this.addRenderableWidget(
                Button.builder(Component.literal("+ All Armor"), button -> toggleItemsByFilter(this::isArmor))
                        .bounds(leftX + 10, presetsY, 82, 18)
                        .build()
        );

        this.weaponsPresetButton = this.addRenderableWidget(
                Button.builder(Component.literal("+ All Weapons"), button -> toggleItemsByFilter(this::isWeapon))
                        .bounds(leftX + 96, presetsY, 88, 18)
                        .build()
        );

        this.toolsPresetButton = this.addRenderableWidget(
                Button.builder(Component.literal("+ All Tools"), button -> toggleItemsByFilter(this::isTool))
                        .bounds(leftX + 188, presetsY, 78, 18)
                        .build()
        );

        // Left Panel - Navigation (Y = topY + 210)
        int navY = topY + 210;
        this.searchPrevButton = this.addRenderableWidget(Button.builder(Component.literal("◀ Prev"), button -> {
            if (searchPage > 0) {
                searchPage--;
                updateNavigation();
            }
        }).bounds(leftX + 10, navY, 52, 18).build());

        this.searchNextButton = this.addRenderableWidget(Button.builder(Component.literal("Next ▶"), button -> {
            if ((searchPage + 1) * ITEMS_PER_PAGE < searchResults.size()) {
                searchPage++;
                updateNavigation();
            }
        }).bounds(leftX + leftW - 62, navY, 52, 18).build());

        // Right Panel - Clear All Button (Y = topY + 188)
        this.addRenderableWidget(Button.builder(Component.literal("§c✕ Clear Whitelist"), button -> {
            ModConfig.filteredItems.clear();
            ModConfig.itemPackOverrides.clear();
            ModConfig.save();
            updateNavigation();
        }).bounds(rightX + 10, presetsY, rightW - 20, 18)
        .tooltip(Tooltip.create(Component.literal("Remove all items from the whitelist")))
        .build());

        // Right Panel - Whitelist Navigation (Y = topY + 210)
        this.filterPrevButton = this.addRenderableWidget(Button.builder(Component.literal("◀ Prev"), button -> {
            if (filterPage > 0) {
                filterPage--;
                updateNavigation();
            }
        }).bounds(rightX + 10, navY, 48, 18).build());

        this.filterNextButton = this.addRenderableWidget(Button.builder(Component.literal("Next ▶"), button -> {
            if ((filterPage + 1) * TRACKED_ITEMS_PER_PAGE < ModConfig.filteredItems.size()) {
                filterPage++;
                updateNavigation();
            }
        }).bounds(rightX + rightW - 58, navY, 48, 18).build());

        // Bottom - Done / Save Button
        this.addRenderableWidget(Button.builder(Component.literal("Done / Save"), button -> {
            ModConfig.save();
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 65, this.height - 24, 130, 20).build());

        filterSearchResults();
        updatePresetButtonLabels();
    }

    private boolean isArmor(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return false;
        String path = id.getPath();
        return path.contains("helmet") || path.contains("chestplate") || path.contains("leggings")
                || path.contains("boots") || path.contains("elytra") || path.contains("shield")
                || path.contains("wolf_armor") || path.contains("turtle_helmet")
                || path.contains("cap") || path.contains("tunic") || path.contains("pants")
                || path.contains("horse_armor");
    }

    private boolean isWeapon(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return false;
        String path = id.getPath();
        return path.endsWith("_sword") || path.endsWith("_mace") || path.equals("bow")
                || path.equals("crossbow") || path.equals("trident") || path.equals("mace");
    }

    private boolean isTool(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return false;
        String path = id.getPath();
        return path.endsWith("_pickaxe") || path.endsWith("_axe") || path.endsWith("_shovel")
                || path.endsWith("_hoe") || path.equals("shears") || path.equals("fishing_rod")
                || path.equals("flint_and_steel") || path.equals("brush") || path.equals("spyglass")
                || path.equals("compass") || path.equals("clock") || path.equals("lead");
    }

    private boolean matchesCategory(Item item, Category cat) {
        return switch (cat) {
            case ALL -> true;
            case ARMOR -> isArmor(item);
            case WEAPONS -> isWeapon(item);
            case TOOLS -> isTool(item) && !isWeapon(item);
            case OTHER -> !isArmor(item) && !isWeapon(item) && !isTool(item);
        };
    }

    private void filterSearchResults() {
        String query = (this.searchBox != null ? this.searchBox.getValue() : "").trim().toLowerCase(Locale.ROOT);
        this.searchResults = allItems.stream()
                .filter(item -> matchesCategory(item, selectedCategory))
                .filter(item -> {
                    if (query.isEmpty()) return true;
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    if (id == null) return false;
                    String idStr = id.toString().toLowerCase(Locale.ROOT);
                    String name = Component.translatable(item.getDescriptionId()).getString().toLowerCase(Locale.ROOT);
                    return idStr.contains(query) || name.contains(query);
                })
                .collect(Collectors.toList());
        this.searchPage = 0;
        updateNavigation();
    }

    private boolean areAllItemsWhitelisted(java.util.function.Predicate<Item> predicate) {
        int matchCount = 0;
        int presentCount = 0;
        for (Item item : allItems) {
            if (predicate.test(item)) {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null) {
                    matchCount++;
                    if (ModConfig.filteredItems.contains(id.toString())) {
                        presentCount++;
                    }
                }
            }
        }
        return matchCount > 0 && presentCount == matchCount;
    }

    private void updatePresetButtonLabels() {
        if (armorPresetButton != null) {
            boolean all = areAllItemsWhitelisted(this::isArmor);
            armorPresetButton.setMessage(Component.literal(all ? "§c- All Armor" : "+ All Armor"));
            armorPresetButton.setTooltip(Tooltip.create(Component.literal(all ? "Remove all armor items from whitelist" : "Add all armor items to whitelist")));
        }
        if (weaponsPresetButton != null) {
            boolean all = areAllItemsWhitelisted(this::isWeapon);
            weaponsPresetButton.setMessage(Component.literal(all ? "§c- All Weapons" : "+ All Weapons"));
            weaponsPresetButton.setTooltip(Tooltip.create(Component.literal(all ? "Remove all weapons from whitelist" : "Add all weapons to whitelist")));
        }
        if (toolsPresetButton != null) {
            boolean all = areAllItemsWhitelisted(this::isTool);
            toolsPresetButton.setMessage(Component.literal(all ? "§c- All Tools" : "+ All Tools"));
            toolsPresetButton.setTooltip(Tooltip.create(Component.literal(all ? "Remove all tools from whitelist" : "Add all tools to whitelist")));
        }
    }

    private void toggleItemsByFilter(java.util.function.Predicate<Item> predicate) {
        List<String> matchingIds = new ArrayList<>();
        for (Item item : allItems) {
            if (predicate.test(item)) {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null) {
                    matchingIds.add(id.toString());
                }
            }
        }

        if (matchingIds.isEmpty()) return;

        boolean allPresent = ModConfig.filteredItems.containsAll(matchingIds);
        if (allPresent) {
            ModConfig.filteredItems.removeAll(matchingIds);
            for (String id : matchingIds) {
                ModConfig.itemPackOverrides.remove(id);
            }
        } else {
            for (String idStr : matchingIds) {
                if (!ModConfig.filteredItems.contains(idStr)) {
                    ModConfig.filteredItems.add(idStr);
                }
            }
        }

        ModConfig.save();
        updateNavigation();
    }

    private void updateNavigation() {
        if (this.searchPrevButton != null) {
            this.searchPrevButton.active = searchPage > 0;
        }
        if (this.searchNextButton != null) {
            this.searchNextButton.active = (searchPage + 1) * ITEMS_PER_PAGE < searchResults.size();
        }

        List<String> addedList = ModConfig.filteredItems;
        if (filterPage * TRACKED_ITEMS_PER_PAGE >= addedList.size() && filterPage > 0) {
            filterPage--;
        }

        if (this.filterPrevButton != null) {
            this.filterPrevButton.active = filterPage > 0;
        }
        if (this.filterNextButton != null) {
            this.filterNextButton.active = (filterPage + 1) * TRACKED_ITEMS_PER_PAGE < addedList.size();
        }

        updatePresetButtonLabels();
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, boolean isSelected, boolean isHovered) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF1C1C1E);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFF38383C);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF141416);

        if (isSelected) {
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x4500E676);
            graphics.outline(x, y, 18, 18, 0xFF00E676);
        }

        if (isHovered) {
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x45FFFFFF);
            if (!isSelected) {
                graphics.outline(x, y, 18, 18, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        double mouseX = event.x();
        double mouseY = event.y();

        int leftW = 276;
        int rightW = 224;
        int gap = 16;
        int totalW = leftW + rightW + gap;
        int startX = (this.width - totalW) / 2;
        int leftX = startX;
        int rightX = startX + leftW + gap;
        int topY = 18;

        // 1. Category Tab click
        int tabStripY = topY + 40;
        if (mouseX >= leftX + 10 && mouseX < leftX + 266 && mouseY >= tabStripY && mouseY < tabStripY + 16) {
            int tabW = 51;
            int tabIdx = (int) ((mouseX - (leftX + 10)) / tabW);
            Category[] cats = Category.values();
            if (tabIdx >= 0 && tabIdx < cats.length) {
                this.selectedCategory = cats[tabIdx];
                filterSearchResults();
                return true;
            }
        }

        // 2. Item Grid click (Left side)
        int gridStartX = leftX + (leftW - COLS * 20) / 2;
        int gridStartY = topY + 60;
        int cellSize = 20;

        if (mouseX >= gridStartX && mouseX < gridStartX + COLS * cellSize &&
            mouseY >= gridStartY && mouseY < gridStartY + ROWS * cellSize) {
            int col = ((int) mouseX - gridStartX) / cellSize;
            int row = ((int) mouseY - gridStartY) / cellSize;

            int slotX = gridStartX + col * cellSize + 1;
            int slotY = gridStartY + row * cellSize + 1;
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                int cellIdx = row * COLS + col;
                int currentIdx = searchPage * ITEMS_PER_PAGE + cellIdx;

                if (currentIdx < searchResults.size()) {
                    Item item = searchResults.get(currentIdx);
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null) {
                        String itemStrId = id.toString();
                        if (ModConfig.filteredItems.contains(itemStrId)) {
                            ModConfig.filteredItems.remove(itemStrId);
                            ModConfig.itemPackOverrides.remove(itemStrId);
                        } else {
                            ModConfig.filteredItems.add(itemStrId);
                        }
                        ModConfig.save();
                        updateNavigation();
                    }
                }
                return true;
            }
        }

        // 3. Tracked Items Row click (Right side)
        int listStartY = topY + 24;
        int rowHeight = 26;
        int rowW = rightW - 20;

        if (mouseX >= rightX + 10 && mouseX < rightX + 10 + rowW &&
            mouseY >= listStartY && mouseY < listStartY + TRACKED_ITEMS_PER_PAGE * rowHeight) {
            int clickedRow = ((int) mouseY - listStartY) / rowHeight;
            int currentIdx = filterPage * TRACKED_ITEMS_PER_PAGE + clickedRow;

            if (currentIdx < ModConfig.filteredItems.size()) {
                String itemStrId = ModConfig.filteredItems.get(currentIdx);
                int rowY = listStartY + clickedRow * rowHeight;
                int delBtnX = rightX + 10 + rowW - 18;
                int packBtnX = delBtnX - 20;

                // Delete button clicked
                if (mouseX >= delBtnX && mouseX <= delBtnX + 16 && mouseY >= rowY + 4 && mouseY <= rowY + 20) {
                    ModConfig.filteredItems.remove(currentIdx);
                    ModConfig.itemPackOverrides.remove(itemStrId);
                    ModConfig.save();
                    updateNavigation();
                    return true;
                }

                // Pack Change button clicked
                if (mouseX >= packBtnX && mouseX <= packBtnX + 16 && mouseY >= rowY + 4 && mouseY <= rowY + 20) {
                    this.minecraft.setScreen(new ItemPackPickerScreen(this, itemStrId));
                    return true;
                }

                // Row clicked -> Open pack picker for this item!
                this.minecraft.setScreen(new ItemPackPickerScreen(this, itemStrId));
                return true;
            }
        }

        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int leftW = 276;
        int rightW = 224;
        int panelH = 236;
        int gap = 16;
        int totalW = leftW + rightW + gap;
        int startX = (this.width - totalW) / 2;
        int leftX = startX;
        int rightX = startX + leftW + gap;
        int topY = 18;

        // Main Screen Title
        graphics.centeredText(this.font, "§f§lItem Whitelist Manager", this.width / 2, 6, 0xFFFFFFFF);

        // Panel 1: Left Container (Item Catalog)
        graphics.fill(leftX, topY, leftX + leftW, topY + panelH, 0x40000000);
        graphics.outline(leftX, topY, leftW, panelH, 0x20FFFFFF);
        graphics.text(this.font, "§f§lItem Catalog", leftX + 10, topY + 6, 0xFFFFFFFF);

        // Category Tab Strip (Width 255px total across 5 tabs = 51px each)
        int tabStripY = topY + 40;
        Category[] cats = Category.values();
        int tabW = 51;
        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            int tabX = leftX + 10 + i * tabW;
            boolean isSel = (cat == selectedCategory);
            boolean isTabHover = mouseX >= tabX && mouseX < tabX + tabW && mouseY >= tabStripY && mouseY < tabStripY + 16;

            if (isSel) {
                graphics.fill(tabX, tabStripY, tabX + tabW - 2, tabStripY + 15, 0x3500E676);
                graphics.fill(tabX, tabStripY + 14, tabX + tabW - 2, tabStripY + 16, 0xFF00E676);
            } else if (isTabHover) {
                graphics.fill(tabX, tabStripY, tabX + tabW - 2, tabStripY + 15, 0x20FFFFFF);
            }

            String tabLabel = (isSel ? "§a§l" : (isTabHover ? "§f" : "§7")) + cat.getLabel();
            graphics.centeredText(this.font, tabLabel, tabX + (tabW - 2) / 2, tabStripY + 4, 0xFFFFFFFF);
        }

        // Left Item Grid (12 cols x 6 rows = 72 items!)
        int gridStartX = leftX + (leftW - COLS * 20) / 2;
        int gridStartY = topY + 60;
        int cellSize = 20;
        int startSearchIdx = searchPage * ITEMS_PER_PAGE;
        Item hoveredGridItem = null;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int cellIdx = row * COLS + col;
                int currentIdx = startSearchIdx + cellIdx;
                int slotX = gridStartX + col * cellSize + 1;
                int slotY = gridStartY + row * cellSize + 1;

                boolean isHovered = mouseX >= slotX && mouseX < slotX + 18 &&
                                    mouseY >= slotY && mouseY < slotY + 18;

                boolean isSelected = false;
                if (currentIdx < searchResults.size()) {
                    Item item = searchResults.get(currentIdx);
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null && ModConfig.filteredItems.contains(id.toString())) {
                        isSelected = true;
                    }
                }

                drawSlot(graphics, slotX, slotY, isSelected, isHovered);

                if (currentIdx < searchResults.size()) {
                    Item item = searchResults.get(currentIdx);
                    graphics.fakeItem(new ItemStack(item), slotX + 1, slotY + 1);

                    if (isHovered) {
                        hoveredGridItem = item;
                    }
                }
            }
        }

        // Left Pagination Indicator
        int totalSearchPages = Math.max(1, (searchResults.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        String searchPageStr = String.format("§7Page §f%d§7/§f%d §8(%d items)", searchPage + 1, totalSearchPages, searchResults.size());
        graphics.centeredText(this.font, searchPageStr, leftX + leftW / 2, topY + 215, 0xFF888888);

        // Panel 2: Right Container (Active Whitelist)
        graphics.fill(rightX, topY, rightX + rightW, topY + panelH, 0x40000000);
        graphics.outline(rightX, topY, rightW, panelH, 0x20FFFFFF);

        int filterCount = ModConfig.filteredItems.size();
        graphics.text(this.font, "§f§lActive Whitelist", rightX + 10, topY + 6, 0xFFFFFFFF);
        String countBadge = "§e" + filterCount + " " + (filterCount == 1 ? "item" : "items");
        graphics.text(this.font, countBadge, rightX + rightW - this.font.width(countBadge) - 10, topY + 6, 0xFFFFFFFF);

        // Tracked Items List Rows (6 items per page)
        int listStartY = topY + 24;
        int rowHeight = 26;
        int startFilterIdx = filterPage * TRACKED_ITEMS_PER_PAGE;
        List<String> addedList = ModConfig.filteredItems;
        String hoveredRowTooltip = null;

        for (int i = 0; i < TRACKED_ITEMS_PER_PAGE; i++) {
            int currentIdx = startFilterIdx + i;
            if (currentIdx >= addedList.size()) break;

            int rowY = listStartY + i * rowHeight;
            String itemStrId = addedList.get(currentIdx);
            int rowW = rightW - 20;

            boolean isRowHovered = mouseX >= rightX + 10 && mouseX < rightX + 10 + rowW &&
                                   mouseY >= rowY && mouseY < rowY + 24;

            int delBtnX = rightX + 10 + rowW - 18;
            boolean isDelHovered = isRowHovered && mouseX >= delBtnX && mouseX <= delBtnX + 16 && mouseY >= rowY + 4 && mouseY <= rowY + 20;

            int packBtnX = delBtnX - 20;
            boolean isPackHovered = isRowHovered && mouseX >= packBtnX && mouseX <= packBtnX + 16 && mouseY >= rowY + 4 && mouseY <= rowY + 20;

            graphics.fill(rightX + 10, rowY, rightX + 10 + rowW, rowY + 24, isRowHovered ? 0x30FFFFFF : 0x20000000);
            graphics.outline(rightX + 10, rowY, rowW, 24, isRowHovered ? 0x50FFFFFF : 0x18FFFFFF);

            Identifier id = Identifier.tryParse(itemStrId);
            ItemStack stack = ItemStack.EMPTY;
            String displayName = itemStrId;
            if (id != null) {
                Item item = BuiltInRegistries.ITEM.get(id)
                        .map(net.minecraft.core.Holder.Reference::value)
                        .orElse(Items.AIR);
                if (item != Items.AIR) {
                    stack = new ItemStack(item);
                    displayName = Component.translatable(item.getDescriptionId()).getString();
                } else {
                    displayName = id.getPath();
                }
            }

            if (!stack.isEmpty()) {
                graphics.fakeItem(stack, rightX + 13, rowY + 4);
            }

            String renderedName = displayName;
            if (renderedName.length() > 15) {
                renderedName = renderedName.substring(0, 13) + "..";
            }

            graphics.text(this.font, renderedName, rightX + 33, rowY + 3, 0xFFFFFFFF);

            // Pack Source Indicator
            String packSource = ModConfig.getItemPack(itemStrId);
            String packDisplay = packSource.equals("default") ? "Default" : (packSource.equals("vanilla") ? "Vanilla" : packSource);
            if (packDisplay.length() > 13) packDisplay = packDisplay.substring(0, 11) + "..";
            graphics.text(this.font, "§8Pack: §e" + packDisplay, rightX + 33, rowY + 13, 0xFFAAAAAA);

            // Pack Change Button: [ 📦 ] (16x16 square, vertically & horizontally centered)
            graphics.fill(packBtnX, rowY + 4, packBtnX + 16, rowY + 20, isPackHovered ? 0x5000E676 : 0x20000000);
            graphics.outline(packBtnX, rowY + 4, 16, 16, isPackHovered ? 0xFF00E676 : 0x30FFFFFF);
            graphics.centeredText(this.font, "§e📦", packBtnX + 8, rowY + 8, 0xFFFFFFFF);

            // Delete Button: [ ✕ ] (16x16 square, vertically & horizontally centered)
            graphics.fill(delBtnX, rowY + 4, delBtnX + 16, rowY + 20, isDelHovered ? 0x60FF5252 : 0x20000000);
            graphics.outline(delBtnX, rowY + 4, 16, 16, isDelHovered ? 0xFFFF5252 : 0x30FFFFFF);
            graphics.centeredText(this.font, "§c✕", delBtnX + 8, rowY + 8, 0xFFFFFFFF);

            if (isDelHovered) {
                hoveredRowTooltip = "§cRemove " + displayName + " from whitelist";
            } else if (isPackHovered) {
                hoveredRowTooltip = "§eChange resource pack source for " + displayName + "\n§7Current: §f" + packDisplay;
            } else if (isRowHovered) {
                hoveredRowTooltip = "§f" + displayName + "\n§8" + itemStrId + "\n§eClick to change source pack (§f" + packDisplay + "§e)";
            }
        }

        // Empty Whitelist State
        if (ModConfig.filteredItems.isEmpty()) {
            graphics.centeredText(this.font, "§7No items whitelisted", rightX + rightW / 2, topY + 76, 0xFFAAAAAA);
            graphics.centeredText(this.font, "§8Click catalog items to add", rightX + rightW / 2, topY + 92, 0xFF888888);
            graphics.centeredText(this.font, "§8or use preset buttons", rightX + rightW / 2, topY + 104, 0xFF888888);
        }

        // Right Pagination Indicator
        int totalFilterPages = Math.max(1, (addedList.size() + TRACKED_ITEMS_PER_PAGE - 1) / TRACKED_ITEMS_PER_PAGE);
        String filterPageStr = String.format("§7Page §f%d§7/§f%d", filterPage + 1, totalFilterPages);
        graphics.centeredText(this.font, filterPageStr, rightX + rightW / 2, topY + 215, 0xFF888888);

        // Tooltips (Rendered on top)
        if (hoveredGridItem != null) {
            Identifier id = BuiltInRegistries.ITEM.getKey(hoveredGridItem);
            boolean isSel = id != null && ModConfig.filteredItems.contains(id.toString());
            String tooltipText = "§f" + Component.translatable(hoveredGridItem.getDescriptionId()).getString()
                    + (id != null ? "\n§8" + id : "")
                    + "\n" + (isSel ? "§c✕ Click to remove from whitelist" : "§a+ Click to add to whitelist");
            graphics.setTooltipForNextFrame(this.font, Component.literal(tooltipText), mouseX, mouseY);
        } else if (hoveredRowTooltip != null) {
            graphics.setTooltipForNextFrame(this.font, Component.literal(hoveredRowTooltip), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(this.parent);
    }
}
