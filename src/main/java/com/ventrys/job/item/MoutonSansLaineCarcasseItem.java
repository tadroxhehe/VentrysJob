package com.ventrys.job.item;

import com.ventrys.job.client.renderer.item.MoutonSansLaineCarcasseRenderer;
import com.ventrys.job.init.ModBlocks;
import com.ventrys.job.init.VentrysMobCreativeTab;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.IItemRenderProperties;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import java.util.function.Consumer;

/** Carcasse de mouton tondu : item GeckoLib statique (pose "morte"), destine a etre pose en tete d'armor stand pour le systeme de chasse. */
public class MoutonSansLaineCarcasseItem extends Item implements IAnimatable {
   private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

   public MoutonSansLaineCarcasseItem() {
      super(new Item.Properties().tab(VentrysMobCreativeTab.getOrFallback(ModBlocks.VENTRYS_JOBS_TAB)).stacksTo(1));
   }

   @Override
   public void initializeClient(Consumer<IItemRenderProperties> consumer) {
      consumer.accept(new IItemRenderProperties() {
         private final BlockEntityWithoutLevelRenderer renderer = new MoutonSansLaineCarcasseRenderer();

         @Override
         public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
            return renderer;
         }
      });
   }

   private <P extends Item & IAnimatable> PlayState predicate(AnimationEvent<P> event) {
      return PlayState.STOP;
   }

   @Override
   public void registerControllers(AnimationData data) {
      data.addAnimationController(new AnimationController(this, "controller", 0, this::predicate));
   }

   @Override
   public AnimationFactory getFactory() {
      return factory;
   }
}
