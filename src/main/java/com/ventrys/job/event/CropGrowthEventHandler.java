package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.audio.ExtractionSounds;
import com.ventrys.job.data.CropGrowthConfig;
import com.ventrys.job.data.CropGrowthManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.ventrys.job.util.AgriChunkScanner;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class CropGrowthEventHandler {

    private CropGrowthEventHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        CropGrowthManager.handleServerTick(server);
    }

    @SubscribeEvent
    public static void onCropGrowPre(BlockEvent.CropGrowEvent.Pre event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }

        if (CropGrowthConfig.isConfiguredCrop(event.getState().getBlock())) {
            // Empêcher complètement la croissance vanilla
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            // Enregistrer la culture pour notre système
            CropGrowthManager.registerCrop(level, event.getPos(), event.getState());
        }
    }
    
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        
        // Enregistrer les nouvelles cultures plantées
        if (CropGrowthConfig.isConfiguredCrop(event.getPlacedBlock().getBlock())) {
            CropGrowthManager.registerCrop(level, event.getPos(), event.getPlacedBlock());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }

        if (CropGrowthConfig.isConfiguredCrop(event.getState().getBlock())) {
            if (!(event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }

            ItemStack heldItem = player.getMainHandItem();
            boolean isFork = com.ventrys.job.data.ForkConfig.isFork(heldItem.getItem());
            String playerJob = com.ventrys.job.data.PlayerJobData.getPlayerJob(player);

            if (!"paysan".equals(playerJob) || !isFork) {
                event.setCanceled(true);
                player.sendMessage(new net.minecraft.network.chat.TranslatableComponent(
                    "ventrysjob.message.crop.harvest.require_fork"), player.getUUID());
                return;
            }

            BlockState state = event.getState();
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock cropBlock)) {
                return;
            }

            int currentAge = state.getValue(cropBlock.getAgeProperty());
            int maxAge = cropBlock.getMaxAge();

            if (currentAge < maxAge) {
                event.setCanceled(true);
                player.sendMessage(new net.minecraft.network.chat.TranslatableComponent(
                    "ventrysjob.message.crop.harvest.not_mature"), player.getUUID());
                return;
            }

            var blockRegistryName = state.getBlock().getRegistryName();
            if (blockRegistryName == null) {
                return;
            }
            String blockId = blockRegistryName.toString();
            var cropConfigOpt = com.ventrys.job.data.ForkConfig.getCropConfig(blockId);
            if (cropConfigOpt.isEmpty()) {
                return;
            }

            event.setCanceled(true);

            boolean success = com.ventrys.job.data.JobActions.giveCropDrops(
                player, level, event.getPos(), cropConfigOpt.get());
            if (!success) {
                player.sendMessage(new net.minecraft.network.chat.TranslatableComponent(
                    "ventrysjob.message.crop.harvest.invalid_config"), player.getUUID());
                return;
            }

            level.setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 3);
            CropGrowthManager.unregisterCrop(level, event.getPos());
            ExtractionSounds.play(level, event.getPos(), ExtractionSounds.Kind.CROP);

            if (heldItem.isDamageableItem()) {
                heldItem.hurt(1, level.random, player);
            }
        }
    }
    
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        if (event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk) {
            AgriChunkScanner.scanChunk(level, chunk);
        }
    }
    
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        // À l'arrêt du serveur, Minecraft décharge tous les chunks d'un coup. Le classloader de
        // ModLauncher, déjà en cours de fermeture, peut refuser de charger une classe encore jamais
        // utilisée durant la session (NoClassDefFoundError / ClassNotFoundException → « Failed to save
        // chunk »). Le nettoyage des index mémoire est inutile à l'arrêt (le process se termine et la
        // persistance est gérée par la sauvegarde du monde), donc on l'ignore quand le serveur s'arrête.
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isRunning()) {
            return;
        }
        AgriChunkScanner.forgetChunk(level, event.getChunk().getPos());
    }
}

