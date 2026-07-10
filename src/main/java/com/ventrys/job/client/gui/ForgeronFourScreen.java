package com.ventrys.job.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ventrys.job.menu.ForgeronFourMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Écran pour le Four de Forgeron - Interface avec texture vanilla
 */
public class ForgeronFourScreen extends AbstractContainerScreen<ForgeronFourMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");

    public ForgeronFourScreen(ForgeronFourMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(@NotNull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Fond principal
        RenderSystem.setShaderTexture(0, TEXTURE);
        this.blit(poseStack, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Flèche de progression
        if (menu.isCooking()) {
            this.blit(poseStack, x + 79, y + 34, 176, 14, menu.getScaledProgress(), 16);
        }

        // Flamme si allumé
        if (menu.isLit()) {
            int fuelHeight = menu.getScaledFuelTime();
            this.blit(poseStack, x + 56, y + 36 + 12 - fuelHeight, 176, 0 + 12 - fuelHeight, 14, fuelHeight + 1);
        }
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull PoseStack poseStack, int mouseX, int mouseY) {
        this.font.draw(poseStack, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 0xC8B890);
        this.font.draw(poseStack, this.playerInventoryTitle, (float)this.inventoryLabelX, (float)this.inventoryLabelY, 0xA59B86);
    }
}
