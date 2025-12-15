package com.bountymod.gui;

import com.bountymod.util.ItemUtil;
import com.bountymod.util.RewardValidator;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class KingsBountyRewardSelectionScreen extends ScreenHandler {
    private final Inventory chestInventory;
    private final GameProfile targetProfile;
    private boolean confirmed = false;
    
    public KingsBountyRewardSelectionScreen(int syncId, PlayerInventory playerInventory, GameProfile targetProfile) {
        this(syncId, playerInventory, new SimpleInventory(27), targetProfile);
    }
    
    public KingsBountyRewardSelectionScreen(int syncId, PlayerInventory playerInventory, Inventory chestInventory, GameProfile targetProfile) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.chestInventory = chestInventory;
        this.targetProfile = targetProfile;
        this.confirmed = false;
        
        checkSize(chestInventory, 27);
        chestInventory.onOpen(playerInventory.player);
        
        // Chest inventory (3 rows of 9 slots) - only accept valid rewards
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(chestInventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        int slotIndex = this.getIndex();
                        // Don't allow items in button/info slots
                        if (slotIndex == 4 || slotIndex == 18 || slotIndex == 26) {
                            return false;
                        }
                        return RewardValidator.isValidReward(stack);
                    }
                });
            }
        }
        
        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            }
        }
        
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 143));
        }
        
        setupChestButtons();
    }
    
    private void setupChestButtons() {
        chestInventory.clear();
        
        // Info item (center top)
        ItemStack info = new ItemStack(Items.PAPER);
        ItemUtil.setCustomName(info, Text.literal("⚜ King's Bounty Rewards (10x) ⚜").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        chestInventory.setStack(4, info);
        
        // Cancel button (bottom left corner - slot 18)
        ItemStack cancelButton = new ItemStack(Items.RED_WOOL);
        ItemUtil.setCustomName(cancelButton, Text.literal("Annuleren").formatted(Formatting.RED, Formatting.BOLD));
        chestInventory.setStack(18, cancelButton);
        
        // Confirm button (bottom right corner - slot 26)
        ItemStack confirmButton = new ItemStack(Items.LIME_WOOL);
        ItemUtil.setCustomName(confirmButton, Text.literal("Bevestigen").formatted(Formatting.GREEN, Formatting.BOLD));
        chestInventory.setStack(26, confirmButton);
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
    
    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex < 0 || slotIndex >= 27) {
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }
        
        ItemStack clicked = chestInventory.getStack(slotIndex);
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Cancel button (slot 18 - bottom left)
        if (slotIndex == 18 && clicked.getItem() == Items.RED_WOOL) {
            // Return items to player
            for (int i = 0; i < chestInventory.size(); i++) {
                if (i == 4 || i == 18 || i == 26) continue; // Skip button slots
                ItemStack stack = chestInventory.getStack(i);
                if (!stack.isEmpty()) {
                    player.getInventory().offerOrDrop(stack);
                }
            }
            serverPlayer.closeHandledScreen();
            serverPlayer.openHandledScreen(new KingsBountyPlayerSelectionScreen.Factory());
            return;
        }
        
        // Confirm button (slot 26 - bottom right)
        if (slotIndex == 26 && clicked.getItem() == Items.LIME_WOOL) {
            // Collect and validate rewards
            List<ItemStack> rewards = new ArrayList<>();
            for (int i = 0; i < chestInventory.size(); i++) {
                if (i == 4 || i == 18 || i == 26) continue; // Skip button/info slots
                ItemStack stack = chestInventory.getStack(i);
                if (!stack.isEmpty()) {
                    if (!RewardValidator.isValidReward(stack)) {
                        player.sendMessage(Text.literal("Ongeldige reward gedetecteerd! ").formatted(Formatting.RED)
                            .append(Text.literal(RewardValidator.getInvalidItemMessage()).formatted(Formatting.YELLOW)), false);
                        return;
                    }
                    rewards.add(stack.copy());
                }
            }
            
            if (rewards.isEmpty()) {
                player.sendMessage(Text.literal("Je moet minstens 1 item als reward toevoegen!").formatted(Formatting.RED), false);
                return;
            }
            
            // Mark as confirmed so items won't be returned on close
            confirmed = true;
            
            // Open time selection screen (with forceKingsBounty flag)
            serverPlayer.closeHandledScreen();
            serverPlayer.openHandledScreen(new TimeSelectionScreen.Factory(targetProfile, rewards, true));
            return;
        }
        
        // Info item (slot 4) - prevent interaction
        if (slotIndex == 4 && clicked.getItem() == Items.PAPER) {
            return;
        }
        
        // Allow normal inventory interactions for other slots
        super.onSlotClick(slotIndex, button, actionType, player);
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        
        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            
            // From chest to player inventory
            if (slotIndex < 27) {
                // Don't allow shift-clicking buttons or info
                if (slotIndex == 4 || slotIndex == 18 || slotIndex == 26) {
                    return ItemStack.EMPTY;
                }
                if (!this.insertItem(originalStack, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory to chest
            else if (slotIndex >= 27) {
                if (!this.insertItem(originalStack, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        
        return newStack;
    }
    
    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        
        // Only return items if NOT confirmed (cancelled or closed)
        if (!confirmed && !player.getWorld().isClient) {
            for (int i = 0; i < chestInventory.size(); i++) {
                if (i == 4 || i == 18 || i == 26) continue; // Skip button/info slots
                ItemStack stack = chestInventory.getStack(i);
                if (!stack.isEmpty()) {
                    player.getInventory().offerOrDrop(stack);
                }
            }
        }
        
        chestInventory.onClose(player);
    }
    
    public static class Factory implements NamedScreenHandlerFactory {
        private final GameProfile targetProfile;
        
        public Factory(GameProfile targetProfile) {
            this.targetProfile = targetProfile;
        }
        
        @Override
        public Text getDisplayName() {
            return Text.literal("⚜ King's Bounty Rewards ⚜").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
        }
        
        @Nullable
        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new KingsBountyRewardSelectionScreen(syncId, playerInventory, targetProfile);
        }
    }
}
