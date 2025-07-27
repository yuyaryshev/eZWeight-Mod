package com.armilp.ezweight.client.gui.edit;

import com.armilp.ezweight.data.ItemWeightRegistry;
import com.armilp.ezweight.network.EZWeightNetwork;
import com.armilp.ezweight.network.sync.WeightUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class WeightEditScreen extends Screen {

    private final Screen parent;
    private final ItemStack stack;
    private final ResourceLocation id;
    private EditBox weightBox;

    public WeightEditScreen(Screen parent, ItemStack stack, ResourceLocation id) {
        super(Component.translatable("screen.ezweight.edit_weight"));
        this.parent = parent;
        this.stack = stack;
        this.id = id;
    }

    @Override
    protected void init() {
        double currentWeight = ItemWeightRegistry.getWeight(stack);

        weightBox = new EditBox(this.font, this.width / 2 - 50, this.height / 2 - 10, 100, 20,
                Component.translatable("gui.ezweight.weight_placeholder"));
        weightBox.setValue(String.valueOf(currentWeight));
        this.addRenderableWidget(weightBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.ezweight.save"), b -> {
            try {
                double newWeight = Double.parseDouble(weightBox.getValue());
                EZWeightNetwork.CHANNEL.sendToServer(new WeightUpdatePacket(id, newWeight));
                this.minecraft.setScreen(parent);
            } catch (NumberFormatException e) {
                weightBox.setTextColor(0xFF5555); // rojo si hay error
            }
        }).bounds(this.width / 2 - 50, this.height / 2 + 20, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.ezweight.cancel"), b -> {
            this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 50, this.height / 2 + 50, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, stack.getHoverName().getString(), this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        weightBox.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
