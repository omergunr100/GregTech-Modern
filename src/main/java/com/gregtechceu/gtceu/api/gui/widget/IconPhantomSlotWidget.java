package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.items.IItemHandlerModifiable;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@LDLRegister(name = "icon_phantom_item_slot", group = "widget.container", priority = 50)
public class IconPhantomSlotWidget extends PhantomSlotWidget {

    @Getter
    @Setter
    private boolean iconActive;
    @Getter
    @Setter
    @Configurable(name = "ldlib.gui.editor.name.icon.texture")
    private IGuiTexture iconTexture;

    public IconPhantomSlotWidget(IItemHandlerModifiable itemHandler, int slotIndex, int xPosition, int yPosition) {
        super(itemHandler, slotIndex, xPosition, yPosition);
        itemHook = stack -> stack.copyWithCount(1);
    }

    @Override
    public Widget setHoverTexture(IGuiTexture... hoverTexture) {
        return super.setHoverTexture(hoverTexture);
    }

    @Override
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        var icon = getIconTexture();
        if (isIconActive() && icon != null) {
            var pos = getPosition();
            icon.draw(graphics, mouseX, mouseY, pos.x + 11, pos.y + 11, 6, 6);
        }
    }
}
