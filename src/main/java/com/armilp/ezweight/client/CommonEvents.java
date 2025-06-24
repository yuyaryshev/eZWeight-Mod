package com.armilp.ezweight.client;

import com.armilp.ezweight.EZWeight;
import com.armilp.ezweight.config.WeightConfig;
import com.armilp.ezweight.levels.WeightLevelManager;
import com.armilp.ezweight.network.EZWeightNetwork;
import com.armilp.ezweight.network.WeightLevelsSyncPacket;
import com.armilp.ezweight.network.WeightSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

@Mod.EventBusSubscriber(modid = EZWeight.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            double serverMaxWeight = WeightConfig.COMMON.MAX_WEIGHT.get();
            EZWeightNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new WeightSyncPacket(serverMaxWeight)
            );

            EZWeightNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    WeightLevelsSyncPacket.fromLevels(WeightLevelManager.getLevels()));
        }
    }
}
