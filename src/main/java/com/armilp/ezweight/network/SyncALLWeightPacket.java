package com.armilp.ezweight.network;

import com.armilp.ezweight.data.ItemWeightRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncALLWeightPacket {
    private final ResourceLocation itemId;
    private final double weight;

    public SyncALLWeightPacket(ResourceLocation itemId, double weight) {
        this.itemId = itemId;
        this.weight = weight;
    }

    public static void encode(SyncALLWeightPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.itemId);
        buf.writeDouble(packet.weight);
    }

    public static SyncALLWeightPacket decode(FriendlyByteBuf buf) {
        return new SyncALLWeightPacket(buf.readResourceLocation(), buf.readDouble());
    }

    public static void handle(SyncALLWeightPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ItemWeightRegistry.setWeight(packet.itemId, packet.weight);
        });
        context.get().setPacketHandled(true);
    }
}
