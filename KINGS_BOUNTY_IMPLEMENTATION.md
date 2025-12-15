# King's Bounty Mechanic - Implementation Summary

## Overview
The King's Bounty mechanic adds a premium random event to the bounty system where bounties have a 1/60 chance to become "King's Bounties" with enhanced rewards and special features.

## Key Features Implemented

### 1. Random King's Bounty Creation (1/60 chance)
- **Location**: `TimeSelectionScreen.java` and `ChatMessageHandler.java`
- Every bounty creation has a 1.67% chance to become a King's Bounty
- Uses `Random.nextInt(60) == 0` check
- Applied automatically when bounty is created

### 2. Reward Multiplier (10x)
- **Location**: `Bounty.java` constructor
- When `isKingsBounty = true`, all reward items are multiplied by 10
- Respects item stack max count limits
- Applied during bounty creation, not during claim

### 3. Extended Chest Spawn Radius (1000 blocks)
- **Location**: `BountyManager.spawnRewardChest()`
- Normal bounties: 500 block radius
- King's Bounties: 1000 block radius
- Applied when creator kills their own bounty

### 4. King's Bounty Immunity (2 hours)
- **Location**: `BountyManager.java`
- Killer receives 2-hour immunity after claiming King's Bounty
- Players with immunity cannot have bounties placed on them
- Immunity tracked in `kingsBountyImmunity` Map and persisted to JSON
- Checked in `PlayerSelectionScreen` before allowing bounty creation

### 5. Special Broadcast Messages
- **Location**: `BountyManager.addBounty()` and `BountyManager.claimBounty()`
- King's Bounty creation: Purple themed message with ⚜ symbols
- King's Bounty claim: Enhanced claim message with immunity notification
- Different formatting than normal bounties (LIGHT_PURPLE, BOLD)

### 6. OP Command - /kingsbounty
- **Location**: `KingsBountyCommand.java`
- Requires OP level 2
- Opens special GUI flow for guaranteed King's Bounty
- Uses modified screens: `KingsBountyPlayerSelectionScreen` and `KingsBountyRewardSelectionScreen`
- Bypasses cooldown check but still respects immunity

## Modified Files

### Data Model
- **Bounty.java**
  - Added `isKingsBounty` boolean field
  - Added constructor overload with `isKingsBounty` parameter
  - Reward multiplier logic in constructor
  - JSON serialization/deserialization updated
  - Added `isKingsBounty()` getter

### Manager
- **BountyManager.java**
  - Added `kingsBountyImmunity` Map<UUID, Long>
  - Added `immunityFile` Path for persistence
  - Added immunity methods: `hasImmunity()`, `getImmunityRemaining()`, `getFormattedImmunityRemaining()`
  - Updated `loadBounties()` to load immunity data
  - Updated `saveBounties()` to save immunity data
  - Updated `addBounty()` with King's Bounty broadcast messages
  - Updated `claimBounty()` to grant immunity and special broadcast
  - Updated `spawnRewardChest()` to use variable radius based on bounty type

### GUI Screens
- **TimeSelectionScreen.java**
  - Added `forceKingsBounty` boolean parameter
  - Updated constructors to support forced King's Bounty
  - Modified `createBounty()` to implement 1/60 chance logic
  - Updated Factory to support forced mode

- **PlayerSelectionScreen.java**
  - Added immunity check before opening reward selection
  - Shows immunity remaining time to player

- **KingsBountyPlayerSelectionScreen.java** (NEW)
  - Special player selection screen for /kingsbounty command
  - Purple themed UI
  - Still checks immunity but bypasses cooldown

- **KingsBountyRewardSelectionScreen.java** (NEW)
  - Chest-based reward selection for King's Bounty
  - Purple themed with "⚜ King's Bounty Rewards (10x) ⚜"
  - Links to TimeSelectionScreen with forceKingsBounty=true

### Commands
- **KingsBountyCommand.java** (NEW)
  - Registers `/kingsbounty` command
  - Requires OP permission level 2
  - Opens KingsBountyPlayerSelectionScreen

- **BountyMod.java**
  - Registered KingsBountyCommand in command callback

### Event Handlers
- **ChatMessageHandler.java**
  - Updated to implement 1/60 King's Bounty chance
  - Creates bounty with `isKingsBounty` parameter

## Data Persistence

### New JSON File
- **bountymod_immunity.json**
  - Stores King's Bounty immunity data
  - Format: `{"immunity": {"player-uuid": timestamp}}`
  - Only saves non-expired immunity
  - Loaded on server start

### Updated JSON Files
- **bountymod_data.json**
  - Added `isKingsBounty` boolean field to bounty objects
  - Backwards compatible (defaults to false if not present)

## Testing Checklist

- [ ] Normal bounty creation has 1/60 chance for King's Bounty
- [ ] King's Bounty multiplies rewards by 10
- [ ] King's Bounty broadcast messages appear correctly
- [ ] Claiming King's Bounty grants 2-hour immunity
- [ ] Players with immunity cannot have bounties placed on them
- [ ] Immunity remaining time displays correctly
- [ ] Own bounty kill spawns chest at 1000 blocks for King's Bounty
- [ ] /kingsbounty command works for OPs
- [ ] King's Bounty immunity persists across server restarts
- [ ] Item stack max count is respected in 10x multiplier

## Configuration
No configuration needed - all values are hardcoded:
- King's Bounty chance: 1/60 (1.67%)
- Reward multiplier: 10x
- Immunity duration: 2 hours (7,200,000 ms)
- Normal chest radius: 500 blocks
- King's Bounty chest radius: 1000 blocks

## Known Limitations
1. Item multiplier respects vanilla max stack size (e.g., 64 for most items)
2. King's Bounty immunity only prevents bounty CREATION, not PvP combat
3. Offline players cannot be selected in King's Bounty GUI (only online players)
