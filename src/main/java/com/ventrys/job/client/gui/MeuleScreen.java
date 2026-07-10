package com.ventrys.job.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ventrys.job.menu.MeuleMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class MeuleScreen extends AbstractContainerScreen<MeuleMenu> {

    // Couleurs personnalisées noir et bleu
    private static final int BORDER_OUTER = 0xFF000000; // Noir pur pour bordure extérieure
    private static final int BORDER_INNER = 0xFF1A1A2E; // Noir bleuté pour bordure intérieure
    private static final int BACKGROUND_DARK = 0xFF0F0F1E; // Fond très sombre
    private static final int BACKGROUND_MEDIUM = 0xFF16213E; // Fond moyen bleu foncé
    private static final int ACCENT_BLUE = 0xFF1E3A8A; // Bleu accent
    private static final int ACCENT_LIGHT_BLUE = 0xFF3B82F6; // Bleu clair
    private static final int PROGRESS_BG = 0xFF1E293B; // Fond barre de progression
    private static final int PROGRESS_FG = 0xFF3B82F6; // Barre de progression bleue
    private static final int SLOT_BG = 0xFF0A0A0A; // Fond des slots
    private static final int SLOT_BORDER = 0xFF1E3A8A; // Bordure des slots

    public MeuleScreen(MeuleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 166; // Taille standard pour inclure l'inventaire
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 10;
        this.titleLabelY = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }


    @Override
    protected void renderBg(@NotNull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Désactiver les textures pour dessiner avec des couleurs
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Bordure extérieure (noir) - seulement pour la partie supérieure (zone meule)
        fill(poseStack, x, y, x + imageWidth, y + 2, BORDER_OUTER); // Haut
        fill(poseStack, x, y, x + 2, y + 73, BORDER_OUTER); // Gauche (partie supérieure)
        fill(poseStack, x + imageWidth - 2, y, x + imageWidth, y + 73, BORDER_OUTER); // Droite (partie supérieure)

        // Bordure intérieure (bleu foncé) - seulement pour la partie supérieure
        fill(poseStack, x + 2, y + 2, x + imageWidth - 2, y + 4, BORDER_INNER);
        fill(poseStack, x + 2, y + 2, x + 4, y + 73, BORDER_INNER);
        fill(poseStack, x + imageWidth - 4, y + 2, x + imageWidth - 2, y + 73, BORDER_INNER);

        // Fond principal avec dégradé - seulement pour la partie supérieure (zone de la meule)
        fill(poseStack, x + 4, y + 4, x + imageWidth - 4, y + 73, BACKGROUND_DARK);
        
        // Zone centrale avec fond légèrement plus clair - seulement pour la partie supérieure
        fill(poseStack, x + 6, y + 6, x + imageWidth - 6, y + 73, BACKGROUND_MEDIUM);
        
        // L'inventaire du joueur sera rendu automatiquement par super.render()

        // Slot input (gauche) - fond
        int slotX1 = x + 30;
        int slotY = y + 40;
        int slotSize = 18;
        fill(poseStack, slotX1 - 1, slotY - 1, slotX1 + slotSize + 1, slotY + slotSize + 1, SLOT_BORDER);
        fill(poseStack, slotX1, slotY, slotX1 + slotSize, slotY + slotSize, SLOT_BG);

        // Slot output (droite) - fond
        int slotX2 = x + 152;
        fill(poseStack, slotX2 - 1, slotY - 1, slotX2 + slotSize + 1, slotY + slotSize + 1, SLOT_BORDER);
        fill(poseStack, slotX2, slotY, slotX2 + slotSize, slotY + slotSize, SLOT_BG);

        // Barre de progression - fond
        int progressX = x + 60;
        int progressY = y + 45;
        int progressWidth = 80;
        int progressHeight = 8;
        fill(poseStack, progressX - 1, progressY - 1, progressX + progressWidth + 1, progressY + progressHeight + 1, ACCENT_BLUE);
        fill(poseStack, progressX, progressY, progressX + progressWidth, progressY + progressHeight, PROGRESS_BG);

        // Barre de progression - remplissage avec effet de brillance
        int filledWidth = getProgressScaled(progressWidth);
        if (filledWidth > 0) {
            fill(poseStack, progressX, progressY, progressX + filledWidth, progressY + progressHeight, PROGRESS_FG);
            // Effet de brillance en haut de la barre
            fill(poseStack, progressX, progressY, progressX + filledWidth, progressY + 2, ACCENT_LIGHT_BLUE);
        }

        // Lignes décoratives bleues
        fill(poseStack, x + 10, y + 25, x + imageWidth - 10, y + 26, ACCENT_BLUE);

        // Réactiver les textures
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }


    @Override
    protected void renderLabels(@NotNull PoseStack poseStack, int mouseX, int mouseY) {
        this.font.draw(poseStack, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 0xFFFFFF);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        renderTooltip(poseStack, mouseX, mouseY);
    }

    private int getProgressScaled(int pixels) {
        int progress = menu.getProgress();
        int max = menu.getMaxProgress();
        if (progress <= 0 || max <= 0) {
            return 0;
        }
        return progress * pixels / max;
    }
}

