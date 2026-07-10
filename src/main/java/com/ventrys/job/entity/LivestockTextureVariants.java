package com.ventrys.job.entity;

import com.ventrys.job.VentrysJob;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/** Variantes de texture aléatoires pour les animaux d'élevage VentrysJob. */
public final class LivestockTextureVariants {

    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();

    private LivestockTextureVariants() {
    }

    @Nonnull
    public static String random(@Nonnull List<String> variants, @Nonnull Random random) {
        return variants.get(random.nextInt(variants.size()));
    }

    @Nonnull
    public static String normalize(@Nonnull String texture, @Nonnull List<String> variants, @Nonnull String defaultId) {
        if (texture.isEmpty() || "undefined".equals(texture)) {
            return defaultId;
        }
        String lower = texture.toLowerCase(Locale.ROOT);
        return variants.contains(lower) ? lower : defaultId;
    }

    @Nonnull
    public static ResourceLocation toLocation(@Nonnull String entityFolder, @Nonnull String variantId) {
        String key = entityFolder + "/" + variantId;
        return TEXTURE_CACHE.computeIfAbsent(key,
            k -> new ResourceLocation(VentrysJob.MOD_ID, "textures/entity/" + entityFolder + "/" + variantId + ".png"));
    }
}
