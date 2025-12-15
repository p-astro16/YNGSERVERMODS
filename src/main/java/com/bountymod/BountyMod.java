package com.bountymod;

import com.bountymod.command.BountyCommand;
import com.bountymod.command.KingsBountyCommand;
import com.bountymod.event.ChatMessageHandler;
import com.bountymod.event.PlayerDeathHandler;
import com.bountymod.manager.BountyManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BountyMod implements ModInitializer {
	public static final String MOD_ID = "bountymod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static BountyManager bountyManager;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing BountyMod");
		
		// Initialize bounty manager
		bountyManager = new BountyManager();
		
		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BountyCommand.register(dispatcher);
			KingsBountyCommand.register(dispatcher);
		});
		
		// Register event handlers
		PlayerDeathHandler.register();
		ChatMessageHandler.register();
		
		// Load bounties on server start
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			bountyManager.loadBounties();
			bountyManager.startExpirationTask(server);
		});
		
		// Save bounties on server stop
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			bountyManager.saveBounties();
		});
		
		LOGGER.info("BountyMod initialized successfully");
	}
	
	public static BountyManager getBountyManager() {
		return bountyManager;
	}
}
