package com.ventrys.job.client.renderer;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomChicken;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class CustomChickenModel extends AnimatedGeoModel<CustomChicken> {

    private static final ResourceLocation MODEL = new ResourceLocation(VentrysJob.MOD_ID, "geo/poule.geo.json");
    private static final ResourceLocation ANIMATION = new ResourceLocation(VentrysJob.MOD_ID, "animations/poule.animation.json");

    @Override
    public ResourceLocation getModelLocation(CustomChicken object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(CustomChicken object) {
        return CustomChicken.toTextureLocation(object.getTextureVariant());
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CustomChicken animatable) {
        return ANIMATION;
    }
}
