package com.ventrys.job.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ventrys.job.menu.ChickenNestMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ChickenNestScreen extends AbstractContainerScreen<ChickenNestMenu> {

    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");

    public ChickenNestScreen(ChickenNestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // Taille minimale pour contenir les slots
        this.imageWidth = 4 * 18; // 4 slots de 18 pixels
        this.imageHeight = 18; // Hauteur d'un slot
    }

    @Override
    protected void init() {
        super.init();
        // Centrer les slots
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    protected void renderBg(@NotNull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        // Pas de fond, juste les slots vanilla
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, INVENTORY_TEXTURE);

        // 4 slots vanilla centrés
        int slotSize = 18;
        int slotTextureX = 7; // Position X de la texture du slot dans inventory.png
        int slotTextureY = 83; // Position Y de la texture du slot dans inventory.png
        
        for (int i = 0; i < 4; i++) {
            int slotX = this.leftPos + i * slotSize;
            int slotY = this.topPos;
            // Dessiner la texture vanilla du slot
            this.blit(poseStack, slotX, slotY, slotTextureX, slotTextureY, slotSize, slotSize);
        }
    }
    
    @Override
    protected void renderLabels(@NotNull PoseStack poseStack, int mouseX, int mouseY) {
        // Pas de labels
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // Pas de fond
        super.render(poseStack, mouseX, mouseY, partialTick);
        renderTooltip(poseStack, mouseX, mouseY);
    }
}

