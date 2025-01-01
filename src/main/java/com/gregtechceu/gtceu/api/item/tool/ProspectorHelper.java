package com.gregtechceu.gtceu.api.item.tool;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData;
import com.gregtechceu.gtceu.api.gui.misc.ProspectorMode;
import com.gregtechceu.gtceu.common.item.ProspectorBehaviour;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectBedrockFluid;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = GTCEu.MOD_ID)
public class ProspectorHelper {

    private ServerPlayer player;
    private ItemStack prospector;
    private int tick;
    private ChunkPos lastPos;
    private int offsetX, offsetZ;
    private ProspectorBehaviour behaviour;

    private static final List<ProspectorHelper> helpers = new ObjectArrayList<>();

    public ProspectorHelper(ServerPlayer player, ProspectorBehaviour behaviour, ItemStack prospector) {
        this.player = player;
        this.behaviour = behaviour;
        this.prospector = prospector;
        this.lastPos = player.chunkPosition();
        tick = 0;
        helpers.add(this);
    }

    private boolean remove() {
        var radius = behaviour.getRadius();

        if (prospector.isEmpty()) {
            return true;
        }

        if (radius < offsetX) {
            offsetX = -radius;
            offsetZ++;
        }
        if (radius < offsetZ) {
            return true;
        }

        return false;
    }

    private void prospect() {
        var mode = behaviour.getMode(prospector);
        if (mode.equals(ProspectorMode.ORE)) {

        } else if (mode.equals(ProspectorMode.FLUID)) {
            var chunk = player.chunkPosition();
            var fluidVein = BedrockFluidVeinSavedData.getOrCreate(player.serverLevel())
                    .getFluidVeinWorldEntry(chunk.x + offsetX, chunk.z + offsetZ);
            var info = ProspectorMode.FluidInfo.fromVeinWorldEntry(fluidVein);
            var packet = new SPacketProspectBedrockFluid(player.level().dimension(), player.blockPosition(), info);
            GTNetwork.NETWORK.sendToPlayer(packet, player);
        } else if (mode.equals(ProspectorMode.BEDROCK_ORE)) {

        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END || event.side == LogicalSide.CLIENT || helpers.isEmpty()) {
            return;
        }

        var iterator = helpers.iterator();
        while (iterator.hasNext()) {
            var helper = iterator.next();
            if (helper.remove()) {
                iterator.remove();
                continue;
            }
            if (event.level != helper.player.level() ||
                    helper.tick++ % helper.behaviour.getScanTickTime() != 0) {
                continue;
            }

            if (!helper.lastPos.equals(helper.player.chunkPosition())) {
                helper.lastPos = helper.player.chunkPosition();
                helper.offsetX = helper.offsetZ = -helper.behaviour.getRadius();
            }

            helper.prospect();

            helper.offsetX++;
        }
    }
}
