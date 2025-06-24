package com.armilp.ezweight.network;

import com.armilp.ezweight.data.ItemWeightRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.function.Supplier;

public class WeightUpdatePacket {
    private final ResourceLocation itemId;
    private final double weight;

    public WeightUpdatePacket(ResourceLocation itemId, double weight) {
        this.itemId = itemId;
        this.weight = weight;
    }

    public static void encode(WeightUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.itemId);
        buffer.writeDouble(packet.weight);
    }

    public static WeightUpdatePacket decode(FriendlyByteBuf buffer) {
        return new WeightUpdatePacket(buffer.readResourceLocation(), buffer.readDouble());
    }

    public static void handle(WeightUpdatePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ItemWeightRegistry.setWeight(packet.itemId, packet.weight);
            ItemWeightRegistry.saveToFile(ItemWeightRegistry.getConfigFile());
                ItemWeightRegistry.reloadFromFile();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    EZWeightNetwork.CHANNEL.sendTo(new SyncALLWeightPacket(packet.itemId, packet.weight), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            }
        });
        context.get().setPacketHandled(true);
    }

}
