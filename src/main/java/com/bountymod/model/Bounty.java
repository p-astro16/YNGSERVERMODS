package com.bountymod.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Bounty {
	private final UUID id;
	private final UUID targetUUID;
	private final String targetName;
	private final UUID creatorUUID;
	private final String creatorName;
	private final List<ItemStack> rewards;
	private final long createdAt;
	private final long expiresAt;
	private final boolean isKingsBounty;
	
	public Bounty(UUID targetUUID, String targetName, UUID creatorUUID, String creatorName, 
	              List<ItemStack> rewards, long durationMillis) {
		this(targetUUID, targetName, creatorUUID, creatorName, rewards, durationMillis, false);
	}
	
	public Bounty(UUID targetUUID, String targetName, UUID creatorUUID, String creatorName, 
	              List<ItemStack> rewards, long durationMillis, boolean isKingsBounty) {
		this.id = UUID.randomUUID();
		this.targetUUID = targetUUID;
		this.targetName = targetName;
		this.creatorUUID = creatorUUID;
		this.creatorName = creatorName;
		this.isKingsBounty = isKingsBounty;
		
		// If King's Bounty, multiply rewards by 10
		if (isKingsBounty) {
			this.rewards = new ArrayList<>();
			for (ItemStack stack : rewards) {
				ItemStack multiplied = stack.copy();
				multiplied.setCount(Math.min(multiplied.getCount() * 10, multiplied.getMaxCount()));
				this.rewards.add(multiplied);
			}
		} else {
			this.rewards = new ArrayList<>(rewards);
		}
		
		this.createdAt = System.currentTimeMillis();
		this.expiresAt = createdAt + durationMillis;
	}
	
	// Constructor for loading from JSON
	public Bounty(UUID id, UUID targetUUID, String targetName, UUID creatorUUID, String creatorName,
	              List<ItemStack> rewards, long createdAt, long expiresAt, boolean isKingsBounty) {
		this.id = id;
		this.targetUUID = targetUUID;
		this.targetName = targetName;
		this.creatorUUID = creatorUUID;
		this.creatorName = creatorName;
		this.rewards = new ArrayList<>(rewards);
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
		this.isKingsBounty = isKingsBounty;
	}
	
	public UUID getId() {
		return id;
	}
	
	public UUID getTargetUUID() {
		return targetUUID;
	}
	
	public String getTargetName() {
		return targetName;
	}
	
	public UUID getCreatorUUID() {
		return creatorUUID;
	}
	
	public String getCreatorName() {
		return creatorName;
	}
	
	public List<ItemStack> getRewards() {
		return new ArrayList<>(rewards);
	}
	
	public long getCreatedAt() {
		return createdAt;
	}
	
	public long getExpiresAt() {
		return expiresAt;
	}
	
	public boolean isKingsBounty() {
		return isKingsBounty;
	}
	
	public boolean isExpired() {
		return System.currentTimeMillis() >= expiresAt;
	}
	
	public long getTimeRemaining() {
		return Math.max(0, expiresAt - System.currentTimeMillis());
	}
	
	public String getFormattedTimeRemaining() {
		long millis = getTimeRemaining();
		long seconds = millis / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;
		
		if (days > 0) {
			return days + "d " + (hours % 24) + "h";
		} else if (hours > 0) {
			return hours + "h " + (minutes % 60) + "m";
		} else if (minutes > 0) {
			return minutes + "m " + (seconds % 60) + "s";
		} else {
			return seconds + "s";
		}
	}
	
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("id", id.toString());
		json.addProperty("targetUUID", targetUUID.toString());
		json.addProperty("targetName", targetName);
		json.addProperty("creatorUUID", creatorUUID.toString());
		json.addProperty("creatorName", creatorName);
		json.addProperty("createdAt", createdAt);
		json.addProperty("expiresAt", expiresAt);
		json.addProperty("isKingsBounty", isKingsBounty);
		
		JsonArray rewardsArray = new JsonArray();
		for (ItemStack stack : rewards) {
			if (!stack.isEmpty()) {
				JsonObject itemJson = new JsonObject();
				// Store item ID and count
				itemJson.addProperty("id", Registries.ITEM.getId(stack.getItem()).toString());
				itemJson.addProperty("count", stack.getCount());
				// Store components as string
				if (stack.getComponents() != null) {
					itemJson.addProperty("components", stack.getComponents().toString());
				}
				rewardsArray.add(itemJson);
			}
		}
		json.add("rewards", rewardsArray);
		
		return json;
	}
	
	public static Bounty fromJson(JsonObject json) {
		UUID id = UUID.fromString(json.get("id").getAsString());
		UUID targetUUID = UUID.fromString(json.get("targetUUID").getAsString());
		String targetName = json.get("targetName").getAsString();
		UUID creatorUUID = UUID.fromString(json.get("creatorUUID").getAsString());
		String creatorName = json.get("creatorName").getAsString();
		long createdAt = json.get("createdAt").getAsLong();
		long expiresAt = json.get("expiresAt").getAsLong();
		boolean isKingsBounty = json.has("isKingsBounty") ? json.get("isKingsBounty").getAsBoolean() : false;
		
		List<ItemStack> rewards = new ArrayList<>();
		JsonArray rewardsArray = json.getAsJsonArray("rewards");
		for (int i = 0; i < rewardsArray.size(); i++) {
			JsonObject itemJson = rewardsArray.get(i).getAsJsonObject();
			try {
				Identifier itemId = Identifier.of(itemJson.get("id").getAsString());
				int count = itemJson.get("count").getAsInt();
				var item = Registries.ITEM.get(itemId);
				if (item != null) {
					ItemStack stack = new ItemStack(item, count);
					rewards.add(stack);
				}
			} catch (Exception e) {
				// Skip invalid items
			}
		}
		
		return new Bounty(id, targetUUID, targetName, creatorUUID, creatorName, rewards, createdAt, expiresAt, isKingsBounty);
	}
}
