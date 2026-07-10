package com.ventrys.job.util;

import com.ventrys.job.VentrysJob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Chemins de persistance VentrysJob — dossier {@code {monde}/ventrysjob/} quand le serveur tourne.
 */
public final class VentrysJobDataPaths {

    private static final String DATA_FOLDER = "ventrysjob";
    private static final String LEGACY_FOLDER = "ventrysjob_data";

    private VentrysJobDataPaths() {
    }

    public static Path getDataDirectory() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getWorldPath(LevelResource.ROOT).resolve(DATA_FOLDER);
        }
        return FMLPaths.GAMEDIR.get().resolve(LEGACY_FOLDER);
    }

    public static void ensureDataDirectory() throws IOException {
        Path dir = getDataDirectory();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    public static void migrateLegacyFilesIfNeeded(String... fileNames) {
        Path targetDir = getDataDirectory();
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
        } catch (IOException e) {
            VentrysJob.LOGGER.warn("Impossible de créer le dossier ventrysjob: {}", e.getMessage());
            return;
        }

        for (String fileName : fileNames) {
            Path target = targetDir.resolve(fileName);
            if (Files.exists(target)) {
                continue;
            }
            for (Path legacyDir : legacyDirectories()) {
                Path legacyFile = legacyDir.resolve(fileName);
                if (!Files.exists(legacyFile)) {
                    continue;
                }
                try {
                    Files.copy(legacyFile, target, StandardCopyOption.REPLACE_EXISTING);
                    VentrysJob.LOGGER.info(
                        "Migration persistence VentrysJob: {} -> {}",
                        legacyFile.toAbsolutePath(),
                        target.toAbsolutePath());
                } catch (IOException e) {
                    VentrysJob.LOGGER.warn("Échec migration {}: {}", fileName, e.getMessage());
                }
                break;
            }
        }
    }

    private static List<Path> legacyDirectories() {
        return List.of(
            Paths.get(System.getProperty("user.dir")).resolve(LEGACY_FOLDER),
            FMLPaths.GAMEDIR.get().resolve(LEGACY_FOLDER)
        );
    }
}
