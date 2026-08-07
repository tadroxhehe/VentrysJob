package com.ventrys.job.block.entity;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * BlockEntity pour le Four de Forgeron
 * Interface : 2 slots à gauche (combustible + minerai brut), 1 slot à droite (minerai fondu)
 */
public class ForgeronFourBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // Données pour la synchronisation client-serveur
    public final ContainerData data;
    private long smeltingStartTick = 0;
    private static final long SMELTING_DURATION_TICKS = 20L * 20L; // 20 secondes
    private boolean isLit = false;
    private int particleTickCounter = 0;
    private long fuelStartTick = 0;
    private long fuelDurationTicks = 0;
    private long flintAndSteelFireStartTick = 0;
    private static final long FLINT_AND_STEEL_FIRE_DURATION_TICKS = 60L * 20L; // 60 secondes

    // Configuration des combustibles et recettes
    private static final Map<String, Integer> FUEL_VALUES = new HashMap<>();
    private static final Map<String, String> SMELTING_RECIPES = new HashMap<>();
    private static final Map<String, Integer> SMELTING_COUNTS = new HashMap<>();
    private static final Set<String> VALID_FUELS = new HashSet<>();
    private static final Set<String> VALID_ORES = new HashSet<>();

    static {
        initializeConfiguration();
    }

    public ForgeronFourBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGERON_FOUR.get(), pos, state);
        
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> ForgeronFourBlockEntity.this.getProgress();
                    case 1 -> 100; // Max progress toujours 100%
                    case 2 -> ForgeronFourBlockEntity.this.isLit ? 1 : 0;
                    case 3 -> ForgeronFourBlockEntity.this.getFuelProgress();
                    case 4 -> 100; // Max fuel progress toujours 100%
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> {} // Progress calculé automatiquement
                    case 1 -> {} // Max progress fixe
                    case 2 -> ForgeronFourBlockEntity.this.isLit = value != 0;
                    case 3 -> {} // Fuel progress calculé automatiquement
                    case 4 -> {} // Max fuel progress fixe
                }
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }

    /**
     * Initialise la configuration des combustibles et recettes de cuisson.
     */
    private static void initializeConfiguration() {
        VALID_FUELS.add("minecraft:charcoal");
        FUEL_VALUES.put("minecraft:charcoal", 1200); // 60 secondes = 3 fontes (20s chacune)
        VALID_FUELS.add("minecraft:coal");
        FUEL_VALUES.put("minecraft:coal", 1200); // 60 secondes = 3 fontes (20s chacune)

        addOreRecipe("ventrysitem:res_etain_brut", "ventrysitem:res_etain_lingot");
        addOreRecipe("ventrysitem:res_calcaire", "ventrysitem:res_chaux");
        addOreRecipe("minecraft:raw_gold", "minecraft:gold_ingot");
        addOreRecipe("minecraft:raw_copper", "minecraft:copper_ingot");
        addOreRecipe("minecraft:raw_iron", "minecraft:iron_ingot");
        // 1 sable → 16 fragments : 2 sables cassés = 1 bloc de verre (32 fragments)
        addOreRecipe("minecraft:red_sand", "ventrysitem:res_verre", 16);

        VentrysJob.LOGGER.debug("Four forgeron — {} recettes de cuisson", SMELTING_RECIPES.size());
        VentrysJob.LOGGER.debug("Combustible: {}", VALID_FUELS);
        VentrysJob.LOGGER.debug("Entrées cuisson: {}", VALID_ORES);
        VentrysJob.LOGGER.debug("Recettes: {}", SMELTING_RECIPES);
        VentrysJob.LOGGER.debug("Efficacités: {}", FUEL_VALUES);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return new TranslatableComponent("container.ventrysjob.forgeron_four");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new com.ventrys.job.menu.ForgeronFourMenu(id, inv, this, this.data);
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
        tag.putLong("smeltingStartTick", smeltingStartTick);
        tag.putBoolean("isLit", isLit);
        tag.putLong("fuelStartTick", fuelStartTick);
        tag.putLong("fuelDurationTicks", fuelDurationTicks);
        tag.putLong("flintAndSteelFireStartTick", flintAndSteelFireStartTick);
        super.saveAdditional(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        smeltingStartTick = tag.getLong("smeltingStartTick");
        isLit = tag.getBoolean("isLit");
        fuelStartTick = tag.getLong("fuelStartTick");
        fuelDurationTicks = tag.getLong("fuelDurationTicks");
        flintAndSteelFireStartTick = tag.getLong("flintAndSteelFireStartTick");
        
        // Synchroniser l'état du bloc après chargement
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.getBlock() instanceof com.ventrys.job.block.ForgeronFourBlock) {
                this.level.setBlock(this.worldPosition, state.setValue(com.ventrys.job.block.ForgeronFourBlock.LIT, isLit), 3);
            }
        }
        
        // Compatibilité avec anciennes sauvegardes basées millisecondes.
        if (smeltingStartTick == 0 && tag.contains("smeltingStartTime")) {
            smeltingStartTick = 1;
        }
        if (fuelStartTick == 0 && tag.contains("fuelStartTime")) {
            fuelStartTick = 1;
        }
        if (fuelDurationTicks == 0 && tag.contains("fuelDurationMs")) {
            long oldMs = tag.getLong("fuelDurationMs");
            fuelDurationTicks = Math.max(1L, oldMs / 50L);
        }
        if (flintAndSteelFireStartTick == 0 && tag.contains("flintAndSteelFireStartTime")) {
            flintAndSteelFireStartTick = 1;
        }
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        // Particules et sons quand allumé (optimisé pour multijoueur)
        if (this.isLit()) {
            this.particleTickCounter++;
            
            // Optimisation multijoueur : Réduire la fréquence des particules
            // Particules de fumée
            if (this.particleTickCounter % 8 == 0) { // Toutes les 8 ticks (au lieu de 4)
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
                    
                    double offsetX = (this.level.random.nextDouble() - 0.5) * 0.3;
                    double offsetZ = (this.level.random.nextDouble() - 0.5) * 0.3;
                    double velocityY = 0.1 + (this.level.random.nextDouble() * 0.05);
                    double velocityX = (this.level.random.nextDouble() - 0.5) * 0.01;
                    double velocityZ = (this.level.random.nextDouble() - 0.5) * 0.01;
                    
                    this.level.addAlwaysVisibleParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, 
                        x + offsetX, y, z + offsetZ, 
                        velocityX, velocityY, velocityZ);
                }
            }
            
            // Son de four allumé (optimisé : moins fréquent et seulement si joueurs proches)
            if (this.particleTickCounter % 40 == 0) { // Toutes les 40 ticks (au lieu de 20)
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

        // Logique de cuisson
        long currentTick = this.level.getGameTime();
        
        // Vérifier si le feu du briquet est expiré
        if (isLit && flintAndSteelFireStartTick > 0) {
            long fireElapsed = currentTick - flintAndSteelFireStartTick;
            if (fireElapsed >= FLINT_AND_STEEL_FIRE_DURATION_TICKS) {
                // Le feu du briquet est éteint - arrêter tout
                setLit(false);
                flintAndSteelFireStartTick = 0;
                resetAllProgress(); // Arrêter combustible + cuisson
                return; // Sortir du tick, le four est éteint
            }
        }
        
        // Vérifier que le feu du briquet est actif avant de continuer
        boolean fireIsActive = isLit && flintAndSteelFireStartTick > 0 &&
            (currentTick - flintAndSteelFireStartTick) < FLINT_AND_STEEL_FIRE_DURATION_TICKS;
        
        if (isLit && fireIsActive) {
            // Gérer le combustible (utilise timestamps réels)
            // Le combustible ne fonctionne que si le feu du briquet est actif
            if (fuelStartTick > 0 && fuelDurationTicks > 0) {
                long fuelElapsed = currentTick - fuelStartTick;
                if (fuelElapsed >= fuelDurationTicks) {
                    // Le combustible est épuisé
                    fuelStartTick = 0;
                    fuelDurationTicks = 0;
                    
                    // Essayer de consommer un nouveau combustible
                    ItemStack fuel = itemHandler.getStackInSlot(0);
                    if (!fuel.isEmpty()) {
                        var registryName = fuel.getItem().getRegistryName();
                        if (registryName != null) {
                            String fuelId = registryName.toString();
                            Integer burnTimeTicks = FUEL_VALUES.get(fuelId);
                            if (burnTimeTicks != null) {
                                fuelDurationTicks = burnTimeTicks;
                                fuelStartTick = currentTick;
                                fuel.shrink(1); // Consommer le combustible
                                setChanged();
                            }
                        }
                    } else {
                        // Plus de combustible - arrêter seulement le combustible et la cuisson
                        // Ne pas éteindre le four si le feu du briquet est encore actif
                        resetAllProgress();
                    }
                }
            } else {
                // Pas de combustible actif, essayer d'en consommer un nouveau
                // Le combustible est utilisé pour la cuisson, pas pour prolonger le feu du briquet
                ItemStack fuel = itemHandler.getStackInSlot(0);
                if (!fuel.isEmpty()) {
                    var registryName = fuel.getItem().getRegistryName();
                    if (registryName != null) {
                        String fuelId = registryName.toString();
                        Integer burnTimeTicks = FUEL_VALUES.get(fuelId);
                        if (burnTimeTicks != null) {
                            fuelDurationTicks = burnTimeTicks;
                            fuelStartTick = currentTick;
                            fuel.shrink(1);
                            setChanged();
                        }
                    }
                }
            }
            
            // Gérer la cuisson (utilise timestamps réels)
            // La cuisson ne se fait que si le feu du briquet est actif
            if (hasRecipe()) {
                if (smeltingStartTick == 0) {
                    // Démarrer la cuisson
                    smeltingStartTick = currentTick;
                    setChanged();
                } else {
                    // Vérifier si la cuisson est terminée
                    long smeltingElapsed = currentTick - smeltingStartTick;
                    if (smeltingElapsed >= SMELTING_DURATION_TICKS) {
                        craftItem();
                        resetSmelting();
                    }
                }
            } else {
                resetSmelting();
            }
        } else {
            // Le four est éteint - arrêter tout
            resetAllProgress();
        }
    }

    private boolean hasRecipe() {
        ItemStack ore = itemHandler.getStackInSlot(1);  // Slot minerai brut
        ItemStack result = itemHandler.getStackInSlot(2); // Slot résultat
        
        if (ore.isEmpty()) return false;
        
        // Vérifier si le minerai est valide
        var registryName = ore.getItem().getRegistryName();
        if (registryName == null) return false;
        String oreId = registryName.toString();
        if (!VALID_ORES.contains(oreId)) return false;
        
        // Vérifier si on peut placer le résultat
        String resultId = SMELTING_RECIPES.get(oreId);
        if (resultId == null) return false;
        int resultCount = SMELTING_COUNTS.getOrDefault(oreId, 1);
        
        if (result.isEmpty()) return true;
        
        // Vérifier si c'est le même type d'item et qu'on peut ajouter
        var resultItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            new net.minecraft.resources.ResourceLocation(resultId)
        );
        if (resultItem == null) return false;
        
        return result.getItem() == resultItem
                && result.getCount() + resultCount <= result.getMaxStackSize();
    }

    private void craftItem() {
        ItemStack ore = itemHandler.getStackInSlot(1);
        
        var registryName = ore.getItem().getRegistryName();
        if (registryName == null) return;
        String oreId = registryName.toString();
        String resultId = SMELTING_RECIPES.get(oreId);
        
        if (resultId != null) {
            var resultItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation(resultId)
            );
            
            if (resultItem != null) {
                int resultCount = SMELTING_COUNTS.getOrDefault(oreId, 1);
                ItemStack result = new ItemStack(resultItem, resultCount);
                
                // Consommer le minerai
                ore.shrink(1);
                
                // Ajouter le résultat
                ItemStack currentResult = itemHandler.getStackInSlot(2);
                if (currentResult.isEmpty()) {
                    itemHandler.setStackInSlot(2, result);
                } else {
                    currentResult.grow(resultCount);
                }
                
                resetSmelting();
            }
        }
    }

    private void resetSmelting() {
        this.smeltingStartTick = 0;
    }
    
    /**
     * Réinitialise complètement l'état du four (combustible + cuisson)
     * Appelé quand le feu s'éteint
     */
    private void resetAllProgress() {
        this.smeltingStartTick = 0;
        this.fuelStartTick = 0;
        this.fuelDurationTicks = 0;
    }
    
    private int getProgress() {
        if (smeltingStartTick == 0 || this.level == null) {
            return 0;
        }
        long elapsed = this.level.getGameTime() - smeltingStartTick;
        if (elapsed >= SMELTING_DURATION_TICKS) {
            return 100;
        }
        return (int) ((elapsed * 100L) / SMELTING_DURATION_TICKS);
    }
    
    private int getFuelProgress() {
        if (fuelStartTick == 0 || fuelDurationTicks == 0 || this.level == null) {
            return 0;
        }
        long elapsed = this.level.getGameTime() - fuelStartTick;
        if (elapsed >= fuelDurationTicks) {
            return 0; // Combustible épuisé
        }
        return (int) ((elapsed * 100L) / fuelDurationTicks);
    }

    // Getters pour l'interface
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
            if (state.getBlock() instanceof com.ventrys.job.block.ForgeronFourBlock) {
                this.level.setBlock(this.worldPosition, state.setValue(com.ventrys.job.block.ForgeronFourBlock.LIT, lit), 3);
            }
        }
        
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
            if (this.level != null) {
                this.flintAndSteelFireStartTick = this.level.getGameTime();
            } else {
                this.flintAndSteelFireStartTick = 1;
            }
            setLit(true);
        }
    }
    
    public boolean isCooking() {
        return smeltingStartTick > 0;
    }

    // Méthodes pour la configuration externe
    public static void addFuel(String itemId, int burnTime) {
        VALID_FUELS.add(itemId);
        FUEL_VALUES.put(itemId, burnTime);
        VentrysJob.LOGGER.debug("Four forgeron — combustible: {} ({} ticks)", itemId, burnTime);
    }
    
    public static void addOreRecipe(String oreId, String resultId) {
        addOreRecipe(oreId, resultId, 1);
    }

    public static void addOreRecipe(String oreId, String resultId, int resultCount) {
        VALID_ORES.add(oreId);
        SMELTING_RECIPES.put(oreId, resultId);
        SMELTING_COUNTS.put(oreId, Math.max(1, resultCount));
        VentrysJob.LOGGER.debug("Four forgeron — recette: {} → {} x{}", oreId, resultId, resultCount);
    }
    
    public static void removeFuel(String itemId) {
        VALID_FUELS.remove(itemId);
        FUEL_VALUES.remove(itemId);
        VentrysJob.LOGGER.debug("Four forgeron — combustible retiré: {}", itemId);
    }
    
    public static void removeOreRecipe(String oreId) {
        VALID_ORES.remove(oreId);
        SMELTING_RECIPES.remove(oreId);
        SMELTING_COUNTS.remove(oreId);
        VentrysJob.LOGGER.debug("Four forgeron — recette retirée: {}", oreId);
    }
    
    // Méthodes statiques pour la validation
    public static boolean isValidFuel(String itemId) {
        return VALID_FUELS.contains(itemId);
    }
    
    public static boolean isValidOre(String itemId) {
        return VALID_ORES.contains(itemId);
    }
    
    public static void loadConfiguration() {
        // Pour l'instant, on utilise la configuration par défaut
        VentrysJob.LOGGER.debug("Four forgeron — loadConfiguration (défaut)");
    }
}
