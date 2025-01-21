package com.gregtechceu.gtceu.common.cover.workbench;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.PhantomSlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.cover.workbench.recipe.RecipeMemory;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public class WorkbenchCover extends CoverBehavior implements IUICover {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(WorkbenchCover.class,
            CoverBehavior.MANAGED_FIELD_HOLDER);

    public static final int MAIN_SIZE = 18;
    public static final int TOOL_SIZE = 9;
    public static final int CRAFTING_SIZE = 9;
    public static final int OUTPUT_SIZE = 1;

    @Persisted
    @DescSynced
    protected final CustomItemStackHandler mainInventory;
    @Persisted
    @DescSynced
    protected final CustomItemStackHandler toolInventory;
    @Persisted
    @DescSynced
    protected final CustomItemStackHandler craftingInventory;
    @DescSynced
    protected final CustomItemStackHandler outputInventory;

    @Persisted
    @DescSynced
    protected RecipeMemory memory;
    protected CraftingContainer containerWrapper;

    public WorkbenchCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);

        mainInventory = new CustomItemStackHandler(MAIN_SIZE);
        toolInventory = new CustomItemStackHandler(TOOL_SIZE) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        toolInventory.setFilter(stack -> stack.getItem() instanceof IGTTool tool &&
                tool.getToolStats().isSuitableForCrafting(stack));
        craftingInventory = new CustomItemStackHandler(CRAFTING_SIZE) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        craftingInventory.setOnContentsChanged(this::onCraftingSlotChange);
        outputInventory = new CustomItemStackHandler(OUTPUT_SIZE) {

            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                stacks.set(slot, stack);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!simulate) {
                    memory.memorize();
                }
                return super.extractItem(slot, amount, simulate);
            }
        };
        outputInventory.setOnContentsChanged(() -> {
            if (memory.getActiveRecipe() == null) {
                outputInventory.setStackInSlot(0, ItemStack.EMPTY.copy());
            } else {
                outputInventory.setStackInSlot(0, memory.getActiveRecipe().getResultItem(null).copy());
            }
        });
        memory = new RecipeMemory(3, 3);
        containerWrapper = new CraftingContainer() {

            @Override
            public void fillStackedContents(@NotNull StackedContents stackedContents) {}

            @Override
            public void clearContent() {}

            @Override
            public int getContainerSize() {
                return craftingInventory.getSlots();
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public @NotNull ItemStack getItem(int i) {
                return craftingInventory.getStackInSlot(i);
            }

            @Override
            public @NotNull ItemStack removeItem(int i, int i1) {
                return ItemStack.EMPTY;
            }

            @Override
            public @NotNull ItemStack removeItemNoUpdate(int i) {
                return ItemStack.EMPTY;
            }

            @Override
            public void setItem(int i, @NotNull ItemStack itemStack) {}

            @Override
            public void setChanged() {}

            @Override
            public boolean stillValid(@NotNull Player player) {
                return true;
            }

            @Override
            public int getWidth() {
                return IntMath.sqrt(getContainerSize(), RoundingMode.UNNECESSARY);
            }

            @Override
            public int getHeight() {
                return getWidth();
            }

            @Override
            public @NotNull List<ItemStack> getItems() {
                var items = new ObjectArrayList<ItemStack>();
                for (int i = 0; i < craftingInventory.getSlots(); i++) {
                    items.add(craftingInventory.getStackInSlot(i));
                }
                return items;
            }
        };
    }

    public @Nullable RecipeManager getRecipeManager() {
        // noinspection DataFlowIssue
        return coverHolder.getLevel().getServer().getRecipeManager();
    }

    @Override
    public boolean canAttach() {
        for (var side : Direction.values()) {
            if (side != attachedSide && coverHolder.hasCover(side) &&
                    coverHolder.getCoverAtSide(side) instanceof WorkbenchCover) {
                return false;
            }
        }
        return super.canAttach();
    }

    @Override
    public Widget createUIWidget() {
        return createUIWidget(false);
    }

    public Widget createUIWidget(boolean configurator) {
        var group = new WidgetGroup(Position.ORIGIN);
        if (!configurator) {
            // label
            group.addWidget(new LabelWidget(0, 5, LocalizationUtils.format(getTitle())));
        }
        // todo: clear grid button
        // todo: items crafted counter
        // add crafting slots (3x3)
        var sqrt = IntMath.sqrt(CRAFTING_SIZE, RoundingMode.UNNECESSARY);
        var craftingSlots = new PhantomSlotWidget[CRAFTING_SIZE];
        for (int i = 0; i < sqrt; i++) {
            for (int j = 0; j < sqrt; j++) {
                var slotIndex = j + i * 3;
                craftingSlots[slotIndex] = new PhantomSlotWidget(craftingInventory, slotIndex, j * 18, 20 + i * 18);
                craftingSlots[slotIndex].setClearSlotOnRightClick(true).setBackgroundTexture(GuiTextures.SLOT);
                group.addWidget(craftingSlots[slotIndex]);
            }
        }
        // output slot
        group.addWidget(new SlotWidget(outputInventory, 0, 71, 38) {

            @Override
            public void handleClientAction(int id, FriendlyByteBuf buffer) {
                getGui().getModularUIGui().getMenu().setCarried(outputInventory.getStackInSlot(0));
                super.handleClientAction(id, buffer);
            }
        }
                .setCanPutItems(false));
        // memorized recipes slots
        group.addWidget(memory.createUIWidget(106, 20, craftingSlots));
        // tool inventory slots (1x9)
        for (int i = 0; i < 9; i++) {
            group.addWidget(new SlotWidget(toolInventory, i, i * 18, 80)
                    .setBackgroundTexture(new GuiTextureGroup(GuiTextures.SLOT, GuiTextures.TOOL_SLOT_OVERLAY)));
        }
        // internal inventory slots (2x9)
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 9; j++) {
                group.addWidget(new SlotWidget(mainInventory, j + i * 9, j * 18, 104 + i * 18)
                        .setBackgroundTexture(GuiTextures.SLOT));
            }
        }
        // buffer between machine and player inventory
        group.addWidget(new ImageWidget(0, 140 + (configurator ? 6 : 9), 1, 1, GuiTextures.BLANK_TRANSPARENT));
        onCraftingSlotChange();
        return group;
    }

    private void onCraftingSlotChange() {
        if (!coverHolder.isRemote()) {
            var optRecipe = Objects.requireNonNull(getRecipeManager())
                    .getRecipeFor(RecipeType.CRAFTING, containerWrapper, coverHolder.getLevel());
            if (optRecipe.isPresent()) {
                var recipe = optRecipe.get();
                var result = recipe.getResultItem(null);
                outputInventory.setStackInSlot(0, result.copy());
                memory.setActiveRecipe(recipe);
            } else {
                outputInventory.setStackInSlot(0, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public @NotNull List<ItemStack> getAdditionalDrops() {
        var stacks = new ObjectArrayList<ItemStack>();
        for (int i = 0; i < mainInventory.getSlots(); i++) {
            var stack = mainInventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            stacks.add(stack.copy());
        }
        for (int i = 0; i < toolInventory.getSlots(); i++) {
            var stack = toolInventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            stacks.add(stack.copy());
        }
        return stacks;
    }

    protected String getTitle() {
        return "item.gtceu.workbench_cover";
    }

    @Override
    public @Nullable IFancyConfigurator getConfigurator() {
        return new WorkbenchCoverConfigurator();
    }

    private class WorkbenchCoverConfigurator implements IFancyConfigurator {

        public static final ResourceTexture icon = new ResourceTexture(
                "gtceu:textures/block/cover/workbench_cover.png");

        @Override
        public Component getTitle() {
            return Component.translatable(WorkbenchCover.this.getTitle());
        }

        @Override
        public IGuiTexture getIcon() {
            return icon;
        }

        @Override
        public Widget createConfigurator() {
            return createUIWidget(true);
        }
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }
}
