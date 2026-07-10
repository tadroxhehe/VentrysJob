package com.ventrys.job.client.renderer;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomPig;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class CustomPigModel extends AnimatedGeoModel<CustomPig> {

    private static final ResourceLocation MODEL = new ResourceLocation(VentrysJob.MOD_ID, "geo/porc.geo.json");
    private static final ResourceLocation ANIMATION = new ResourceLocation(VentrysJob.MOD_ID, "animations/porc.animation.json");

    @Override
    public ResourceLocation getModelLocation(CustomPig object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(CustomPig object) {
        return CustomPig.toTextureLocation(object.getTextureVariant());
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CustomPig animatable) {
        return ANIMATION;
    }
}
