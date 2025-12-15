package com.bountymod.gui;

import com.bountymod.BountyMod;
import com.bountymod.model.Bounty;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeSelectionScreen extends ScreenHandler {
    private final Inventory inventory;
    private final PlayerEntity player;
    private final GameProfile targetProfile;
    private final List<ItemStack> rewards;
    private final boolean forceKingsBounty;
    
    public TimeSelectionScreen(int syncId, PlayerInventory playerInventory, GameProfile targetProfile, List<ItemStack> rewards) {
        this(syncId, playerInventory, new SimpleInventory(27), targetProfile, rewards, false);
    }
    
    public TimeSelectionScreen(int syncId, PlayerInventory playerInventory, GameProfile targetProfile, List<ItemStack> rewards, boolean forceKingsBounty) {
        this(syncId, playerInventory, new SimpleInventory(27), targetProfile, rewards, forceKingsBounty);
    }
    
    public TimeSelectionScreen(int syncId, PlayerInventory playerInventory, Inventory inventory, 
                               GameProfile targetProfile, List<ItemStack> rewards, boolean forceKingsBounty) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.inventory = inventory;
        this.player = playerInventory.player;
        this.targetProfile = targetProfile;
        this.rewards = rewards;
        this.forceKingsBounty = forceKingsBounty;
        
        checkSize(inventory, 27);
        inventory.onOpen(playerInventory.player);
        
        // Add inventory slots (3 rows of 9)
        for (int row = 0; row < 3; row++) {
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
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        
        // Add player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        
        populateInventory();
    }
    
    private void populateInventory() {
        inventory.clear();
        
        // Title
        ItemStack title = new ItemStack(Items.CLOCK);
        ItemUtil.setCustomName(title, Text.literal("Selecteer Bounty Duur").formatted(Formatting.GOLD, Formatting.BOLD));
        inventory.setStack(4, title);
        
        // 10 minutes
        ItemStack min10 = new ItemStack(Items.PAPER);
        ItemUtil.setCustomName(min10, Text.literal("10 Minuten").formatted(Formatting.YELLOW));
        inventory.setStack(10, min10);
        
        // 1 hour
        ItemStack hour1 = new ItemStack(Items.PAPER);
        ItemUtil.setCustomName(hour1, Text.literal("1 Uur").formatted(Formatting.YELLOW));
        inventory.setStack(11, hour1);
        
        // 6 hours
        ItemStack hour6 = new ItemStack(Items.PAPER);
        ItemUtil.setCustomName(hour6, Text.literal("6 Uur").formatted(Formatting.YELLOW));
        inventory.setStack(12, hour6);
        
        // 1 day
        ItemStack day1 = new ItemStack(Items.PAPER);
        ItemUtil.setCustomName(day1, Text.literal("1 Dag").formatted(Formatting.YELLOW));
        inventory.setStack(13, day1);
        
        // 3 days
        ItemStack day3 = new ItemStack(Items.PAPER);
        ItemUtil.setCustomName(day3, Text.literal("3 Dagen").formatted(Formatting.YELLOW));
        inventory.setStack(14, day3);
        
        // 1 week
        ItemStack week1 = new ItemStack(Items.PAPER);
        ItemUtil.setCustomName(week1, Text.literal("1 Week").formatted(Formatting.YELLOW));
        inventory.setStack(15, week1);
        
        // Custom (Sign)
        ItemStack custom = new ItemStack(Items.OAK_SIGN);
        ItemUtil.setCustomName(custom, Text.literal("Aangepast (Sign)").formatted(Formatting.AQUA));
        inventory.setStack(16, custom);
        
        // Back button
        ItemStack backButton = new ItemStack(Items.ARROW);
        ItemUtil.setCustomName(backButton, Text.literal("Terug").formatted(Formatting.WHITE, Formatting.BOLD));
        inventory.setStack(18, backButton);
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
    
    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex < 0 || slotIndex >= 27) {
            return;
        }
        
        ItemStack clicked = inventory.getStack(slotIndex);
        if (clicked.isEmpty()) {
            return;
        }
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        long durationMillis = -1;
        
        // Back button
        if (slotIndex == 18 && clicked.getItem() == Items.ARROW) {
            serverPlayer.closeHandledScreen();
            serverPlayer.openHandledScreen(new RewardSelectionScreen.Factory(targetProfile));
            return;
        }
        
        // Time selections
        if (slotIndex == 10) { // 10 minutes
            durationMillis = TimeUnit.MINUTES.toMillis(10);
        } else if (slotIndex == 11) { // 1 hour
            durationMillis = TimeUnit.HOURS.toMillis(1);
        } else if (slotIndex == 12) { // 6 hours
            durationMillis = TimeUnit.HOURS.toMillis(6);
        } else if (slotIndex == 13) { // 1 day
            durationMillis = TimeUnit.DAYS.toMillis(1);
        } else if (slotIndex == 14) { // 3 days
            durationMillis = TimeUnit.DAYS.toMillis(3);
        } else if (slotIndex == 15) { // 1 week
            durationMillis = TimeUnit.DAYS.toMillis(7);
        } else if (slotIndex == 16 && clicked.getItem() == Items.OAK_SIGN) { // Custom sign
            openSignEditor(serverPlayer);
            return;
        }
        
        if (durationMillis > 0) {
            createBounty(serverPlayer, durationMillis, forceKingsBounty);
        }
    }
    
    private void openSignEditor(ServerPlayerEntity player) {
        player.closeHandledScreen();
        
        // Send instructions via chat
        player.sendMessage(
            Text.literal("Typ in de chat de duur in minuten (minimaal 10, maximaal 10080 voor 1 week)")
                .formatted(Formatting.YELLOW),
            false
        );
        player.sendMessage(
            Text.literal("Voorbeeld: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal("30").formatted(Formatting.WHITE))
                .append(Text.literal(" voor 30 minuten").formatted(Formatting.GRAY)),
            false
        );
        player.sendMessage(
            Text.literal("Typ ")
                .formatted(Formatting.GRAY)
                .append(Text.literal("cancel").formatted(Formatting.RED))
                .append(Text.literal(" om te annuleren").formatted(Formatting.GRAY)),
            false
        );
        
        // Store context for chat handler
        BountyMod.getBountyManager().setPendingBountyCreation(player.getUuid(), targetProfile, rewards);
    }
    
    private void createBounty(ServerPlayerEntity player, long durationMillis) {
        createBounty(player, durationMillis, false);
    }
    
    private void createBounty(ServerPlayerEntity player, long durationMillis, boolean forceKingsBounty) {
        // 1/60 chance for King's Bounty (unless forced)
        boolean isKingsBounty = forceKingsBounty || (new java.util.Random().nextInt(60) == 0);
        
        Bounty bounty = new Bounty(
            targetProfile.getId(),
            targetProfile.getName(),
            player.getUuid(),
            player.getName().getString(),
            rewards,
            durationMillis,
            isKingsBounty
        );
        
        BountyMod.getBountyManager().addBounty(bounty);
        
        player.sendMessage(
            Text.literal("Bounty succesvol aangemaakt op ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(targetProfile.getName()).formatted(Formatting.GOLD))
                .append(Text.literal("!").formatted(Formatting.GREEN)),
            false
        );
        
        player.closeHandledScreen();
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
    
    public static class Factory implements NamedScreenHandlerFactory {
        private final GameProfile targetProfile;
        private final List<ItemStack> rewards;
        private final boolean forceKingsBounty;
        
        public Factory(GameProfile targetProfile, List<ItemStack> rewards) {
            this(targetProfile, rewards, false);
        }
        
        public Factory(GameProfile targetProfile, List<ItemStack> rewards, boolean forceKingsBounty) {
            this.targetProfile = targetProfile;
            this.rewards = rewards;
            this.forceKingsBounty = forceKingsBounty;
        }
        
        @Override
        public Text getDisplayName() {
            return forceKingsBounty ? 
                Text.literal("⚜ King's Bounty - Selecteer Duur ⚜").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD) :
                Text.literal("Selecteer Duur");
        }
        
        @Nullable
        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new TimeSelectionScreen(syncId, playerInventory, targetProfile, rewards, forceKingsBounty);
        }
    }
}
