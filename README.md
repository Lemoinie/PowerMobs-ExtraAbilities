# PowerMobs-ExtraAbilities

An add-on for the [PowerMobs](../PowerMobs) plugin, adding a vast array of high-quality, configurable custom abilities for your mobs.

## Features
- **Seamless Integration**: Automatically registers with PowerMobs.
- **GUI Support**: All abilities and their parameters are fully editable via the in-game PowerMobs GUI.
- **Sync Reload**: Automatically reloads when you run `/powermob reload`, ensuring your custom configs are always up to date.
- **True Damage**: Advanced damage calculations (like Blood Pool) bypass armor and defense for consistent difficulty.
- **Visual Excellence**: Includes custom particles (Soul Fire, Sonic Booms, Portals) and spatial sounds for a premium feel.

## Abilities

| Ability | ID | Description |
|---|---|---|
| **Void Pull** | `void-pull` | Periodically pulls nearby players toward the mob. |
| **Mount Crash** | `mount-crash` | Knocks players off their mounts (Horses, Camels, etc.) and slows them. |
| **Berserk** | `berserk` | Massive damage and speed boost when health drops low. |
| **Venom Bite** | `venom-bite` | Inflicts Poison on hit (configurable chance/strength). |
| **Wither Touch** | `wither-touch` | Inflicts Wither on hit (configurable chance/strength). |
| **Life Steal** | `life-steal` | Heals the mob based on damage dealt to players. |
| **Starvation Curse**| `starvation-curse`| Inflicts Hunger on hit. |
| **Dread Screech** | `dread-screech` | Debuffs players with Darkness, Slowness, and Fatigue in a radius. |
| **Tenacity** | `tenacity` | Reduces all incoming damage by 50% when below 30% HP. |
| **Quicksilver** | `quicksilver` | Chance to gain a significant temporary speed boost. |
| **Domain Expansion**| `domain-expansion`| Creates a zone that buffs the mob and debuffs nearby players. |
| **Explosive Death** | `explosive-death` | Mob explodes violently upon death. |
| **Armor Shatter** | `armor-shatter` | Attacks have a chance to permanently damage player armor durability. |
| **Shadow Extraction**| `shadow-extraction`| Resurrect nearby fallen mobs as loyal shadow minions. |
| **Fear Aura** | `fear-aura` | Constant black particle field that slows and weakens nearby players. |
| **Repulsion Burst**| `repulsion-burst` | Blasts players away with a shockwave when the mob's health is low. |
| **Blood Pool** | `blood-pool` | Summons a red zone of boiling blood that deals % Max HP true damage. |
| **Crippling Strike**| `crippling-strike`| Attacks apply stacking slowness, becoming heavier with every hit. |
| **Chain Lightning** | `chain-lightning` | Strike lightning that scales in power with the number of players. |
| **Crushing Blow** | `crushing-blow` | Charges a heavy attack that deals massive damage if the player hits a wall. |
| **Death Zone** | `death-zone` | Creates a fatal area where staying too long results in instant death. |
| **Parry** | `parry` | Chance to block melee damage and knock back the attacker. |
| **Potion Immunity** | `potion-immunity` | Immune to effects from splash and lingering potions. |
| **Potion Reflection**| `potion-reflection`| Chance to reflect thrown potions back at the thrower. |
| **Effect Purge** | `effect-purge` | Periodically removes all negative potion effects. |
| **Lava Walker** | `lava-walker` | Complete immunity to fire, lava, and magma damage. |
| **Fall Immunity** | `fall-immunity` | Complete immunity to fall damage. |
| **Aqua Anchor** | `aqua-anchor` | Move normally in water and ignore pushing currents. |

## Installation

1. Ensure the **PowerMobs** core plugin is in your `plugins/` folder.
2. Place `PowerMobs-ExtraAbilities.jar` into the `plugins/` folder.
3. Restart or reload the server.

The plugin will automatically register all abilities and create `extraabilitiesconfig.yml` in your PowerMobs folder.

## Configuration

Settings are managed in `plugins/PowerMobs/extraabilitiesconfig.yml`. The file contains detailed comments for every parameter.

### Synchronization
This add-on hooks into the core reload command. When you run:
` /powermob reload`
The add-on will automatically refresh its own configuration and re-inject it into the core system, allowing for instant updates without a restart.

## Requirements
- Paper 1.21.1+
- [PowerMobs Core Plugin](../PowerMobs)

## Development
To build the project:
```bash
.\gradlew build
```
The output JAR will be in `build/libs/`.
