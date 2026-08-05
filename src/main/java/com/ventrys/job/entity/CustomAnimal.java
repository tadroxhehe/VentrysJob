package com.ventrys.job.entity;

import com.ventrys.job.data.LivestockProgressManager;
import com.ventrys.job.data.LivestockProgressSavedData;
import com.ventrys.job.data.MobConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.Optional;
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
    
    // Données synchronisées
    private static final EntityDataAccessor<Boolean> IS_MALE = SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> NUTRITION = SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HYDRATION = SynchedEntityData.defineId(CustomAnimal.class, EntityDataSerializers.INT);
    // Utiliser un CompoundTag pour stocker les données longues non synchronisées
    
    // Données non synchronisées (gérées côté serveur) - Utilisation de timestamps réels (ms)
    private long lastNutritionDecrease = 0;
    private long lastHydrationDecrease = 0;
    private long lastMilkExtraction = 0;
    private long reproductionProgressMs = 0L;
    private long lastRegenerationTime = 0;
    
    protected CustomAnimal(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        if (!level.isClientSide) {
            // Initialiser aléatoirement le sexe
            this.entityData.set(IS_MALE, new Random().nextBoolean());
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
        }
    }
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_MALE, true);
        this.entityData.define(NUTRITION, 100);
        this.entityData.define(HYDRATION, 100);
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
        }
    }

    public long getReproductionProgressMs() {
        return reproductionProgressMs;
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
        setNutrition(entry.nutrition);
        setHydration(entry.hydration);
        this.reproductionProgressMs = entry.reproductionProgressMs;
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

    public boolean isReproductionProgressComplete() {
        long requiredMs = MobConfig.getRequiredTimeMinutes() * 60_000L;
        return reproductionProgressMs >= requiredMs;
    }

    /** Les deux parents doivent avoir accumulé le délai configuré (ex. 48 h). */
    public boolean isReproductionReadyWith(CustomAnimal mate) {
        if (mate == null || mate == this) {
            return false;
        }
        return canReproduce()
            && mate.canReproduce()
            && isMale() != mate.isMale()
            && isReproductionProgressComplete()
            && mate.isReproductionProgressComplete();
    }
    
    public boolean isMale() {
        return this.entityData.get(IS_MALE);
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
    }
    
    public void addHydration(int amount) {
        setHydration(getHydration() + amount);
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
     * Vérifie si cet animal peut accumuler du temps de reproduction (faim/soif au-dessus du seuil).
     */
    public boolean canReproduce() {
        int minNutrition = MobConfig.getMinNutritionPercent();
        int minHydration = MobConfig.getMinHydrationPercent();
        return getNutrition() >= minNutrition && getHydration() >= minHydration;
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
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);
        
        ResourceLocation entityKey = ForgeRegistries.ENTITIES.getKey(this.getType());
        if (entityKey == null) {
            return;
        }
        String animalId = entityKey.toString();
        Optional<MobConfig.AnimalConfig> cfg = MobConfig.getAnimalConfig(animalId);
        if (cfg.isEmpty()) {
            com.ventrys.job.VentrysJob.LOGGER.debug(
                "MobConfig: aucune entrée pour l'entité {} — les ids dans mobs_config.json doivent correspondre (ex. ventrysjob:custom_cow)",
                animalId);
            return;
        }
        cfg.ifPresent(config -> {
            Random random = new Random();
            for (MobConfig.DropConfig drop : config.drops()) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(drop.itemId()));
                if (item != null) {
                    int count = drop.minCount() + random.nextInt(drop.maxCount() - drop.minCount() + 1);
                    this.spawnAtLocation(new ItemStack(item, count));
                } else {
                    com.ventrys.job.VentrysJob.LOGGER.warn(
                        "MobConfig: item de drop introuvable {} pour {}", drop.itemId(), animalId);
                }
            }
        });
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsMale", isMale());
        tag.putInt("Nutrition", getNutrition());
        tag.putInt("Hydration", getHydration());
        tag.putLong("ReproductionProgressMs", this.reproductionProgressMs);
        tag.putLong("LastNutritionDecrease", lastNutritionDecrease);
        tag.putLong("LastHydrationDecrease", lastHydrationDecrease);
        tag.putLong("LastMilkExtraction", lastMilkExtraction);
        tag.putLong("LastRegenerationTime", lastRegenerationTime);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("IsMale")) {
            this.entityData.set(IS_MALE, tag.getBoolean("IsMale"));
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
        return false; // Ne jamais supprimer automatiquement
    }
    
    @Override
    public boolean requiresCustomPersistence() {
        return true; // Toujours persister
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

