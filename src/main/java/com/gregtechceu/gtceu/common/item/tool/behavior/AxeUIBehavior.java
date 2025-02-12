package com.gregtechceu.gtceu.common.item.tool.behavior;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.item.tool.behavior.IToolUIBehavior;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;

import static com.gregtechceu.gtceu.api.item.tool.ToolHelper.getBehaviorsTag;

public class AxeUIBehavior implements IToolUIBehavior {

    public static final AxeUIBehavior INSTANCE = new AxeUIBehavior();

    @Override
    public boolean openUI(@NotNull Player player, @NotNull InteractionHand hand) {
        return player.isShiftKeyDown();
    }

    @Override
    public ModularUI createUI(Player player, HeldItemUIFactory.HeldItemHolder holder) {
        var inner = new WidgetGroup(Position.of(10, 5));
        inner.addWidget(new ToggleButtonWidget(0, 0, 18, 18, GuiTextures.BUTTON_POWER,
                () -> getBehaviorsTag(holder.getHeld()).getBoolean(ToolHelper.DISABLE_TREE_FELLING_KEY), val -> {
                    if (val) {
                        getBehaviorsTag(holder.getHeld()).putBoolean(ToolHelper.DISABLE_TREE_FELLING_KEY, val);
                    } else {
                        getBehaviorsTag(holder.getHeld()).remove(ToolHelper.DISABLE_TREE_FELLING_KEY);
                    }
                    holder.markAsDirty();
                }));
        inner.addWidget(new LabelWidget(26, 4, "Disable tree felling"));
        var group = new WidgetGroup(Position.ORIGIN, inner.getSize().add(20, 10));
        group.addWidget(inner);
        return new ModularUI(group, holder, player).background(GuiTextures.BACKGROUND);
    }
}
