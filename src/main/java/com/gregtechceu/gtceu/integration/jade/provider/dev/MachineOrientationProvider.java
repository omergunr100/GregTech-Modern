package com.gregtechceu.gtceu.integration.jade.provider.dev;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class MachineOrientationProvider implements IBlockComponentProvider {

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (!ConfigHolder.INSTANCE.dev.enabledDebugDataProviders) {
            return;
        }
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            var machine = blockEntity.getMetaMachine();
            iTooltip.add(Component.literal("Allow Extended Facing: " + machine.allowExtendedFacing()));
            if (machine.allowExtendedFacing()) {
                iTooltip.add(Component.literal("Upward Facing: " + machine.getUpwardsFacing()));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return GTCEu.id("dev/machine_orientation");
    }
}
