package com.bountymod.gui;

import com.bountymod.BountyMod;
import com.bountymod.model.Bounty;
import com.bountymod.util.ItemUtil;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BountyListScreen extends ScreenHandler {
    private final Inventory inventory;
    private final PlayerEntity player;
    
    public BountyListScreen(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(54));
    }
    
    public BountyListScreen(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.inventory = inventory;
        this.player = playerInventory.player;
        
        checkSize(inventory, 54);
        inventory.onOpen(playerInventory.player);
        
        // Add inventory slots (6 rows of 9)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false; // Display only
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
        
        List<Bounty> bounties = BountyMod.getBountyManager().getAllBounties();
        
        // Create "New Bounty" button (Green wool)
        ItemStack createButton = new ItemStack(Items.LIME_WOOL);
        ItemUtil.setCustomName(createButton, Text.literal("Nieuwe Bounty Maken").formatted(Formatting.GREEN, Formatting.BOLD));
        inventory.setStack(0, createButton);
        
        // Display active bounties (Player heads)
        int slot = 2;
        for (Bounty bounty : bounties) {
            if (slot >= 54) break;
            if (bounty.isExpired()) continue;
            
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            ItemUtil.setCustomName(head, Text.literal(bounty.getTargetName()).formatted(Formatting.RED, Formatting.BOLD));
            
            inventory.setStack(slot, head);
            slot++;
        }
        
        // Add "Refresh" button (Blue wool) at bottom right
        ItemStack refreshButton = new ItemStack(Items.LIGHT_BLUE_WOOL);
        ItemUtil.setCustomName(refreshButton, Text.literal("Vernieuwen").formatted(Formatting.AQUA, Formatting.BOLD));
        inventory.setStack(53, refreshButton);
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
        
        // Create new bounty button
        if (slotIndex == 0 && clicked.getItem() == Items.LIME_WOOL) {
            serverPlayer.closeHandledScreen();
            serverPlayer.openHandledScreen(new PlayerSelectionScreen.Factory());
            return;
        }
        
        // Refresh button
        if (slotIndex == 53 && clicked.getItem() == Items.LIGHT_BLUE_WOOL) {
            populateInventory();
            return;
        }
        
        // Bounty head clicked - show details
        if (clicked.getItem() == Items.PLAYER_HEAD) {
            List<Bounty> bounties = BountyMod.getBountyManager().getAllBounties();
            int bountyIndex = slotIndex - 2;
            
            if (bountyIndex >= 0 && bountyIndex < bounties.size()) {
                Bounty bounty = bounties.get(bountyIndex);
                showBountyDetails(serverPlayer, bounty);
            }
        }
    }
    
    private void showBountyDetails(ServerPlayerEntity player, Bounty bounty) {
        player.closeHandledScreen();
        player.openHandledScreen(new BountyDetailsScreen.Factory(bounty));
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
    
    public static class Factory implements NamedScreenHandlerFactory {
        @Override
        public Text getDisplayName() {
            return Text.literal("Bounty Menu");
        }
        
        @Nullable
        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new BountyListScreen(syncId, playerInventory);
        }
    }
}
