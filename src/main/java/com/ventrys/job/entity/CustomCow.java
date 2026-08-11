package com.ventrys.job.entity;

import com.ventrys.job.data.MobConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

public class CustomCow extends CustomAnimal implements LivestockTextureHolder {

    public static final String DEFAULT_TEXTURE = "vache";

    private static final List<String> TEXTURE_VARIANTS = List.of(
        "vache", "vache_grise", "vache_grise_marron", "vache_sombre"
    );

    public static List<String> getTextureVariantIds() {
        return TEXTURE_VARIANTS;
    }

    // defineId(CustomAnimal.class, ...) et non CustomCow.class : CustomCow, CustomSheep et
    // CustomChicken sont tous des sous-classes directes de CustomAnimal, donc s'ils reservent
    // leurs champs chacun sous leur propre classe, l'allocation d'ID de SynchedEntityData
    // repart independamment de "dernier index de CustomAnimal + 1" pour chacun -> collision
    // d'index entre especes (meme index, types differents -> crash cote client au decodage).
    // Passer CustomAnimal.class fait partager le meme compteur a toutes les sous-classes.
    private static final EntityDataAccessor<String> TEXTURE_VARIANT =
        SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.STRING);
    /** Minutes restantes avant traite (0 = prêt). -1 = mâle / indisponible. */
    private static final EntityDataAccessor<Integer> MILK_READY_IN_MIN =
        SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.INT);

    private long milkProductionStartTime = 0;

    public CustomCow(EntityType<? extends CustomAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TEXTURE_VARIANT, DEFAULT_TEXTURE);
        this.entityData.define(MILK_READY_IN_MIN, -1);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        net.minecraft.world.DifficultyInstance difficulty,
        MobSpawnType reason,
        @Nullable SpawnGroupData spawnData,
        @Nullable CompoundTag dataTag
    ) {
        if (!level.isClientSide()) {
            this.setTextureVariant(randomTexture(this.random));
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    public void setTextureVariant(String texture) {
        this.entityData.set(TEXTURE_VARIANT, normalizeTexture(texture));
    }

    public String getTextureVariant() {
        return normalizeTexture(this.entityData.get(TEXTURE_VARIANT));
    }

    @Nonnull
    public static String randomTexture(@Nonnull Random random) {
        return LivestockTextureVariants.random(TEXTURE_VARIANTS, random);
    }

    @Nonnull
    public static String normalizeTexture(@Nonnull String texture) {
        return LivestockTextureVariants.normalize(texture, TEXTURE_VARIANTS, DEFAULT_TEXTURE);
    }

    @Nonnull
    public static net.minecraft.resources.ResourceLocation toTextureLocation(@Nonnull String texture) {
        return LivestockTextureVariants.toLocation("cow", normalizeTexture(texture));
    }

    @Override
    public String getModelResourceName() {
        return "vache";
    }

    @Override
    public String getTextureResourceName() {
        return this.getTextureVariant();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide || this.tickCount % 20 != 0) {
            return;
        }
        syncMilkHud();
    }

    private void syncMilkHud() {
        if (isMale()) {
            this.entityData.set(MILK_READY_IN_MIN, -1);
            return;
        }
        if (getNutrition() < 30 || getHydration() < 30) {
            // Indisponible faute de soins — affiché comme -2 côté client
            this.entityData.set(MILK_READY_IN_MIN, -2);
            return;
        }
        long remaining = getMilkCooldownRemainingMs();
        int minutes = remaining <= 0L ? 0 : (int) Math.ceil(remaining / 60_000.0);
        this.entityData.set(MILK_READY_IN_MIN, minutes);
    }

    /** 0 = prêt, &gt;0 = minutes, -1 = mâle, -2 = trop affamée/assoiffée. */
    public int getMilkReadyInMinutes() {
        return this.entityData.get(MILK_READY_IN_MIN);
    }

    @Override
    public boolean canExtractMilk() {
        if (isMale()) {
            return false;
        }

        if (getNutrition() < 30 || getHydration() < 30) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long interval = MobConfig.getMilkExtractionIntervalMs();

        if (milkProductionStartTime == 0) {
            milkProductionStartTime = currentTime;
            return false;
        }

        return (currentTime - milkProductionStartTime) >= interval;
    }

    public long getMilkCooldownRemainingMs() {
        long currentTime = System.currentTimeMillis();
        if (milkProductionStartTime == 0) {
            milkProductionStartTime = currentTime;
        }
        long interval = MobConfig.getMilkExtractionIntervalMs();
        return Math.max(0L, interval - (currentTime - milkProductionStartTime));
    }

    public ItemStack extractMilk() {
        if (canExtractMilk()) {
            milkProductionStartTime = System.currentTimeMillis();
            setLastMilkExtraction(System.currentTimeMillis());
            return new ItemStack(Items.MILK_BUCKET);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected float getLengthScale() {
        return 2.0f;
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        if (otherParent instanceof CustomCow mate && canReproduce() && mate.canReproduce()) {
            if (isMale() != mate.isMale()) {
                @SuppressWarnings("unchecked")
                EntityType<? extends CustomAnimal> entityType = (EntityType<? extends CustomAnimal>) this.getType();
                CustomCow offspring = new CustomCow(entityType, level);
                offspring.setTextureVariant(this.random.nextBoolean()
                    ? this.getTextureVariant()
                    : mate.getTextureVariant());
                resetReproductionTimer();
                mate.resetReproductionTimer();
                return offspring;
            }
        }
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("TextureVariant", this.getTextureVariant());
        tag.putLong("MilkProductionStartTime", milkProductionStartTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TextureVariant")) {
            this.setTextureVariant(tag.getString("TextureVariant"));
        } else if (!this.level.isClientSide()) {
            this.setTextureVariant(randomTexture(this.random));
        }
        if (tag.contains("MilkProductionStartTime")) {
            milkProductionStartTime = tag.getLong("MilkProductionStartTime");
            if (milkProductionStartTime < 1_000_000_000L) {
                milkProductionStartTime = System.currentTimeMillis();
            }
        } else {
            milkProductionStartTime = System.currentTimeMillis();
        }
    }
}
