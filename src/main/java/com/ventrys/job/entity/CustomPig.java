package com.ventrys.job.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

public class CustomPig extends CustomAnimal implements LivestockTextureHolder {

    public static final String DEFAULT_TEXTURE = "texture_cochon";

    private static final List<String> TEXTURE_VARIANTS = List.of(
        "cochon_rose_gris", "texture_cochon", "texture_cochon_noir", "texture_cochon_violace"
    );

    public static List<String> getTextureVariantIds() {
        return TEXTURE_VARIANTS;
    }

    private static final EntityDataAccessor<String> TEXTURE_VARIANT =
        SynchedEntityData.defineId(CustomPig.class, EntityDataSerializers.STRING);

    public CustomPig(EntityType<? extends CustomAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TEXTURE_VARIANT, DEFAULT_TEXTURE);
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
        return LivestockTextureVariants.toLocation("pig", normalizeTexture(texture));
    }

    @Override
    public String getModelResourceName() {
        return "porc";
    }

    @Override
    public String getTextureResourceName() {
        return this.getTextureVariant();
    }

    @Override
    protected float getWidthScale() {
        return 0.525f; // +5% vs ancienne largeur
    }

    @Override
    protected float getLengthScale() {
        return 1.0f; // +100% vs ancienne longueur (0.5 -> 1.0)
    }

    @Override
    protected float getHeightOffset() {
        return -0.84f; // +20% de hauteur approx.
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.PIG_STEP, 0.15F, 1.0F);
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        if (otherParent instanceof CustomPig mate && canReproduce() && mate.canReproduce()) {
            if (isMale() != mate.isMale()) {
                @SuppressWarnings("unchecked")
                EntityType<? extends CustomAnimal> entityType = (EntityType<? extends CustomAnimal>) this.getType();
                CustomPig offspring = new CustomPig(entityType, level);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TextureVariant")) {
            this.setTextureVariant(tag.getString("TextureVariant"));
        } else if (!this.level.isClientSide()) {
            this.setTextureVariant(randomTexture(this.random));
        }
    }
}
