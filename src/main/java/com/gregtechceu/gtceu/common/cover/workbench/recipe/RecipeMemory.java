package com.gregtechceu.gtceu.common.cover.workbench.recipe;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IconPhantomSlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.PhantomSlotWidget;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.google.common.math.IntMath;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.RoundingMode;

@Data
@NoArgsConstructor
public class RecipeMemory implements ITagSerializable<CompoundTag>, IContentChangeAware {

    private Runnable onContentsChanged;
    @Persisted
    @DescSynced
    private CustomItemStackHandler handler;
    @Persisted
    @DescSynced
    private MemorizedRecipe[] recipeMemory;
    @Setter
    @Getter
    @DescSynced
    private CustomItemStackHandler activeRecipe;
    @Persisted
    @DescSynced
    private int rows;
    @Persisted
    @DescSynced
    private int cols;

    public RecipeMemory(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        recipeMemory = new MemorizedRecipe[rows * cols];
        activeRecipe = new CustomItemStackHandler(rows * cols);
        for (int i = 0; i < recipeMemory.length; i++) {
            recipeMemory[i] = new MemorizedRecipe();
        }
        handler = new CustomItemStackHandler(rows * cols) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    public Widget createUIWidget(int xPosition, int yPosition, PhantomSlotWidget[] craftingGrid) {
        var group = new WidgetGroup(Position.of(xPosition, yPosition));

        // background dark texture
        group.addWidget(new ImageWidget(0, 0, cols * 18, rows * 18, GuiTextures.SLOT_DARK));
        // create components
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                group.addWidget(new MemorizedRecipeSlotWidget(handler, col + row * cols,
                        col * 18, row * 18,
                        recipeMemory, col + row * cols, craftingGrid));
            }
        }

        return group;
    }

    public void memorize() {
        var unlockedSlot = -1;
        var emptySlot = -1;
        var ingredients = new ItemStack[rows * cols];
        for (int i = 0; i < recipeMemory.length; i++) {
            var mem = recipeMemory[i];
            if (mem.isEmpty()) {
                if (emptySlot == -1) {
                    emptySlot = i;
                }
            } else if (mem.hasRecipe()) {
                return;
            }
            if (!mem.isLocked() && unlockedSlot == -1) {
                unlockedSlot = i;
            }
        }
        // todo: push all recipes up and set new recipe at the end of the list
        if (emptySlot != -1) {
            modifySlot(emptySlot, getActiveRecipe());
        } else if (unlockedSlot != -1) {
            modifySlot(unlockedSlot, getActiveRecipe());
        }
    }

    public void modifySlot(int slot, CraftingRecipe recipe) {
        var slotRecipe = recipeMemory[slot];
        slotRecipe.clear();
        slotRecipe.recipe = recipe;
        handler.setStackInSlot(slot, slotRecipe.getResult().copy());
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putInt("Rows", getRows());
        tag.putInt("Cols", getCols());
        tag.put("Handler", getHandler().serializeNBT());
        var memory = new ListTag();
        for (var recipe : getRecipeMemory()) {
            if (recipe == null) {
                memory.add(new CompoundTag());
            } else {
                memory.add(recipe.serializeNBT());
            }
        }
        tag.put("RecipeMemory", memory);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        setRows(tag.getInt("Rows"));
        setCols(tag.getInt("Cols"));
        var handler = new CustomItemStackHandler();
        handler.deserializeNBT(tag.getCompound("Handler"));
        setHandler(handler);
        var recipeList = tag.getList("RecipeMemory", Tag.TAG_COMPOUND);
        recipeMemory = new MemorizedRecipe[recipeList.size()];
        for (int i = 0; i < recipeMemory.length; i++) {
            recipeMemory[i] = new MemorizedRecipe((CompoundTag) recipeList.get(i));
        }
    }

    @Data
    public static class MemorizedRecipe implements ITagSerializable<CompoundTag> {
        @Persisted
        private ItemStack[] ingredients;
        @Persisted
        @DescSynced
        private ItemStack result;
        @Persisted
        @DescSynced
        private boolean locked;
        @Persisted
        @DescSynced
        private int timesUsed;

        public MemorizedRecipe() {
            result = ItemStack.EMPTY;
        }

        public MemorizedRecipe(CompoundTag tag) {
            deserializeNBT(tag);
        }

        public boolean isEmpty() {
            return result.equals(ItemStack.EMPTY);
        }

        public boolean isLocked() {
            return locked && !result.equals(ItemStack.EMPTY);
        }

        public void clear() {
            result = ItemStack.EMPTY;
            ingredients = null;
            locked = false;
            timesUsed = 0;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            if (result != null) {
                tag.put("Result", result.serializeNBT());
                tag.putInt("Count", ingredients == null ? 0 : ingredients.length);
                if (ingredients != null) {
                    for (int i = 0; i < ingredients.length; i++) {
                        tag.put("Ingredient" + i, ingredients[i].serializeNBT());
                    }
                }
                tag.putBoolean("Locked", isLocked());
                tag.putInt("TimesUsed", getTimesUsed());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            if (tag.isEmpty()) {
                return;
            }
            result = ItemStack.of(tag.getCompound("Result"));
            var count = tag.getInt("Count");
            if (count > 0) {
                ingredients = new ItemStack[count];
                for (int i = 0; i < count; i++) {
                    ingredients[i] = ItemStack.of(tag.getCompound("Ingredient" + i));
                }
            }
            setLocked(isEmpty() && tag.getBoolean("Locked"));
            setTimesUsed(isEmpty() ? 0 : tag.getInt("TimesUsed"));
        }
        
        public boolean hasRecipe(ItemStack[] ingredients, ItemStack result) {
            if (this.ingredients == null || this.result == null || !this.result.equals(result)) {
                return false;
            }
            if (!isLocked()) {
                this.ingredients = ingredients;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return getResult().hashCode();
        }
    }

    @LDLRegister(name = "memorized_recipe_slot", group = "widget.container", priority = 50)
    public static class MemorizedRecipeSlotWidget extends IconPhantomSlotWidget {

        public static final ResourceTexture LOCK_ICON = new ResourceTexture(
                "gtceu:textures/gui/widget/button_lock.png");

        @Getter
        @Setter
        private MemorizedRecipe[] recipes;
        @Getter
        @Setter
        private int index;
        @Getter
        @Setter
        private PhantomSlotWidget[] craftingGrid;

        public MemorizedRecipeSlotWidget(IItemHandlerModifiable itemHandler, int slotIndex, int xPosition,
                                         int yPosition, MemorizedRecipe[] recipes, int index,
                                         PhantomSlotWidget[] craftingGrid) {
            super(itemHandler, slotIndex, xPosition, yPosition);
            this.recipes = recipes;
            this.index = index;
            this.craftingGrid = craftingGrid;
            setBackgroundTexture(GuiTextures.BLANK_TRANSPARENT);
            setDrawHoverOverlay(false);
        }

        @Override
        public IGuiTexture getIconTexture() {
            return LOCK_ICON;
        }

        @Override
        public boolean isIconActive() {
            return getRecipes()[getIndex()].isLocked();
        }

        @Override
        public ItemStack slotClickPhantom(Slot slot, int mouseButton, ClickType clickTypeIn, ItemStack stackHeld) {
            var recipe = getRecipes()[getIndex()];
            if (recipe.isEmpty()) {
                return ItemStack.EMPTY.copy();
            }
            if (mouseButton == 0) {
                var ingredients = recipe.getIngredients();
                var sqrt = IntMath.sqrt(craftingGrid.length, RoundingMode.UNNECESSARY);
                for (int i = 0; i < sqrt; i++) {
                    for (int j = 0; j < sqrt; j++) {
                        ItemStack stack;
                        var slotInd = j + i * 3;
                        if (ingredients != null && i < ingredients.length) {
                            // todo: change the crafting grid visual slots to ingredient slots instead of item stacks
                            stack = ingredients[i];
                        } else {
                            stack = ItemStack.EMPTY;
                        }
                        craftingGrid[slotInd].getHandler().set(stack.copy());
                    }
                }
            } else if (mouseButton == 1) {
                recipe.setLocked(!recipe.isLocked());
            }
            return ItemStack.EMPTY.copy();
        }
    }
}
