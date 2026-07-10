package com.ventrys.job.event;

import com.ventrys.job.data.BlockBreakRules;
import com.ventrys.job.data.CropGrowthConfig;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.JobPermissionService;
import com.ventrys.job.data.MalletUsage;
import com.ventrys.job.VentrysJob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public class BlockBreakEventHandler {
    
    // Événement principal pour empêcher la casse avec des outils non autorisés
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            // En mode créatif, permettre la casse de tous les blocs
            if (player.isCreative()) {
                return;
            }
            
            BlockPos pos = event.getPos();
            Level level = event.getWorld();
            BlockState state = level.getBlockState(pos);
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            
            // Machines / tables du mod uniquement — pas les minerais ni cultures custom
            if (JobPermissionService.isUnrestrictedVentrysJobBlock(state, pos)) {
                return;
            }
            
            // Vérifier si le joueur est bâtisseur
            String playerJob = JobPermissionService.getJob(player);
            boolean isConfiguredCrop = CropGrowthConfig.isConfiguredCrop(state.getBlock());

            if (isConfiguredCrop) {
                // Vérifier si le joueur tient une fourche
                boolean isFork = com.ventrys.job.data.ForkConfig.isFork(heldItem.getItem());
                
                // Seul un paysan avec une fourche peut casser les cultures
                if (!"paysan".equals(playerJob) || !isFork) {
                    event.setCanceled(true);
                    return;
                }
                
                // Paysan avec fourche : permettre la casse et gérer la récolte
                // La récolte sera gérée dans onBlockBreak
            }

            // Les blocs extractibles doivent toujours passer par le système d'extraction,
            // jamais par la casse vanilla (quel que soit l'outil/le métier).
            if (JobActions.isAnyExtractableBlock(state, pos) || JobActions.isStoneProtectedFromMining(state)) {
                event.setCanceled(true);
                return;
            }

            if (BlockBreakRules.isDecorativeBuildingBlock(state)) {
                if (!BlockBreakRules.canPlayerBreakBlock(player, state)) {
                    event.setCanceled(true);
                    return;
                }
                return;
            }

            if (JobPermissionService.isBatisseur(player)) {
                // Le bâtisseur peut casser tous les blocs SAUF ceux qui sont extractibles
                if (JobActions.isAnyExtractableBlock(state, pos)) {
                    event.setCanceled(true);
                    return;
                }
                // Le bâtisseur peut casser tous les autres blocs normalement
                return;
            }
            
            // Empêcher la casse directe des blocs extraits
            if (JobActions.isExtractedBlock(level, pos)) {
                event.setCanceled(true);
                return;
            }
            
            // Empêcher la casse des blocs configurables avec des outils non autorisés
            if (shouldPreventBreaking(state, heldItem, player)) {
                event.setCanceled(true);
            }
        }
    }
    
    // Événement pour réduire la vitesse de casse avec des outils non autorisés
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            // En mode créatif, permettre la casse normale de tous les blocs
            if (player.isCreative()) {
                return;
            }
            
            BlockState state = event.getState();
            BlockPos pos = event.getPos();
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (JobPermissionService.isUnrestrictedVentrysJobBlock(state, pos)) {
                return;
            }
            
            // Vérifier si le joueur est bâtisseur
            String playerJob = JobPermissionService.getJob(player);
            boolean isConfiguredCrop = CropGrowthConfig.isConfiguredCrop(state.getBlock());

            if (isConfiguredCrop) {
                boolean isFork = com.ventrys.job.data.ForkConfig.isFork(heldItem.getItem());
                // Autoriser la casse uniquement pour un paysan avec une fourche (clic gauche)
                if ("paysan".equals(playerJob) && isFork) {
                    return;
                }
                // Sinon bloquer totalement la vitesse de casse des cultures
                event.setNewSpeed(0.0f);
                return;
            }

            if (JobActions.isAnyExtractableBlock(state, pos) || JobActions.isStoneProtectedFromMining(state)) {
                event.setNewSpeed(0.0f);
                return;
            }

            if (BlockBreakRules.isDecorativeBuildingBlock(state)) {
                if (!BlockBreakRules.canPlayerBreakBlock(player, state)) {
                    event.setNewSpeed(0.0f);
                    return;
                }
                event.setNewSpeed(BlockBreakRules.capDecorativeBreakSpeed(event.getNewSpeed()));
                return;
            }

            if (JobPermissionService.isBatisseur(player)) {
                return;
            }
            
            // Réduire drastiquement la vitesse de casse pour les outils non autorisés
            if (shouldPreventBreaking(state, heldItem, player)) {
                event.setNewSpeed(0.0f); // Vitesse de casse à 0
            }
        }
    }
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
            return;
        }
        BlockState state = event.getState();
        if (JobActions.isStoneProtectedFromMining(state)) {
            event.setCanceled(true);
            return;
        }
        if (BlockBreakRules.isDecorativeBuildingBlock(state)) {
            if (!BlockBreakRules.canPlayerBreakBlock(player, state)) {
                event.setCanceled(true);
                return;
            }
            MalletUsage.applyWear(player, player.getOffhandItem());
        }
    }

    /**
     * Vérifie si la casse du bloc doit être empêchée
     */
    private static boolean shouldPreventBreaking(BlockState state, ItemStack tool, ServerPlayer player) {
        var blockRegistryName = state.getBlock().getRegistryName();
        if (blockRegistryName == null) return false;

        if (JobActions.isStoneProtectedFromMining(state)) {
            return !JobActions.isChiselTool(tool);
        }
        
        String playerJob = JobPermissionService.getJob(player);
        
        // Vérifier les restrictions par métier et outil
        if ("ouvrier".equals(playerJob)) {
            // Ouvrier : doit avoir le bon outil pour chaque action
            // Vérifier les blocs d'extraction de logs
            if (JobActions.isExtractableLog(state, BlockPos.ZERO)) {
                boolean isAxe = JobActions.isAxe(tool);
                return !isAxe;
            }
            
            // Vérifier les blocs de pierre
            if (JobActions.isExtractableStone(state)) {
                boolean isChisel = JobActions.isChiselTool(tool);
                return !isChisel;
            }
            
            // Vérifier les blocs de calcite
            if (JobActions.isExtractableCalcite(state)) {
                boolean isChisel = JobActions.isChiselTool(tool);
                return !isChisel;
            }
            
            // Vérifier les blocs de sable
            if (JobActions.isExtractableSand(state, BlockPos.ZERO)) {
                boolean isShovel = JobActions.isShovel(tool);
                return !isShovel;
            }
            
            // Vérifier les minerais
            if (JobActions.isExtractableOre(state, BlockPos.ZERO)) {
                boolean isPickaxe = JobActions.isPickaxe(tool);
                return !isPickaxe;
            }
            
            // Vérifier les blocs de clay
            if (JobActions.isExtractableClay(state, BlockPos.ZERO)) {
                boolean isShovel = JobActions.isShovel(tool);
                return !isShovel;
            }
        } else if ("paysan".equals(playerJob)) {
            // Paysan : peut récolter les cultures avec une fourche
            // La récolte est gérée dans CropGrowthConfig, pas ici
            // Pas de restriction supplémentaire pour les autres blocs
        } else if ("batisseur".equals(playerJob)) {
            // Bâtisseur : peut casser tous les blocs SAUF ceux qui sont extractibles
            // (déjà géré dans onLeftClickBlock)
        } else {
            // Autres métiers : pas de restrictions spéciales pour la casse
        }
        
        return false;
    }
}
