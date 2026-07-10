package com.ventrys.job.block.entity;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.init.ModBlockEntities;
import com.ventrys.job.menu.OuvrierFourMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Block Entity pour le Four Ouvrier - Transforme les planches en charbon
 */
@Mod.EventBusSubscriber
public class OuvrierFourBlockEntity extends BlockEntity implements MenuProvider {
    
    // Configuration des transformations - CONFIGURATION MANUELLE
    private static final Map<String, String> TRANSFORMATION_RECIPES = new HashMap<>();
    private static final Set<String> VALID_INPUT_ITEMS = new HashSet<>();
    private static final Set<String> VALID_OUTPUT_ITEMS = new HashSet<>();
    
    static {
        // Configuration manuelle des items valides
        initializeValidItems();
    }
    
    /**
     * Initialise les items valides selon la configuration manuelle
     */
    private static void initializeValidItems() {
        // Configuration par défaut - SEULEMENT les items spécifiés
        VALID_INPUT_ITEMS.add("ventrysitem:res_planche_chene");
        VALID_INPUT_ITEMS.add("ventrysitem:res_planche_bouleau");
        VALID_INPUT_ITEMS.add("ventrysitem:res_planche_sapin");
        
        VALID_OUTPUT_ITEMS.add("minecraft:charcoal");
        
        // Recettes par défaut (ratio 1:1 - 1 planche = 1 charbon)
        TRANSFORMATION_RECIPES.put("ventrysitem:res_planche_chene", "minecraft:charcoal");
        TRANSFORMATION_RECIPES.put("ventrysitem:res_planche_bouleau", "minecraft:charcoal");
        TRANSFORMATION_RECIPES.put("ventrysitem:res_planche_sapin", "minecraft:charcoal");
        
        VentrysJob.LOGGER.debug("Four ouvrier — configuration par défaut");
        VentrysJob.LOGGER.debug("Items d'entrée: {}", VALID_INPUT_ITEMS);
        VentrysJob.LOGGER.debug("Items de sortie: {}", VALID_OUTPUT_ITEMS);
        VentrysJob.LOGGER.debug("Recettes: {}", TRANSFORMATION_RECIPES);
    }
    
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull net.minecraft.world.item.ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            Item item = stack.getItem();
            var rn = item.getRegistryName();
            return rn != null && OuvrierFourBlockEntity.isValidItem(rn.toString());
        }
    };
    
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    
    // Données pour le menu (progression de la transformation)
    public final ContainerData data;
    private long startTick = 0;
    private static final long TRANSFORMATION_DURATION_TICKS = 24L * 60L * 60L * 20L; // 24 heures
    private boolean isLit = false; // État d'allumage du four
    private int particleTickCounter = 0; // Compteur pour les particules
    private boolean isTransforming = false; // État de transformation
    private long flintAndSteelFireStartTick = 0;
    // Le feu dure un peu plus longtemps que la transformation (24 h + 5 min)
    // pour garantir que la fournée se termine avant l'extinction du feu.
    private static final long FLINT_AND_STEEL_FIRE_DURATION_TICKS = TRANSFORMATION_DURATION_TICKS + (5L * 60L * 20L); // 24 h 05
    
    public OuvrierFourBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OUVRIER_FOUR.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> OuvrierFourBlockEntity.this.getProgressPercentage();
                    case 1 -> 100; // Max progress toujours 100%
                    case 2 -> OuvrierFourBlockEntity.this.isLit ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> {} // Progress calculé automatiquement
                    case 1 -> {} // Max progress fixe
                    case 2 -> OuvrierFourBlockEntity.this.isLit = value > 0;
                }
            }

            @Override
            public int getCount() {
                return 3; // progress, maxProgress, isLit
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.ventrysjob.ouvrier_four");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new OuvrierFourMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putLong("startTick", startTick);
        tag.putBoolean("isLit", isLit);
        tag.putBoolean("isTransforming", isTransforming);
        tag.putLong("flintAndSteelFireStartTick", flintAndSteelFireStartTick);
        super.saveAdditional(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        startTick = tag.getLong("startTick");
        isLit = tag.getBoolean("isLit");
        isTransforming = tag.getBoolean("isTransforming");
        flintAndSteelFireStartTick = tag.getLong("flintAndSteelFireStartTick");
        
        // Synchroniser l'état du bloc après chargement
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.getBlock() instanceof com.ventrys.job.block.OuvrierFourBlock) {
                this.level.setBlock(this.worldPosition, state.setValue(com.ventrys.job.block.OuvrierFourBlock.LIT, isLit), 3);
            }
        }
        
        // Compatibilité avec anciennes sauvegardes en millisecondes.
        if (startTick == 0 && tag.contains("startTime")) {
            startTick = 1;
        }
        if (flintAndSteelFireStartTick == 0 && tag.contains("flintAndSteelFireStartTime")) {
            flintAndSteelFireStartTick = 1;
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    // Méthode pour démarrer la transformation
    public void startTransformation() {
        if (!canStartTransformation()) {
            stopTransformation();
            setChanged();
            return;
        }
        
        if (!isTransforming) {
            this.startTick = this.level != null ? this.level.getGameTime() : 1;
            this.isTransforming = true;
            setChanged();
        }
    }

    // Méthode pour arrêter la transformation
    public void stopTransformation() {
        this.isTransforming = false;
        this.startTick = 0;
        setChanged();
    }
    
    /**
     * Réinitialise complètement l'état du four (transformation)
     * Appelé quand le feu s'éteint
     */
    private void resetAllProgress() {
        this.isTransforming = false;
        this.startTick = 0;
    }

    // Méthode pour vérifier si la transformation est en cours
    public boolean isTransforming() {
        return isTransforming && startTick > 0;
    }
    
    // Méthodes pour l'allumage du four
    public boolean isLit() {
        return isLit;
    }
    
    public void setLit(boolean lit) {
        boolean wasLit = this.isLit;
        this.isLit = lit;
        setChanged();
        
        // Synchroniser l'état du bloc
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.getBlock() instanceof com.ventrys.job.block.OuvrierFourBlock) {
                this.level.setBlock(this.worldPosition, state.setValue(com.ventrys.job.block.OuvrierFourBlock.LIT, lit), 3);
            }
        }
        
        // Jouer le son d'allumage si le four vient d'être allumé
        if (lit && !wasLit && this.level != null) {
            this.level.playSound(null, this.worldPosition, 
                SoundEvents.FLINTANDSTEEL_USE, 
                SoundSource.BLOCKS, 
                0.5f, 1.0f);
        }
        
        // Si on éteint le four, réinitialiser le feu du briquet
        if (!lit) {
            flintAndSteelFireStartTick = 0;
        }
    }
    
    /**
     * Allume le four avec un briquet (feu temporaire de durée fixe)
     */
    public void lightWithFlintAndSteel() {
        if (!this.isLit) {
            this.flintAndSteelFireStartTick = this.level != null ? this.level.getGameTime() : 1;
            setLit(true);
        }
    }
    
    public boolean canStartTransformation() {
        return isLit && hasRecipe(this) && !isTransformationComplete();
    }

    // Méthode pour vérifier si la transformation est terminée
    public boolean isTransformationComplete() {
        if (!isTransforming || startTick == 0 || this.level == null) return false;
        return (this.level.getGameTime() - startTick) >= TRANSFORMATION_DURATION_TICKS;
    }

    // Méthode pour obtenir le pourcentage de progression
    public int getProgressPercentage() {
        if (!isTransforming || startTick == 0 || this.level == null) return 0;
        
        long elapsed = this.level.getGameTime() - startTick;
        if (elapsed >= TRANSFORMATION_DURATION_TICKS) return 100;
        
        return (int) ((elapsed * 100) / TRANSFORMATION_DURATION_TICKS);
    }

    // Méthode pour obtenir le temps restant en millisecondes
    public long getTimeRemaining() {
        if (!isTransforming || startTick == 0 || this.level == null) return 0;
        
        long elapsed = this.level.getGameTime() - startTick;
        long remainingTicks = TRANSFORMATION_DURATION_TICKS - elapsed;
        long remaining = remainingTicks * 50L;
        return Math.max(0, remaining);
    }

    private static boolean hasRecipe(OuvrierFourBlockEntity entity) {
        ItemStack stack = entity.itemHandler.getStackInSlot(0);
        
        // Une seule planche suffit (ratio 1:1)
        if (stack.isEmpty()) {
            return false;
        }
        
        var registryName = stack.getItem().getRegistryName();
        if (registryName == null) return false;
        String inputId = registryName.toString();
        String outputId = TRANSFORMATION_RECIPES.get(inputId);
        
        return outputId != null; // Vérifier seulement si la recette existe
    }

    private static void craftItem(OuvrierFourBlockEntity entity) {
        if (hasRecipe(entity)) {
            ItemStack input = entity.itemHandler.getStackInSlot(0);
            var registryName = input.getItem().getRegistryName();
            if (registryName == null) return;
            String inputId = registryName.toString();
            String outputId = TRANSFORMATION_RECIPES.get(inputId);
            
            if (outputId != null) {
                // Récupérer la quantité dans le slot
                int inputCount = input.getCount();
                
                // Ratio 1:1 - 1 planche = 1 charbon
                int outputCount = inputCount;
                if (outputCount < 1) {
                    outputCount = 1; // Minimum 1 charbon
                }
                
                ItemStack result;
                
                // Essayer d'abord de trouver l'item par son ID (support mods)
                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.resources.ResourceLocation(outputId)
                );
                
                if (item != null) {
                    result = new ItemStack(item, outputCount);
                } else {
                    // Fallback vers le charbon vanilla si l'item n'est pas trouvé
                    result = new ItemStack(net.minecraft.world.item.Items.CHARCOAL, outputCount);
                }

                // Remplacer directement le contenu du slot
                entity.itemHandler.setStackInSlot(0, result);

                entity.stopTransformation();
                
                // Ne pas éteindre le four automatiquement - il reste allumé jusqu'à ce que le feu du briquet s'éteigne
            }
        }
    }
    
    
    /**
     * Méthode de tick automatique appelée par Minecraft
     * Cette méthode sera appelée automatiquement chaque tick
     */
    public void tick() {
        if (this.level == null) {
            return;
        }
        
        long currentTick = this.level.getGameTime();
        
        // Vérifier si le feu du briquet est expiré
        if (isLit && flintAndSteelFireStartTick > 0) {
            long fireElapsed = currentTick - flintAndSteelFireStartTick;
            if (fireElapsed >= FLINT_AND_STEEL_FIRE_DURATION_TICKS) {
                // Le feu du briquet est éteint - arrêter tout
                setLit(false);
                flintAndSteelFireStartTick = 0;
                resetAllProgress(); // Arrêter la transformation
                return; // Sortir du tick, le four est éteint
            }
        }
        
        // Vérifier que le feu du briquet est actif avant de continuer
        boolean fireIsActive = isLit && flintAndSteelFireStartTick > 0 &&
            (currentTick - flintAndSteelFireStartTick) < FLINT_AND_STEEL_FIRE_DURATION_TICKS;
        
        // Côté serveur : logique de transformation
        if (!this.level.isClientSide()) {
            // La transformation ne fonctionne que si le feu du briquet est actif
            if (fireIsActive && hasRecipe(this) && !this.isTransforming()) {
                this.startTransformation();
            }
            
            // Vérifier si la transformation est terminée
            if (this.isTransforming() && this.isTransformationComplete()) {
                craftItem(this);
            }
            
            // Arrêter la transformation si le feu n'est plus actif, le four n'est plus allumé ou plus de recette
            if (this.isTransforming() && (!fireIsActive || !this.isLit() || !hasRecipe(this))) {
                this.stopTransformation();
            }
        }
        
        // Particules et son (optimisé pour multijoueur)
        if (this.isLit()) {
            this.particleTickCounter++;
            
            // Optimisation multijoueur : Réduire la fréquence des particules
            // Particules de fumée (côté serveur avec packet réseau)
            if (!this.level.isClientSide() && this.particleTickCounter % 8 == 0) { // Toutes les 8 ticks (au lieu de 4)
                // Vérifier s'il y a des joueurs à proximité pour éviter les particules inutiles
                boolean hasNearbyPlayers = this.level.hasNearbyAlivePlayer(
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5,
                    16.0D // Rayon de 16 blocs
                );
                
                if (hasNearbyPlayers) {
                    double x = this.worldPosition.getX() + 0.5;
                    double y = this.worldPosition.getY() + 1.0;
                    double z = this.worldPosition.getZ() + 0.5;
                    
                    // Particules de fumée avec mouvement réaliste
                    double offsetX = (this.level.random.nextDouble() - 0.5) * 0.3;
                    double offsetZ = (this.level.random.nextDouble() - 0.5) * 0.3;
                    double velocityY = 0.1 + (this.level.random.nextDouble() * 0.05); // Vitesse verticale
                    double velocityX = (this.level.random.nextDouble() - 0.5) * 0.01; // Légère dérive
                    double velocityZ = (this.level.random.nextDouble() - 0.5) * 0.01;
                    
                    // Fumée principale - utiliser addAlwaysVisibleParticle pour forcer l'affichage
                    this.level.addAlwaysVisibleParticle(ParticleTypes.SMOKE, 
                        x + offsetX, y, z + offsetZ, 
                        velocityX, velocityY, velocityZ);
                    
                    // Particules de cendres occasionnelles (moins fréquentes)
                    if (this.particleTickCounter % 32 == 0) { // Toutes les 32 ticks (au lieu de 16)
                        this.level.addAlwaysVisibleParticle(ParticleTypes.ASH, 
                            x + offsetX * 0.5, y, z + offsetZ * 0.5, 
                            velocityX * 0.5, velocityY * 0.2, velocityZ * 0.5);
                    }
                }
            }
            
            // Son de four allumé (optimisé : moins fréquent et seulement si joueurs proches)
            if (!this.level.isClientSide() && this.particleTickCounter % 40 == 0) { // Toutes les 40 ticks (au lieu de 20)
                boolean hasNearbyPlayers = this.level.hasNearbyAlivePlayer(
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5,
                    16.0D // Rayon de 16 blocs
                );
                
                if (hasNearbyPlayers) {
                    this.level.playSound(null, this.worldPosition, 
                        SoundEvents.FURNACE_FIRE_CRACKLE, 
                        SoundSource.BLOCKS, 
                        0.7f, 1.0f);
                }
            }
        }
    }

    // Méthodes statiques pour la configuration
    public static void addTransformationRecipe(String inputItem, String outputItem) {
        TRANSFORMATION_RECIPES.put(inputItem, outputItem);
    }

    public static void removeTransformationRecipe(String inputItem) {
        TRANSFORMATION_RECIPES.remove(inputItem);
    }

    public static Map<String, String> getTransformationRecipes() {
        return new HashMap<>(TRANSFORMATION_RECIPES);
    }
    
    /**
     * Vérifie si un item est valide pour le four (entrée ou sortie)
     */
    public static boolean isValidItem(String itemId) {
        return VALID_INPUT_ITEMS.contains(itemId) || VALID_OUTPUT_ITEMS.contains(itemId);
    }
    
    /**
     * Vérifie si un item peut être utilisé en entrée
     */
    public static boolean isValidInputItem(String itemId) {
        return VALID_INPUT_ITEMS.contains(itemId);
    }
    
    /**
     * Vérifie si un item peut être produit en sortie
     */
    public static boolean isValidOutputItem(String itemId) {
        return VALID_OUTPUT_ITEMS.contains(itemId);
    }
    
    /**
     * Ajoute un item d'entrée à la configuration (pour les mods externes)
     */
    public static void addInputItem(String itemId, String outputItemId) {
        VALID_INPUT_ITEMS.add(itemId);
        TRANSFORMATION_RECIPES.put(itemId, outputItemId);
        VentrysJob.LOGGER.debug("Four ouvrier — entrée ajoutée: {} → {}", itemId, outputItemId);
    }
    
    /**
     * Ajoute un item de sortie à la configuration
     */
    public static void addOutputItem(String itemId) {
        VALID_OUTPUT_ITEMS.add(itemId);
        VentrysJob.LOGGER.debug("Four ouvrier — sortie ajoutée: {}", itemId);
    }
    
    /**
     * Supprime un item de la configuration
     */
    public static void removeItem(String itemId) {
        VALID_INPUT_ITEMS.remove(itemId);
        VALID_OUTPUT_ITEMS.remove(itemId);
        TRANSFORMATION_RECIPES.remove(itemId);
        VentrysJob.LOGGER.debug("Four ouvrier — item retiré: {}", itemId);
    }
    
    /**
     * Charge la configuration depuis un fichier (pour l'instant, méthode vide)
     * Cette méthode peut être étendue pour charger depuis un fichier JSON
     */
    public static void loadConfiguration() {
        // Pour l'instant, on utilise la configuration par défaut
        // Plus tard, on peut charger depuis un fichier config/ventrysjob/four_config.json
        VentrysJob.LOGGER.debug("Four ouvrier — loadConfiguration (défaut)");
    }
}
