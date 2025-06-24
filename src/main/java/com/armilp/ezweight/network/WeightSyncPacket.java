package com.armilp.ezweight.network;

import com.armilp.ezweight.data.WeightSyncData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WeightSyncPacket {
    private final double maxWeight;

    public WeightSyncPacket(double maxWeight) {
        this.maxWeight = maxWeight;
    }

    public static void encode(WeightSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.maxWeight);
    }

    public static WeightSyncPacket decode(FriendlyByteBuf buf) {
        return new WeightSyncPacket(buf.readDouble());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                WeightSyncData.setMaxWeight(this.maxWeight);
            }
        });
        context.setPacketHandled(true);
    }

}
