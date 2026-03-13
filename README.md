# PowerMobs-ExtraAbilities

An add-on for the [PowerMobs](https://github.com/LittleExxGames/PowerMobs) plugin, adding a vast array of high-quality, configurable custom abilities for your mobs.

## Features
- **Seamless Integration**: Automatically registers with PowerMobs.
- **GUI Support**: All abilities and their parameters are fully editable via the in-game PowerMobs GUI.
- **Sync Reload**: Automatically reloads when you run `/powermob reload`, ensuring your custom configs are always up to date.
- **True Damage**: Advanced damage calculations (like Blood Pool) bypass armor and defense for consistent difficulty.
- **Visual Excellence**: Includes custom particles (Soul Fire, Sonic Booms, Portals) and spatial sounds for a premium feel.

## Abilities

PowerMobs-ExtraAbilities includes over 30 unique abilities categorized into Offensive, Defensive, Utility, and more. 

> [!TIP]
> **View the [Full Wiki](wiki.md) for detailed configuration options and mechanics for every ability.**

| Category | Description | Examples |
|---|---|---|
| **Offensive** | Targeted attacks and debuffs | `crushing-blow`, `meteor-strike`, `disarm` |
| **AoE & Zones** | Spatial control and area damage | `death-zone`, `blood-pool`, `meteor-shower` |
| **Combat** | Mechanical advantages and counters | `advanced-aggro`, `shield-breaking`, `parry` |
| **Defensive** | Immunities and survival tools | `tenacity`, `potion-reflection`, `lava-walker` |
| **Mobility** | Movement and environmental buffs | `quicksilver`, `aqua-anchor`, `siege-breaker` |

For a complete list of IDs and descriptions, please refer to the [Wiki](wiki.md).

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
- [PowerMobs Core Plugin](https://github.com/LittleExxGames/PowerMobs)

## Development
To build the project:
```bash
.\gradlew build
```
The output JAR will be in `build/libs/`.
