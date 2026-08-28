package com.ventrys.job.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ventrys.job.entity.CustomAnimal;
import com.ventrys.job.entity.CustomCow;
import com.ventrys.job.entity.CustomSheep;
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
        PoseStack poseStack = event.getMatrixStack();
        Font font = mc.font;
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2;
        int y = screenHeight / 2 - 70;

        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(HUD_SCALE, HUD_SCALE, 1f);
        poseStack.translate(-x, -y, 0);

        int line = 0;
        int panelBottom = y + 92;
        if (animal instanceof CustomCow || animal instanceof CustomSheep) {
            panelBottom += 12;
        }
        net.minecraft.client.gui.GuiComponent.fill(poseStack, x - 100, y - 5, x + 100, panelBottom, 0x80000000);

        Component sexText = animal.isMale() ? MALE_TEXT : FEMALE_TEXT;
        font.draw(poseStack, sexText, x - 95, y + line, 0xFFFFFF);
        line += 12;

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
        font.draw(poseStack, nutritionText, x - 95, y + line, nutritionColor);
        line += 12;

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
        font.draw(poseStack, hydrationText, x - 95, y + line, hydrationColor);
        line += 12;

        int barWidth = 90;
        int barHeight = 4;
        int barX = x - 95;
        int barY = y + line;
        net.minecraft.client.gui.GuiComponent.fill(poseStack, barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        int nutritionWidth = (int) (barWidth * (animal.getNutrition() / 100.0));
        net.minecraft.client.gui.GuiComponent.fill(poseStack, barX, barY, barX + nutritionWidth, barY + barHeight, nutritionColor);
        barY += 8;
        net.minecraft.client.gui.GuiComponent.fill(poseStack, barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        int hydrationWidth = (int) (barWidth * (animal.getHydration() / 100.0));
        net.minecraft.client.gui.GuiComponent.fill(poseStack, barX, barY, barX + hydrationWidth, barY + barHeight, hydrationColor);
        line = barY - y + 10;

        // Gestation / reproduction (femelle)
        Component reproLine = reproductionLine(animal);
        if (reproLine != null) {
            font.draw(poseStack, reproLine, x - 95, y + line, reproColor(animal.getReproductionHudState()));
            line += 12;
        }

        if (animal instanceof CustomCow cow) {
            font.draw(poseStack, milkLine(cow), x - 95, y + line, 0xFFDDDDFF);
            line += 12;
        }
        if (animal instanceof CustomSheep sheep) {
            font.draw(poseStack, woolLine(sheep), x - 95, y + line, 0xFFEEDDCC);
        }

        poseStack.popPose();
    }

    private static Component reproductionLine(CustomAnimal animal) {
        int state = animal.getReproductionHudState();
        int pct = animal.getReproductionProgressPercent();
        return switch (state) {
            case CustomAnimal.REPRO_HUD_MALE ->
                new TranslatableComponent("ventrysjob.animal.repro.male");
            case CustomAnimal.REPRO_HUD_NEEDS_CARE ->
                new TranslatableComponent("ventrysjob.animal.repro.needs_care");
            case CustomAnimal.REPRO_HUD_NO_PARTNER ->
                new TranslatableComponent("ventrysjob.animal.repro.no_partner");
            case CustomAnimal.REPRO_HUD_GESTATING ->
                new TranslatableComponent("ventrysjob.animal.repro.mating", pct);
            case CustomAnimal.REPRO_HUD_READY ->
                new TranslatableComponent("ventrysjob.animal.repro.ready");
            case CustomAnimal.REPRO_HUD_PREGNANT ->
                new TranslatableComponent("ventrysjob.animal.repro.pregnant", pct);
            default -> null;
        };
    }

    private static int reproColor(int state) {
        return switch (state) {
            case CustomAnimal.REPRO_HUD_NEEDS_CARE, CustomAnimal.REPRO_HUD_NO_PARTNER -> 0xFFFFAA00;
            case CustomAnimal.REPRO_HUD_GESTATING -> 0xFF88AAFF;
            case CustomAnimal.REPRO_HUD_READY -> 0xFF55FF55;
            case CustomAnimal.REPRO_HUD_PREGNANT -> 0xFFFF88CC;
            default -> 0xFFAAAAAA;
        };
    }

    private static Component milkLine(CustomCow cow) {
        int min = cow.getMilkReadyInMinutes();
        if (min == -1) {
            return new TranslatableComponent("ventrysjob.animal.milk.male");
        }
        if (min == -2) {
            return new TranslatableComponent("ventrysjob.animal.milk.needs_care");
        }
        if (min <= 0) {
            return new TranslatableComponent("ventrysjob.animal.milk.ready");
        }
        return new TranslatableComponent("ventrysjob.animal.milk.wait", formatDuration(min));
    }

    private static Component woolLine(CustomSheep sheep) {
        int min = sheep.getWoolReadyInMinutes();
        if (sheep.hasWool() || min == 0) {
            return new TranslatableComponent("ventrysjob.animal.wool.ready");
        }
        if (min == -2) {
            return new TranslatableComponent("ventrysjob.animal.wool.needs_care");
        }
        return new TranslatableComponent("ventrysjob.animal.wool.wait", formatDuration(min));
    }

    private static String formatDuration(int totalMinutes) {
        if (totalMinutes < 60) {
            return totalMinutes + " min";
        }
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (minutes == 0) {
            return hours + " h";
        }
        return hours + " h " + minutes + " min";
    }
}
