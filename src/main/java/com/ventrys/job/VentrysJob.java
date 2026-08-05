package com.ventrys.job;

import com.ventrys.job.block.entity.OuvrierFourBlockEntity;
import com.ventrys.job.block.entity.ForgeronFourBlockEntity;
import com.ventrys.job.command.JobCommand;
import com.ventrys.job.command.TimeDebugCommand;
import com.ventrys.job.data.CropGrowthConfig;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.JobManager;
import com.ventrys.job.data.MeuleConfig;
import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.event.RealTimeSyncHandler;
import com.ventrys.job.init.ModBlockEntities;
import com.ventrys.job.init.ModBlocks;
import com.ventrys.job.init.ModMenuTypes;
import com.ventrys.job.init.ModSounds;
import com.ventrys.job.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(VentrysJob.MOD_ID)
public class VentrysJob {
    public static final String MOD_ID = "ventrysjob";
    public static final Logger LOGGER = LogManager.getLogger();

    /** true dès le début de ServerStopping — évite les ClassNotFound pendant le déchargement des chunks. */
    private static volatile boolean shuttingDown;

    public static boolean isShuttingDown() {
        return shuttingDown;
    }

    public VentrysJob() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Enregistrer les blocs, block entities, menus, entités
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        com.ventrys.job.init.ModEntities.ENTITIES.register(modEventBus);
        
        modEventBus.addListener((FMLCommonSetupEvent evt) -> commonSetup(evt));
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        String modVersion = ModList.get().getModContainerById(MOD_ID)
            .map(c -> c.getModInfo().getVersion().toString())
            .orElse("?");
        shuttingDown = false;
        LOGGER.info("VentrysJob {} — chargement (vérifiez cette version si la prod ne correspond pas au JAR attendu)", modVersion);
        
        event.enqueueWork(() -> {
            NetworkHandler.register();
            try {
                JobManager.loadJobs();
            } catch (Exception e) {
                LOGGER.error("Erreur critique lors du chargement des jobs au demarrage", e);
                // Ne pas bloquer le demarrage du mod, mais logger l'erreur
            }
            com.ventrys.job.data.MalletConfig.loadConfig();
            com.ventrys.job.data.HammerConfig.loadConfig();
            com.ventrys.job.data.SawConfig.loadConfig();
            com.ventrys.job.data.ForkConfig.loadConfig();
            com.ventrys.job.data.ChiselConfig.loadConfig();
            com.ventrys.job.data.JobActions.loadExtractionToolsConfig();
            MeuleConfig.load();
            CropGrowthConfig.loadConfig();
            com.ventrys.job.data.MobConfig.load();
            com.ventrys.job.data.OreConfig.loadConfig();
            com.ventrys.job.data.WeatherConfig.load();
            
            OuvrierFourBlockEntity.loadConfiguration();
            ForgeronFourBlockEntity.loadConfiguration();

            // Précharge les classes souvent absentes du chemin chaud : au shutdown le classloader
            // ModLauncher peut refuser de les résoudre → NoClassDefFoundError / Failed to save chunk.
            preloadShutdownSensitiveClasses();
        });
    }

    private static void preloadShutdownSensitiveClasses() {
        try {
            Class.forName("com.ventrys.job.util.FarmlandMoistureManager");
            Class.forName("com.ventrys.job.util.ChickenNestIndex");
            Class.forName("com.ventrys.job.util.AgriChunkScanner");
            Class.forName("com.ventrys.job.extraction.ExtractionProgressManager");
            Class.forName("com.ventrys.job.data.CropGrowthManager");
        } catch (Throwable t) {
            LOGGER.warn("Préchargement classes shutdown: {}", t.toString());
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CropGrowthConfig.loadConfig();
        LOGGER.info("VentrysJob — chargement des données joueurs (dossier {}/ventrysjob/)",
            event.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
        JobActions.loadExtractedPositions();
        PlayerJobData.loadPlayerJobs();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        JobCommand.register(event.getDispatcher());
        TimeDebugCommand.register(event.getDispatcher());
        LOGGER.debug("Commandes VentrysJob enregistrées");
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        shuttingDown = true;
        LOGGER.debug("Arrêt serveur — nettoyage VentrysJob");
        try {
            if (event.getServer() != null) {
                RealTimeSyncHandler.restoreDaylightCycle(event.getServer());
            }
        } catch (Throwable t) {
            LOGGER.warn("Arrêt — restoreDaylightCycle: {}", t.toString());
        }
        try {
            JobActions.forceCleanup();
        } catch (Throwable t) {
            // Ne jamais faire crasher l'arrêt serveur (JAR partiel / hot-swap / classloader)
            LOGGER.warn("Arrêt — forceCleanup ignoré: {}", t.toString());
        }
        try {
            JobActions.shutdownExtractedPositionsSaver();
            JobActions.saveExtractedPositions();
        } catch (Throwable t) {
            LOGGER.warn("Arrêt — save extracted positions: {}", t.toString());
        }
        try {
            PlayerJobData.forceSave();
            PlayerJobData.shutdown();
        } catch (Throwable t) {
            LOGGER.warn("Arrêt — PlayerJobData: {}", t.toString());
        }
        LOGGER.info("VentrysJob — sauvegardes terminées (progressions, métiers joueurs, positions extraites)");
    }

}
