package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.gui.misc.ProspectorMode;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.tool.ProspectorHelper;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ProspectorBehaviour implements IInteractionItem, IAddInformation {

    @Getter
    private final int radius;
    private final long cost;
    private final ProspectorMode<?>[] modes;
    @Getter
    private final int scanTickTime;

    public ProspectorBehaviour(int radius, long cost, int scanTickTime, ProspectorMode<?>... modes) {
        this.radius = radius + 1;
        this.modes = Arrays.stream(modes).filter(Objects::nonNull).toArray(ProspectorMode[]::new);
        this.cost = cost;
        this.scanTickTime = scanTickTime;
    }

    @NotNull
    public ProspectorMode<?> getMode(ItemStack stack) {
        if (stack == ItemStack.EMPTY) {
            return modes[0];
        }
        var tag = stack.getTag();
        if (tag == null) {
            return modes[0];
        }
        return modes[tag.getInt("Mode") % modes.length];
    }

    public void setNextMode(ItemStack stack) {
        var tag = stack.getOrCreateTag();
        tag.putInt("Mode", (tag.getInt("Mode") + 1) % modes.length);
    }

    public boolean drainEnergy(@NotNull ItemStack stack, boolean simulate) {
        var electricItem = GTCapabilityHelper.getElectricItem(stack);
        if (electricItem == null) return false;

        var amount = Math.round(cost * (ConfigHolder.INSTANCE.machines.prospectorEnergyUseMultiplier / 100F));

        return electricItem.discharge(amount, Integer.MAX_VALUE, true, false, simulate) >= amount;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        var heldItem = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown() && modes.length > 1) {
            if (!level.isClientSide) {
                setNextMode(heldItem);
                var mode = getMode(heldItem);
                player.sendSystemMessage(Component.translatable(mode.unlocalizedName));
            }
            return InteractionResultHolder.success(heldItem);
        }
        if (!player.isCreative() && !drainEnergy(heldItem, true)) {
            player.sendSystemMessage(Component.translatable("behavior.prospector.not_enough_energy"));
            return InteractionResultHolder.success(heldItem);
        }
        if (!level.isClientSide) {
            new ProspectorHelper((ServerPlayer) player, this, heldItem);
        }
        // todo: create a new ui and init here
        return IInteractionItem.super.use(item, level, player, usedHand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("metaitem.prospector.tooltip.radius", radius));
        tooltipComponents.add(Component.translatable("metaitem.prospector.tooltip.modes"));
        for (var mode : modes) {
            tooltipComponents.add(Component.literal(" -").append(Component.translatable(mode.unlocalizedName))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        }
    }
}
