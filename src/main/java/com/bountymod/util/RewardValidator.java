package com.bountymod.util;

import net.minecraft.item.*;

public class RewardValidator {
    
    public static boolean isValidReward(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        Item item = stack.getItem();
        
        // Allowed materials
        if (item == Items.GOLD_INGOT || 
            item == Items.IRON_INGOT || 
            item == Items.EMERALD || 
            item == Items.NETHERITE_INGOT || 
            item == Items.ANCIENT_DEBRIS || 
            item == Items.DIAMOND ||
            item == Items.BELL ||
            item == Items.ENCHANTED_BOOK) {
            return true;
        }
        
        // Check if it's a weapon (swords, axes, tridents, bows, crossbows)
        if (item instanceof SwordItem || 
            item instanceof AxeItem || 
            item instanceof TridentItem || 
            item instanceof BowItem || 
            item instanceof CrossbowItem ||
            item == Items.MACE) {
            return true;
        }
        
        // Check if it's a tool (pickaxe, shovel, hoe)
        if (item instanceof PickaxeItem || 
            item instanceof ShovelItem || 
            item instanceof HoeItem) {
            return true;
        }
        
        // Check if it's armor
        if (item instanceof ArmorItem) {
            return true;
        }
        
        return false;
    }
    
    public static String getInvalidItemMessage() {
        return "Alleen de volgende items zijn toegestaan als reward: Gold/Iron/Netherite Ingots, Emeralds, Diamonds, Ancient Debris, Wapens, Tools, Armor, Enchanted Books en Bells";
    }
}
