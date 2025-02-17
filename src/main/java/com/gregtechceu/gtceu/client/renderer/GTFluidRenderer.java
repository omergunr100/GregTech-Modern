package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.api.quickhull3d.QuickHull3D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector3d;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public class GTFluidRenderer {

    protected Fluid fluid;
    @Getter
    protected QuickHull3D hull;
    @Setter
    protected int lightLevelOverride = -1;

    public GTFluidRenderer(Fluid fluid, BlockPos... positions) {
        this.fluid = fluid;
        var points = Arrays.stream(positions).mapMulti((pos, consumer) -> {
            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = 0; z <= 1; z++) {
                        consumer.accept(new Vector3d(pos.getX() + x, pos.getY() + y, pos.getZ() + z));
                    }
                }
            }
        }).map(v -> (Vector3d) v).collect(Collectors.toSet());
        this.hull = new QuickHull3D(points.toArray(Vector3d[]::new));
    }

    public GTFluidRenderer(Fluid fluid, Vector3d... positions) {
        this.fluid = fluid;
        this.hull = new QuickHull3D(positions);
    }

    @OnlyIn(Dist.CLIENT)
    public void draw(PoseStack stack, MultiBufferSource bufferSource,
                     Function<IClientFluidTypeExtensions, TextureAtlasSprite> spriteGetter, int lightOverlay, int combinedOverlay, byte opacity) {
        var fluidInfo = IClientFluidTypeExtensions.of(fluid);
        var sprite = spriteGetter.apply(fluidInfo);
        var u0 = sprite.getU0();
        var u1 = sprite.getU1();
        var v0 = sprite.getV0();
        var v1 = sprite.getV1();
        
        var layer = ItemBlockRenderTypes.getRenderLayer(fluid.defaultFluidState());
        var consumer = bufferSource.getBuffer(RenderTypeHelper.getEntityRenderType(layer, false));
        var color = fluidInfo.getTintColor();
        color = (((color >>> 24) * opacity) << 24) | (color & 0x00FFFFFF);
        var r = FastColor.ARGB32.red(color);
        var g = FastColor.ARGB32.green(color);
        var b = FastColor.ARGB32.blue(color);
        var a = FastColor.ARGB32.alpha(color);
        var lightLevel = lightLevelOverride != -1 ? lightLevelOverride : lightOverlay;
        
        var idx = hull.getFacesIdx();
        var faces = hull.getFaces();
        
        var mc = Minecraft.getInstance();
        var camPos = new Vector3d(mc.cameraEntity.getX(), mc.cameraEntity.getY(), mc.cameraEntity.getZ());
        var subResult = new Vector3d();
        stack.pushPose();
        for (int i = 0; i < hull.getNumFaces(); i++) {
            var face = faces.get(i);
            var n = face.getNormal();
            var sign = Math.signum(n.dot(camPos.sub(face.getCentroid(), subResult)));
            drawQuad(consumer, r, g, b, a, u0, u1, v0, v1, lightLevel, combinedOverlay, idx[i], 
                    (float) (sign * n.x), (float) (sign * n.y), (float) (sign * n.z));
        }
        stack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    public void drawQuad(VertexConsumer consumer, 
                         int r, int g, int b, int a, float u0, float u1, float v0, float v1, 
                         int lightOverlay, int combinedOverlay, int[] vtxIdx, float nX, float nY, float nZ) {
        var vtx = hull.getVertices();
        var v = vtx[vtxIdx[0]];
        consumer.vertex(v.x, v.y, v.z)
                .color(r, g, b, a)
                .uv(u0, v1)
                .overlayCoords(combinedOverlay)
                .uv2(lightOverlay)
                .endVertex();
        v = vtx[vtxIdx[1]];
        consumer.vertex(v.x, v.y, v.z)
                .color(r, g, b, a)
                .uv(u0, v0)
                .overlayCoords(combinedOverlay)
                .uv2(lightOverlay)
                .endVertex();
        v = vtx[vtxIdx[2]];
        consumer.vertex(v.x, v.y, v.z)
                .color(r, g, b, a)
                .uv(u1, v0)
                .overlayCoords(combinedOverlay)
                .uv2(lightOverlay)
                .endVertex();
        v = vtx[vtxIdx[3]];
        consumer.vertex(v.x, v.y, v.z)
                .color(r, g, b, a)
                .uv(u1, v1)
                .overlayCoords(combinedOverlay)
                .uv2(lightOverlay)
                .normal(nX, nY, nZ)
                .endVertex();
    }
}
