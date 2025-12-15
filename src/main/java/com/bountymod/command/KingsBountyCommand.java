package com.bountymod.command;

import com.bountymod.BountyMod;
import com.bountymod.gui.KingsBountyPlayerSelectionScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class KingsBountyCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("kingsbounty")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2
                .executes(KingsBountyCommand::execute)
        );
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            player.sendMessage(
                Text.literal("⚜ King's Bounty Mode ⚜")
                    .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD),
                false
            );
            player.sendMessage(
                Text.literal("De volgende bounty die je plaatst wordt automatisch een King's Bounty (10x rewards, 1000 blok radius).")
                    .formatted(Formatting.YELLOW),
                false
            );
            player.openHandledScreen(new KingsBountyPlayerSelectionScreen.Factory());
            return 1;
        }
        
        return 0;
    }
}
