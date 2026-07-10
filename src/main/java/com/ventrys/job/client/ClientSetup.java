package com.ventrys.job.client;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.init.ModBlocks;
import com.ventrys.job.init.ModEntities;
import com.ventrys.job.init.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Enregistrements purement client : évite @OnlyIn et références client dans {@link com.ventrys.job.VentrysJob}
 * (meilleure analyse IDE / Eclipse JDT).
 */
@SuppressWarnings("null") // RegistryObject.get() vs @Nonnull des APIs Minecraft — faux positifs fréquents
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        VentrysJob.LOGGER.debug("VentrysJob — initialisation client (rendu, écrans)");

        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VASE_APOTHICAIRE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHICKEN_NEST.get(), RenderType.cutout());

            RenderType cropLayer = RenderType.cutout();
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CROP_ORGE.get(), cropLayer);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CROP_TOMATES.get(), cropLayer);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CROP_OIGNON.get(), cropLayer);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CROP_SALADE.get(), cropLayer);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CROP_RAISIN.get(), cropLayer);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CROP_CHOUX.get(), cropLayer);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CROP_CAROTTE.get(), cropLayer);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CROP_BETRAVE.get(), cropLayer);

            MenuScreens.register(ModMenuTypes.JOB_TABLE.get(), com.ventrys.job.client.gui.JobTableScreen::new);
            MenuScreens.register(ModMenuTypes.OUVRIER_FOUR.get(), com.ventrys.job.client.gui.OuvrierFourScreen::new);
            MenuScreens.register(ModMenuTypes.FORGERON_FOUR.get(), com.ventrys.job.client.gui.ForgeronFourScreen::new);
            MenuScreens.register(ModMenuTypes.SAC_SEL.get(), com.ventrys.job.client.gui.SacSelScreen::new);
            MenuScreens.register(ModMenuTypes.MEULE.get(), com.ventrys.job.client.gui.MeuleScreen::new);
            MenuScreens.register(ModMenuTypes.CHICKEN_NEST.get(), com.ventrys.job.client.gui.ChickenNestScreen::new);

            EntityRenderers.register(ModEntities.CUSTOM_PIG.get(), com.ventrys.job.client.renderer.CustomPigRenderer::new);
            EntityRenderers.register(ModEntities.CUSTOM_COW.get(), com.ventrys.job.client.renderer.CustomCowRenderer::new);
            EntityRenderers.register(ModEntities.CUSTOM_CHICKEN.get(), com.ventrys.job.client.renderer.CustomChickenRenderer::new);
            EntityRenderers.register(ModEntities.CUSTOM_SHEEP.get(), com.ventrys.job.client.renderer.CustomSheepRenderer::new);
        });
    }
}
