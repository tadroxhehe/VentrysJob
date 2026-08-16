package com.ventrys.job.client.renderer.item;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomChicken;
import com.ventrys.job.item.PouleCarcasseItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class PouleCarcasseModel extends AnimatedGeoModel<PouleCarcasseItem> {
   private static final ResourceLocation MODEL = new ResourceLocation(VentrysJob.MOD_ID, "geo/poule_carcasse.geo.json");
   private static final ResourceLocation ANIMATIONS = new ResourceLocation(VentrysJob.MOD_ID, "animations/poule.animation.json");

   @Override
   public ResourceLocation getModelLocation(PouleCarcasseItem object) {
      return MODEL;
   }

   @Override
   public ResourceLocation getTextureLocation(PouleCarcasseItem object) {
      return CustomChicken.toTextureLocation(CustomChicken.DEFAULT_TEXTURE);
   }

   @Override
   public ResourceLocation getAnimationFileLocation(PouleCarcasseItem animatable) {
      return ANIMATIONS;
   }
}
