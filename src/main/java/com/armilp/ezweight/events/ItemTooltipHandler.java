package com.armilp.ezweight.events;

import com.armilp.ezweight.data.ItemWeightRegistry;
import com.armilp.ezweight.player.PlayerWeightHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ezweight", value = Dist.CLIENT)
public class ItemTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.isEmpty()) {

            double baseWeight = ItemWeightRegistry.getWeight(stack);
            double totalWeight = PlayerWeightHandler.getExtendedStackWeightWithContents(stack, event.getEntity());

            if (totalWeight > 0.0) {
                // Si el peso total es exactamente el base * cantidad, no hay contenido extra → mostrar como antes
                if (totalWeight == baseWeight * stack.getCount()) {
                    event.getToolTip().add(Component.translatable("tooltip.ezweight.item_weight",
                            String.format("%.2f", totalWeight)).withStyle(ChatFormatting.GOLD));
                } else {
                    // Si hay contenido extra, mostrar dos líneas
                    if (baseWeight > 0.0) {
                        event.getToolTip().add(Component.translatable("tooltip.ezweight.item_weight_base",
                                String.format("%.2f", baseWeight)).withStyle(ChatFormatting.GOLD));
                    }

                    event.getToolTip().add(Component.translatable("tooltip.ezweight.item_weight_total",
                            String.format("%.2f", totalWeight)).withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }
}
