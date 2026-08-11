package com.ventrys.job.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ventrys.job.compat.VentrysPermsBridge;
import com.ventrys.job.util.RealTimeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeDebugCommand {
    
    private static boolean debugEnabled = false;
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ventrystime")
            .requires(source -> VentrysPermsBridge.staff(source, "ventryspermissions.job.time"))
            .then(Commands.literal("info")
                .executes(TimeDebugCommand::showTimeInfo))
            .then(Commands.literal("debug")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(ctx -> toggleDebug(ctx, BoolArgumentType.getBool(ctx, "enabled"))))
                .executes(ctx -> toggleDebug(ctx, !debugEnabled)))
            .then(Commands.literal("sync")
                .executes(TimeDebugCommand::forceSync))
        );
    }
    
    private static int showTimeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        
        if (level == null) {
            source.sendFailure(new TranslatableComponent("ventrysjob.command.time.server_not_found"));
            return 0;
        }
        
        // Heure IRL Paris
        ZonedDateTime parisTime = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
        int irlHour = parisTime.getHour();
        int irlMinute = parisTime.getMinute();
        
        // Conversion
        long targetTicks = RealTimeManager.getCurrentGameTime();
        long currentTicks = level.getDayTime();
        long difference = targetTicks - currentTicks;
        
        // Normaliser différence
        if (difference > 12000) difference -= 24000;
        else if (difference < -12000) difference += 24000;
        
        long targetGameTicksNormalized = (targetTicks % 24000 + 24000) % 24000;
        int targetGameHour = (int) (targetGameTicksNormalized / 1000);
        int targetGameMinute = (int) ((targetGameTicksNormalized % 1000) * 60 / 1000);
        
        boolean doDaylightCycle = level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).get();
        
        // Calculer heure de jeu actuelle
        long currentGameTicks = currentTicks % 24000;
        int currentGameHour = (int) (currentGameTicks / 1000);
        int currentGameMinute = (int) ((currentGameTicks % 1000) * 60 / 1000);
        
        // Afficher les infos
        source.sendSuccess(new TranslatableComponent("ventrysjob.command.time.debug.header"), false);
        source.sendSuccess(new TranslatableComponent("ventrysjob.command.time.irl_paris", String.format("%02d:%02d", irlHour, irlMinute)), false);
        boolean running = RealTimeManager.isTimeRunning(parisTime);
        source.sendSuccess(new TranslatableComponent(
                running ? "ventrysjob.command.time.window.active" : "ventrysjob.command.time.window.frozen",
                String.format("%02d:00", RealTimeManager.WINDOW_START_HOUR),
                String.format("%02d:00", RealTimeManager.WINDOW_END_HOUR),
                String.format("%.3f", RealTimeManager.gameHoursPerIrlHour())
        ), false);
        if (running) {
            long sec = RealTimeManager.secondsIntoWindow(parisTime);
            source.sendSuccess(new TranslatableComponent(
                    "ventrysjob.command.time.window.progress",
                    sec,
                    RealTimeManager.WINDOW_SECONDS,
                    String.format("%.1f", RealTimeManager.windowProgress(parisTime) * 100.0D)
            ), false);
        }
        source.sendSuccess(new TranslatableComponent(
            "ventrysjob.command.time.game_target",
            String.format("%02d:%02d (doDaylightCycle=%s)", targetGameHour, targetGameMinute, doDaylightCycle),
            targetGameTicksNormalized
        ), false);
        source.sendSuccess(new TranslatableComponent("ventrysjob.command.time.game_current", String.format("%02d:%02d", currentGameHour, currentGameMinute), currentGameTicks), false);
        source.sendSuccess(new TranslatableComponent("ventrysjob.command.time.difference", difference, String.format("%.1f", difference / 20.0)), false);
        source.sendSuccess(new TranslatableComponent("ventrysjob.command.time.world_raw", currentTicks), false);
        source.sendSuccess(new TranslatableComponent("ventrysjob.command.time.world_normalized", currentTicks % 24000), false);
        source.sendSuccess(new TranslatableComponent(
            "ventrysjob.command.time.do_daylight_cycle",
            doDaylightCycle
        ), false);
        
        return 1;
    }
    
    private static int toggleDebug(CommandContext<CommandSourceStack> context, boolean enabled) {
        debugEnabled = enabled;
        CommandSourceStack source = context.getSource();
        source.sendSuccess(new TranslatableComponent(enabled ? "ventrysjob.command.time.debug.enabled" : "ventrysjob.command.time.debug.disabled"), true);
        return 1;
    }
    
    private static int forceSync(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        
        if (level == null) {
            source.sendFailure(new TranslatableComponent("ventrysjob.command.time.server_not_found"));
            return 0;
        }
        
        long before = level.getDayTime();
        RealTimeManager.syncWorldTime(level);
        long after = level.getDayTime();
        
        source.sendSuccess(new TranslatableComponent("ventrysjob.command.time.sync.forced", before, after), true);
        return 1;
    }
    
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
}

