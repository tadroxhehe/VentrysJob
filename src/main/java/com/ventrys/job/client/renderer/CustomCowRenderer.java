package com.ventrys.job.client.renderer;

import com.ventrys.job.entity.CustomCow;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class CustomCowRenderer extends GeoEntityRenderer<CustomCow> {

    private static final float SCALE = 0.30f;

    public CustomCowRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CustomCowModel());
        this.shadowRadius = 0.7f * SCALE;
    }

    @Override
    public void render(CustomCow entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        stack.pushPose();
        stack.scale(SCALE, SCALE, SCALE);
        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
        stack.popPose();
    }

    @Override
    protected void renderNameTag(CustomCow entity, net.minecraft.network.chat.Component name, PoseStack stack, MultiBufferSource buffer, int packedLight) {
        stack.pushPose();
        stack.scale(0.5f, 0.5f, 0.5f);
        try {
            super.renderNameTag(entity, name, stack, buffer, packedLight);
        } catch (Exception e) {
            // Rendu du nom ignoré cette frame en cas d'erreur de shader
        } finally {
            stack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CustomCow entity) {
        return CustomCow.toTextureLocation(entity.getTextureVariant());
    }
}
