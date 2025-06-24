package com.armilp.ezweight.network;

import com.armilp.ezweight.EZWeight;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;


public class EZWeightNetwork {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EZWeight.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(
                id++,
                OpenWeightGuiPacket.class,
                OpenWeightGuiPacket::encode,
                OpenWeightGuiPacket::decode,
                OpenWeightGuiPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                id++,
                WeightUpdatePacket.class,
                WeightUpdatePacket::encode,
                WeightUpdatePacket::decode,
                WeightUpdatePacket::handle);

        CHANNEL.registerMessage(
                id++,
                WeightSyncPacket.class,
                WeightSyncPacket::encode,
                WeightSyncPacket::decode,
                WeightSyncPacket::handle
        );

        CHANNEL.registerMessage(id++,
                WeightLevelsSyncPacket.class,
                WeightLevelsSyncPacket::encode,
                WeightLevelsSyncPacket::decode,
                WeightLevelsSyncPacket::handle
        );

        CHANNEL.registerMessage(id++,
                SyncALLWeightPacket.class,
                SyncALLWeightPacket::encode,
                SyncALLWeightPacket::decode,
                SyncALLWeightPacket::handle
        );
    }



    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
