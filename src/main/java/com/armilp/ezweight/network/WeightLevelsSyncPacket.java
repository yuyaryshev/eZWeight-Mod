package com.armilp.ezweight.network;

import com.armilp.ezweight.levels.WeightLevel;
import com.armilp.ezweight.levels.WeightLevelManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class WeightLevelsSyncPacket {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final String json;

    public WeightLevelsSyncPacket(String json) {
        this.json = json;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(json);
    }

    public static WeightLevelsSyncPacket decode(FriendlyByteBuf buf) {
        return new WeightLevelsSyncPacket(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            WeightLevelManager.loadFromJsonString(json);
        });
        contextSupplier.get().setPacketHandled(true);
    }

    public static WeightLevelsSyncPacket fromLevels(List<WeightLevel> levels) {
        return new WeightLevelsSyncPacket(WeightLevelManager.toJsonString(levels));
    }
}
