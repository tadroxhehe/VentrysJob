package com.ventrys.job.event;

import com.ventrys.job.data.BlockBreakRules;
import com.ventrys.job.data.CropGrowthConfig;
import com.ventrys.job.data.FurnitureAccess;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.ToolDurability;
import com.ventrys.job.VentrysJob;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Casse de blocs : plus aucune restriction de métier (gérée côté plugin désormais).
 * Les blocs cassent en vanilla (tag de bloc + tier d'outil). Seuls les meubles, le texte HRP,
 * les blocs protégés staff et les cultures (récolte multi-clics fourche) gardent un traitement
 * spécial. La scie (bûche → planche) est désormais entièrement gérée côté plugin (Skript).
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public class BlockBreakEventHandler {

    /**
     * Filet de sécurité : vitesse 0 côté client ET serveur pour les cas encore spéciaux.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeedExtractable(PlayerEvent.BreakSpeed event) {
        if (event.getPlayer().isCreative()) {
            return;
        }
        Player p = event.getPlayer();
        BlockState st = event.getState();
        if (FurnitureAccess.isFurniture(st) || BlockBreakRules.isNarrationTextBlock(st)) {
            return;
        }
        // Cultures : récolte multi-clics (pas de casse vanilla)
        if (CropGrowthConfig.isConfiguredCrop(st.getBlock())) {
            event.setNewSpeed(0.0f);
            return;
        }
        // Granite (mines) : pioche uniquement, aucun drop (voir onBlockBreak)
        if (JobActions.canAttemptGranitePickaxeBreak(p, st)) {
            return;
        }
        if (JobActions.isMineGranite(st)) {
            event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getPlayer();
        if (player.isCreative()) {
            return;
        }

        BlockPos pos = event.getPos();
        Level level = event.getWorld();
        BlockState state = level.getBlockState(pos);
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (BlockBreakRules.isStaffProtectedUnbreakable(state)) {
            event.setCanceled(true);
            return;
        }

        if (FurnitureAccess.isFurniture(state) || BlockBreakRules.isNarrationTextBlock(state)) {
            return;
        }

        // Cultures : multi-clics fourche (overlay)
        if (CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer && !level.isClientSide) {
                InteractionHand harvestHand = InteractionHand.MAIN_HAND;
                ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
                ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
                if (!com.ventrys.job.data.ForkConfig.isFork(main.getItem())
                        && com.ventrys.job.data.ForkConfig.isFork(off.getItem())
                        && ToolDurability.isUsable(off)) {
                    harvestHand = InteractionHand.OFF_HAND;
                }
                JobActions.handleBlockInteraction(serverPlayer, level, pos, state, harvestHand);
                // Les messages d'erreur (pas fourche / immature) sont déjà envoyés par
                // ExtractionInteractionHandler — ne pas en renvoyer un second.
            }
            return;
        }

        // Granite = pierre des mines : pioche uniquement, aucun drop (voir onBlockBreak)
        if (JobActions.isMineGranite(state)) {
            if (JobActions.canAttemptGranitePickaxeBreak(player, state)) {
                return;
            }
            event.setCanceled(true);
            return;
        }

        if (JobActions.isExtractedBlock(level, pos)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getPlayer();
        if (player.isCreative()) {
            return;
        }

        BlockState state = event.getState();

        if (BlockBreakRules.isStaffProtectedUnbreakable(state)) {
            event.setNewSpeed(0.0f);
            return;
        }

        if (FurnitureAccess.isFurniture(state) || BlockBreakRules.isNarrationTextBlock(state)) {
            return;
        }

        if (CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            event.setNewSpeed(0.0f);
            return;
        }

        if (JobActions.canAttemptGranitePickaxeBreak(player, state)) {
            return;
        }
        if (JobActions.isMineGranite(state)) {
            event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
            return;
        }
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        ItemStack held = player.getMainHandItem();

        if (BlockBreakRules.isStaffProtectedUnbreakable(state)) {
            event.setCanceled(true);
            return;
        }

        // Granite (pierre des mines) : pioche requise, aucun drop
        if (JobActions.isMineGranite(state)) {
            if (!JobActions.canAttemptGranitePickaxeBreak(player, state)) {
                event.setCanceled(true);
                return;
            }
            event.setCanceled(true);
            removeBlockNoDrop(player, pos, state);
            ToolDurability.hurtAndBreak(held, player, InteractionHand.MAIN_HAND);
            return;
        }

        // Texte HRP : casse libre à la main, aucun drop
        if (BlockBreakRules.isNarrationTextBlock(state)) {
            event.setCanceled(true);
            removeBlockNoDrop(player, pos, state);
            return;
        }

        // Meubles : casse libre + drop item-bloc (loot Westeros souvent vide)
        if (FurnitureAccess.isFurniture(state)) {
            VentrysJob.LOGGER.info("[DEBUG-FURNITURE] forceDropBlockAsItem appele pour {} en {} par {}",
                    state.getBlock(), pos, player.getName().getString());
            forceDropBlockAsItem(event, player, state, pos);
            return;
        }

        // Cultures : jamais de casse vanilla (récolte = LeftClick + fourche)
        if (CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            event.setCanceled(true);
        }
    }

    private static void removeBlockNoDrop(ServerPlayer player, BlockPos pos, BlockState state) {
        Level level = player.getLevel();
        level.removeBlock(pos, false);
        level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(pos);
            player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
        }
    }

    /**
     * Force le drop de l'item-bloc (Westeros / déco : loot souvent vide).
     * Coffres / tonneaux : laisse le vanilla (contenu + conteneur).
     * @return true si casse custom effectuée
     */
    private static boolean forceDropBlockAsItem(BlockEvent.BreakEvent event, ServerPlayer player,
                                               BlockState state, BlockPos pos) {
        var block = state.getBlock();
        if (block instanceof net.minecraft.world.level.block.ChestBlock
                || block instanceof net.minecraft.world.level.block.AbstractChestBlock
                || block instanceof net.minecraft.world.level.block.BarrelBlock) {
            return false;
        }
        ItemStack asItem = new ItemStack(block.asItem());
        if (asItem.isEmpty()) {
            return false;
        }
        event.setCanceled(true);
        removeBlockNoDrop(player, pos, state);
        net.minecraft.world.level.block.Block.popResource(player.getLevel(), pos, asItem);
        return true;
    }
}
