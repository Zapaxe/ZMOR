package com.example.client;

import com.example.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private List<Item> allItems = new ArrayList<>();
    
    private int searchPage = 0;
    private int filterPage = 0;
    private static final int COLS = 9;
    private static final int ROWS = 5;
    private static final int ITEMS_PER_PAGE = COLS * ROWS; // 45 items
    private static final int TRACKED_ITEMS_PER_PAGE = 6;
    
    private Button searchPrevButton;
    private Button searchNextButton;
    private Button filterPrevButton;
    private Button filterNextButton;

    public ItemFilterScreen(Screen parent) {
        super(Component.literal("Select Items to Apply Custom Textures"));
        this.parent = parent;
        
        // Cache all items from registry
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                allItems.add(item);
            }
        }
    }
    
    @Override
    protected void init() {
        int leftColX = this.width / 2 - 200;
        int rightColX = this.width / 2 + 20;
        
        // Create search box
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 25, 200, 20, Component.literal("Search..."));
        this.searchBox.setResponder(this::onSearchTextChanged);
        this.searchBox.setFocused(true);
        this.addRenderableWidget(this.searchBox);
        
        // Add Back button at the bottom
        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> {
                    ModConfig.save();
                    this.minecraft.setScreen(this.parent);
                }
        ).bounds(this.width / 2 - 75, this.height - 30, 150, 20).build());
        
        // Create navigation buttons for search
        this.searchPrevButton = this.addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> {
                    if (searchPage > 0) {
                        searchPage--;
                        updateNavigation();
                    }
                }
        ).bounds(leftColX, 165, 20, 20).build());
        
        this.searchNextButton = this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> {
                    if ((searchPage + 1) * ITEMS_PER_PAGE < searchResults.size()) {
                        searchPage++;
                        updateNavigation();
                    }
                }
        ).bounds(leftColX + (COLS * 20) - 20, 165, 20, 20).build());
        
        // Create navigation buttons for filter
        this.filterPrevButton = this.addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> {
                    if (filterPage > 0) {
                        filterPage--;
                        updateNavigation();
                    }
                }
        ).bounds(rightColX, 195, 20, 20).build());
        
        this.filterNextButton = this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> {
                    if ((filterPage + 1) * TRACKED_ITEMS_PER_PAGE < ModConfig.filteredItems.size()) {
                        filterPage++;
                        updateNavigation();
                    }
                }
        ).bounds(rightColX + 180 - 20, 195, 20, 20).build());

        // Perform initial search to populate results
        onSearchTextChanged(this.searchBox.getValue());
    }
    
    private void onSearchTextChanged(String text) {
        String query = text.trim().toLowerCase(Locale.ROOT);
        this.searchResults = allItems.stream()
                .filter(item -> {
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
    
    private void updateNavigation() {
        if (this.searchPrevButton != null) {
            this.searchPrevButton.active = searchPage > 0;
        }
        if (this.searchNextButton != null) {
            this.searchNextButton.active = (searchPage + 1) * ITEMS_PER_PAGE < searchResults.size();
        }
        
        List<String> addedList = ModConfig.filteredItems;
        // Adjust page if items were deleted and page is out of bounds
        if (filterPage * TRACKED_ITEMS_PER_PAGE >= addedList.size() && filterPage > 0) {
            filterPage--;
        }
        
        if (this.filterPrevButton != null) {
            this.filterPrevButton.active = filterPage > 0;
        }
        if (this.filterNextButton != null) {
            this.filterNextButton.active = (filterPage + 1) * TRACKED_ITEMS_PER_PAGE < addedList.size();
        }
    }
    
    private void drawSlot(GuiGraphics graphics, int x, int y, boolean hovered) {
        // Draw recessed standard Minecraft 3D slot
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737); // Dark border
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF); // Light highlight border
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B); // Inner recessed background
        
        if (hovered) {
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF); // Hover overlay
        }
    }
    
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        
        int gridStartX = this.width / 2 - 200;
        int gridStartY = 60;
        int cellSize = 20;
        
        // 1. Check item grid click
        if (mouseX >= gridStartX && mouseX < gridStartX + COLS * cellSize &&
            mouseY >= gridStartY && mouseY < gridStartY + ROWS * cellSize) {
            int col = ((int) mouseX - gridStartX) / cellSize;
            int row = ((int) mouseY - gridStartY) / cellSize;
            
            // Check if within slot boundaries
            int slotX = gridStartX + col * cellSize + 1;
            int slotY = gridStartY + row * cellSize + 1;
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                int cellIdx = row * COLS + col;
                int startSearchIdx = searchPage * ITEMS_PER_PAGE;
                int currentIdx = startSearchIdx + cellIdx;
                
                if (currentIdx < searchResults.size()) {
                    Item item = searchResults.get(currentIdx);
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null) {
                        String itemStrId = id.toString();
                        if (!ModConfig.filteredItems.contains(itemStrId)) {
                            ModConfig.filteredItems.add(itemStrId);
                            ModConfig.save();
                            updateNavigation();
                        }
                    }
                }
                return true;
            }
        }
        
        // 2. Check tracked items list click
        int rightColX = this.width / 2 + 20;
        int rightColY = 60;
        int rowHeight = 22;
        
        if (mouseX >= rightColX && mouseX < rightColX + 180 &&
            mouseY >= rightColY && mouseY < rightColY + TRACKED_ITEMS_PER_PAGE * rowHeight) {
            int clickedRow = ((int) mouseY - rightColY) / rowHeight;
            int startFilterIdx = filterPage * TRACKED_ITEMS_PER_PAGE;
            int currentIdx = startFilterIdx + clickedRow;
            
            if (currentIdx < ModConfig.filteredItems.size()) {
                ModConfig.filteredItems.remove(currentIdx);
                ModConfig.save();
                updateNavigation();
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        
        // Draw title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        
        int gridStartX = this.width / 2 - 200;
        int gridStartY = 60;
        int cellSize = 20;
        
        // Draw labels
        graphics.drawString(this.font, "Search Results", gridStartX, 48, 0xFFAAAAAA);
        graphics.drawString(this.font, "Search:", this.width / 2 - 150, 29, 0xFFFFFFFF);
        
        // Draw grid slots and item icons
        int startSearchIdx = searchPage * ITEMS_PER_PAGE;
        Item hoveredItem = null;
        
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int cellIdx = row * COLS + col;
                int currentIdx = startSearchIdx + cellIdx;
                int slotX = gridStartX + col * cellSize + 1;
                int slotY = gridStartY + row * cellSize + 1;
                
                boolean isHovered = mouseX >= slotX && mouseX < slotX + 18 &&
                                    mouseY >= slotY && mouseY < slotY + 18;
                
                drawSlot(graphics, slotX, slotY, isHovered);
                
                if (currentIdx < searchResults.size()) {
                    Item item = searchResults.get(currentIdx);
                    graphics.renderFakeItem(new ItemStack(item), slotX + 1, slotY + 1);
                    
                    // Draw highlight if already selected
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null && ModConfig.filteredItems.contains(id.toString())) {
                        graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x5000FF00); // Tint green
                    }
                    
                    if (isHovered) {
                        hoveredItem = item;
                    }
                }
            }
        }
        
        // Draw tracked list
        int rightColX = this.width / 2 + 20;
        int rightColY = 60;
        int rowHeight = 22;
        
        graphics.drawString(this.font, "Tracked Items (" + ModConfig.filteredItems.size() + ")", rightColX, 48, 0xFFAAAAAA);
        
        int startFilterIdx = filterPage * TRACKED_ITEMS_PER_PAGE;
        List<String> addedList = ModConfig.filteredItems;
        
        for (int i = 0; i < TRACKED_ITEMS_PER_PAGE; i++) {
            int currentIdx = startFilterIdx + i;
            if (currentIdx >= addedList.size()) break;
            
            int rowY = rightColY + i * rowHeight;
            String itemStrId = addedList.get(currentIdx);
            
            boolean isRowHovered = mouseX >= rightColX && mouseX < rightColX + 180 &&
                                   mouseY >= rowY && mouseY < rowY + 20;
            
            // Recessed row background
            graphics.fill(rightColX, rowY, rightColX + 180, rowY + 20, isRowHovered ? 0x40FFFFFF : 0x20000000);
            
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
            
            // Draw item icon
            if (!stack.isEmpty()) {
                graphics.renderFakeItem(stack, rightColX + 2, rowY + 2);
            }
            
            // Truncate name
            if (displayName.length() > 20) {
                displayName = displayName.substring(0, 17) + "...";
            }
            
            // Render text: highlighted/red on hover to indicate click deletes it
            graphics.drawString(this.font, displayName, rightColX + 22, rowY + 6, isRowHovered ? 0xFFFF5555 : 0xFFFFFFFF);
        }
        
        // Draw search results page label
        String searchPageStr = String.format("Page %d/%d", searchPage + 1, Math.max(1, (searchResults.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE));
        graphics.drawCenteredString(this.font, searchPageStr, gridStartX + (COLS * cellSize) / 2, 170, 0xFF888888);
        
        // Draw tracked list page label
        String filterPageStr = String.format("Page %d/%d", filterPage + 1, Math.max(1, (ModConfig.filteredItems.size() + TRACKED_ITEMS_PER_PAGE - 1) / TRACKED_ITEMS_PER_PAGE));
        graphics.drawCenteredString(this.font, filterPageStr, rightColX + 180 / 2, 200, 0xFF888888);
        
        // Empty message
        if (ModConfig.filteredItems.isEmpty()) {
            graphics.drawString(this.font, "Mod applies to all items", rightColX + 5, 70, 0xFF555555);
            graphics.drawString(this.font, "(empty list)", rightColX + 5, 82, 0xFF555555);
        }
        
        // Render item tooltip last so it is on top of everything
        if (hoveredItem != null) {
            graphics.setTooltipForNextFrame(this.font, new ItemStack(hoveredItem), mouseX, mouseY);
        }
    }
    
    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(this.parent);
    }
}
