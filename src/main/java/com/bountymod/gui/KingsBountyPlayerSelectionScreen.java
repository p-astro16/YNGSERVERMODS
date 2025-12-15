package com.bountymod.gui;

import com.bountymod.util.ItemUtil;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class KingsBountyPlayerSelectionScreen extends ScreenHandler {
    private final Inventory inventory;
    private final List<GameProfile> availablePlayers;
    
    public KingsBountyPlayerSelectionScreen(int syncId, PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.inventory = new SimpleInventory(54);
        this.availablePlayers = new ArrayList<>();
        
        // Create 6 rows of 9 slots each
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        
        setupPlayerHeads(playerInventory.player);
    }
    
    private void setupPlayerHeads(PlayerEntity player) {
        inventory.clear();
        availablePlayers.clear();
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }
        
        // Get online players
        for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
            if (!onlinePlayer.getUuid().equals(player.getUuid())) {
                availablePlayers.add(onlinePlayer.getGameProfile());
            }
        }
        
        // Add player heads to inventory (max 45 players to leave room for back button)
        int slot = 0;
        for (int i = 0; i < Math.min(availablePlayers.size(), 45); i++) {
            GameProfile profile = availablePlayers.get(i);
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            ItemUtil.setCustomName(head, Text.literal(profile.getName()).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
            inventory.setStack(slot, head);
            slot++;
        }
        
        // Back button
        ItemStack backButton = new ItemStack(Items.ARROW);
        ItemUtil.setCustomName(backButton, Text.literal("Terug").formatted(Formatting.WHITE, Formatting.BOLD));
        inventory.setStack(45, backButton);
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
    
    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex < 0 || slotIndex >= 54) {
            return;
        }
        
        ItemStack clicked = inventory.getStack(slotIndex);
        if (clicked.isEmpty()) {
            return;
        }
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Back button
        if (slotIndex == 45 && clicked.getItem() == Items.ARROW) {
            serverPlayer.closeHandledScreen();
            return;
        }
        
        // Player head clicked
        if (clicked.getItem() == Items.PLAYER_HEAD && slotIndex < availablePlayers.size()) {
            // No cooldown check for ops using /kingsbounty
            // Check if target has immunity (even for ops)
            GameProfile targetProfile = availablePlayers.get(slotIndex);
            com.bountymod.manager.BountyManager manager = com.bountymod.BountyMod.getBountyManager();
            
            if (manager.hasImmunity(targetProfile.getId())) {
                String timeRemaining = manager.getFormattedImmunityRemaining(targetProfile.getId());
                serverPlayer.closeHandledScreen();
                serverPlayer.sendMessage(Text.literal(targetProfile.getName()).formatted(Formatting.GOLD)
                    .append(Text.literal(" heeft King's Bounty immunity voor nog ").formatted(Formatting.LIGHT_PURPLE))
                    .append(Text.literal(timeRemaining).formatted(Formatting.YELLOW))
                    .append(Text.literal("!").formatted(Formatting.LIGHT_PURPLE)), false);
                return;
            }
            
            // Open reward selection screen for King's Bounty
            serverPlayer.closeHandledScreen();
            serverPlayer.openHandledScreen(new KingsBountyRewardSelectionScreen.Factory(targetProfile));
        }
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }
    
    public static class Factory implements NamedScreenHandlerFactory {
        @Override
        public Text getDisplayName() {
            return Text.literal("⚜ King's Bounty - Selecteer Target ⚜")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
        }
        
        @Nullable
        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new KingsBountyPlayerSelectionScreen(syncId, playerInventory);
        }
    }
}
