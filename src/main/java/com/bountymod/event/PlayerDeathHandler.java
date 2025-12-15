package com.bountymod.event;

import com.bountymod.BountyMod;
import com.bountymod.manager.BountyManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerDeathHandler implements ServerLivingEntityEvents.AfterDeath {
    
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(new PlayerDeathHandler());
    }
    
    @Override
    public void afterDeath(LivingEntity entity, DamageSource damageSource) {
        // Check if the killed entity is a player
        if (!(entity instanceof ServerPlayerEntity victim)) {
            return;
        }
        
        // Check if there's an attacker and if it's a player
        if (damageSource.getAttacker() instanceof ServerPlayerEntity killer) {
            // Check if victim has active bounty
            BountyManager bountyManager = BountyMod.getBountyManager();
            
            if (bountyManager.hasActiveBounty(victim.getUuid())) {
                bountyManager.claimBounty(killer, victim);
            }
        }
    }
}
