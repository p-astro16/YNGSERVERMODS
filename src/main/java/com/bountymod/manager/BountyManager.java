package com.bountymod.manager;

import com.bountymod.BountyMod;
import com.bountymod.model.Bounty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BountyManager {
    private final Map<UUID, Bounty> bounties = new ConcurrentHashMap<>();
    private final Map<UUID, PendingBounty> pendingBounties = new ConcurrentHashMap<>();
    private final Map<UUID, Long> bountyCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> kingsBountyImmunity = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path dataFile;
    private Path cooldownFile;
    private Path immunityFile;
    private MinecraftServer server;
    private static final long COOLDOWN_DURATION = 2 * 60 * 60 * 1000L; // 2 hours in milliseconds
    private static final long IMMUNITY_DURATION = 2 * 60 * 60 * 1000L; // 2 hours in milliseconds
    
    public static class PendingBounty {
        public final GameProfile targetProfile;
        public final List<ItemStack> rewards;
        
        public PendingBounty(GameProfile targetProfile, List<ItemStack> rewards) {
            this.targetProfile = targetProfile;
            this.rewards = rewards;
        }
    }

    public void loadBounties() {
        if (server == null) return;
        
        dataFile = server.getRunDirectory().resolve("world").resolve("bountymod_data.json");
        cooldownFile = server.getRunDirectory().resolve("world").resolve("bountymod_cooldowns.json");
        immunityFile = server.getRunDirectory().resolve("world").resolve("bountymod_immunity.json");
        
        // Load bounties
        if (Files.exists(dataFile)) {
            try (Reader reader = new FileReader(dataFile.toFile())) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                JsonArray bountiesArray = root.getAsJsonArray("bounties");
                
                for (int i = 0; i < bountiesArray.size(); i++) {
                    JsonObject bountyJson = bountiesArray.get(i).getAsJsonObject();
                    Bounty bounty = Bounty.fromJson(bountyJson);
                    
                    // Don't load expired bounties
                    if (!bounty.isExpired()) {
                        bounties.put(bounty.getId(), bounty);
                    }
                }
                
                BountyMod.LOGGER.info("Loaded {} active bounties", bounties.size());
            } catch (Exception e) {
                BountyMod.LOGGER.error("Failed to load bounties", e);
            }
        }
        
        // Load cooldowns
        if (Files.exists(cooldownFile)) {
            try (Reader reader = new FileReader(cooldownFile.toFile())) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                JsonObject cooldownsObj = root.getAsJsonObject("cooldowns");
                
                for (String uuidStr : cooldownsObj.keySet()) {
                    UUID uuid = UUID.fromString(uuidStr);
                    long cooldownEnd = cooldownsObj.get(uuidStr).getAsLong();
                    
                    // Only load cooldowns that haven't expired yet
                    if (cooldownEnd > System.currentTimeMillis()) {
                        bountyCooldowns.put(uuid, cooldownEnd);
                    }
                }
                
                BountyMod.LOGGER.info("Loaded {} active cooldowns", bountyCooldowns.size());
            } catch (Exception e) {
                BountyMod.LOGGER.error("Failed to load cooldowns", e);
            }
        }
        
        // Load immunity
        if (Files.exists(immunityFile)) {
            try (Reader reader = new FileReader(immunityFile.toFile())) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                JsonObject immunityObj = root.getAsJsonObject("immunity");
                
                for (String uuidStr : immunityObj.keySet()) {
                    UUID uuid = UUID.fromString(uuidStr);
                    long immunityEnd = immunityObj.get(uuidStr).getAsLong();
                    
                    // Only load immunity that hasn't expired yet
                    if (immunityEnd > System.currentTimeMillis()) {
                        kingsBountyImmunity.put(uuid, immunityEnd);
                    }
                }
                
                BountyMod.LOGGER.info("Loaded {} active immunities", kingsBountyImmunity.size());
            } catch (Exception e) {
                BountyMod.LOGGER.error("Failed to load immunity", e);
            }
        }
    }

    public void saveBounties() {
        if (dataFile == null || server == null) return;

        try {
            Files.createDirectories(dataFile.getParent());
            
            // Save bounties
            JsonObject root = new JsonObject();
            JsonArray bountiesArray = new JsonArray();
            
            for (Bounty bounty : bounties.values()) {
                if (!bounty.isExpired()) {
                    bountiesArray.add(bounty.toJson());
                }
            }
            
            root.add("bounties", bountiesArray);
            
            try (Writer writer = new FileWriter(dataFile.toFile())) {
                gson.toJson(root, writer);
            }
            
            BountyMod.LOGGER.info("Saved {} bounties", bountiesArray.size());
            
            // Save cooldowns
            JsonObject cooldownRoot = new JsonObject();
            JsonObject cooldownsObj = new JsonObject();
            
            for (Map.Entry<UUID, Long> entry : bountyCooldowns.entrySet()) {
                // Only save cooldowns that haven't expired yet
                if (entry.getValue() > System.currentTimeMillis()) {
                    cooldownsObj.addProperty(entry.getKey().toString(), entry.getValue());
                }
            }
            
            cooldownRoot.add("cooldowns", cooldownsObj);
            
            try (Writer writer = new FileWriter(cooldownFile.toFile())) {
                gson.toJson(cooldownRoot, writer);
            }
            
            BountyMod.LOGGER.info("Saved {} cooldowns", cooldownsObj.size());
            
            // Save immunity
            JsonObject immunityRoot = new JsonObject();
            JsonObject immunityObj = new JsonObject();
            
            for (Map.Entry<UUID, Long> entry : kingsBountyImmunity.entrySet()) {
                // Only save immunity that hasn't expired yet
                if (entry.getValue() > System.currentTimeMillis()) {
                    immunityObj.addProperty(entry.getKey().toString(), entry.getValue());
                }
            }
            
            immunityRoot.add("immunity", immunityObj);
            
            try (Writer writer = new FileWriter(immunityFile.toFile())) {
                gson.toJson(immunityRoot, writer);
            }
            
            BountyMod.LOGGER.info("Saved {} immunities", immunityObj.size());
        } catch (Exception e) {
            BountyMod.LOGGER.error("Failed to save bounties/cooldowns/immunity", e);
        }
    }

    public void addBounty(Bounty bounty) {
        bounties.put(bounty.getId(), bounty);
        
        // Set cooldown for creator (2 hours from now)
        bountyCooldowns.put(bounty.getCreatorUUID(), System.currentTimeMillis() + COOLDOWN_DURATION);
        
        saveBounties();
        
        // Broadcast to all players
        if (server != null) {
            Text message;
            if (bounty.isKingsBounty()) {
                message = Text.literal("⚜ KING'S BOUNTY ⚜ ")
                    .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                    .append(Text.literal(bounty.getCreatorName()).formatted(Formatting.GOLD))
                    .append(Text.literal(" zette een KING'S BOUNTY op ").formatted(Formatting.LIGHT_PURPLE))
                    .append(Text.literal(bounty.getTargetName()).formatted(Formatting.RED, Formatting.BOLD))
                    .append(Text.literal("! 10x rewards! ").formatted(Formatting.LIGHT_PURPLE))
                    .append(Text.literal("/bounty").formatted(Formatting.AQUA))
                    .append(Text.literal(" voor details").formatted(Formatting.LIGHT_PURPLE));
            } else {
                message = Text.literal(bounty.getCreatorName())
                    .formatted(Formatting.GOLD)
                    .append(Text.literal(" zette een bounty op ").formatted(Formatting.YELLOW))
                    .append(Text.literal(bounty.getTargetName()).formatted(Formatting.RED))
                    .append(Text.literal(", kijk ").formatted(Formatting.YELLOW))
                    .append(Text.literal("/bounty").formatted(Formatting.AQUA))
                    .append(Text.literal(" voor de reward").formatted(Formatting.YELLOW));
            }
            
            server.getPlayerManager().broadcast(message, false);
        }
    }
    
    public boolean isOnCooldown(UUID playerUUID) {
        Long cooldownEnd = bountyCooldowns.get(playerUUID);
        if (cooldownEnd == null) {
            return false;
        }
        
        // Check if cooldown has expired
        if (System.currentTimeMillis() >= cooldownEnd) {
            bountyCooldowns.remove(playerUUID);
            return false;
        }
        
        return true;
    }
    
    public long getCooldownRemaining(UUID playerUUID) {
        Long cooldownEnd = bountyCooldowns.get(playerUUID);
        if (cooldownEnd == null) {
            return 0;
        }
        
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    public String getFormattedCooldownRemaining(UUID playerUUID) {
        long millis = getCooldownRemaining(playerUUID);
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return hours + " uur en " + (minutes % 60) + " minuten";
        } else if (minutes > 0) {
            return minutes + " minuten";
        } else {
            return seconds + " seconden";
        }
    }
    
    public boolean hasImmunity(UUID playerUUID) {
        Long immunityEnd = kingsBountyImmunity.get(playerUUID);
        if (immunityEnd == null) {
            return false;
        }
        
        // Check if immunity has expired
        if (System.currentTimeMillis() >= immunityEnd) {
            kingsBountyImmunity.remove(playerUUID);
            return false;
        }
        
        return true;
    }
    
    public long getImmunityRemaining(UUID playerUUID) {
        Long immunityEnd = kingsBountyImmunity.get(playerUUID);
        if (immunityEnd == null) {
            return 0;
        }
        
        long remaining = immunityEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    public String getFormattedImmunityRemaining(UUID playerUUID) {
        long millis = getImmunityRemaining(playerUUID);
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return hours + " uur en " + (minutes % 60) + " minuten";
        } else if (minutes > 0) {
            return minutes + " minuten";
        } else {
            return seconds + " seconden";
        }
    }

    public void removeBounty(UUID bountyId) {
        bounties.remove(bountyId);
        saveBounties();
    }

    public Bounty getBounty(UUID bountyId) {
        return bounties.get(bountyId);
    }

    public List<Bounty> getAllBounties() {
        return new ArrayList<>(bounties.values());
    }

    public List<Bounty> getBountiesForTarget(UUID targetUUID) {
        List<Bounty> targetBounties = new ArrayList<>();
        for (Bounty bounty : bounties.values()) {
            if (bounty.getTargetUUID().equals(targetUUID) && !bounty.isExpired()) {
                targetBounties.add(bounty);
            }
        }
        return targetBounties;
    }

    public boolean hasActiveBounty(UUID targetUUID) {
        return getBountiesForTarget(targetUUID).size() > 0;
    }

    public void claimBounty(ServerPlayerEntity killer, ServerPlayerEntity victim) {
        List<Bounty> targetBounties = getBountiesForTarget(victim.getUuid());
        
        if (targetBounties.isEmpty()) {
            return;
        }

        // Can only claim one bounty per player (no multiple bounties on same target)
        Bounty bounty = targetBounties.get(0);
        
        // Check if killer is the creator (own bounty)
        if (bounty.getCreatorUUID().equals(killer.getUuid())) {
            // Spawn chest with rewards in random location
            spawnRewardChest(killer, bounty);
            
            // Broadcast message
            Text message = Text.literal(killer.getName().getString())
                .formatted(Formatting.GOLD)
                .append(Text.literal(" heeft zijn eigen bounty op ").formatted(Formatting.YELLOW))
                .append(Text.literal(victim.getName().getString()).formatted(Formatting.RED))
                .append(Text.literal(" geclaimed! De rewards zijn verstopt in een kist.").formatted(Formatting.YELLOW));
            
            server.getPlayerManager().broadcast(message, false);
            
            // Remove bounty
            removeBounty(bounty.getId());
            return;
        }

        // Normal bounty claim - give rewards to killer
        for (ItemStack reward : bounty.getRewards()) {
            if (!reward.isEmpty()) {
                ItemStack copy = reward.copy();
                if (!killer.getInventory().insertStack(copy)) {
                    // If inventory is full, drop at player's location
                    killer.dropItem(copy, false);
                }
            }
        }

        // Broadcast claim message and grant immunity for King's Bounty
        Text message;
        if (bounty.isKingsBounty()) {
            message = Text.literal("⚜ KING'S BOUNTY CLAIMED ⚜ ")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.literal(killer.getName().getString()).formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal(" heeft de KING'S BOUNTY op ").formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal(victim.getName().getString()).formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal(" geclaimed! 2 uur immunity!").formatted(Formatting.LIGHT_PURPLE));
            
            // Grant 2-hour immunity to killer
            kingsBountyImmunity.put(killer.getUuid(), System.currentTimeMillis() + IMMUNITY_DURATION);
            saveBounties();
            
            killer.sendMessage(
                Text.literal("Je hebt 2 uur immunity! Er kunnen geen bounties op jou geplaatst worden.")
                    .formatted(Formatting.LIGHT_PURPLE),
                false
            );
        } else {
            message = Text.literal(killer.getName().getString())
                .formatted(Formatting.GREEN)
                .append(Text.literal(" heeft de bounty op ").formatted(Formatting.YELLOW))
                .append(Text.literal(victim.getName().getString()).formatted(Formatting.RED))
                .append(Text.literal(" geclaimed!").formatted(Formatting.YELLOW));
        }
        
        server.getPlayerManager().broadcast(message, false);

        // Remove bounty
        removeBounty(bounty.getId());
    }

    public void checkExpiredBounties() {
        List<Bounty> expiredBounties = new ArrayList<>();
        
        for (Bounty bounty : bounties.values()) {
            if (bounty.isExpired()) {
                expiredBounties.add(bounty);
            }
        }

        for (Bounty bounty : expiredBounties) {
            returnBountyItems(bounty);
            removeBounty(bounty.getId());
        }
    }

    private void returnBountyItems(Bounty bounty) {
        if (server == null) return;

        ServerPlayerEntity creator = server.getPlayerManager().getPlayer(bounty.getCreatorUUID());
        
        if (creator != null) {
            // Creator is online, return items
            for (ItemStack reward : bounty.getRewards()) {
                if (!reward.isEmpty()) {
                    ItemStack copy = reward.copy();
                    if (!creator.getInventory().insertStack(copy)) {
                        creator.dropItem(copy, false);
                    }
                }
            }
            
            creator.sendMessage(
                Text.literal("Je bounty op ")
                    .formatted(Formatting.YELLOW)
                    .append(Text.literal(bounty.getTargetName()).formatted(Formatting.RED))
                    .append(Text.literal(" is verlopen. Je items zijn teruggegeven.").formatted(Formatting.YELLOW)),
                false
            );
        }
        // If creator is offline, items are lost (could implement offline storage in future)
    }

    public void startExpirationTask(MinecraftServer server) {
        this.server = server;
        
        // Check for expired bounties every minute
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkExpiredBounties();
            }
        }, 60000, 60000); // Check every 60 seconds
    }
    
    private void spawnRewardChest(ServerPlayerEntity player, Bounty bounty) {
        if (server == null) return;
        
        var world = player.getServerWorld();
        var random = world.getRandom();
        var playerPos = player.getBlockPos();
        
        // King's Bounty uses 1000 blocks, normal uses 500 blocks
        int radius = bounty.isKingsBounty() ? 1000 : 500;
        int range = radius * 2;
        int offsetX = random.nextInt(range) - radius;
        int offsetZ = random.nextInt(range) - radius;
        
        int targetX = playerPos.getX() + offsetX;
        int targetZ = playerPos.getZ() + offsetZ;
        
        // Find suitable Y position (top block)
        int targetY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, targetX, targetZ);
        
        net.minecraft.util.math.BlockPos chestPos = new net.minecraft.util.math.BlockPos(targetX, targetY, targetZ);
        
        // Place chest
        world.setBlockState(chestPos, net.minecraft.block.Blocks.CHEST.getDefaultState());
        
        // Get chest block entity and fill with rewards
        if (world.getBlockEntity(chestPos) instanceof net.minecraft.block.entity.ChestBlockEntity chestEntity) {
            int slot = 0;
            for (ItemStack reward : bounty.getRewards()) {
                if (!reward.isEmpty() && slot < chestEntity.size()) {
                    chestEntity.setStack(slot, reward.copy());
                    slot++;
                }
            }
            chestEntity.markDirty();
        }
        
        // Broadcast coordinates
        Text coordMessage = Text.literal("De bounty rewards zijn verstopt in een kist op: ")
            .formatted(Formatting.GOLD)
            .append(Text.literal("X: " + targetX + " Y: " + targetY + " Z: " + targetZ).formatted(Formatting.AQUA, Formatting.BOLD));
        
        server.getPlayerManager().broadcast(coordMessage, false);
        
        BountyMod.LOGGER.info("Spawned reward chest at X: {} Y: {} Z: {}", targetX, targetY, targetZ);
    }
    
    public void setPendingBountyCreation(UUID playerUUID, GameProfile targetProfile, List<ItemStack> rewards) {
        pendingBounties.put(playerUUID, new PendingBounty(targetProfile, rewards));
    }
    
    public PendingBounty getPendingBounty(UUID playerUUID) {
        return pendingBounties.get(playerUUID);
    }
    
    public void removePendingBounty(UUID playerUUID) {
        pendingBounties.remove(playerUUID);
    }
}
