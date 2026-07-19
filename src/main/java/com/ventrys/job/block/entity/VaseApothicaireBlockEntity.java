package com.ventrys.job.block.entity;

import com.ventrys.job.energy.JobActionEnergyCosts;
import com.ventrys.job.energy.JobEnergyHelper;
import com.ventrys.job.audio.PositionalSounds;
import com.ventrys.job.init.ModBlockEntities;
import com.ventrys.job.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

public class VaseApothicaireBlockEntity extends BlockEntity {
    
    // Configuration des items supportés pour la plantation
    private static final Set<String> SUPPORTED_PLANT_ITEMS = new HashSet<>();
    
    static {
        initializeSupportedItems();
    }
    
    // État du vase
    private String plantedItemId = null;
    private boolean isWatered = false;
    private long plantedTime = 0; // Timestamp Unix en millisecondes
    private long growthTimeMs = 10 * 1000; // 10 secondes en millisecondes (TEST)
    /** Aligné sur {@link com.ventrys.job.block.VaseApothicaireBlock#GROWTH_STAGE} : 0 = base, 1 = mature. */
    private int currentStage = 0;
    private long lastStageCheckTimeMs = 0; // Limite les recalculs de stage (anti jitter perf)
    
    public VaseApothicaireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VASE_APOTHICAIRE.get(), pos, state);
    }
    
    private static void initializeSupportedItems() {
        // Configuration des items supportés pour la plantation
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_tournesol");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_sauge_du_nord");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_verveine_noir");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_soucis_des_mers");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_racine_dorage");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_ronce_pourpre");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_plantin");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_menthe_des_collines");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_lierre_mortel");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_iris_cendre");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_fleur_de_lys");
        SUPPORTED_PLANT_ITEMS.add("ventrysitem:res_feuille_de_lune");
    }
    
    public static void addSupportedItem(String itemId) {
        SUPPORTED_PLANT_ITEMS.add(itemId);
    }
    
    public static void removeSupportedItem(String itemId) {
        SUPPORTED_PLANT_ITEMS.remove(itemId);
    }
    
    public static boolean isItemSupported(String itemId) {
        return SUPPORTED_PLANT_ITEMS.contains(itemId);
    }
    
    public InteractionResult handleWatering(Player player, ItemStack waterBucket) {
        if (plantedItemId == null) {
            sendMessage(player, "ventrysjob.message.vase.empty");
            return InteractionResult.FAIL;
        }
        
        if (isWatered) {
            sendMessage(player, "ventrysjob.message.vase.already_watered");
            return InteractionResult.FAIL;
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && !JobEnergyHelper.consumeForAction(serverPlayer, JobActionEnergyCosts.VASE_WATER)) {
            return InteractionResult.FAIL;
        }
        
        // Arroser le vase
        isWatered = true;
        plantedTime = System.currentTimeMillis(); // Timestamp Unix
        currentStage = 0; // Texture base pendant la croissance ; passe à 1 à maturité
        lastStageCheckTimeMs = plantedTime;
        updateBlockState();
        setChanged();
        
        // Consommer le seau d'eau et donner un seau vide
        waterBucket.shrink(1);
        if (!player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.BUCKET))) {
            player.drop(new ItemStack(net.minecraft.world.item.Items.BUCKET), false);
        }
        
        sendMessage(player, "ventrysjob.message.vase.watered");

        if (this.level instanceof ServerLevel serverLevel) {
            PositionalSounds.playNearBlock(serverLevel, worldPosition, SoundEvents.BUCKET_EMPTY,
                PositionalSounds.VASE_SOUND_MAX_DISTANCE, 1.0f, 1.0f);
        }

        return InteractionResult.SUCCESS;
    }
    
    public InteractionResult handlePlantingOrHarvest(Player player, ItemStack heldItem) {
        // Vérifier si on peut récolter (priorité à la récolte)
        if (canHarvest()) {
            return harvestPlant(player);
        }
        
        // Si le vase a déjà une plante mais n'est pas prête à récolter
        if (plantedItemId != null) {
            sendMessage(player, "ventrysjob.message.vase.already_planted");
            return InteractionResult.FAIL;
        }
        
        // Si le joueur n'a pas d'item en main, ne rien faire (pas de message)
        if (heldItem.isEmpty()) {
            return InteractionResult.PASS; // PASS au lieu de FAIL pour éviter les messages
        }
        
        // Vérifier si l'item est supporté
        String itemId = getItemId(heldItem);
        
        if (itemId.isEmpty()) {
            // Item invalide ou non reconnu
            sendMessage(player, "ventrysjob.message.vase.cannot_plant");
            return InteractionResult.FAIL;
        }
        
        if (!isItemSupported(itemId)) {
            // Item non supporté - envoyer un message d'erreur
            sendMessage(player, "ventrysjob.message.vase.cannot_plant");
            return InteractionResult.FAIL;
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && !JobEnergyHelper.consumeForAction(serverPlayer, JobActionEnergyCosts.VASE_PLANT)) {
            return InteractionResult.FAIL;
        }
        
        // Planter l'item
        plantedItemId = itemId;
        isWatered = false;
        plantedTime = 0;
        currentStage = 0;
        lastStageCheckTimeMs = 0;
        updateBlockState();
        setChanged();
        
        // Consommer l'item du joueur
        heldItem.shrink(1);
        
        sendMessage(player, "ventrysjob.message.vase.planted");
        sendMessage(player, "ventrysjob.message.vase.use_water");

        if (this.level instanceof ServerLevel serverLevel) {
            PositionalSounds.playNearBlock(serverLevel, worldPosition, ModSounds.VASE_PLANTE.get(),
                PositionalSounds.VASE_SOUND_MAX_DISTANCE, 0.9f, 1.02f);
        }

        return InteractionResult.SUCCESS;
    }
    
    private boolean canHarvest() {
        // OPTIMISATION: Vérifications rapides en premier
        if (plantedItemId == null || !isWatered || plantedTime == 0) {
            return false;
        }
        
        // OPTIMISATION: Éviter l'appel System.currentTimeMillis() si possible
        long currentTime = System.currentTimeMillis();
        return (currentTime - plantedTime) >= growthTimeMs;
    }
    
    private InteractionResult harvestPlant(Player player) {
        if (plantedItemId == null) {
            return InteractionResult.FAIL;
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && !JobEnergyHelper.consumeForAction(serverPlayer, JobActionEnergyCosts.VASE_HARVEST)) {
            return InteractionResult.FAIL;
        }
        
        // Créer 2 items de récolte (input x1 → output x2)
        Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(plantedItemId));
        if (item != null) {
            ItemStack harvestStack = new ItemStack(item, 2);
            if (!player.getInventory().add(harvestStack)) {
                // Si l'inventaire est plein, faire tomber l'item
                player.drop(harvestStack, false);
            }
            
            sendMessage(player, "ventrysjob.message.vase.harvest.success");
        }

        if (this.level instanceof ServerLevel serverLevel) {
            PositionalSounds.playNearBlock(serverLevel, worldPosition, ModSounds.VASE_PLANTE.get(),
                PositionalSounds.VASE_SOUND_MAX_DISTANCE, 0.92f, 0.98f);
        }

        // Réinitialiser complètement le vase après récolte
        plantedItemId = null;
        isWatered = false;
        plantedTime = 0;
        currentStage = 0;
        lastStageCheckTimeMs = 0;
        
        // Mettre à jour le blockstate AVANT setChanged pour s'assurer que la synchronisation est correcte
        updateBlockState();
        setChanged();
        
        return InteractionResult.SUCCESS;
    }
    
    private String getItemId(ItemStack itemStack) {
        // OPTIMISATION: Cache le registry name pour éviter les appels répétés
        var registryName = itemStack.getItem().getRegistryName();
        return registryName != null ? registryName.toString() : "";
    }
    
    private void sendMessage(Player player, String translationKey, Object... args) {
        player.sendMessage(new TranslatableComponent(translationKey, args), player.getUUID());
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, VaseApothicaireBlockEntity entity) {
        // OPTIMISATION: Ne tick que si nécessaire
        if (entity.plantedItemId == null || !entity.isWatered || entity.plantedTime == 0) {
            return; // Pas de plante ou pas arrosé = pas de tick nécessaire
        }
        
        // UTILISER System.currentTimeMillis() - ignore le temps du monde
        // Cooldown strict: au plus 1 check de stage par seconde (réduit les updates répétées)
        long currentTime = System.currentTimeMillis();
        if (entity.lastStageCheckTimeMs != 0 && (currentTime - entity.lastStageCheckTimeMs) < 1000L) {
            return;
        }
        entity.lastStageCheckTimeMs = currentTime;

        long elapsed = currentTime - entity.plantedTime;
        int newStage = elapsed >= entity.growthTimeMs ? 1 : 0;

        if (newStage != entity.currentStage) {
            entity.currentStage = newStage;
            entity.updateBlockState();
            entity.setChanged();
        }
    }
    
    private void updateBlockState() {
        Level currentLevel = this.level;
        if (currentLevel != null && !currentLevel.isClientSide) {
            int stage = Math.min(1, Math.max(0, currentStage));
            BlockState state = getBlockState();
            if (state.getValue(com.ventrys.job.block.VaseApothicaireBlock.GROWTH_STAGE) != stage) {
                currentLevel.setBlock(worldPosition, state.setValue(com.ventrys.job.block.VaseApothicaireBlock.GROWTH_STAGE, stage), 3);
            }
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (plantedItemId != null) {
            tag.putString("planted_item", plantedItemId);
        }
        tag.putBoolean("is_watered", isWatered);
        tag.putLong("planted_time", plantedTime);
        tag.putInt("current_stage", currentStage);
        tag.putLong("last_stage_check_time_ms", lastStageCheckTimeMs);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        plantedItemId = tag.contains("planted_item") ? tag.getString("planted_item") : null;
        isWatered = tag.getBoolean("is_watered");
        plantedTime = tag.getLong("planted_time");
        currentStage = tag.contains("current_stage") ? tag.getInt("current_stage") : 0;
        if (currentStage > 1) {
            currentStage = 1; // migration anciennes sauvegardes (ex- stade 2)
        }
        lastStageCheckTimeMs = tag.contains("last_stage_check_time_ms") ? tag.getLong("last_stage_check_time_ms") : 0;
        
        // Protection contre les timestamps invalides après crash/redémarrage
        long currentTime = System.currentTimeMillis();
        if (plantedTime > 0) {
            // Si le timestamp est dans le futur ou trop ancien (plus de 10x la durée de croissance), réinitialiser
            if (plantedTime > currentTime || plantedTime < currentTime - (growthTimeMs * 10)) {
                plantedTime = 0;
                isWatered = false;
                currentStage = 0;
                if (plantedItemId != null) {
                    plantedTime = currentTime;
                    currentStage = 0;
                }
            }
        }
        
        // Mettre à jour le blockstate après chargement
        updateBlockState();
    }
    
    // Getters pour l'état du vase
    public String getPlantedItemId() {
        return plantedItemId;
    }
    
    public boolean isWatered() {
        return isWatered;
    }
    
    public long getPlantedTime() {
        return plantedTime;
    }
    
    public long getRemainingTime() {
        // OPTIMISATION: Vérifications rapides en premier
        if (!isWatered || plantedTime == 0) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - plantedTime;
        return Math.max(0, growthTimeMs - elapsed);
    }
}
