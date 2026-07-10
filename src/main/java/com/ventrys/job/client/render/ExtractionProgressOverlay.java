package com.ventrys.job.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ventrys.job.VentrysJob;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Affiche une barre de progression visuelle pour les extractions en cours
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = VentrysJob.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExtractionProgressOverlay {
    private static final int CLEANUP_INTERVAL_TICKS = 20;
    private static long nextCleanupTick = 0;
    
    // Stockage temporaire de la progression côté client (synchronisé depuis les messages)
    private static final Map<String, ExtractionProgress> activeProgressions = new ConcurrentHashMap<>();
    
    /**
     * Classe pour stocker les données de progression
     */
    private static class ExtractionProgress {
        final int current;
        final int max;
        final String message;
        final long timestamp;
        
        ExtractionProgress(int current, int max, String message) {
            this.current = current;
            this.max = max;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Met à jour la progression affichée (appelé depuis les messages du serveur)
     */
    public static void updateProgress(String key, int current, int max, String message) {
        activeProgressions.put(key, new ExtractionProgress(current, max, message));
    }
    
    /**
     * Supprime une progression (appelé depuis le packet)
     */
    public static void removeProgress(String key) {
        if (key != null && !key.isEmpty()) {
            activeProgressions.remove(key);
        }
    }
    
    /**
     * Nettoie les progressions expirées (plus de 5 secondes)
     */
    private static void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        activeProgressions.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().timestamp > 5000);
    }
    
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long gameTime = mc.level.getGameTime();
        if (gameTime >= nextCleanupTick) {
            cleanupExpired();
            nextCleanupTick = gameTime + CLEANUP_INTERVAL_TICKS;
        }
        
        if (activeProgressions.isEmpty()) return;
        
        PoseStack poseStack = event.getMatrixStack();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        
        // Position de la barre (en bas au centre)
        int barX = screenWidth / 2 - 100;
        int barY = screenHeight - 60;
        
        int index = 0;
        for (Map.Entry<String, ExtractionProgress> entry : activeProgressions.entrySet()) {
            ExtractionProgress progress = entry.getValue();
            
            // Ignorer les progressions avec message vide (suppression)
            if (progress.message == null || progress.message.isEmpty()) {
                continue;
            }
            
            int yOffset = barY - (index * 30);
            
            // Fond de la barre
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GuiComponent.fill(poseStack, barX - 2, yOffset - 2, barX + 202, yOffset + 12, 0x80000000);
            
            // Barre de progression
            if (progress.max <= 0) {
                continue;
            }
            float progressPercent = Math.max(0.0f, Math.min(1.0f, (float) progress.current / (float) progress.max));
            int barWidth = (int) (200 * progressPercent);
            
            // Couleur de la barre (vert -> jaune -> rouge)
            int barColor;
            if (progressPercent < 0.5f) {
                // Vert à jaune
                int green = 255;
                int red = (int) (255 * (progressPercent * 2));
                barColor = (0xFF << 24) | (red << 16) | (green << 8) | 0x00;
            } else {
                // Jaune à rouge
                int red = 255;
                int green = (int) (255 * (1.0f - (progressPercent - 0.5f) * 2));
                barColor = (0xFF << 24) | (red << 16) | (green << 8) | 0x00;
            }
            
            GuiComponent.fill(poseStack, barX, yOffset, barX + barWidth, yOffset + 10, barColor);
            
            // Bordure
            GuiComponent.fill(poseStack, barX - 1, yOffset - 1, barX + 201, yOffset, 0xFF808080);
            GuiComponent.fill(poseStack, barX - 1, yOffset + 10, barX + 201, yOffset + 11, 0xFF808080);
            GuiComponent.fill(poseStack, barX - 1, yOffset, barX, yOffset + 10, 0xFF808080);
            GuiComponent.fill(poseStack, barX + 200, yOffset, barX + 201, yOffset + 10, 0xFF808080);
            
            // Texte de progression
            String progressText = progress.message + ": " + progress.current + "/" + progress.max;
            int textWidth = mc.font.width(progressText);
            mc.font.draw(poseStack, progressText, barX + 100 - textWidth / 2, yOffset + 1, 0xFFFFFF);
            
            index++;
            
            // Limiter à 3 barres maximum pour éviter l'encombrement
            if (index >= 3) break;
        }
    }
}

