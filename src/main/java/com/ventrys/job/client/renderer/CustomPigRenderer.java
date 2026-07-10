package com.ventrys.job.client.renderer;

import com.ventrys.job.entity.CustomPig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class CustomPigRenderer extends GeoEntityRenderer<CustomPig> {

    private static final float SCALE = 0.30f;

    public CustomPigRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CustomPigModel());
        this.shadowRadius = 0.7f * SCALE;
    }

    @Override
    public void render(CustomPig entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        stack.pushPose();
        stack.scale(SCALE, SCALE, SCALE);
        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
        stack.popPose();
    }

    @Override
    protected void renderNameTag(CustomPig entity, net.minecraft.network.chat.Component name, PoseStack stack, MultiBufferSource buffer, int packedLight) {
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
    public ResourceLocation getTextureLocation(CustomPig entity) {
        return CustomPig.toTextureLocation(entity.getTextureVariant());
    }
}
