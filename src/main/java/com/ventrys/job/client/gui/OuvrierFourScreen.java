package com.ventrys.job.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ventrys.job.menu.OuvrierFourMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Écran pour le Four à charbon - Interface complètement transparente et personnalisée
 */
public class OuvrierFourScreen extends AbstractContainerScreen<OuvrierFourMenu> {

    public OuvrierFourScreen(OuvrierFourMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // Interface minimale - juste assez pour le slot central
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        // Interface complètement transparente - AUCUNE texture
        
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Désactiver complètement les textures
        RenderSystem.disableTexture();
        RenderSystem.disableBlend();
        
        // Dessiner un slot central simple avec des rectangles colorés uniquement
        RenderSystem.setShaderColor(0.2F, 0.2F, 0.2F, 0.8F); // Gris foncé semi-transparent
        
        // Bordure du slot
        fill(poseStack, x + 79, y + 34, x + 99, y + 36, 0xFF404040); // Bordure haute
        fill(poseStack, x + 79, y + 52, x + 99, y + 54, 0xFF404040); // Bordure basse
        fill(poseStack, x + 79, y + 34, x + 81, y + 54, 0xFF404040); // Bordure gauche
        fill(poseStack, x + 97, y + 34, x + 99, y + 54, 0xFF404040); // Bordure droite
        
        // Fond du slot
        RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, 0.6F); // Gris très foncé
        fill(poseStack, x + 81, y + 36, x + 97, y + 52, 0xFF202020);

        // Pas de barre de progression - interface minimaliste

        // Pas d'icône flamme - la fumée suffit
        
        // Réactiver les textures pour le reste
        RenderSystem.enableTexture();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // Interface complètement transparente - pas de renderBackground
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Titre du four
        this.font.draw(poseStack, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 4210752);
        // Label de l'inventaire
        this.font.draw(poseStack, this.playerInventoryTitle, (float)this.inventoryLabelX, (float)this.inventoryLabelY, 4210752);
    }
}
