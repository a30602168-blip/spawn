# SpawnSMT

Fabric mod for Minecraft 1.21.11. Adds the OP-only command `/spawnsmt <item>` (permission level "gamemaster") that gives you one of the custom items below. Everything runs on the server, so it works in multiplayer; the mod must be installed on server and clients.

| Item | Command | Ability |
|------|---------|---------|
| Portal Gun | `/spawnsmt portalgun` | Right click shoots a blue portal, sneak + right click an orange one. The two portals of a player are linked; players, mobs and dropped items travel through (also across dimensions) and keep their momentum, rotated to the exit direction. Portals go on walls (1x2) and are removed when the owner leaves or the server stops. |
| Grapple Hook | `/spawnsmt grapplehook` | Hurls you towards the block you look at (40 blocks). |
| Blink Staff | `/spawnsmt blinkstaff` | Teleports you up to 20 blocks forward, stopping before obstacles. |
| Thunder Staff | `/spawnsmt thunderstaff` | Calls down lightning on the targeted block (min. 4 blocks away). |
| Gravity Wand | `/spawnsmt gravitywand` | Pulls creatures and items in front of you towards you; sneak to push them away. |
| Fire Extinguisher | `/spawnsmt fireextinguisher` | Sprays foam that removes fire blocks and extinguishes burning creatures where you aim. |
| Defibrillator | `/spawnsmt defibrillator` | Shocks the creature in front of you (or yourself) and restores 6 hearts. |

## Build

```
./gradlew build
```

The jar is written to `build/libs/`. Requires JDK 21.
