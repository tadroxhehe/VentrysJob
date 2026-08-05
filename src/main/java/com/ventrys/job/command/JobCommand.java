package com.ventrys.job.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ventrys.job.compat.VentrysPermsBridge;
import com.ventrys.job.data.JobManager;
import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.Job;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class JobCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("job")
            .then(Commands.literal("me")
                .executes(JobCommand::showMyJob))
            .then(Commands.literal("set")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.job.set"))
                .then(Commands.argument("player", EntityArgument.players())
                    .then(Commands.argument("jobId", StringArgumentType.string())
                        .executes(JobCommand::setPlayerJob))))
            .then(Commands.literal("remove")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.job.remove"))
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(JobCommand::removePlayerJob)))
            .then(Commands.literal("list")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.job.list"))
                .executes(JobCommand::listJobs))
            .then(Commands.literal("check")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.job.check"))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(JobCommand::checkPlayerJob)))
            .then(Commands.literal("reset")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.job.reset"))
                .executes(JobCommand::resetPlayerProgress))
            .then(Commands.literal("cleanup")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.job.cleanup"))
                .executes(JobCommand::forceCleanup))
            .then(Commands.literal("reload")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.job.reload"))
                .executes(JobCommand::reloadJobs))
            .then(Commands.literal("debug")
                .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.job.debug"))
                .executes(JobCommand::debugJobs))
        );
    }
    
    private static int setPlayerJob(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        String jobId = StringArgumentType.getString(context, "jobId");
        
        // Vérifier que le métier existe
        if (JobManager.getJob(jobId) == null) {
            context.getSource().sendFailure(new TranslatableComponent("ventrysjob.command.job.not_found", jobId));
            return 0;
        }
        
        int count = 0;
        for (ServerPlayer player : players) {
            PlayerJobData.setPlayerJob(player, jobId);
            com.ventrys.job.network.NetworkHandler.syncPlayerJob(player);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.job.set", JobManager.getJob(jobId).getName()), player.getUUID());
            count++;
        }
        
        context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.set.success", jobId, count), true);
        return count;
    }
    
    private static int removePlayerJob(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        
        int count = 0;
        for (ServerPlayer player : players) {
            PlayerJobData.removePlayerJob(player);
            com.ventrys.job.network.NetworkHandler.syncPlayerJob(player);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.job.removed"), player.getUUID());
            count++;
        }
        
        context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.remove.success", count), true);
        return count;
    }
    
    private static int listJobs(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.list"), false);
        
        JobManager.getAllJobs().forEach(job -> {
            context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.list.item", job.getId(), job.getName()), false);
        });
        
        return JobManager.getAllJobs().size();
    }
    
    private static int checkPlayerJob(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String jobId = PlayerJobData.getPlayerJob(player);
        
        if (jobId != null && !jobId.isEmpty()) {
            context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.check.has", player.getName().getString(), jobId), false);
        } else {
            context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.check.none", player.getName().getString()), false);
        }
        
        return 1;
    }
    
    private static int showMyJob(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String jobId = PlayerJobData.getPlayerJob(player);
        
        if (jobId != null && !jobId.isEmpty()) {
            context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.message.job.current", jobId), false);
        } else {
            context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.message.job.none"), false);
        }
        
        return 1;
    }
    
    private static int forceCleanup(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        JobActions.forceCleanup();
        context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.cleanup"), false);
        return 1;
    }
    
    private static int reloadJobs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            // Nettoyer le cache des items avant de recharger
            JobManager.clearItemCache();
            
            // Recharger les jobs
            JobManager.loadJobs();
            
            int jobCount = JobManager.getAllJobs().size();
            context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.reload.success", jobCount), false);
            return 1;
        } catch (Exception e) {
            com.ventrys.job.VentrysJob.LOGGER.error("Erreur lors du rechargement des jobs", e);
            context.getSource().sendFailure(new TranslatableComponent("ventrysjob.command.job.reload.error", e.getMessage()));
            return 0;
        }
    }
    
    private static int debugJobs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.debug.header"), false);
        context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.debug.loaded", JobManager.getJobs().size()), false);
        for (String jobId : JobManager.getJobs().keySet()) {
            Job job = JobManager.getJob(jobId);
            context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.command.job.debug.item", jobId, job.getName(), job.getRecipes().size()), false);
        }
        return 1;
    }
    
    private static int resetPlayerProgress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        JobActions.resetPlayerProgress(player);
        context.getSource().sendSuccess(new TranslatableComponent("ventrysjob.message.job.progress_reset"), false);
        return 1;
    }
}
