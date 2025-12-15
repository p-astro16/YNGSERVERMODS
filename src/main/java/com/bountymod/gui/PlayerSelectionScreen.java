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
import net.minecraft.util.UserCache;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PlayerSelectionScreen extends ScreenHandler {
    private final Inventory inventory;
    private final PlayerEntity player;
    private final List<GameProfile> availablePlayers;
    
    public PlayerSelectionScreen(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(54));
    }
    
    public PlayerSelectionScreen(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.inventory = inventory;
        this.player = playerInventory.player;
        this.availablePlayers = new ArrayList<>();
        
        checkSize(inventory, 54);
        inventory.onOpen(playerInventory.player);
        
        // Add inventory slots
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
        
        // Add player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        
        // Add player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
        
        populateInventory();
    }
    
    private void populateInventory() {
        inventory.clear();
        availablePlayers.clear();
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }
        
        // Get all known players (online and offline)
        UserCache userCache = server.getUserCache();
        if (userCache != null) {
            // Add online players first
            for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
                if (!onlinePlayer.getUuid().equals(player.getUuid())) {
                    availablePlayers.add(onlinePlayer.getGameProfile());
                }
            }
        }
        
        // Display players as heads
        int slot = 0;
        for (GameProfile profile : availablePlayers) {
            if (slot >= 45) break; // Leave space for back button
            
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            ItemUtil.setCustomName(head, Text.literal(profile.getName()).formatted(Formatting.YELLOW, Formatting.BOLD));
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
            serverPlayer.openHandledScreen(new BountyListScreen.Factory());
            return;
        }
        
        // Player head clicked
        if (clicked.getItem() == Items.PLAYER_HEAD && slotIndex < availablePlayers.size()) {
            // Check cooldown first
            com.bountymod.manager.BountyManager manager = com.bountymod.BountyMod.getBountyManager();
            if (manager.isOnCooldown(serverPlayer.getUuid())) {
                String timeRemaining = manager.getFormattedCooldownRemaining(serverPlayer.getUuid());
                serverPlayer.closeHandledScreen();
                serverPlayer.sendMessage(Text.literal("Je moet nog ").formatted(Formatting.RED)
                    .append(Text.literal(timeRemaining).formatted(Formatting.GOLD))
                    .append(Text.literal(" wachten voordat je een nieuwe bounty kan plaatsen.").formatted(Formatting.RED)), false);
                return;
            }
            
            // Check if target has King's Bounty immunity
            GameProfile targetProfile = availablePlayers.get(slotIndex);
            if (manager.hasImmunity(targetProfile.getId())) {
                String timeRemaining = manager.getFormattedImmunityRemaining(targetProfile.getId());
                serverPlayer.closeHandledScreen();
                serverPlayer.sendMessage(Text.literal(targetProfile.getName()).formatted(Formatting.GOLD)
                    .append(Text.literal(" heeft King's Bounty immunity voor nog ").formatted(Formatting.LIGHT_PURPLE))
                    .append(Text.literal(timeRemaining).formatted(Formatting.YELLOW))
                    .append(Text.literal("!").formatted(Formatting.LIGHT_PURPLE)), false);
                return;
            }
            
            GameProfile selectedProfile = availablePlayers.get(slotIndex);
            serverPlayer.closeHandledScreen();
            serverPlayer.openHandledScreen(new RewardSelectionScreen.Factory(selectedProfile));
        }
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
    
    public static class Factory implements NamedScreenHandlerFactory {
        @Override
        public Text getDisplayName() {
            return Text.literal("Selecteer een Speler");
        }
        
        @Nullable
        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new PlayerSelectionScreen(syncId, playerInventory);
        }
    }
}
