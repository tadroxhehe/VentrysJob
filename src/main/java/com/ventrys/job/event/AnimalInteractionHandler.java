package com.ventrys.job.event;

import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.entity.CustomAnimal;
import com.ventrys.job.entity.CustomChicken;
import com.ventrys.job.entity.CustomCow;
import com.ventrys.job.entity.CustomPig;
import com.ventrys.job.entity.CustomSheep;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class AnimalInteractionHandler {
    
    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof CustomAnimal animal) {
            // L'event se déclenche une fois par main : on ne traite que la main principale.
            // Sinon, une graine en main principale (non annulée) relance l'event pour la main
            // secondaire vide, ce qui déclenchait la reproduction = duplication d'animaux.
            if (event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }
            Player player = event.getPlayer();
            ItemStack heldItem = player.getItemInHand(event.getHand());
            
            // Extraction de lait pour les vaches (seau vide en main)
            if (animal instanceof CustomCow cow && heldItem.getItem() == Items.BUCKET) {
                // Vache mâle : pas de lait
                if (cow.isMale()) {
                    if (!player.level.isClientSide) {
                        player.sendMessage(new TextComponent("§cLes vaches mâles ne produisent pas de lait."), player.getUUID());
                    }
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                // Conditions de nutrition / hydratation
                if (cow.getNutrition() < 30 || cow.getHydration() < 30) {
                    if (!player.level.isClientSide) {
                        player.sendMessage(new TextComponent("§cCette vache est trop affamée ou déshydratée pour produire du lait (nutrition et hydratation ≥ 30 % requis)."), player.getUUID());
                    }
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                // Cooldown de production : on bloque la traite sans afficher le temps restant au joueur.
                long remaining = cow.getMilkCooldownRemainingMs();
                if (remaining > 0) {
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                // Toutes les conditions sont remplies : traire
                if (!player.level.isClientSide) {
                    ItemStack milk = cow.extractMilk();
                    if (!milk.isEmpty()) {
                        heldItem.shrink(1);
                        if (heldItem.isEmpty()) {
                            player.setItemInHand(event.getHand(), milk);
                        } else {
                            if (!player.getInventory().add(milk)) {
                                player.drop(milk, false);
                            }
                        }
                    }
                }
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            
            // Nourrir l'animal (augmente la nutrition) en fonction de son espèce
            // RESTRICTION : Seul le job paysan peut nourrir
            if (canFeed(animal, heldItem.getItem())) {
                String playerJob = PlayerJobData.getPlayerJob(player);
                if (!"paysan".equals(playerJob)) {
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                
                if (!player.level.isClientSide) {
                    animal.addNutrition(20);
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                }
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            
            // Hydrater l'animal (avec un seau d'eau)
            // RESTRICTION : Seul le job paysan peut hydrater
            if (heldItem.getItem() == Items.WATER_BUCKET) {
                String playerJob = PlayerJobData.getPlayerJob(player);
                if (!"paysan".equals(playerJob)) {
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                
                if (!player.level.isClientSide) {
                    animal.addHydration(30);
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                        player.setItemInHand(event.getHand(), new ItemStack(Items.BUCKET));
                    }
                }
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            
            // Reproduction entre deux animaux
            if (heldItem.isEmpty() && animal.canReproduce()) {
                // Chercher un autre animal du même type à proximité
                Animal mate = findMate(animal);
                if (mate instanceof CustomAnimal customMate && customMate.canReproduce()) {
                    if (animal.isMale() != customMate.isMale()) {
                        // Reproduction possible
                        if (!player.level.isClientSide && player.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            net.minecraft.world.entity.AgeableMob offspring = animal.getBreedOffspring(serverLevel, customMate);
                            if (offspring != null) {
                                offspring.setAge(-24000); // Bébé
                                offspring.moveTo(animal.getX(), animal.getY(), animal.getZ(), 0.0F, 0.0F);
                                player.level.addFreshEntity(offspring);
                                // Coût de reproduction : les deux parents passent sous le seuil
                                // et devront être re-nourris/hydratés avant de pouvoir se reproduire
                                // à nouveau (anti-spam de bébés).
                                animal.addNutrition(-BREEDING_COST);
                                animal.addHydration(-BREEDING_COST);
                                customMate.addNutrition(-BREEDING_COST);
                                customMate.addHydration(-BREEDING_COST);
                            }
                        }
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }
    
    /** Nutrition/hydratation retirée à chaque parent après une reproduction (anti-spam). */
    private static final int BREEDING_COST = 40;

    private static boolean canFeed(CustomAnimal animal, Item item) {
        // Mouton et poule : graines VentrysItem (ventrysitem:item_graine_*)
        if (animal instanceof CustomSheep || animal instanceof CustomChicken) {
            return isVentrysSeed(item);
        }
        // Porc : patate VentrysItem (ventrysitem:item_patate)
        if (animal instanceof CustomPig) {
            return isVentrysItem(item, "item_patate");
        }
        // Vache : blé (vanilla)
        if (animal instanceof CustomCow) {
            return item == Items.WHEAT;
        }
        // Autres animaux personnalisés : fallback sur les aliments génériques
        return item == Items.WHEAT || item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT;
    }

    /** Vrai si l'item est une graine VentrysItem (ventrysitem:item_graine_*). */
    private static boolean isVentrysSeed(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null
            && "ventrysitem".equals(id.getNamespace())
            && id.getPath().startsWith("item_graine_");
    }

    /** Vrai si l'item est l'item VentrysItem dont le chemin correspond exactement. */
    private static boolean isVentrysItem(Item item, String path) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null
            && "ventrysitem".equals(id.getNamespace())
            && path.equals(id.getPath());
    }
    
    private static Animal findMate(CustomAnimal animal) {
        // Chercher dans un rayon de 10 blocs pour la reproduction
        double radius = 10.0D;
        return animal.level.getEntitiesOfClass(Animal.class, 
            animal.getBoundingBox().inflate(radius),
            entity -> entity != animal && 
                     entity.getClass() == animal.getClass() &&
                     entity instanceof CustomAnimal).stream()
            .findFirst()
            .orElse(null);
    }
}

