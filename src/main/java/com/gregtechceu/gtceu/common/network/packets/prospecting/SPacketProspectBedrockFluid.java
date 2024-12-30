package com.gregtechceu.gtceu.common.network.packets.prospecting;

import com.gregtechceu.gtceu.api.gui.misc.ProspectorMode;
import com.gregtechceu.gtceu.integration.map.cache.client.GTClientCache;

import com.lowdragmc.lowdraglib.networking.IHandlerContext;

import net.minecraft.network.FriendlyByteBuf;

public class SPacketProspectBedrockFluid extends SPacketProspect<ProspectorMode.FluidInfo> {

    @Override
    public void encodeData(FriendlyByteBuf buf, ProspectorMode.FluidInfo data) {
        data.toBuffer(buf);
    }

    @Override
    public ProspectorMode.FluidInfo decodeData(FriendlyByteBuf buf) {
        return ProspectorMode.FluidInfo.fromBuffer(buf);
    }

    @Override
    public void execute(IHandlerContext handler) {
        data.rowMap().forEach((level, fluids) -> fluids
                .forEach((blockPos, fluid) -> GTClientCache.instance.addFluid(level,
                        blockPos.getX() >> 4, blockPos.getZ() >> 4, fluid)));
    }
}
