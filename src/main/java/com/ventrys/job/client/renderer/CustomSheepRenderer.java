package com.ventrys.job.client.renderer;

import com.ventrys.job.entity.CustomSheep;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class CustomSheepRenderer extends GeoEntityRenderer<CustomSheep> {

    private static final float SCALE = 0.29f;

    public CustomSheepRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CustomSheepModel());
        this.shadowRadius = 0.7f * SCALE;
    }

    @Override
    public void render(CustomSheep entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        ((CustomSheepModel) this.modelProvider).prepareFor(entity);
        stack.pushPose();
        stack.scale(SCALE, SCALE, SCALE);
        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
        stack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CustomSheep entity) {
        return CustomSheep.toTextureLocation(entity.getTextureVariant());
    }
}
