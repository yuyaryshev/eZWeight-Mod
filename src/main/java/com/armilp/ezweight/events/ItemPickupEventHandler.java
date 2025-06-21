package com.armilp.ezweight.events;

import com.armilp.ezweight.EZWeight;
import com.armilp.ezweight.commands.WeightCommands;
import com.armilp.ezweight.config.WeightConfig;
import com.armilp.ezweight.data.ItemWeightRegistry;
import com.armilp.ezweight.player.PlayerWeightHandler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EZWeight.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemPickupEventHandler {

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!WeightCommands.isWeightEnabledFor(player)) return;

        double currentWeight = PlayerWeightHandler.getTotalWeight(player);
        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();

        double itemWeight = ItemWeightRegistry.getWeight(stack.getItem());
        double maxWeight = WeightConfig.COMMON.MAX_WEIGHT.get();

        int maxPickupCount = (int) Math.floor((maxWeight - currentWeight) / itemWeight);

        if (maxPickupCount <= 0) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("message.ezweight.pickup_blocked",
                            String.format("%.1f", currentWeight),
                            String.format("%.1f", maxWeight)
                    ),
                    true
            );
            return;
        }

        int availableCount = stack.getCount();
        if (maxPickupCount < availableCount) {
            // Recoger solo parte del stack
            ItemStack partialPickup = stack.copy();
            partialPickup.setCount(maxPickupCount);

            // Añadir al inventario
            boolean success = player.getInventory().add(partialPickup);
            if (success) {
                stack.shrink(maxPickupCount);
                itemEntity.setItem(stack);
                event.setCanceled(true);
                itemEntity.setPickUpDelay(10);
            }
        }
    }
}
