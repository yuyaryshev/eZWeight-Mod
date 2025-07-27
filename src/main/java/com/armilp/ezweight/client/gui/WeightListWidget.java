package com.armilp.ezweight.client.gui;

import com.armilp.ezweight.client.gui.edit.WeightEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.HashSet;
import java.util.Set;

public class WeightListWidget extends ObjectSelectionList<WeightListWidget.Entry> {
    public static final Set<ItemEntry> selectedEntries = new HashSet<>();
    private final WeightMenuScreen parent;

    public WeightListWidget(WeightMenuScreen parent, Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, bottom, itemHeight);
        this.parent = parent;
    }

    public Set<ItemEntry> getSelectedEntries() {
        return selectedEntries;
    }

    public void addCategory(String namespace) {
        this.addEntry(new CategoryEntry(namespace));
    }

    public void addItem(ItemStack stack, double weight) {
        this.addEntry(new ItemEntry(stack, weight));
    }

    public void clear() {
        this.clearEntries();
    }

    public abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
    }

    public static class CategoryEntry extends Entry {
        private final Component title;

        public CategoryEntry(String namespace) {
            this.title = Component.literal("§6[" + namespace + "]");
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.drawString(Minecraft.getInstance().font, title, left + 5, top + 5, 0xFFFFAA);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public Component getNarration() {
            return title;
        }
    }

    public class ItemEntry extends Entry {
        public final ItemStack stack;
        private final double weight;

        public ItemEntry(ItemStack stack, double weight) {
            this.stack = stack;
            this.weight = weight;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            int color = selectedEntries.contains(this) ? 0xFFAA00 : 0xFFFFFF;

            // Fondo resaltado para ítems seleccionados
            if (selectedEntries.contains(this)) {
                graphics.fill(left, top, left + width, top + height, 0x8000FF00);
            }

            graphics.renderItem(stack, left + 5, top + 2);
            graphics.renderItemDecorations(Minecraft.getInstance().font, stack, left + 5, top + 2);

            String itemName = stack.getHoverName().getString();
            String weightText = Component.translatable("tooltip.ezweight.item_weight", String.format("%.1f", weight)).getString();

            graphics.drawString(Minecraft.getInstance().font, itemName, left + 26, top + 4, color);
            graphics.drawString(Minecraft.getInstance().font, weightText, left + 26, top + 14, 0xAAAAAA);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                long window = Minecraft.getInstance().getWindow().getWindow();
                boolean ctrlDown = InputConstants.isKeyDown(window, 341) || InputConstants.isKeyDown(window, 345); // LCtrl o RCtrl

                if (ctrlDown) {
                    if (selectedEntries.contains(this)) {
                        selectedEntries.remove(this);
                    } else {
                        selectedEntries.add(this);
                    }
                } else {
                    selectedEntries.clear();
                    selectedEntries.add(this);
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (id != null) {
                        Minecraft.getInstance().setScreen(new WeightEditScreen(parent, stack, id));
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(stack.getHoverName().getString() + " - Weight: " + weight);
        }
    }
}
