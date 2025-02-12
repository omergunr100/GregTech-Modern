package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;

import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;

public class SteamItemBusPartMachine extends ItemBusPartMachine {

    private final String autoTooltipKey;

    public SteamItemBusPartMachine(IMachineBlockEntity holder, IO io, Object... args) {
        super(holder, 1, io, args);
        autoTooltipKey = io == IO.IN ? "gtceu.gui.item_auto_input.tooltip" : "gtceu.gui.item_auto_output.tooltip";
    }

    @NotNull
    @Override
    public ModularUI createUI(@NotNull Player entityPlayer) {
        int rowSize = (int) Math.sqrt(getInventorySize());
        int xOffset = rowSize == 10 ? 9 : 0;
        var modular = new ModularUI(176 + xOffset * 2,
                18 + 18 * rowSize + 105, this, entityPlayer)
                .background(GuiTextures.BACKGROUND_STEAM.get(ConfigHolder.INSTANCE.machines.steelSteamMultiblocks))
                .widget(new LabelWidget(10, 5, getBlockState().getBlock().getDescriptionId()))
                .widget(new ToggleButtonWidget(7 + xOffset, 18 + 18 * rowSize, 18, 18,
                        GuiTextures.BUTTON_ITEM_OUTPUT, this::isWorkingEnabled, this::setWorkingEnabled)
                        .setShouldUseBaseBackground() // TODO: Steamify background
                        .setTooltipText(autoTooltipKey))
                .widget(UITemplate.bindPlayerInventory(entityPlayer.getInventory(),
                        GuiTextures.SLOT_STEAM.get(ConfigHolder.INSTANCE.machines.steelSteamMultiblocks),
                        7 + xOffset, 18 + 18 * rowSize + 24, true));

        for (int y = 0; y < rowSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                int index = y * rowSize + x;
                modular.widget(new SlotWidget(getInventory().storage, index,
                        (88 - rowSize * 9 + x * 18) + xOffset, 18 + y * 18 + 6, true, io.support(IO.IN))
                        .setBackgroundTexture(
                                GuiTextures.SLOT_STEAM.get(ConfigHolder.INSTANCE.machines.steelSteamMultiblocks)));
            }
        }

        return modular;
    }
}
