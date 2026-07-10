package com.ventrys.job.client.renderer;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomSheep;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomSheepModel extends AnimatedGeoModel<CustomSheep> {

    // Le modèle dépend de la variante (laine / sans laine) : on met en cache par nom
    // pour éviter une allocation de ResourceLocation par frame. L'animation est constante.
    private static final Map<String, ResourceLocation> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final ResourceLocation ANIMATION = new ResourceLocation(VentrysJob.MOD_ID, "animations/mouton.animation.json");

    private ResourceLocation activeModelLocation;

    @Override
    public ResourceLocation getModelLocation(CustomSheep object) {
        return MODEL_CACHE.computeIfAbsent(object.getModelResourceName(),
            name -> new ResourceLocation(VentrysJob.MOD_ID, "geo/" + name + ".geo.json"));
    }

    @Override
    public ResourceLocation getTextureLocation(CustomSheep object) {
        return CustomSheep.toTextureLocation(object.getTextureVariant());
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CustomSheep animatable) {
        return ANIMATION;
    }

    /** Force le swap de modèle GeckoLib quand la laine est tondue / repousse. */
    public void prepareFor(CustomSheep sheep) {
        ResourceLocation location = getModelLocation(sheep);
        if (activeModelLocation == null || !activeModelLocation.equals(location)) {
            getModel(location);
            activeModelLocation = location;
        }
    }
}
