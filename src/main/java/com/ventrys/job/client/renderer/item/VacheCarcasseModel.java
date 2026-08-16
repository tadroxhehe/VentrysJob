package com.ventrys.job.client.renderer.item;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomCow;
import com.ventrys.job.item.VacheCarcasseItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class VacheCarcasseModel extends AnimatedGeoModel<VacheCarcasseItem> {
   private static final ResourceLocation MODEL = new ResourceLocation(VentrysJob.MOD_ID, "geo/vache_carcasse.geo.json");
   private static final ResourceLocation ANIMATIONS = new ResourceLocation(VentrysJob.MOD_ID, "animations/vache.animation.json");

   @Override
   public ResourceLocation getModelLocation(VacheCarcasseItem object) {
      return MODEL;
   }

   @Override
   public ResourceLocation getTextureLocation(VacheCarcasseItem object) {
      return CustomCow.toTextureLocation(CustomCow.DEFAULT_TEXTURE);
   }

   @Override
   public ResourceLocation getAnimationFileLocation(VacheCarcasseItem animatable) {
      return ANIMATIONS;
   }
}
