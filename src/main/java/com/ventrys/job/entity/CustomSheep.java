package com.ventrys.job.entity;

import com.ventrys.job.data.MobConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CustomSheep extends CustomAnimal implements IForgeShearable, LivestockTextureHolder {
    /** Apport laine par tonte (nerfé : 5 → 2). */
    public static final int WOOL_DROP_COUNT = 2;
    public static final long WOOL_REGROW_INTERVAL_MS = 86_400_000L; // 24 heures
    public static final String DEFAULT_TEXTURE = "mouton_parfais";

    private static final List<String> TEXTURE_VARIANTS = List.of(
        "mouton_parfais"
    );

    public static List<String> getTextureVariantIds() {
        return TEXTURE_VARIANTS;
    }

    private static final ResourceLocation WOOL_ITEM = new ResourceLocation("ventrysitem", "res_laine_blanche");

    // defineId sur CustomSheep.class (pattern vanilla) — ne pas partager le compteur
    // CustomAnimal.class avec vache/poule : ca decalait les IDs selon l'ordre de chargement
    // des classes et cassait la sync client (sexe reste au defaut "male").
    private static final EntityDataAccessor<Boolean> HAS_WOOL =
        SynchedEntityData.defineId(CustomSheep.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> TEXTURE_VARIANT =
        SynchedEntityData.defineId(CustomSheep.class, EntityDataSerializers.STRING);
    /** Modèle GeckoLib synchronisé (mouton / mouton_sans_laine) — évite un client désynchronisé. */
    private static final EntityDataAccessor<String> GEO_MODEL =
        SynchedEntityData.defineId(CustomSheep.class, EntityDataSerializers.STRING);
    /** Minutes avant repousse (0 = laine prête / présente). -2 = trop affamé pour repousser. */
    private static final EntityDataAccessor<Integer> WOOL_READY_IN_MIN =
        SynchedEntityData.defineId(CustomSheep.class, EntityDataSerializers.INT);

    private long woolRegrowStartTime;

    public CustomSheep(EntityType<? extends CustomAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HAS_WOOL, true);
        this.entityData.define(TEXTURE_VARIANT, DEFAULT_TEXTURE);
        this.entityData.define(GEO_MODEL, "mouton");
        this.entityData.define(WOOL_READY_IN_MIN, 0);
    }

    private void syncGeoModel() {
        this.entityData.set(GEO_MODEL, this.hasWool() ? "mouton" : "mouton_sans_laine");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level.isClientSide && (HAS_WOOL.equals(key) || GEO_MODEL.equals(key) || TEXTURE_VARIANT.equals(key))) {
            var data = this.getFactory().getOrCreateAnimationData(this.getId());
            data.getAnimationControllers().values().forEach(controller -> controller.markNeedsReload());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            return;
        }
        if (!this.hasWool() && this.woolRegrowStartTime > 0L && this.canRegrowWool()) {
            if (System.currentTimeMillis() - this.woolRegrowStartTime >= WOOL_REGROW_INTERVAL_MS) {
                this.setHasWool(true);
                this.woolRegrowStartTime = 0L;
                this.refreshDimensions();
            }
        }
        if (this.tickCount % 20 == 0) {
            syncWoolHud();
        }
    }

    /** Même seuils que reproduction / production (vache, poule). */
    private boolean canRegrowWool() {
        return getNutrition() >= MobConfig.getMinNutritionPercent()
            && getHydration() >= MobConfig.getMinHydrationPercent();
    }

    private void syncWoolHud() {
        if (this.hasWool()) {
            this.entityData.set(WOOL_READY_IN_MIN, 0);
            return;
        }
        if (!this.canRegrowWool()) {
            this.entityData.set(WOOL_READY_IN_MIN, -2);
            return;
        }
        long remaining = getWoolRegrowRemainingMs();
        int minutes = remaining <= 0L ? 0 : (int) Math.ceil(remaining / 60_000.0);
        this.entityData.set(WOOL_READY_IN_MIN, minutes);
    }

    public long getWoolRegrowRemainingMs() {
        if (this.hasWool()) {
            return 0L;
        }
        if (this.woolRegrowStartTime <= 0L) {
            return WOOL_REGROW_INTERVAL_MS;
        }
        return Math.max(0L, WOOL_REGROW_INTERVAL_MS - (System.currentTimeMillis() - this.woolRegrowStartTime));
    }

    /** 0 = laine prête, &gt;0 = minutes, -2 = trop affamé/assoiffé. */
    public int getWoolReadyInMinutes() {
        return this.entityData.get(WOOL_READY_IN_MIN);
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
            this.setHasWool(true);
            this.woolRegrowStartTime = 0L;
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    public boolean hasWool() {
        return this.entityData.get(HAS_WOOL);
    }

    public void setHasWool(boolean hasWool) {
        this.entityData.set(HAS_WOOL, hasWool);
        syncGeoModel();
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
    public static ResourceLocation toTextureLocation(@Nonnull String texture) {
        return LivestockTextureVariants.toLocation("sheep", normalizeTexture(texture));
    }

    @Override
    public String getModelResourceName() {
        String model = this.entityData.get(GEO_MODEL);
        if (model == null || model.isBlank()) {
            return this.hasWool() ? "mouton" : "mouton_sans_laine";
        }
        return model;
    }

    @Override
    public String getTextureResourceName() {
        return this.getTextureVariant();
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == Items.SHEARS && this.hasWool()) {
            if (!this.level.isClientSide) {
                this.onSheared(player, held, this.level, this.blockPosition(), 0)
                    .forEach(this::spawnAtLocation);
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }
        // Blé / seau d'eau / reproduction : gérés par AnimalInteractionHandler
        return InteractionResult.PASS;
    }

    @Override
    public boolean isShearable(@Nonnull ItemStack item, Level level, BlockPos pos) {
        return this.hasWool();
    }

    @Override
    public List<ItemStack> onSheared(@Nullable Player player, @Nonnull ItemStack item, Level level, BlockPos pos, int fortune) {
        if (this.level.isClientSide || !this.hasWool()) {
            return Collections.emptyList();
        }
        ItemStack wool = this.createWoolStack();
        this.setHasWool(false);
        this.woolRegrowStartTime = System.currentTimeMillis();
        this.refreshDimensions();
        this.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
        if (player != null && !player.getAbilities().instabuild) {
            item.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
        return Collections.singletonList(wool);
    }

    private ItemStack createWoolStack() {
        Item item = ForgeRegistries.ITEMS.getValue(WOOL_ITEM);
        if (item == null || item == Items.AIR) {
            return new ItemStack(Items.WHITE_WOOL, WOOL_DROP_COUNT);
        }
        return new ItemStack(item, WOOL_DROP_COUNT);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        // Viande mouton : Skript chasse.sk. On ne droppe que la laine (hors table Skript).
        if (this.hasWool()) {
            this.spawnAtLocation(this.createWoolStack());
        }
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        if (otherParent instanceof CustomSheep mate && this.canReproduce() && mate.canReproduce()) {
            if (this.isMale() != mate.isMale()) {
                @SuppressWarnings("unchecked")
                EntityType<? extends CustomAnimal> entityType = (EntityType<? extends CustomAnimal>) this.getType();
                CustomSheep offspring = new CustomSheep(entityType, level);
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
        tag.putBoolean("HasWool", this.hasWool());
        tag.putString("TextureVariant", this.getTextureVariant());
        tag.putString("GeoModel", this.getModelResourceName());
        tag.putLong("WoolRegrowStartTime", this.woolRegrowStartTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HasWool")) {
            this.setHasWool(tag.getBoolean("HasWool"));
        }
        if (tag.contains("TextureVariant")) {
            this.setTextureVariant(tag.getString("TextureVariant"));
        } else if (!this.level.isClientSide) {
            this.setTextureVariant(randomTexture(this.random));
        }
        if (tag.contains("GeoModel")) {
            this.entityData.set(GEO_MODEL, tag.getString("GeoModel"));
        }
        if (tag.contains("WoolRegrowStartTime")) {
            this.woolRegrowStartTime = tag.getLong("WoolRegrowStartTime");
        }
        if (!tag.contains("GeoModel")) {
            syncGeoModel();
        }
        // Re-applique le sexe après les champs mouton (évite toute réécriture accidentelle).
        if (tag.contains("IsMale")) {
            setMale(tag.getBoolean("IsMale"));
        }
    }
}
