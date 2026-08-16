package com.ventrys.job.client.renderer.item;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomSheep;
import com.ventrys.job.item.MoutonCarcasseItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class MoutonCarcasseModel extends AnimatedGeoModel<MoutonCarcasseItem> {
   private static final ResourceLocation MODEL = new ResourceLocation(VentrysJob.MOD_ID, "geo/mouton_carcasse.geo.json");
   private static final ResourceLocation ANIMATIONS = new ResourceLocation(VentrysJob.MOD_ID, "animations/mouton.animation.json");

   @Override
   public ResourceLocation getModelLocation(MoutonCarcasseItem object) {
      return MODEL;
   }

   @Override
   public ResourceLocation getTextureLocation(MoutonCarcasseItem object) {
      return CustomSheep.toTextureLocation(CustomSheep.DEFAULT_TEXTURE);
   }

   @Override
   public ResourceLocation getAnimationFileLocation(MoutonCarcasseItem animatable) {
      return ANIMATIONS;
   }
}
