package com.ventrys.job.entity;

import com.ventrys.job.block.entity.ChickenNestBlockEntity;
import com.ventrys.job.util.ChickenNestIndex;
import com.ventrys.job.util.VentrysItemRefs;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;

public class CustomChicken extends CustomAnimal implements LivestockTextureHolder {

    public static final String DEFAULT_TEXTURE = "poule_marron";

    private static final List<String> TEXTURE_VARIANTS = List.of(
        "poule_gris_blanc", "poule_grise", "poule_jauni_clair", "poule_marron",
        "poule_marron_fonce", "poule_marron_grise", "poule_marron_jauni", "poule_marron_sepia", "poule_noir"
    );

    public static List<String> getTextureVariantIds() {
        return TEXTURE_VARIANTS;
    }

    private static final EntityDataAccessor<String> TEXTURE_VARIANT =
        SynchedEntityData.defineId(CustomChicken.class, EntityDataSerializers.STRING);
    
    private static final int NEST_SEARCH_RADIUS = 20;
    private static final int NEST_SEARCH_Y_RANGE = 5;
    private long lastEggLayTime = 0;
    private BlockPos cachedNestPos = null;
    private static final long EGG_LAY_INTERVAL_MIN_MS = 43_200_000L; // 12 heures
    private static final long EGG_LAY_INTERVAL_MAX_MS = 43_200_000L; // 12 heures
    private long nextEggLayIntervalMs = 0; // Intervalle aléatoire pour cette poule (2-5 min)
    
    public CustomChicken(EntityType<? extends CustomAnimal> entityType, Level level) {
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
        return LivestockTextureVariants.toLocation("chicken", normalizeTexture(texture));
    }
    
    @Override
    protected float getLengthScale() {
        return 1.7f; // +70% de longueur
    }
    
    @Override
    protected float getHeightOffset() {
        return -0.6f; // +100% de hauteur finale (double)
    }
    
    @Override
    public String getModelResourceName() {
        return "poule";
    }
    
    @Override
    public String getTextureResourceName() {
        return this.getTextureVariant();
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.CHICKEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }
    
    // Builders d'animation mis en cache (immuables, partagés) : évite une allocation par frame.
    private static final AnimationBuilder ANIM_DEAD = new AnimationBuilder().addAnimation("mort", false);
    private static final AnimationBuilder ANIM_HURT = new AnimationBuilder().addAnimation("tappe", false);
    private static final AnimationBuilder ANIM_WALK = new AnimationBuilder().addAnimation("marche", true);
    private static final AnimationBuilder ANIM_PECK = new AnimationBuilder().addAnimation("picore", false);
    private static final AnimationBuilder ANIM_IDLE = new AnimationBuilder().addAnimation("idle", true);

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 5, this::predicate));
    }
    
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        CustomChicken chicken = (CustomChicken) event.getAnimatable();
        AnimationController<?> controller = event.getController();
        
        // Vérifier si la poule est morte ou en train de mourir
        if (chicken.isDeadOrDying() || chicken.getHealth() <= 0) {
            controller.markNeedsReload();
            controller.setAnimation(ANIM_DEAD);
            return PlayState.CONTINUE;
        }
        
        // Vérifier si la poule a pris des dégâts récemment (animation "tappe")
        // hurtTime est mis à jour chaque tick quand l'entité prend des dégâts
        if (chicken.hurtTime > 0) {
            controller.setAnimation(ANIM_HURT);
            return PlayState.CONTINUE;
        }
        
        // Vérifier si la poule bouge (seuil ajusté pour détecter le mouvement)
        if (chicken.shouldPlayLeadWalkAnimation()) {
            controller.setAnimation(ANIM_WALK);
            return PlayState.CONTINUE;
        }
        
        // Animation "picore" quand la poule est immobile (avec une chance)
        // Utiliser un système basé sur le tick pour éviter de changer trop souvent
        if (chicken.tickCount % 100 == 0 && chicken.getRandom().nextFloat() < 0.3f) {
            // 30% de chance toutes les 5 secondes (100 ticks) de jouer "picore"
            controller.setAnimation(ANIM_PECK);
            return PlayState.CONTINUE;
        }
        
        // Animation par défaut : idle
        controller.setAnimation(ANIM_IDLE);
        return PlayState.CONTINUE;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level.isClientSide && !isMale()) {
            long currentTime = System.currentTimeMillis();
            
            // Initialiser le timestamp et l'intervalle aléatoire si c'est la première fois
            if (lastEggLayTime == 0) {
                lastEggLayTime = currentTime;
                // Générer un intervalle aléatoire entre 2 et 5 minutes pour cette poule
                nextEggLayIntervalMs = EGG_LAY_INTERVAL_MIN_MS + 
                    (long)(this.getRandom().nextDouble() * (EGG_LAY_INTERVAL_MAX_MS - EGG_LAY_INTERVAL_MIN_MS));
            }
            
            // Vérifier les conditions de nutrition et hydratation
            boolean canProduce = getNutrition() >= 30 && getHydration() >= 30;
            
            if (!canProduce) {
                // Si les conditions ne sont pas remplies, "geler" le timer en ajustant lastEggLayTime
                // pour qu'il reste à la même position relative
                // On ne fait rien, le timer reste figé
                return;
            }
            
            // Si les conditions sont remplies, vérifier si la poule peut pondre un œuf
            // Le timer reprend là où il en était car lastEggLayTime n'a pas été modifié
            if (currentTime - lastEggLayTime >= nextEggLayIntervalMs) {
                tryLayEgg();
            }
        }
    }
    
    private void tryLayEgg() {
        if (getNutrition() < 30 || getHydration() < 30) {
            return;
        }
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (cachedNestPos != null && layEggAt(cachedNestPos)) {
            scheduleNextEggLay();
            return;
        }
        cachedNestPos = null;

        BlockPos nestPos = ChickenNestIndex.findNestWithSpace(
            serverLevel, this.blockPosition(), NEST_SEARCH_RADIUS, NEST_SEARCH_Y_RANGE);
        if (nestPos != null && layEggAt(nestPos)) {
            cachedNestPos = nestPos.immutable();
            scheduleNextEggLay();
            return;
        }

        scheduleNextEggLay();
    }

    private boolean layEggAt(BlockPos nestPos) {
        BlockEntity be = this.level.getBlockEntity(nestPos);
        if (!(be instanceof ChickenNestBlockEntity nest) || !nest.canAddEgg()) {
            return false;
        }
        var eggItem = VentrysItemRefs.resOeufItemOrNull();
        if (eggItem == null) {
            return false;
        }
        nest.addEgg(new ItemStack(eggItem, 1));
        this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        return true;
    }

    private void scheduleNextEggLay() {
        lastEggLayTime = System.currentTimeMillis();
        nextEggLayIntervalMs = EGG_LAY_INTERVAL_MIN_MS
            + (long) (this.getRandom().nextDouble() * (EGG_LAY_INTERVAL_MAX_MS - EGG_LAY_INTERVAL_MIN_MS));
    }
    
    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        if (otherParent instanceof CustomChicken && canReproduce() && ((CustomChicken) otherParent).canReproduce()) {
            // Vérifier qu'un est mâle et l'autre femelle
            if (isMale() != ((CustomChicken) otherParent).isMale()) {
                @SuppressWarnings("unchecked")
                EntityType<? extends CustomAnimal> entityType = (EntityType<? extends CustomAnimal>) this.getType();
                CustomChicken offspring = new CustomChicken(entityType, level);
                if (otherParent instanceof CustomChicken mate) {
                    offspring.setTextureVariant(this.random.nextBoolean()
                        ? this.getTextureVariant()
                        : mate.getTextureVariant());
                } else {
                    offspring.setTextureVariant(randomTexture(this.random));
                }
                resetReproductionTimer();
                ((CustomChicken) otherParent).resetReproductionTimer();
                return offspring;
            }
        }
        return null;
    }
    
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("TextureVariant", this.getTextureVariant());
        tag.putLong("LastEggLayTime", lastEggLayTime);
        tag.putLong("NextEggLayInterval", nextEggLayIntervalMs);
    }
    
    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TextureVariant")) {
            this.setTextureVariant(tag.getString("TextureVariant"));
        } else if (!this.level.isClientSide()) {
            this.setTextureVariant(randomTexture(this.random));
        }
        if (tag.contains("LastEggLayTime")) {
            lastEggLayTime = tag.getLong("LastEggLayTime");
            // Si le timestamp est en ticks (ancien format), le convertir
            // Les timestamps en ms sont > 1 000 000 000 (environ 2001)
            if (lastEggLayTime < 1_000_000_000L) {
                // C'est probablement un ancien timestamp en ticks, réinitialiser
                lastEggLayTime = System.currentTimeMillis();
            }
        } else {
            lastEggLayTime = System.currentTimeMillis();
        }
        
        // Charger l'intervalle aléatoire sauvegardé, ou en générer un nouveau
        if (tag.contains("NextEggLayInterval")) {
            nextEggLayIntervalMs = tag.getLong("NextEggLayInterval");
            // Vérifier que l'intervalle est valide (entre min et max)
            if (nextEggLayIntervalMs < EGG_LAY_INTERVAL_MIN_MS || nextEggLayIntervalMs > EGG_LAY_INTERVAL_MAX_MS) {
                // Générer un nouvel intervalle aléatoire si l'ancien est invalide
                nextEggLayIntervalMs = EGG_LAY_INTERVAL_MIN_MS + 
                    (long)(this.getRandom().nextDouble() * (EGG_LAY_INTERVAL_MAX_MS - EGG_LAY_INTERVAL_MIN_MS));
            }
        } else {
            // Générer un nouvel intervalle aléatoire si pas de sauvegarde
            nextEggLayIntervalMs = EGG_LAY_INTERVAL_MIN_MS + 
                (long)(this.getRandom().nextDouble() * (EGG_LAY_INTERVAL_MAX_MS - EGG_LAY_INTERVAL_MIN_MS));
        }
    }
}

