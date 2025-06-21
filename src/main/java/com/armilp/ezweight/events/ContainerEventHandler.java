package com.armilp.ezweight.events;

import com.armilp.ezweight.EZWeight;
import com.armilp.ezweight.commands.WeightCommands;
import com.armilp.ezweight.config.WeightConfig;
import com.armilp.ezweight.data.ItemWeightRegistry;
import com.armilp.ezweight.player.PlayerWeightHandler;

import com.tiviacz.travelersbackpack.TravelersBackpack;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.items.TravelersBackpackItem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

@Mod.EventBusSubscriber(modid = EZWeight.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ContainerEventHandler {

    private static final boolean TRAVELERS_LOADED = ModList.get().isLoaded(TravelersBackpack.MODID);

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;

        ServerPlayer player = (ServerPlayer) event.player;
        if (!WeightCommands.isWeightEnabledFor(player)) return;

        double totalWeight = PlayerWeightHandler.getTotalWeight(player);
        double maxWeight = WeightConfig.COMMON.MAX_WEIGHT.get();

        if (totalWeight > maxWeight) {
            player.displayClientMessage(
                    Component.translatable("message.ezweight.overburdened",
                            String.format("%.1f", totalWeight),
                            String.format("%.1f", maxWeight)
                    ),
                    true
            );

            dropExcessItems(player, totalWeight, maxWeight);
        }
    }

    private static void dropExcessItems(ServerPlayer player, double currentWeight, double maxWeight) {
        currentWeight = dropItemsFromInventory(player, currentWeight, maxWeight);

        if (currentWeight > maxWeight) {
        }
    }

    private static double dropItemsFromInventory(ServerPlayer player, double currentWeight, double maxWeight) {
        for (int i = player.getInventory().items.size() - 1; i >= 0 && currentWeight > maxWeight; i--) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.isEmpty()) continue;

            if (TRAVELERS_LOADED && stack.getItem() instanceof TravelersBackpackItem) {
                BackpackWrapper wrapper = new BackpackWrapper(stack, 1, player, player.level(), -1);
                IItemHandler backpackInventory = wrapper.getStorage();

                currentWeight = dropFromHandler(player, backpackInventory, currentWeight, maxWeight);

                if (currentWeight > maxWeight && !stack.isEmpty()) {
                    currentWeight = dropSingleItemFromStack(player, stack, currentWeight);
                }

                continue;
            }

            if (stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
                if (handler != null) {
                    currentWeight = dropFromHandler(player, handler, currentWeight, maxWeight);
                }
            }

            currentWeight = dropSingleItemFromStack(player, stack, currentWeight);
        }

        return currentWeight;
    }

    private static double dropSingleItemFromStack(ServerPlayer player, ItemStack stack, double currentWeight) {
        double itemWeight = ItemWeightRegistry.getWeight(stack.getItem());

        while (currentWeight > WeightConfig.COMMON.MAX_WEIGHT.get() && stack.getCount() > 0) {
            ItemStack drop = stack.copy();
            drop.setCount(1);
            stack.shrink(1);
            player.drop(drop, true);
            currentWeight -= itemWeight;
        }

        return currentWeight;
    }

    private static double dropFromHandler(ServerPlayer player, IItemHandler handler, double currentWeight, double maxWeight) {
        for (int slot = handler.getSlots() - 1; slot >= 0 && currentWeight > maxWeight; slot--) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            // Contenedor anidado
            if (stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                IItemHandler nested = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
                if (nested != null) {
                    currentWeight = dropFromHandler(player, nested, currentWeight, maxWeight);
                    stack = handler.getStackInSlot(slot);
                    if (stack.isEmpty()) continue;
                }
            }

            // Extraer ítems de este slot
            while (currentWeight > maxWeight && !stack.isEmpty()) {
                ItemStack extracted = handler.extractItem(slot, 1, false);
                if (!extracted.isEmpty()) {
                    player.drop(extracted, true);
                    currentWeight -= ItemWeightRegistry.getWeight(extracted.getItem());
                } else {
                    break;
                }
            }
        }

        return currentWeight;
    }
}
