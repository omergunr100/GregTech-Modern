package com.gregtechceu.gtceu.api.item.tool;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = GTCEu.MOD_ID)
public class ProspectorHelper {

    private ServerPlayer player;
    private ItemStack prospector;
    private int radius;
    private final int scanTickTime;
    private int tick;
    private ChunkPos lastPos;
    private int offsetX, offsetZ;

    private static final Set<ProspectorHelper> helpers = new HashSet<>();

    public ProspectorHelper(ServerPlayer player, ItemStack prospector, int scanTickTime) {
        this.player = player;
        this.prospector = prospector;
        this.scanTickTime = scanTickTime;
        this.lastPos = player.chunkPosition();
        tick = 0;
        helpers.add(this);
    }

    private void prospect() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END || event.side == LogicalSide.CLIENT) {
            return;
        }

        for (var helper : helpers) {
            if (helper.tick++ % helper.scanTickTime != 0) {
                continue;
            }

            if (!helper.lastPos.equals(helper.player.chunkPosition())) {
                helper.offsetX = helper.offsetZ = -helper.radius;
            }

            helper.prospect();
        }
    }
}
