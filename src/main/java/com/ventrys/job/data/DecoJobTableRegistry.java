package com.ventrys.job.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Blocs décoratifs VentrysDeco utilisables comme tables de métier (sans BlockEntity).
 */
public final class DecoJobTableRegistry {

    private static final Map<ResourceLocation, String> DECO_JOB_TABLES = Map.of(
            new ResourceLocation("ventrys_blocs", "couturier"), "couturier",
            new ResourceLocation("ventrys_blocs", "enclume"), "forgeron",
            new ResourceLocation("ventrys_blocs", "marmite"), "cuisinier",
            new ResourceLocation("ventrys_blocs", "menuisier"), "artisan",
            new ResourceLocation("ventrys_blocs", "alambic"), "apothicaire"
    );

    private DecoJobTableRegistry() {
    }

    public static String getJobId(ResourceLocation blockId) {
        if (blockId == null) {
            return null;
        }
        return DECO_JOB_TABLES.get(blockId);
    }

    public static boolean isDecoJobTable(ResourceLocation blockId) {
        return blockId != null && DECO_JOB_TABLES.containsKey(blockId);
    }
}
