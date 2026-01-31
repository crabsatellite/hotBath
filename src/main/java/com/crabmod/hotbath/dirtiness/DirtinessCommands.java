package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.util.AdvancementHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Debug commands for testing dirtiness system.
 * Commands:
 * - /dirtiness get - Show current dirtiness percentage
 * - /dirtiness set <0.0-1.0> - Set dirtiness to specific value
 * - /dirtiness clean - Reset dirtiness to 0
 * - /dirtiness flies - Trigger fly spawning (sets 100% dirty + 2 days)
 */
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class DirtinessCommands {
    
    // Translation keys
    private static final String KEY_GET = "commands.hotbath.dirtiness.get";
    private static final String KEY_GET_FLIES = "commands.hotbath.dirtiness.get.flies_active";
    private static final String KEY_GET_MAX_DIRTY = "commands.hotbath.dirtiness.get.max_dirty";
    private static final String KEY_SET = "commands.hotbath.dirtiness.set";
    private static final String KEY_CLEAN = "commands.hotbath.dirtiness.clean";
    private static final String KEY_FLIES = "commands.hotbath.dirtiness.flies";
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("dirtiness")
            .requires(source -> source.hasPermission(2)) // Requires OP level 2
            .then(Commands.literal("get")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    DirtinessData data = player.getData(DirtinessAttachment.DIRTINESS);
                    long gameTime = player.level().getGameTime();
                    float dirtiness = data.getDirtiness(gameTime);
                    long ticksAtMax = data.getTicksAtMaxDirty(gameTime);
                    boolean hasFlies = data.shouldSpawnFlies(gameTime);
                    
                    // Build the status message
                    Component statusSuffix;
                    if (hasFlies) {
                        statusSuffix = Component.translatable(KEY_GET_FLIES);
                    } else if (ticksAtMax > 0) {
                        statusSuffix = Component.translatable(KEY_GET_MAX_DIRTY, ticksAtMax, DirtinessData.TICKS_AT_MAX_FOR_FLIES);
                    } else {
                        statusSuffix = Component.empty();
                    }
                    
                    context.getSource().sendSuccess(
                        () -> Component.translatable(KEY_GET, String.format("%.1f", dirtiness * 100), String.format("%.3f", dirtiness)).append(statusSuffix),
                        false
                    );
                    return (int)(dirtiness * 100);
                })
            )
            .then(Commands.literal("set")
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 1.0f))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        float value = FloatArgumentType.getFloat(context, "value");
                        DirtinessData data = player.getData(DirtinessAttachment.DIRTINESS);
                        data.setDirtinessDebug(player.level().getGameTime(), value);
                        DirtinessNetworking.syncToClient(player);
                        context.getSource().sendSuccess(
                            () -> Component.translatable(KEY_SET, String.format("%.1f", value * 100)),
                            true
                        );
                        return (int)(value * 100);
                    })
                )
            )
            .then(Commands.literal("clean")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    DirtinessData data = player.getData(DirtinessAttachment.DIRTINESS);
                    data.takeBath(player.level().getGameTime());
                    DirtinessNetworking.syncToClient(player);
                    context.getSource().sendSuccess(
                        () -> Component.translatable(KEY_CLEAN),
                        true
                    );
                    return 1;
                })
            )
            .then(Commands.literal("flies")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    DirtinessData data = player.getData(DirtinessAttachment.DIRTINESS);
                    long gameTime = player.level().getGameTime();
                    
                    // Set to 100% dirty and simulate being at max for 2+ days
                    data.setDirtinessDebugWithFlies(gameTime);
                    DirtinessNetworking.syncToClient(player);
                    
                    // Award "Something Smells..." advancement
                    AdvancementHelper.tryAwardAdvancement(player, "hotbath:something_smells", "code_triggered");
                    
                    context.getSource().sendSuccess(
                        () -> Component.translatable(KEY_FLIES),
                        true
                    );
                    return 1;
                })
            )
        );
    }
}
