package com.ventrys.job.client.renderer.item;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomPig;
import com.ventrys.job.item.PorcCarcasseItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class PorcCarcasseModel extends AnimatedGeoModel<PorcCarcasseItem> {
   private static final ResourceLocation MODEL = new ResourceLocation(VentrysJob.MOD_ID, "geo/porc_carcasse.geo.json");
   private static final ResourceLocation ANIMATIONS = new ResourceLocation(VentrysJob.MOD_ID, "animations/porc.animation.json");

   @Override
   public ResourceLocation getModelLocation(PorcCarcasseItem object) {
      return MODEL;
   }

   @Override
   public ResourceLocation getTextureLocation(PorcCarcasseItem object) {
      return CustomPig.toTextureLocation(CustomPig.DEFAULT_TEXTURE);
   }

   @Override
   public ResourceLocation getAnimationFileLocation(PorcCarcasseItem animatable) {
      return ANIMATIONS;
   }
}
