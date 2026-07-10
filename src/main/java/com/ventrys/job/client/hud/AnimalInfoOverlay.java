package com.ventrys.job.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ventrys.job.entity.CustomAnimal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = com.ventrys.job.VentrysJob.MOD_ID)
public class AnimalInfoOverlay {
    private static final int LOOKUP_INTERVAL_TICKS = 5;
    /** Distance max (blocs) pour afficher le HUD en visant un animal. */
    private static final double LOOK_RANGE = 3.0;
    /** Échelle globale du panneau (texte + barres). */
    private static final float HUD_SCALE = 0.7f;
    private static long nextLookupTick = 0;
    private static CustomAnimal cachedTarget = null;

    private static final Component MALE_TEXT = new TranslatableComponent("ventrysjob.animal.sex.male");
    private static final Component FEMALE_TEXT = new TranslatableComponent("ventrysjob.animal.sex.female");
    private static final Component STARVING_TEXT = new TranslatableComponent("ventrysjob.animal.nutrition.starving");
    private static final Component FED_TEXT = new TranslatableComponent("ventrysjob.animal.nutrition.fed");
    private static final Component WELL_FED_TEXT = new TranslatableComponent("ventrysjob.animal.nutrition.well_fed");
    private static final Component THIRSTY_TEXT = new TranslatableComponent("ventrysjob.animal.hydration.thirsty");
    private static final Component HYDRATED_TEXT = new TranslatableComponent("ventrysjob.animal.hydration.hydrated");
    private static final Component WELL_HYDRATED_TEXT = new TranslatableComponent("ventrysjob.animal.hydration.well_hydrated");
    
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null || mc.level == null || mc.screen != null) {
            return;
        }

        long gameTime = mc.level.getGameTime();
        if (gameTime >= nextLookupTick) {
            nextLookupTick = gameTime + LOOKUP_INTERVAL_TICKS;
            // Raycast throttlé: assez réactif pour le HUD, bien moins coûteux par frame
            net.minecraft.world.phys.EntityHitResult hitResult =
                net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    player.level, player, player.getEyePosition(),
                    player.getEyePosition().add(player.getLookAngle().scale(LOOK_RANGE)),
                    player.getBoundingBox().expandTowards(player.getLookAngle().scale(LOOK_RANGE)),
                    entity -> entity instanceof CustomAnimal && entity != player
                );
            cachedTarget = (hitResult != null && hitResult.getEntity() instanceof CustomAnimal)
                ? (CustomAnimal) hitResult.getEntity()
                : null;
        }

        if (cachedTarget == null || !cachedTarget.isAlive() || cachedTarget.level != player.level) {
            return;
        }

        CustomAnimal animal = cachedTarget;
        
        {
            PoseStack poseStack = event.getMatrixStack();
            Font font = mc.font;
            int screenWidth = event.getWindow().getGuiScaledWidth();
            int screenHeight = event.getWindow().getGuiScaledHeight();
            
            int x = screenWidth / 2;
            int y = screenHeight / 2 - 50;

            poseStack.pushPose();
            poseStack.translate(x, y, 0);
            poseStack.scale(HUD_SCALE, HUD_SCALE, 1f);
            poseStack.translate(-x, -y, 0);

            // Fond semi-transparent
            net.minecraft.client.gui.GuiComponent.fill(poseStack, x - 100, y - 5, x + 100, y + 60, 0x80000000);
            
            // Sexe
            Component sexText = animal.isMale() ? MALE_TEXT : FEMALE_TEXT;
            font.draw(poseStack, sexText, x - 95, y, 0xFFFFFF);
            
            // Nutrition
            var nutritionStatus = animal.getNutritionStatus();
            Component nutritionText = switch (nutritionStatus) {
                case STARVING -> STARVING_TEXT;
                case FED -> FED_TEXT;
                case WELL_FED -> WELL_FED_TEXT;
            };
            int nutritionColor = switch (nutritionStatus) {
                case STARVING -> 0xFFAA0000;
                case FED -> 0xFFFFFF00;
                case WELL_FED -> 0xFF00AA00;
            };
            font.draw(poseStack, nutritionText, x - 95, y + 12, nutritionColor);
            
            // Hydratation
            var hydrationStatus = animal.getHydrationStatus();
            Component hydrationText = switch (hydrationStatus) {
                case THIRSTY -> THIRSTY_TEXT;
                case HYDRATED -> HYDRATED_TEXT;
                case WELL_HYDRATED -> WELL_HYDRATED_TEXT;
            };
            int hydrationColor = switch (hydrationStatus) {
                case THIRSTY -> 0xFFAA0000;
                case HYDRATED -> 0xFFFFFF00;
                case WELL_HYDRATED -> 0xFF00AA00;
            };
            font.draw(poseStack, hydrationText, x - 95, y + 24, hydrationColor);
            
            // Barres de progression
            int barWidth = 90;
            int barHeight = 4;
            int barX = x - 95;
            int barY = y + 36;
            
            // Barre de nutrition
            net.minecraft.client.gui.GuiComponent.fill(poseStack, barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
            int nutritionWidth = (int) (barWidth * (animal.getNutrition() / 100.0));
            net.minecraft.client.gui.GuiComponent.fill(poseStack, barX, barY, barX + nutritionWidth, barY + barHeight, nutritionColor);
            
            // Barre d'hydratation
            barY += 8;
            net.minecraft.client.gui.GuiComponent.fill(poseStack, barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
            int hydrationWidth = (int) (barWidth * (animal.getHydration() / 100.0));
            net.minecraft.client.gui.GuiComponent.fill(poseStack, barX, barY, barX + hydrationWidth, barY + barHeight, hydrationColor);
            
            poseStack.popPose();
        }
    }
}

