package com.ventrys.job.entity;

import com.ventrys.job.data.LivestockProgressManager;
import com.ventrys.job.data.LivestockProgressSavedData;
import com.ventrys.job.data.MobConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.Random;

/**
 * Classe de base pour les animaux personnalisés avec système de nutrition/hydratation/reproduction
 * Utilise GeckoLib pour les animations
 */
public abstract class CustomAnimal extends Animal implements IAnimatable {
    private static final double POSITION_THRESHOLD = 0.0025D;
    private static final double VELOCITY_THRESHOLD = 0.0004D;
    private static final int SHOW_NAME_CHECK_INTERVAL = 10;

    private int lastShowNameCheckTick = Integer.MIN_VALUE;
    private boolean cachedShowNameNearby = false;
    
    private final AnimationFactory factory = new AnimationFactory(this);
    
    /** HUD gestation : mâle / pas concerné. */
    public static final int REPRO_HUD_MALE = 0;
    /** HUD : nutrition ou hydratation sous le seuil. */
    public static final int REPRO_HUD_NEEDS_CARE = 1;
    /** HUD : pas de partenaire sexe opposé à proximité. */
    public static final int REPRO_HUD_NO_PARTNER = 2;
    /** HUD : accouplement en cours (jauge partenaire, timer 1). */
    public static final int REPRO_HUD_GESTATING = 3;
    /** HUD : jauge accouplement pleine (transition courte). */
    public static final int REPRO_HUD_READY = 4;
    /** HUD : enceinte (timer 2 grossesse 48 h) — seul état avec % enceinte. */
    public static final int REPRO_HUD_PREGNANT = 5;

    // Données synchronisées
    private static final EntityDataAccessor<Boolean> IS_MALE = SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> NUTRITION = SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HYDRATION = SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REPRO_PROGRESS_PERCENT =
        SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REPRO_HUD_STATE =
        SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.INT);
    
    // Données non synchronisées (gérées côté serveur) - Utilisation de timestamps réels (ms)
    private long lastNutritionDecrease = 0;
    private long lastHydrationDecrease = 0;
    private long lastMilkExtraction = 0;
    private long reproductionProgressMs = 0L;
    private long pregnancyProgressMs = 0L;
    private boolean pregnant = false;
    private long lastRegenerationTime = 0;
    
    protected CustomAnimal(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        if (!level.isClientSide) {
            // Initialiser aléatoirement le sexe
            setMale(new Random().nextBoolean());
            // Initialiser nutrition et hydratation à 100%
            this.entityData.set(NUTRITION, 100);
            this.entityData.set(HYDRATION, 100);
            // Initialiser les timestamps avec le temps actuel
            long currentTime = System.currentTimeMillis();
            this.lastNutritionDecrease = currentTime;
            this.lastHydrationDecrease = currentTime;
            this.lastMilkExtraction = currentTime;
            this.lastRegenerationTime = currentTime;
            this.reproductionProgressMs = 0L;
            this.pregnancyProgressMs = 0L;
            this.pregnant = false;
        }
    }

    public void setMale(boolean male) {
        this.entityData.set(IS_MALE, male);
    }
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_MALE, true);
        this.entityData.define(NUTRITION, 100);
        this.entityData.define(HYDRATION, 100);
        this.entityData.define(REPRO_PROGRESS_PERCENT, 0);
        this.entityData.define(REPRO_HUD_STATE, REPRO_HUD_MALE);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }
    
    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        float base = super.getStandingEyeHeight(pose, dimensions);
        // getHeightOffset() est calibré pour l'adulte ; on le met à l'échelle (0.5 pour un bébé)
        // sinon il « écrase » la hauteur déjà réduite du bébé.
        float adjusted = base * getHeightScale() + getHeightOffset() * this.getScale();
        return Math.max(0.2f, adjusted);
    }
    
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        // super.getDimensions() inclut déjà getScale() (x0.5 pour les bébés via LivingEntity).
        EntityDimensions baseDimensions = super.getDimensions(pose);
        float maxHorizontalScale = Math.max(getWidthScale(), getLengthScale());
        float scaledWidth = Math.max(0.1f, baseDimensions.width * maxHorizontalScale);
        // L'offset (calibré adulte) doit être mis à l'échelle, sinon la hitbox du bébé
        // s'effondre au minimum (0.1) et apparaît minuscule au sol.
        float scaledHeight = Math.max(0.1f, baseDimensions.height * getHeightScale() + getHeightOffset() * this.getScale());
        return EntityDimensions.scalable(scaledWidth, scaledHeight);
    }
    
    protected float getWidthScale() {
        return 1.0f;
    }
    
    protected float getLengthScale() {
        return getWidthScale();
    }
    
    protected float getHeightScale() {
        return 2.0f;
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }
    
    @Override
    public void refreshDimensions() {
        super.refreshDimensions();
        applyCustomBoundingBox(this.getPose());
    }
    
    private void applyCustomBoundingBox(Pose pose) {
        EntityDimensions baseDimensions = super.getDimensions(pose);
        float customWidth = Math.max(0.1f, baseDimensions.width * getWidthScale());
        float customLength = Math.max(0.1f, baseDimensions.width * getLengthScale());
        // Offset mis à l'échelle (0.5 bébé) pour éviter une hitbox effondrée au sol.
        float customHeight = Math.max(0.1f, baseDimensions.height * getHeightScale() + getHeightOffset() * this.getScale());
        
        double halfWidth = customWidth / 2.0f;
        double halfLength = customLength / 2.0f;
        
        this.setBoundingBox(new AABB(
                this.getX() - halfWidth,
                this.getY(),
                this.getZ() - halfLength,
                this.getX() + halfWidth,
                this.getY() + customHeight,
                this.getZ() + halfLength
        ));
    }
    
    
    protected float getHeightOffset() {
        return -1.0f;
    }
    
    @Override
    public boolean shouldShowName() {
        int tick = this.tickCount;
        if (tick - lastShowNameCheckTick >= SHOW_NAME_CHECK_INTERVAL) {
            lastShowNameCheckTick = tick;
            cachedShowNameNearby = this.level.getNearestPlayer(this, 2.0) != null;
        }
        return cachedShowNameNearby;
    }
    
    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide && this.tickCount % 20 == 0) {
            LivestockProgressManager.syncEntity(this);
            refreshReproductionHudFromWorld();
        }
    }

    /** HUD reproduction : scan local du partenaire (ne modifie pas la jauge SavedData). */
    public void refreshReproductionHudFromWorld() {
        if (this.level.isClientSide) {
            return;
        }
        double radius = MobConfig.getDetectionRadiusBlocks();
        boolean opposite = !this.level.getEntitiesOfClass(
            CustomAnimal.class,
            this.getBoundingBox().inflate(radius),
            other -> other != this
                && other.getClass() == this.getClass()
                && other.isAlive()
                && !other.isBaby()
                && other.isMale() != this.isMale()
        ).isEmpty();
        LivestockProgressSavedData.Entry fake = new LivestockProgressSavedData.Entry();
        fake.reproductionProgressMs = this.reproductionProgressMs;
        fake.pregnancyProgressMs = this.pregnancyProgressMs;
        fake.pregnant = this.pregnant;
        fake.isMale = this.isMale();
        fake.nutrition = this.getNutrition();
        fake.hydration = this.getHydration();
        updateReproductionHud(fake, opposite);
    }

    public long getReproductionProgressMs() {
        return reproductionProgressMs;
    }

    public long getPregnancyProgressMs() {
        return pregnancyProgressMs;
    }

    public boolean isPregnant() {
        return pregnant;
    }

    public void setPregnant(boolean pregnant, long progressMs) {
        this.pregnant = pregnant;
        this.pregnancyProgressMs = Math.max(0L, progressMs);
        if (!pregnant) {
            this.pregnancyProgressMs = 0L;
        }
    }

    public long getLastNutritionDecreaseMs() {
        return lastNutritionDecrease;
    }

    public long getLastHydrationDecreaseMs() {
        return lastHydrationDecrease;
    }

    public long getLastRegenerationTimeMs() {
        return lastRegenerationTime;
    }

    /** Applique l'état persisté (temps réel, y compris chunk déchargé) sur l'entité chargée. */
    public void applyLivestockEntry(LivestockProgressSavedData.Entry entry) {
        if (entry == null || this.level.isClientSide) {
            return;
        }
        // Sexe : si SavedData et entité divergent, on privilégie l'entité (NBT déjà lu).
        // Ne jamais forcer "mâle" depuis une entrée stale.
        setNutrition(entry.nutrition);
        setHydration(entry.hydration);
        this.reproductionProgressMs = entry.reproductionProgressMs;
        this.pregnancyProgressMs = entry.pregnancyProgressMs;
        this.pregnant = entry.pregnant;
        this.lastNutritionDecrease = entry.lastNutritionDecreaseMs;
        this.lastHydrationDecrease = entry.lastHydrationDecreaseMs;
        this.lastRegenerationTime = entry.lastRegenerationMs;
        if (entry.pendingHealHearts > 0) {
            float currentHealth = this.getHealth();
            float maxHealth = this.getMaxHealth();
            if (currentHealth < maxHealth) {
                this.heal(entry.pendingHealHearts * 2.0f);
            }
            entry.pendingHealHearts = 0;
        }
    }

    /**
     * Met à jour le HUD reproduction (sync client).
     * Le % « enceinte » n'est affiché que pendant la grossesse (timer 2).
     */
    public void updateReproductionHud(LivestockProgressSavedData.Entry entry, boolean oppositeSexNearby) {
        if (this.level.isClientSide || entry == null) {
            return;
        }

        int state;
        int percent;

        if (isMale()) {
            state = REPRO_HUD_MALE;
            percent = 0;
        } else if (entry.pregnant) {
            long pregRequired = Math.max(1L, MobConfig.getPregnancyTimeMinutes() * 60_000L);
            percent = (int) Math.min(100L, Math.max(0L, (entry.pregnancyProgressMs * 100L) / pregRequired));
            state = REPRO_HUD_PREGNANT;
        } else if (!canReproduce()) {
            state = REPRO_HUD_NEEDS_CARE;
            percent = 0;
        } else if (!oppositeSexNearby) {
            state = REPRO_HUD_NO_PARTNER;
            long matingRequired = Math.max(1L, MobConfig.getRequiredTimeMinutes() * 60_000L);
            percent = (int) Math.min(100L, Math.max(0L, (entry.reproductionProgressMs * 100L) / matingRequired));
        } else {
            long matingRequired = Math.max(1L, MobConfig.getRequiredTimeMinutes() * 60_000L);
            percent = (int) Math.min(100L, Math.max(0L, (entry.reproductionProgressMs * 100L) / matingRequired));
            state = percent >= 100 ? REPRO_HUD_READY : REPRO_HUD_GESTATING;
        }

        this.entityData.set(REPRO_PROGRESS_PERCENT, percent);
        this.entityData.set(REPRO_HUD_STATE, state);
    }

    /** @deprecated use {@link #updateReproductionHud(LivestockProgressSavedData.Entry, boolean)} */
    @Deprecated
    public void updateReproductionHud(long progressMs, boolean oppositeSexNearby) {
        LivestockProgressSavedData.Entry fake = new LivestockProgressSavedData.Entry();
        fake.reproductionProgressMs = progressMs;
        fake.pregnancyProgressMs = this.pregnancyProgressMs;
        fake.pregnant = this.pregnant;
        updateReproductionHud(fake, oppositeSexNearby);
    }

    public int getReproductionProgressPercent() {
        return this.entityData.get(REPRO_PROGRESS_PERCENT);
    }

    public int getReproductionHudState() {
        return this.entityData.get(REPRO_HUD_STATE);
    }

    public boolean isReproductionProgressComplete() {
        if (pregnant) {
            return false;
        }
        long requiredMs = MobConfig.getRequiredTimeMinutes() * 60_000L;
        return reproductionProgressMs >= requiredMs;
    }

    /** Les deux parents doivent pouvoir se reproduire ; seule la femelle doit avoir fini l'accouplement. */
    public boolean isReproductionReadyWith(CustomAnimal mate) {
        if (mate == null || mate == this) {
            return false;
        }
        if (this.isBaby() || mate.isBaby()) {
            return false;
        }
        if (this.pregnant) {
            return false;
        }
        if (!canReproduce() || !mate.canReproduce()) {
            return false;
        }
        if (isMale() == mate.isMale()) {
            return false;
        }
        CustomAnimal female = isMale() ? mate : this;
        if (female.pregnant) {
            return false;
        }
        return female.isReproductionProgressComplete();
    }
    
    public boolean isMale() {
        return this.entityData.get(IS_MALE);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        // Pas d'alimentation / love-mode vanilla : nutrition via AnimalInteractionHandler uniquement.
        return false;
    }
    
    public int getNutrition() {
        return this.entityData.get(NUTRITION);
    }
    
    public int getHydration() {
        return this.entityData.get(HYDRATION);
    }
    
    public void setNutrition(int nutrition) {
        this.entityData.set(NUTRITION, Math.max(0, Math.min(100, nutrition)));
    }
    
    public void setHydration(int hydration) {
        this.entityData.set(HYDRATION, Math.max(0, Math.min(100, hydration)));
    }
    
    public void addNutrition(int amount) {
        setNutrition(getNutrition() + amount);
        persistLivestockProgress();
    }
    
    public void addHydration(int amount) {
        setHydration(getHydration() + amount);
        persistLivestockProgress();
    }

    /** Pousse immédiatement nutrition/hydratation dans le SavedData (évite un écrasement au prochain tick). */
    private void persistLivestockProgress() {
        if (!this.level.isClientSide && this.level instanceof ServerLevel) {
            LivestockProgressManager.persistFromEntity(this);
        }
    }
    
    public AnimalNutritionStatus getNutritionStatus() {
        int nutrition = getNutrition();
        if (nutrition < 30) return AnimalNutritionStatus.STARVING;
        if (nutrition < 70) return AnimalNutritionStatus.FED;
        return AnimalNutritionStatus.WELL_FED;
    }
    
    public AnimalHydrationStatus getHydrationStatus() {
        int hydration = getHydration();
        if (hydration < 30) return AnimalHydrationStatus.THIRSTY;
        if (hydration < 70) return AnimalHydrationStatus.HYDRATED;
        return AnimalHydrationStatus.WELL_HYDRATED;
    }
    
    /**
     * Éligibilité de base (démarrer / rester apparié) : seuils min config.
     * La progression des jauges exige en plus {@link MobConfig#getSustainNutritionPercent()}.
     */
    public boolean canReproduce() {
        int minNutrition = MobConfig.getMinNutritionPercent();
        int minHydration = MobConfig.getMinHydrationPercent();
        return getNutrition() >= minNutrition && getHydration() >= minHydration;
    }

    /** Accouplement / grossesse peuvent avancer (nutrition sustain + soif min). */
    public boolean canSustainReproductionProgress() {
        return getNutrition() >= MobConfig.getSustainNutritionPercent()
                && getHydration() >= MobConfig.getMinHydrationPercent();
    }

    public void resetReproductionTimer() {
        this.reproductionProgressMs = 0L;
        if (this.level instanceof ServerLevel serverLevel && !this.level.isClientSide) {
            LivestockProgressManager.resetReproduction(this.getUUID(), serverLevel);
        }
    }
    public boolean canExtractMilk() {
        if (isMale()) {
            return false;
        }
        // Vérifier si c'est une vache sera fait dans la classe CustomCow
        long currentTime = System.currentTimeMillis();
        long interval = MobConfig.getMilkExtractionIntervalMs();
        
        return (currentTime - lastMilkExtraction) >= interval;
    }
    
    public void setLastMilkExtraction(long time) {
        this.lastMilkExtraction = time;
    }
    
    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHitIn) {
        // Viande / peaux élevage : gérées par Skript (job/activité/chasse/chasse.sk).
        // Ne pas spawnAtLocation ici — ça double les drops (Skript clear drops + loot mod).
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsMale", isMale());
        tag.putInt("Nutrition", getNutrition());
        tag.putInt("Hydration", getHydration());
        tag.putLong("ReproductionProgressMs", this.reproductionProgressMs);
        tag.putLong("PregnancyProgressMs", this.pregnancyProgressMs);
        tag.putBoolean("Pregnant", this.pregnant);
        tag.putLong("LastNutritionDecrease", lastNutritionDecrease);
        tag.putLong("LastHydrationDecrease", lastHydrationDecrease);
        tag.putLong("LastMilkExtraction", lastMilkExtraction);
        tag.putLong("LastRegenerationTime", lastRegenerationTime);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("IsMale")) {
            setMale(tag.getBoolean("IsMale"));
        }
        if (tag.contains("Nutrition")) {
            setNutrition(tag.getInt("Nutrition"));
        }
        if (tag.contains("Hydration")) {
            setHydration(tag.getInt("Hydration"));
        }

        long currentTime = System.currentTimeMillis();

        if (tag.contains("ReproductionProgressMs")) {
            this.reproductionProgressMs = Math.max(0L, tag.getLong("ReproductionProgressMs"));
        } else if (tag.contains("ReproductionStartTime")) {
            long legacyStart = tag.getLong("ReproductionStartTime");
            if (legacyStart > 0L) {
                long requiredMs = MobConfig.getRequiredTimeMinutes() * 60_000L;
                this.reproductionProgressMs = Math.min(requiredMs, Math.max(0L, currentTime - legacyStart));
            }
        }
        if (tag.contains("PregnancyProgressMs")) {
            this.pregnancyProgressMs = Math.max(0L, tag.getLong("PregnancyProgressMs"));
        }
        if (tag.contains("Pregnant")) {
            this.pregnant = tag.getBoolean("Pregnant");
        } else {
            this.pregnant = false;
            this.pregnancyProgressMs = 0L;
        }

        if (tag.contains("LastNutritionDecrease")) {
            lastNutritionDecrease = tag.getLong("LastNutritionDecrease");
        } else {
            lastNutritionDecrease = currentTime;
        }

        if (tag.contains("LastHydrationDecrease")) {
            lastHydrationDecrease = tag.getLong("LastHydrationDecrease");
        } else {
            lastHydrationDecrease = currentTime;
        }
        
        if (tag.contains("LastMilkExtraction")) {
            lastMilkExtraction = tag.getLong("LastMilkExtraction");
        } else {
            lastMilkExtraction = currentTime;
        }
        
        if (tag.contains("LastRegenerationTime")) {
            lastRegenerationTime = tag.getLong("LastRegenerationTime");
            if (lastRegenerationTime <= 0 || lastRegenerationTime > currentTime) {
                lastRegenerationTime = currentTime;
            }
        } else {
            lastRegenerationTime = currentTime;
        }
    }
    
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void checkDespawn() {
        // Élevage : jamais de despawn vanilla (distance / paisible).
    }
    
    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        // Sera implémenté dans les classes enfants
        return null;
    }
    
    // Méthodes GeckoLib
    // Builders d'animation mis en cache (immuables, partagés) : évite une allocation par frame.
    private static final AnimationBuilder ANIM_DEAD = new AnimationBuilder().addAnimation("mort", false);
    private static final AnimationBuilder ANIM_WALK = new AnimationBuilder().addAnimation("marche", true);
    private static final AnimationBuilder ANIM_IDLE = new AnimationBuilder().addAnimation("idle", true);

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 5, this::predicate));
    }
    
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        CustomAnimal animal = (CustomAnimal) event.getAnimatable();
        AnimationController<?> controller = event.getController();
        
        if (animal.isDeadOrDying() || animal.getHealth() <= 0) {
            controller.markNeedsReload();
            controller.setAnimation(ANIM_DEAD);
            return PlayState.CONTINUE;
        }
        
        if (animal.shouldPlayLeadWalkAnimation()) {
            controller.setAnimation(ANIM_WALK);
            return PlayState.CONTINUE;
        }
        
        controller.setAnimation(ANIM_IDLE);
        return PlayState.CONTINUE;
    }
    
    protected boolean shouldPlayLeadWalkAnimation() {
        if (!this.isLeashed()) {
            return false;
        }
        double deltaX = Math.abs(this.getX() - this.xOld);
        double deltaZ = Math.abs(this.getZ() - this.zOld);
        double horizontalMovement = this.getDeltaMovement().horizontalDistanceSqr();
        if ((deltaX > POSITION_THRESHOLD || deltaZ > POSITION_THRESHOLD) || horizontalMovement > VELOCITY_THRESHOLD) {
            return this.getLeashHolder() != null;
        }
        return false;
    }
    
    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
    
    /**
     * Retourne le nom de la ressource du modèle GeckoLib (sans extension)
     * Exemple : "porc", "vache", "poule"
     */
    public abstract String getModelResourceName();
    
    /**
     * Retourne le nom de la ressource de la texture (sans extension)
     * Exemple : "texture_porc_retexture", "vache", "texture"
     */
    public abstract String getTextureResourceName();
}

