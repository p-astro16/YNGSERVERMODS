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

public class BountyDetailsScreen extends ScreenHandler {
    private final Inventory inventory;
    private final Bounty bounty;
    private final PlayerEntity player;
    
    public BountyDetailsScreen(int syncId, PlayerInventory playerInventory, Bounty bounty) {
        this(syncId, playerInventory, new SimpleInventory(54), bounty);
    }
    
    public BountyDetailsScreen(int syncId, PlayerInventory playerInventory, Inventory inventory, Bounty bounty) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.inventory = inventory;
        this.bounty = bounty;
        this.player = playerInventory.player;
        
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
        
        // Target info (Player head)
        ItemStack targetHead = new ItemStack(Items.PLAYER_HEAD);
        ItemUtil.setCustomName(targetHead, Text.literal("Target: " + bounty.getTargetName()).formatted(Formatting.RED, Formatting.BOLD));
        inventory.setStack(4, targetHead);
        
        // Creator info
        ItemStack creatorInfo = new ItemStack(Items.PAPER);
        ItemUtil.setCustomName(creatorInfo, Text.literal("Geplaatst door: " + bounty.getCreatorName()).formatted(Formatting.GOLD));
        inventory.setStack(13, creatorInfo);
        
        // Time remaining
        ItemStack timeInfo = new ItemStack(Items.CLOCK);
        ItemUtil.setCustomName(timeInfo, Text.literal("Tijd over: " + bounty.getFormattedTimeRemaining()).formatted(Formatting.YELLOW));
        inventory.setStack(22, timeInfo);
        
        // Rewards display (center area)
        int rewardSlot = 28;
        for (ItemStack reward : bounty.getRewards()) {
            if (!reward.isEmpty() && rewardSlot < 35) {
                inventory.setStack(rewardSlot, reward.copy());
                rewardSlot++;
            }
        }
        
        // Back button
        ItemStack backButton = new ItemStack(Items.ARROW);
        ItemUtil.setCustomName(backButton, Text.literal("Terug").formatted(Formatting.WHITE, Formatting.BOLD));
        inventory.setStack(45, backButton);
        
        // Remove button (only for OPs or creator)
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (serverPlayer.hasPermissionLevel(2) || bounty.getCreatorUUID().equals(player.getUuid())) {
                ItemStack removeButton = new ItemStack(Items.RED_WOOL);
                ItemUtil.setCustomName(removeButton, Text.literal("Verwijder Bounty").formatted(Formatting.RED, Formatting.BOLD));
                inventory.setStack(53, removeButton);
            }
        }
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
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Back button
        if (slotIndex == 45 && !clicked.isEmpty() && clicked.getItem() == Items.ARROW) {
            serverPlayer.closeHandledScreen();
            serverPlayer.openHandledScreen(new BountyListScreen.Factory());
            return;
        }
        
        // Remove button
        if (slotIndex == 53 && !clicked.isEmpty() && clicked.getItem() == Items.RED_WOOL) {
            if (serverPlayer.hasPermissionLevel(2) || bounty.getCreatorUUID().equals(player.getUuid())) {
                BountyMod.getBountyManager().removeBounty(bounty.getId());
                serverPlayer.sendMessage(Text.literal("Bounty verwijderd!").formatted(Formatting.GREEN), false);
                serverPlayer.closeHandledScreen();
                serverPlayer.openHandledScreen(new BountyListScreen.Factory());
            }
            return;
        }
        
        // Prevent any other interactions with display items - it's read-only
        // Don't call super.onSlotClick for display slots
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // Disable shift-clicking items out of the display
        return ItemStack.EMPTY;
    }
    
    public static class Factory implements NamedScreenHandlerFactory {
        private final Bounty bounty;
        
        public Factory(Bounty bounty) {
            this.bounty = bounty;
        }
        
        @Override
        public Text getDisplayName() {
            return Text.literal("Bounty Details: " + bounty.getTargetName());
        }
        
        @Nullable
        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new BountyDetailsScreen(syncId, playerInventory, bounty);
        }
    }
}
