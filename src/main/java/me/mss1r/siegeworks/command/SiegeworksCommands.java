package me.mss1r.siegeworks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.mss1r.siegeworks.debug.SiegeworksDebug;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class SiegeworksCommands {
    private SiegeworksCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext registry,
                                Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("siegeworks")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .executes(SiegeworksCommands::status)
                        .then(Commands.literal("status").executes(SiegeworksCommands::status))
                        .then(Commands.literal("reset").executes(SiegeworksCommands::reset))
                        .then(Commands.literal("instant_fire")
                                .executes(context -> setInstantFire(context, !SiegeworksDebug.instantFire()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setInstantFire(context,
                                                BoolArgumentType.getBool(context, "enabled")))))
                        .then(Commands.literal("auto_drive")
                                .executes(context -> setAutoDrive(context, !SiegeworksDebug.autoDrive()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setAutoDrive(context,
                                                BoolArgumentType.getBool(context, "enabled")))))
                        .then(Commands.literal("recruits")
                                .executes(context -> setRecruits(context, !SiegeworksDebug.recruits()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setRecruits(context,
                                                BoolArgumentType.getBool(context, "enabled")))))));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "Siegeworks debug: instant_fire=" + onOff(SiegeworksDebug.instantFire())
                        + ", auto_drive=" + onOff(SiegeworksDebug.autoDrive())
                        + ", recruits=" + onOff(SiegeworksDebug.recruits())), false);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        SiegeworksDebug.setInstantFire(false);
        SiegeworksDebug.setAutoDrive(false);
        SiegeworksDebug.setRecruits(false);
        stopAutoDrive(context.getSource());
        context.getSource().sendSuccess(() -> Component.literal("Siegeworks debug features disabled"), true);
        return 1;
    }

    private static int setInstantFire(CommandContext<CommandSourceStack> context, boolean enabled) {
        SiegeworksDebug.setInstantFire(enabled);
        return report(context, "instant_fire", enabled);
    }

    private static int setAutoDrive(CommandContext<CommandSourceStack> context, boolean enabled) {
        SiegeworksDebug.setAutoDrive(enabled);
        if (!enabled) {
            stopAutoDrive(context.getSource());
        }
        return report(context, "auto_drive", enabled);
    }

    private static int setRecruits(CommandContext<CommandSourceStack> context, boolean enabled) {
        SiegeworksDebug.setRecruits(enabled);
        return report(context, "recruits", enabled);
    }

    private static int report(CommandContext<CommandSourceStack> context, String feature, boolean enabled) {
        context.getSource().sendSuccess(() -> Component.literal(
                "Siegeworks debug " + feature + ": " + onOff(enabled)), true);
        return 1;
    }

    private static void stopAutoDrive(CommandSourceStack source) {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AbstractSiegeEntity siege) {
                    siege.setDebugAutoDriving(false);
                }
            }
        }
    }

    private static String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }
}
