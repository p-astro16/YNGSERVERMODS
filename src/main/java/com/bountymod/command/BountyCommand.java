package com.bountymod.command;

import com.bountymod.BountyMod;
import com.bountymod.gui.BountyListScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BountyCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bounty")
            .executes(BountyCommand::openBountyMenu));
    }
    
    private static int openBountyMenu(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Dit commando kan alleen door spelers worden uitgevoerd"));
            return 0;
        }
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            
            // Open the bounty list GUI
            player.openHandledScreen(new BountyListScreen.Factory());
            
            return 1;
        } catch (Exception e) {
            BountyMod.LOGGER.error("Failed to open bounty menu", e);
            source.sendError(Text.literal("Er ging iets mis bij het openen van het bounty menu"));
            return 0;
        }
    }
}
