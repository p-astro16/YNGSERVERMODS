package com.bountymod.event;

import com.bountymod.BountyMod;
import com.bountymod.manager.BountyManager;
import com.bountymod.model.Bounty;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.TimeUnit;

public class ChatMessageHandler {
    
    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            handleChatMessage(sender, message.getContent().getString());
        });
    }
    
    private static void handleChatMessage(ServerPlayerEntity player, String messageText) {
        BountyManager bountyManager = BountyMod.getBountyManager();
        BountyManager.PendingBounty pending = bountyManager.getPendingBounty(player.getUuid());
        
        if (pending == null) {
            return; // No pending bounty for this player
        }
        
        // Check for cancel
        if (messageText.equalsIgnoreCase("cancel")) {
            bountyManager.removePendingBounty(player.getUuid());
            
            // Return items to player
            for (net.minecraft.item.ItemStack reward : pending.rewards) {
                if (!reward.isEmpty()) {
                    player.getInventory().offerOrDrop(reward);
                }
            }
            
            player.sendMessage(
                Text.literal("Bounty aanmaken geannuleerd. Items zijn teruggegeven.")
                    .formatted(Formatting.RED),
                false
            );
            return;
        }
        
        // Try to parse as minutes
        try {
            int minutes = Integer.parseInt(messageText.trim());
            
            // Validate range (10 minutes to 1 week)
            if (minutes < 10) {
                player.sendMessage(
                    Text.literal("Minimale duur is 10 minuten!")
                        .formatted(Formatting.RED),
                    false
                );
                return;
            }
            
            if (minutes > 10080) { // 7 days * 24 hours * 60 minutes
                player.sendMessage(
                    Text.literal("Maximale duur is 10080 minuten (1 week)!")
                        .formatted(Formatting.RED),
                    false
                );
                return;
            }
            
            // Create the bounty
            long durationMillis = TimeUnit.MINUTES.toMillis(minutes);
            
            // 1/60 chance for King's Bounty
            boolean isKingsBounty = (new java.util.Random().nextInt(60) == 0);
            
            Bounty bounty = new Bounty(
                pending.targetProfile.getId(),
                pending.targetProfile.getName(),
                player.getUuid(),
                player.getName().getString(),
                pending.rewards,
                durationMillis,
                isKingsBounty
            );
            
            bountyManager.addBounty(bounty);
            bountyManager.removePendingBounty(player.getUuid());
            
            player.sendMessage(
                Text.literal("Bounty succesvol aangemaakt op ")
                    .formatted(Formatting.GREEN)
                    .append(Text.literal(pending.targetProfile.getName()).formatted(Formatting.GOLD))
                    .append(Text.literal(" voor ").formatted(Formatting.GREEN))
                    .append(Text.literal(minutes + " minuten").formatted(Formatting.YELLOW))
                    .append(Text.literal("!").formatted(Formatting.GREEN)),
                false
            );
            
        } catch (NumberFormatException e) {
            player.sendMessage(
                Text.literal("Ongeldige invoer! Voer een getal in (minuten) of typ 'cancel' om te annuleren.")
                    .formatted(Formatting.RED),
                false
            );
        }
    }
}
