package com.ventrys.job.client.renderer;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomCow;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class CustomCowModel extends AnimatedGeoModel<CustomCow> {

    private static final ResourceLocation MODEL = new ResourceLocation(VentrysJob.MOD_ID, "geo/vache.geo.json");
    private static final ResourceLocation ANIMATION = new ResourceLocation(VentrysJob.MOD_ID, "animations/vache.animation.json");

    @Override
    public ResourceLocation getModelLocation(CustomCow object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(CustomCow object) {
        return CustomCow.toTextureLocation(object.getTextureVariant());
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CustomCow animatable) {
        return ANIMATION;
    }
}
