package com.ventrys.job.entity;

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
    private long lastNutritionDecrease = 0; // Timestamp en millisecondes
    private long lastHydrationDecrease = 0; // Timestamp en millisecondes
    private long lastMilkExtraction = 0; // Timestamp en millisecondes (pour les vaches)
    private long reproductionStartTime = 0L; // Timestamp en millisecondes
    private long lastRegenerationTime = 0; // Timestamp en millisecondes pour la régénération
    private long lastReproductionCheck = 0; // Timestamp en millisecondes pour la vérification automatique de reproduction
    
    // Intervalles en millisecondes (indépendants des ticks du monde)
    private static final long NUTRITION_DECREASE_INTERVAL_MS = 1_200_000L; // 20 minutes = 1 200 000 ms
    private static final long HYDRATION_DECREASE_INTERVAL_MS = 600_000L; // 10 minutes = 600 000 ms
    private static final long REGENERATION_INTERVAL_MS = 300_000L; // 5 minutes = 300 000 ms (pour test, normalement 1h = 3 600 000 ms)
    private static final long REPRODUCTION_CHECK_INTERVAL_MS = 5_000L; // Vérifier la reproduction toutes les 5 secondes
    
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
            this.lastReproductionCheck = currentTime;
            this.reproductionStartTime = 0L;
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
        
        if (!this.level.isClientSide) {
            long currentTime = System.currentTimeMillis();
            
            // Initialiser les timestamps si c'est la première fois (pour les animaux chargés)
            if (lastNutritionDecrease == 0) {
                lastNutritionDecrease = currentTime;
            }
            if (lastHydrationDecrease == 0) {
                lastHydrationDecrease = currentTime;
            }
            if (lastRegenerationTime == 0) {
                lastRegenerationTime = currentTime;
            }
            if (lastReproductionCheck == 0) {
                lastReproductionCheck = currentTime;
            }
            
            // Calculer les diminutions de nutrition (avec protection contre les désynchronisations)
            long timeSinceLastNutrition = currentTime - lastNutritionDecrease;
            if (timeSinceLastNutrition < 0) {
                // Timestamp invalide (crash/redémarrage), réinitialiser
                lastNutritionDecrease = currentTime;
            } else if (timeSinceLastNutrition >= NUTRITION_DECREASE_INTERVAL_MS) {
                // Limiter les diminutions pour éviter les pertes massives après un crash
                long maxAllowedTime = NUTRITION_DECREASE_INTERVAL_MS * 10; // Max 10 intervalles
                if (timeSinceLastNutrition > maxAllowedTime) {
                    timeSinceLastNutrition = maxAllowedTime;
                    lastNutritionDecrease = currentTime - maxAllowedTime;
                }
                
                // Calculer combien de diminutions ont dû se produire
                int decreases = (int) (timeSinceLastNutrition / NUTRITION_DECREASE_INTERVAL_MS);
                decreaseNutrition(decreases);
                // Ajuster le timestamp pour éviter les accumulations
                lastNutritionDecrease = currentTime - (timeSinceLastNutrition % NUTRITION_DECREASE_INTERVAL_MS);
            }
            
            // Calculer les diminutions d'hydratation (avec protection contre les désynchronisations)
            long timeSinceLastHydration = currentTime - lastHydrationDecrease;
            if (timeSinceLastHydration < 0) {
                // Timestamp invalide (crash/redémarrage), réinitialiser
                lastHydrationDecrease = currentTime;
            } else if (timeSinceLastHydration >= HYDRATION_DECREASE_INTERVAL_MS) {
                // Limiter les diminutions pour éviter les pertes massives après un crash
                long maxAllowedTime = HYDRATION_DECREASE_INTERVAL_MS * 10; // Max 10 intervalles
                if (timeSinceLastHydration > maxAllowedTime) {
                    timeSinceLastHydration = maxAllowedTime;
                    lastHydrationDecrease = currentTime - maxAllowedTime;
                }
                
                // Calculer combien de diminutions ont dû se produire
                int decreases = (int) (timeSinceLastHydration / HYDRATION_DECREASE_INTERVAL_MS);
                decreaseHydration(decreases);
                // Ajuster le timestamp pour éviter les accumulations
                lastHydrationDecrease = currentTime - (timeSinceLastHydration % HYDRATION_DECREASE_INTERVAL_MS);
            }
            
            // Régénération lente si nutrition >= 50% et hydratation >= 50%
            if (getNutrition() >= 50 && getHydration() >= 50) {
                long timeSinceLastRegeneration = currentTime - lastRegenerationTime;
                // Protection contre les timestamps invalides
                if (timeSinceLastRegeneration < 0) {
                    lastRegenerationTime = currentTime;
                } else if (timeSinceLastRegeneration >= REGENERATION_INTERVAL_MS) {
                    // Régénérer 1 coeur (2 points de vie)
                    float currentHealth = this.getHealth();
                    float maxHealth = this.getMaxHealth();
                    if (currentHealth < maxHealth) {
                        this.heal(2.0f);
                        // Ajuster le timestamp
                        lastRegenerationTime = currentTime - (timeSinceLastRegeneration % REGENERATION_INTERVAL_MS);
                    } else {
                        // Si déjà à vie max, réinitialiser le timer
                        lastRegenerationTime = currentTime;
                    }
                }
            } else {
                // Si les conditions ne sont plus remplies, réinitialiser le timer de régénération
                if (lastRegenerationTime != 0) {
                    lastRegenerationTime = currentTime;
                }
            }
            
            // Les animaux meurent de faim/soif si leurs stats atteignent 0
            if (getNutrition() <= 0 || getHydration() <= 0) {
                this.hurt(net.minecraft.world.damagesource.DamageSource.STARVE, Float.MAX_VALUE);
            }
            
            // Vérifier les conditions de reproduction
            checkReproductionConditions();
            
            // Reproduction automatique : vérifier périodiquement si l'animal peut se reproduire
            long timeSinceLastReproductionCheck = currentTime - lastReproductionCheck;
            if (timeSinceLastReproductionCheck >= REPRODUCTION_CHECK_INTERVAL_MS) {
                // Toujours vérifier la reproduction (même si les conditions ne sont pas remplies,
                // pour réinitialiser le timer si nécessaire)
                attemptAutomaticReproduction();
                lastReproductionCheck = currentTime;
            }
        }
    }
    
    /**
     * Tente une reproduction automatique avec un partenaire à proximité
     * Le timer ne démarre QUE si les 2 mobs sont à proximité ET remplissent les conditions
     */
    private void attemptAutomaticReproduction() {
        if (this.level.isClientSide || !(this.level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        
        // Vérifier d'abord que cet animal remplit les conditions de base
        if (!this.canReproduce() || this.isDeadOrDying() || !this.isAlive()) {
            // Si les conditions ne sont plus remplies, réinitialiser le timer
            this.reproductionStartTime = 0L;
            return;
        }
        
        // Chercher un partenaire dans un rayon de 10 blocs
        double radius = 10.0D;
        java.util.List<net.minecraft.world.entity.animal.Animal> nearbyAnimals = this.level.getEntitiesOfClass(
            net.minecraft.world.entity.animal.Animal.class,
            this.getBoundingBox().inflate(radius),
            entity -> entity != this &&
                     entity.getClass() == this.getClass() &&
                     entity instanceof CustomAnimal &&
                     !entity.isDeadOrDying() &&
                     entity.isAlive()
        );
        
        // Chercher un partenaire valide
        CustomAnimal validMate = null;
        for (net.minecraft.world.entity.animal.Animal potentialMate : nearbyAnimals) {
            if (potentialMate instanceof CustomAnimal customMate) {
                // Vérifier que le partenaire peut aussi se reproduire
                if (!customMate.canReproduce()) {
                    continue;
                }
                
                // Vérifier que les sexes sont opposés
                if (this.isMale() == customMate.isMale()) {
                    continue;
                }
                
                // Partenaire valide trouvé
                validMate = customMate;
                break;
            }
        }
        
        // Si aucun partenaire valide n'est trouvé, réinitialiser le timer
        if (validMate == null) {
            this.reproductionStartTime = 0L;
            return;
        }
        
        // Vérifier si le timer de reproduction est prêt
        if (this.isReproductionTimerReady(validMate)) {
            // Conditions remplies : procéder à la reproduction
            net.minecraft.world.entity.AgeableMob offspring = this.getBreedOffspring(serverLevel, validMate);
            if (offspring != null) {
                offspring.setAge(-24000); // Bébé
                // Placer le bébé entre les deux parents
                double midX = (this.getX() + validMate.getX()) / 2.0;
                double midY = Math.max(this.getY(), validMate.getY());
                double midZ = (this.getZ() + validMate.getZ()) / 2.0;
                offspring.moveTo(midX, midY, midZ, 0.0F, 0.0F);
                this.level.addFreshEntity(offspring);
                
                // Réinitialiser les timers de reproduction pour les deux parents
                this.resetReproductionTimer();
                validMate.resetReproductionTimer();
            }
        }
        // Si le timer n'est pas encore prêt mais qu'un partenaire valide existe,
        // le timer continuera à s'écouler (déjà démarré dans isReproductionTimerReady)
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
    
    private void decreaseNutrition(int amount) {
        setNutrition(getNutrition() - amount);
    }
    
    private void decreaseHydration(int amount) {
        setHydration(getHydration() - amount);
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
     * Vérifie si cet animal peut se reproduire (conditions de base uniquement)
     * Le timer de reproduction est géré dans attemptAutomaticReproduction()
     */
    public boolean canReproduce() {
        int minNutrition = MobConfig.getMinNutritionPercent();
        int minHydration = MobConfig.getMinHydrationPercent();
        
        // Vérifier uniquement les conditions de base (nutrition et hydratation)
        return getNutrition() >= minNutrition && getHydration() >= minHydration;
    }
    
    /**
     * Vérifie si le timer de reproduction avec un partenaire spécifique est terminé
     * @param partner Le partenaire potentiel
     * @return true si le timer est terminé et la reproduction peut avoir lieu
     */
    private boolean isReproductionTimerReady(CustomAnimal partner) {
        if (partner == null || partner == this) {
            return false;
        }
        
        // Vérifier que les deux remplissent toujours les conditions
        if (!this.canReproduce() || !partner.canReproduce()) {
            return false;
        }
        
        // Vérifier que les sexes sont opposés
        if (this.isMale() == partner.isMale()) {
            return false;
        }
        
        // Si le timer n'a pas encore démarré, le démarrer maintenant
        long currentTime = System.currentTimeMillis();
        if (this.reproductionStartTime == 0L) {
            this.reproductionStartTime = currentTime;
            return false;
        }
        
        // Vérifier la robustesse : si le temps est anormal, réinitialiser
        long elapsed = currentTime - this.reproductionStartTime;
        if (elapsed < 0) {
            // Temps négatif = problème de synchronisation, réinitialiser
            this.reproductionStartTime = currentTime;
            return false;
        }
        
        // Protection contre les valeurs anormalement grandes (crash/redémarrage)
        int requiredTime = MobConfig.getRequiredTimeMinutes();
        long requiredMs = requiredTime * 60_000L;
        if (elapsed > requiredMs * 10) {
            // Temps anormalement grand, réinitialiser
            this.reproductionStartTime = currentTime;
            return false;
        }
        
        // Vérifier si le temps requis est écoulé
        return elapsed >= requiredMs;
    }
    
    private void checkReproductionConditions() {
        // Si les conditions ne sont plus remplies, réinitialiser le compteur
        // Cette vérification garantit la robustesse : si les conditions changent, le processus s'arrête
        if (this.reproductionStartTime != 0L) {
            int minNutrition = MobConfig.getMinNutritionPercent();
            int minHydration = MobConfig.getMinHydrationPercent();
            
            // Si les conditions ne sont plus remplies, réinitialiser le timer
            if (getNutrition() < minNutrition || getHydration() < minHydration) {
                this.reproductionStartTime = 0L;
            }
            // Vérifier aussi si l'animal est mort
            else if (this.isDeadOrDying() || !this.isAlive()) {
                this.reproductionStartTime = 0L;
            }
        }
    }
    
    public void resetReproductionTimer() {
        this.reproductionStartTime = 0L;
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
        tag.putLong("ReproductionStartTime", this.reproductionStartTime);
        tag.putLong("LastNutritionDecrease", lastNutritionDecrease);
        tag.putLong("LastHydrationDecrease", lastHydrationDecrease);
        tag.putLong("LastMilkExtraction", lastMilkExtraction);
        tag.putLong("LastRegenerationTime", lastRegenerationTime);
        tag.putLong("LastReproductionCheck", lastReproductionCheck);
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
        if (tag.contains("ReproductionStartTime")) {
            this.reproductionStartTime = tag.getLong("ReproductionStartTime");
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Charger et calculer les diminutions accumulées pour la nutrition
        if (tag.contains("LastNutritionDecrease")) {
            long savedNutritionTime = tag.getLong("LastNutritionDecrease");
            long timeSinceLastNutrition = currentTime - savedNutritionTime;

            if (timeSinceLastNutrition < 0) {
                lastNutritionDecrease = currentTime;
            } else if (timeSinceLastNutrition >= NUTRITION_DECREASE_INTERVAL_MS) {
                long maxAllowedTime = NUTRITION_DECREASE_INTERVAL_MS * 10L;
                if (timeSinceLastNutrition > maxAllowedTime) {
                    timeSinceLastNutrition = maxAllowedTime;
                    lastNutritionDecrease = currentTime - maxAllowedTime;
                }
                int decreases = (int) (timeSinceLastNutrition / NUTRITION_DECREASE_INTERVAL_MS);
                decreaseNutrition(decreases);
                lastNutritionDecrease = currentTime - (timeSinceLastNutrition % NUTRITION_DECREASE_INTERVAL_MS);
            } else {
                lastNutritionDecrease = savedNutritionTime;
            }
        } else {
            lastNutritionDecrease = currentTime;
        }

        // Charger et calculer les diminutions accumulées pour l'hydratation
        if (tag.contains("LastHydrationDecrease")) {
            long savedHydrationTime = tag.getLong("LastHydrationDecrease");
            long timeSinceLastHydration = currentTime - savedHydrationTime;

            if (timeSinceLastHydration < 0) {
                lastHydrationDecrease = currentTime;
            } else if (timeSinceLastHydration >= HYDRATION_DECREASE_INTERVAL_MS) {
                long maxAllowedTime = HYDRATION_DECREASE_INTERVAL_MS * 10L;
                if (timeSinceLastHydration > maxAllowedTime) {
                    timeSinceLastHydration = maxAllowedTime;
                    lastHydrationDecrease = currentTime - maxAllowedTime;
                }
                int decreases = (int) (timeSinceLastHydration / HYDRATION_DECREASE_INTERVAL_MS);
                decreaseHydration(decreases);
                lastHydrationDecrease = currentTime - (timeSinceLastHydration % HYDRATION_DECREASE_INTERVAL_MS);
            } else {
                lastHydrationDecrease = savedHydrationTime;
            }
        } else {
            lastHydrationDecrease = currentTime;
        }
        
        if (tag.contains("LastMilkExtraction")) {
            lastMilkExtraction = tag.getLong("LastMilkExtraction");
        } else {
            lastMilkExtraction = currentTime;
        }
        
        // Charger le timestamp de régénération
        if (tag.contains("LastRegenerationTime")) {
            lastRegenerationTime = tag.getLong("LastRegenerationTime");
            // Vérifier la validité du timestamp
            if (lastRegenerationTime <= 0 || lastRegenerationTime > currentTime) {
                lastRegenerationTime = currentTime;
            }
        } else {
            lastRegenerationTime = currentTime;
        }
        
        // Charger le timestamp de vérification de reproduction
        if (tag.contains("LastReproductionCheck")) {
            lastReproductionCheck = tag.getLong("LastReproductionCheck");
            // Vérifier la validité du timestamp
            if (lastReproductionCheck <= 0 || lastReproductionCheck > currentTime) {
                lastReproductionCheck = currentTime;
            }
        } else {
            lastReproductionCheck = currentTime;
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

