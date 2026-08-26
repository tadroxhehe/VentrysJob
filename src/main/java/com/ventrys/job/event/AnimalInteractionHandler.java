package com.ventrys.job.event;

import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.energy.JobActionEnergyCosts;
import com.ventrys.job.energy.JobEnergyHelper;
import com.ventrys.job.entity.CustomAnimal;
import com.ventrys.job.entity.CustomChicken;
import com.ventrys.job.entity.CustomCow;
import com.ventrys.job.entity.CustomPig;
import com.ventrys.job.entity.CustomSheep;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class AnimalInteractionHandler {
    
    @SubscribeEvent(priority = EventPriority.HIGH)
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
                    if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                            || !JobEnergyHelper.consumeForAction(serverPlayer, JobActionEnergyCosts.MILK_COW)) {
                        event.setCancellationResult(InteractionResult.FAIL);
                        event.setCanceled(true);
                        return;
                    }
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
                    if (!player.level.isClientSide) {
                        player.sendMessage(new TranslatableComponent("ventrysjob.message.animal.feed.restricted"),
                                player.getUUID());
                    }
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                
                if (!player.level.isClientSide) {
                    if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                            || !JobEnergyHelper.consumeForAction(serverPlayer, JobActionEnergyCosts.FEED_ANIMAL)) {
                        event.setCancellationResult(InteractionResult.FAIL);
                        event.setCanceled(true);
                        return;
                    }
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
                    if (!player.level.isClientSide) {
                        player.sendMessage(new TranslatableComponent("ventrysjob.message.animal.hydrate.restricted"),
                                player.getUUID());
                    }
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                
                if (!player.level.isClientSide) {
                    if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                            || !JobEnergyHelper.consumeForAction(serverPlayer, JobActionEnergyCosts.HYDRATE_ANIMAL)) {
                        event.setCancellationResult(InteractionResult.FAIL);
                        event.setCanceled(true);
                        return;
                    }
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
            
            // Reproduction : uniquement automatique via LivestockProgressManager
            // (évite un double spawn si clic main vide + tick auto sur jauge pleine).
        }
    }

    private static boolean canFeed(CustomAnimal animal, Item item) {
        if (animal instanceof CustomChicken) {
            return isVentrysSeed(item) || item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS;
        }
        if (animal instanceof CustomSheep) {
            return isSheepFood(item);
        }
        if (animal instanceof CustomPig) {
            // Patate / carotte vanilla + items Ventrys (récolte paysan)
            return item == Items.POTATO
                    || item == Items.CARROT
                    || item == Items.BEETROOT
                    || isVentrysItem(item, "item_patate")
                    || isVentrysItem(item, "item_carotte")
                    || isVentrysItem(item, "item_betrave");
        }
        if (animal instanceof CustomCow) {
            return item == Items.WHEAT || isVentrysItem(item, "res_orge");
        }
        return item == Items.WHEAT || item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT;
    }

    /** Choux, betterave, salade, carotte, oignon, tomate, orge, graines Ventrys. */
    private static boolean isSheepFood(Item item) {
        if (item == Items.WHEAT) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null || !"ventrysitem".equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        if (path.startsWith("item_graine_")) {
            return true;
        }
        return "item_choux".equals(path)
                || "item_betrave".equals(path)
                || "item_salade".equals(path)
                || "item_carotte".equals(path)
                || "item_oignon".equals(path)
                || "item_tomate".equals(path)
                || "res_orge".equals(path);
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
    
}
