# BountyMod

Een Minecraft Fabric mod voor versie 1.21.3 waarmee spelers bounties kunnen plaatsen op andere spelers.

## Features

- **/bounty** commando voor alle spelers
- Plaats bounties op spelers (ook offline)
- Aangepaste item rewards
- Tijdslimiet tussen 10 minuten en 1 week
- Automatische reward distributie bij kill
- OP kan bounties verwijderen
- Data persistentie (bounties blijven na server restart)

## Installatie

1. Zorg dat je Fabric Loader hebt geïnstalleerd voor Minecraft 1.21.3
2. Download de mod .jar file
3. Plaats in de `mods` folder
4. Start Minecraft

## Gebruik

### Bounty plaatsen
1. Type `/bounty`
2. Klik op "Create New Bounty"
3. Selecteer een speler
4. Plaats items in de chest als reward
5. Vul de tijd in op het sign (bijv. "1d" voor 1 dag)
6. Klik op Confirm

### Bounty claimen
- Kill de speler met een bounty
- Je ontvangt automatisch de reward items
- Je kan je eigen bounty niet claimen

## Time Period Format
- `10m` = 10 minuten
- `1h` = 1 uur  
- `1d` = 1 dag
- `1w` = 1 week

## Building

```bash
./gradlew build
```

De compiled mod vind je in `build/libs/`

## Requirements

- Java 21
- Minecraft 1.21.3
- Fabric Loader 0.16.9+
- Fabric API 0.110.0+
