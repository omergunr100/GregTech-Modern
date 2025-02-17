package com.gregtechceu.gtceu.client.renderer.machine.gcym;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.client.renderer.GTFluidRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.gcym.LargeChemicalBathMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;

public class LargeChemicalBathRenderer extends WorkableCasingMachineRenderer {

    private final GTFluidRenderer fluidRenderer;
    private ResourceLocation cachedRecipe;

    public LargeChemicalBathRenderer(ResourceLocation baseCasing, ResourceLocation workableModel) {
        super(baseCasing, workableModel);

        fluidRenderer = new GTFluidRenderer().lightLevelOverride(LightTexture.FULL_BRIGHT);
    }

    @Override
    public int getViewDistance() {
        return 32;
    }

    @Override
    public boolean isGlobalRenderer(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public boolean hasTESR(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {
        super.render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);

        if (!ConfigHolder.INSTANCE.client.renderer.renderFluids) return;
        if (blockEntity instanceof MetaMachineBlockEntity mm) {
            if (mm.metaMachine instanceof LargeChemicalBathMachine lcb) {
                if (lcb.getFluidBlockOffsets().isEmpty()) {
                    fluidRenderer.clearHull();
                    return;
                }

                if (fluidRenderer.hull() == null) {
                    fluidRenderer.prepareVertexData(lcb.getFluidBlockOffsets());
                }

                var lastRecipe = lcb.recipeLogic.getLastRecipe();
                if (lastRecipe == null) {
                    cachedRecipe = null;
                    fluidRenderer.fluid(null);
                } else if (lcb.getOffsetTimer() % 20 == 0 || lastRecipe.id != cachedRecipe) {
                    cachedRecipe = lastRecipe.id;
                    if (lcb.isActive()) {
                        fluidRenderer.fluid(RenderUtil.getRecipeFluidToRender(lastRecipe));
                    } else {
                        fluidRenderer.fluid(null);
                    }
                }

                if (fluidRenderer.fluid() == null) {
                    return;
                }

                fluidRenderer.draw(stack, buffer, RenderUtil.FluidTextureType.STILL::map, combinedLight,
                        combinedOverlay, Byte.MAX_VALUE);
            }
        }
    }
}
