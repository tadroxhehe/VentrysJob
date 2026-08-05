package com.ventrys.job.network.packet;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.energy.JobEnergyCostCalculator;
import com.ventrys.job.compat.VentrysSurvivalBridge;
import com.ventrys.job.data.HammerUsage;
import com.ventrys.job.data.SawUsage;
import com.ventrys.job.data.Job;
import com.ventrys.job.data.JobManager;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.JobRecipe;
import com.ventrys.job.data.PlankCraftAccess;
import com.ventrys.job.data.RecipeIngredient;
import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.audio.JobCraftSounds;
import com.ventrys.job.audio.PositionalSounds;
import com.ventrys.job.menu.JobTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CraftJobRecipePacket {
    private final String jobId;
    private final String recipeId;
    private final int containerId;
    private final BlockPos tablePos;

    public CraftJobRecipePacket(String jobId, String recipeId, int containerId, BlockPos tablePos) {
        this.jobId = jobId;
        this.recipeId = recipeId;
        this.containerId = containerId;
        this.tablePos = tablePos;
    }

    public CraftJobRecipePacket(FriendlyByteBuf buf) {
        this.tablePos = buf.readBlockPos();
        this.containerId = buf.readVarInt();
        // Validation des données reçues
        String readJobId = buf.readUtf(32767); // Limite de 32767 caractères (max Minecraft)
        String readRecipeId = buf.readUtf(32767);
        
        // Validation basique
        if (readJobId == null || readJobId.isEmpty() || readJobId.length() > 100) {
            VentrysJob.LOGGER.warn("Packet reçu avec jobId invalide: {}", readJobId);
            this.jobId = "";
            this.recipeId = "";
            return;
        }
        
        if (readRecipeId == null || readRecipeId.isEmpty() || readRecipeId.length() > 100) {
            VentrysJob.LOGGER.warn("Packet reçu avec recipeId invalide: {}", readRecipeId);
            this.jobId = readJobId;
            this.recipeId = "";
            return;
        }
        
        // Validation du format ResourceLocation pour les IDs
        if (!isValidResourceLocation(readJobId) || !isValidResourceLocation(readRecipeId)) {
            VentrysJob.LOGGER.warn("Packet reçu avec IDs au format invalide: jobId={}, recipeId={}", readJobId, readRecipeId);
            this.jobId = "";
            this.recipeId = "";
            return;
        }
        
        this.jobId = readJobId;
        this.recipeId = readRecipeId;
    }
    
    /**
     * Valide qu'une string est un ResourceLocation valide
     */
    private boolean isValidResourceLocation(String id) {
        if (id == null || id.isEmpty()) return false;
        try {
            ResourceLocation rl = new ResourceLocation(id);
            return rl.getNamespace() != null && rl.getPath() != null 
                && !rl.getNamespace().isEmpty() && !rl.getPath().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.tablePos != null ? this.tablePos : BlockPos.ZERO);
        buf.writeVarInt(this.containerId);
        // Validation avant envoi
        if (jobId == null || jobId.isEmpty() || jobId.length() > 100) {
            VentrysJob.LOGGER.error("Tentative d'envoi de packet avec jobId invalide: {}", jobId);
            buf.writeUtf("");
            buf.writeUtf("");
            return;
        }
        
        if (recipeId == null || recipeId.isEmpty() || recipeId.length() > 100) {
            VentrysJob.LOGGER.error("Tentative d'envoi de packet avec recipeId invalide: {}", recipeId);
            buf.writeUtf(jobId);
            buf.writeUtf("");
            return;
        }
        
        buf.writeUtf(this.jobId);
        buf.writeUtf(this.recipeId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer sender = ctx.getSender(); // Capturer le joueur avant la lambda
        ctx.enqueueWork(() -> {
            try {
                ServerPlayer player = sender;
                if (player == null) return;

            // Validation supplémentaire côté serveur
            if (jobId == null || jobId.isEmpty() || recipeId == null || recipeId.isEmpty()) {
                VentrysJob.LOGGER.warn("Packet invalide reçu de {}", player.getName().getString());
                return;
            }

            if (!(player.containerMenu instanceof JobTableMenu jobTableMenu)) {
                VentrysJob.LOGGER.warn("Packet craft refusé: menu invalide pour {}", player.getName().getString());
                return;
            }

            if (jobTableMenu.containerId != this.containerId) {
                VentrysJob.LOGGER.warn("Packet craft refusé: containerId invalide pour {}", player.getName().getString());
                return;
            }

            if (!jobTableMenu.getTablePos().equals(this.tablePos) || !jobTableMenu.stillValid(player)) {
                VentrysJob.LOGGER.warn("Packet craft refusé: table invalide/hors portée pour {}", player.getName().getString());
                return;
            }

            if (!jobId.equals(jobTableMenu.getJobId())) {
                VentrysJob.LOGGER.warn("Packet craft refusé: jobId incohérent pour {}", player.getName().getString());
                return;
            }
            
            // Empêcher les crafts en mode créatif
            if (player.isCreative()) return;

            // Vérifier que le joueur a le bon métier
            String playerJob = PlayerJobData.getPlayerJob(player);
            if (playerJob == null || !playerJob.equals(jobId)) {
                player.sendMessage(new TranslatableComponent("ventrysjob.message.craft.must_be_job", jobId), player.getUUID());
                return;
            }

            Job job = JobManager.getJob(jobId);
            if (job == null) {
                VentrysJob.LOGGER.warn("Métier introuvable: {} (demandé par {})", jobId, player.getName().getString());
                return;
            }

            JobRecipe recipe = job.getRecipes().stream()
                .filter(r -> r.getId().equals(recipeId))
                .findFirst()
                .orElse(null);

        if (recipe == null) {
            VentrysJob.LOGGER.warn("Recette introuvable: {} pour métier {} (demandé par {})", 
                recipeId, jobId, player.getName().getString());
            return;
        }

            if ("forgeron".equals(jobId) && requiresForgeHammer(recipe) && !HammerUsage.hasHammerInOffhand(player)) {
                player.sendMessage(new TranslatableComponent("ventrysjob.message.craft.forgeron.no_hammer"), player.getUUID());
                return;
            }

            if ("artisan".equals(jobId) && requiresArtisanSaw(recipe) && !SawUsage.hasSawInOffhand(player)) {
                player.sendMessage(new TranslatableComponent("ventrysjob.message.craft.artisan.no_saw"), player.getUUID());
                return;
            }

            // Vérification énergie métier
            float energyCost = JobEnergyCostCalculator.resolveCraftCost(jobId, recipe);
            VentrysSurvivalBridge.catchUpPassiveRegen(player);
            if (energyCost > 0f && VentrysSurvivalBridge.getJobEnergy(player) < energyCost) {
                VentrysSurvivalBridge.sendInsufficientEnergy(player);
                return;
            }

            // Verifier les ingredients (planches chêne/sapin/bouleau interchangeables si autorisé)
            for (RecipeIngredient ingredient : recipe.getInputs()) {
                boolean ok = PlankCraftAccess.isPlancheResource(ingredient.getItemId())
                        ? PlankCraftAccess.hasEnough(player, recipe, ingredient.getItemId(), ingredient.getCount())
                        : hasItem(player, ingredient.getItemId(), ingredient.getCount());
                if (!ok) {
                    player.sendMessage(new TranslatableComponent("ventrysjob.message.craft.insufficient_ingredients"), player.getUUID());
                    return;
                }
            }

            // Consommer les ingredients avec sauvegarde pour rollback
            List<RecipeIngredient> consumedIngredients = new ArrayList<>();
            boolean allConsumed = true;
            
            for (RecipeIngredient ingredient : recipe.getInputs()) {
                boolean ok = PlankCraftAccess.isPlancheResource(ingredient.getItemId())
                        ? PlankCraftAccess.consume(player, recipe, ingredient.getItemId(), ingredient.getCount())
                        : consumeItem(player, ingredient.getItemId(), ingredient.getCount());
                if (ok) {
                    consumedIngredients.add(ingredient);
                } else {
                    allConsumed = false;
                    break;
                }
            }

            // Donner le(s) resultat(s) seulement si tous les ingredients ont ete consommes
            if (allConsumed) {
                List<RecipeIngredient> outs = recipe.getOutputsForCraft();
                if (outs.isEmpty()) {
                    restoreIngredients(player, consumedIngredients);
                } else {
                    List<ItemStack> resultStacks = new ArrayList<>();
                    boolean invalidOutput = false;
                    for (RecipeIngredient out : outs) {
                        ItemStack result = JobActions.createDropItemPublic(
                            out.getItemId(), out.getCount(), out.getDisplayName());
                        if (result.isEmpty()) {
                            invalidOutput = true;
                            VentrysJob.LOGGER.warn("Item de sortie invalide pour la recette: {}", recipe.getId());
                            break;
                        }
                        resultStacks.add(result);
                    }
                    if (invalidOutput) {
                        restoreIngredients(player, consumedIngredients);
                        player.sendMessage(new TranslatableComponent("ventrysjob.message.craft.error"), player.getUUID());
                    } else {
                        for (ItemStack stack : resultStacks) {
                            giveStackOrDrop(player, stack);
                        }
                        if (energyCost > 0f) {
                            VentrysSurvivalBridge.tryConsumeJobEnergy(player, energyCost);
                        }
                        player.sendMessage(new TranslatableComponent("ventrysjob.message.craft.success", recipe.getName()), player.getUUID());
                        if ("forgeron".equals(jobId) && requiresForgeHammer(recipe)) {
                            HammerUsage.applyWear(player, player.getOffhandItem());
                        }
                        if ("artisan".equals(jobId) && requiresArtisanSaw(recipe)) {
                            SawUsage.applyWear(player, player.getOffhandItem());
                        }
                        if (player.level instanceof ServerLevel serverLevel) {
                            PositionalSounds.playNearBlock(serverLevel, tablePos, JobCraftSounds.forJobTable(jobId),
                                PositionalSounds.CRAFT_SOUND_MAX_DISTANCE, 0.95f, 1.05f);
                        }
                        VentrysJob.LOGGER.debug("Craft: {} — {}", player.getName().getString(), recipe.getName());
                    }
                }
            } else if (!allConsumed) {
                // Restaurer les ingredients consommés en cas d'erreur
                    restoreIngredients(player, consumedIngredients);
                    player.sendMessage(new TranslatableComponent("ventrysjob.message.craft.ingredient_error"), player.getUUID());
                }
            } catch (Exception e) {
                // Capturer toutes les exceptions pour éviter les déconnexions
                VentrysJob.LOGGER.error("Erreur lors du traitement du packet CraftJobRecipePacket: {}", e.getMessage(), e);
                if (sender != null) {
                    try {
                        sender.sendMessage(new TranslatableComponent("ventrysjob.message.craft.error"), sender.getUUID());
                    } catch (Exception e2) {
                        // Ignorer les erreurs d'envoi de message
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static boolean requiresForgeHammer(JobRecipe recipe) {
        return requiresCraftHandTool(recipe, "marteau");
    }

    private static boolean requiresArtisanSaw(JobRecipe recipe) {
        return requiresCraftHandTool(recipe, "scie");
    }

    private static boolean requiresCraftHandTool(JobRecipe recipe, String outputItemSubstring) {
        if (recipe == null) {
            return false;
        }
        for (RecipeIngredient out : recipe.getOutputsForCraft()) {
            String itemId = out.getItemId();
            if (itemId != null && itemId.contains(outputItemSubstring)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasItem(ServerPlayer player, String itemId, int count) {
        ResourceLocation rl = new ResourceLocation(itemId);
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) return false;

        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }

    private boolean consumeItem(ServerPlayer player, String itemId, int count) {
        ResourceLocation rl = new ResourceLocation(itemId);
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) return false;

        // Verifier d'abord si on a assez d'items
        if (!hasItem(player, itemId, count)) {
            return false;
        }

        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
        return remaining == 0; // Retourne true si tout a ete consomme
    }

    private void restoreIngredients(ServerPlayer player, java.util.List<RecipeIngredient> ingredients) {
        for (RecipeIngredient ingredient : ingredients) {
            ItemStack stack = JobActions.createDropItemPublic(ingredient.getItemId(), ingredient.getCount());
            if (!stack.isEmpty()) {
                giveStackOrDrop(player, stack);
            }
        }
    }

    private static void giveStackOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
