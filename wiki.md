# PowerMobs-ExtraAbilities Wiki

Welcome to the PowerMobs-ExtraAbilities Wiki. This add-on provides a wide range of custom abilities for the PowerMobs plugin, ranging from simple passive immunities to complex area-of-effect mechanics and advanced targeting systems.

## Categories

- [Offensive Abilities](#offensive-abilities)
- [Area of Effect & Zones](#area-of-effect--zones)
- [Combat Mechanics](#combat-mechanics)
- [Defensive & Passive](#defensive--passive)
- [Mobility & Utility](#mobility--utility)

---

## Offensive Abilities

### **Armor Shatter** (`armor-shatter`)
Attacks have a chance to permanently damage the durability of the player's armor.
- **Config**: `chance`

### **Chain Lightning** (`chain-lightning`)
Strikes a player with lightning that can bounce to nearby players. Damage increases based on how many players are hit.
- **Config**: `chance`, `range`, `base-damage`, `damage-increase-per-jump`

### **Crippling Strike** (`crippling-strike`)
Attacks apply stacking Slowness. Each subsequent hit increases the slowness level.
- **Config**: `hits-per-stack`, `max-stacks`, `duration-ticks`

### **Crushing Blow** (`crushing-blow`)
The mob stands still to charge, then rushes the target. Deals massive damage if the player is knocked into a wall.
- **Config**: `chance`, `charge-ticks`, `approach-speed`, `impact-damage`, `knockback-strength`, `wall-collision-damage`

### **Disarm** (`disarm`)
Chance to make the player drop their current main-hand item upon being hit. The item has a brief pickup delay.
- **Config**: `chance`

### **Meteor Strike** (`meteor-strike`)
Calls a single meteor from the sky targeting the player with the lowest HP in range.
- **Config**: `cooldown-seconds`, `search-radius`, `warning-ticks`, `damage`, `knockback`

### **Starvation Curse** (`starvation-curse`)
Inflicts the Hunger effect on the player upon hitting them.
- **Config**: `chance`, `duration-ticks`, `amplifier`

### **Venom Bite** (`venom-bite`)
Inflicts Poison on the player upon hitting them.
- **Config**: `chance`, `duration-ticks`, `amplifier`

### **Wither Touch** (`wither-touch`)
Inflicts Wither on the player upon hitting them.
- **Config**: `chance`, `duration-ticks`, `amplifier`

---

## Area of Effect & Zones

### **Blood Pool** (`blood-pool`)
Summons a pool of boiling blood on the ground. Players standing in it for too long take damage equal to a percentage of their maximum health.
- **Config**: `summon-chance`, `radius`, `stay-time-seconds`, `damage-percent`

### **Death Zone** (`death-zone`)
Creates a black zone on the ground. Players who stay inside for too long are instantly killed.
- **Config**: `summon-chance`, `radius`, `deadly-time-seconds`, `zone-duration-seconds`

### **Domain Expansion** (`domain-expansion`)
The mob creates a massive thematic zone. Inside, the mob gains buffs while players receive various debuffs.
- **Config**: `cooldown-seconds`, `radius`, `duration-seconds`

### **Dread Screech** (`dread-screech`)
The mob lets out a screech, applying Slowness, Weakness, and Darkness to all players in a radius.
- **Config**: `cooldown-seconds`, `radius`, `duration-ticks`

### **Fear Aura** (`fear-aura`)
A constant aura around the mob that weakens and slows nearby players.
- **Config**: `radius`, `slowness-level`, `weakness-level`

### **Meteor Shower** (`meteor-shower`)
Summons a rain of meteors in an area. Intensity scales with the number of nearby players.
- **Config**: `cooldown-seconds`, `search-radius`, `base-count`, `spread-radius`, `increase-per-player`

### **Void Pull** (`void-pull`)
Periodically pulls all nearby players toward the mob's location.
- **Config**: `interval-ticks`, `radius`, `pull-strength`

---

## Combat Mechanics

### **Advanced Aggro System** (`advanced-aggro`)
A complex targeting model that calculates "Threat" and "Priority." Mobs will prioritize players who are healing, have low armor, or are running away. 
- **Config**: Extensive modifiers for threat gain (melee, range, heal) and priority (HP%, distance, gear).

### **Berserk** (`berserk`)
When the mob's health drops below a certain threshold, it gains massive speed and strength bonuses.
- **Config**: `health-threshold-percent`, `speed-amplifier`, `strength-amplifier`

### **Life Steal** (`life-steal`)
The mob heals itself by a percentage of the damage it deals to players.
- **Config**: `percent-stolen`

### **Mount Crash** (`mount-crash`)
A ground slam that forces players off their mounts (Horses, Camels, etc.) and applies Slowness.
- **Config**: `radius`, `slowness-duration`, `tick-rate`

### **Parry** (`parry`)
Chance to block an incoming melee attack, negating damage and knocking the attacker back.
- **Config**: `chance`, `knockback-strength`

### **Shield Breaking** (`shield-breaking`)
Chance to disable a player's shield (similar to an axe) when they are blocking.
- **Config**: `chance`, `disable-time-ticks`

### **Tenacity** (`tenacity`)
Significantly reduces all incoming damage when the mob's health is low.
- **Config**: `health-threshold-percent`, `damage-reduction-percent`

---

## Defensive & Passive

### **Effect Purge** (`effect-purge`)
The mob periodically removes all negative potion effects from itself.
- **Config**: `interval-ticks`

### **Explosive Death** (`explosive-death`)
The mob explodes upon dying, dealing damage to nearby players.
- **Config**: `explosion-yield`, `set-fire`

### **Fall Immunity** (`fall-immunity`)
The mob takes no damage from falling.
- **No Configuration**

### **Lava Walker** (`lava-walker`)
The mob is completely immune to damage from Fire, Lava, and Magma blocks.
- **No Configuration**

### **Potion Immunity** (`potion-immunity`)
The mob is immune to all splash and lingering potion effects.
- **No Configuration**

### **Potion Reflection** (`potion-reflection`)
Chance to reflect thrown splash potions back toward the thrower.
- **Config**: `chance`

### **Repulsion Burst** (`repulsion-burst`)
When the mob is at low health, it emits a powerful shockwave that blasts all nearby players away.
- **Config**: `health-threshold-percent`, `push-strength`, `cooldown-seconds`

---

## Mobility & Utility

### **Aqua Anchor** (`aqua-anchor`)
The mob moves at normal speed underwater and is not pushed by currents.
- **No Configuration**

### **Quicksilver** (`quicksilver`)
Chance on hit for the mob to gain a massive temporary speed boost.
- **Config**: `chance`, `speed-amplifier`, `duration-ticks`

### **Shadow Extraction** (`shadow-extraction`)
Nearby fallen mobs are resurrected as "Shadows" to fight alongside the mob for a limited time.
- **Config**: `chance`, `duration-seconds`, `max-shadows`

### **Siege Breaker** (`siege-breaker`)
Detects if a player is "pillaring" (building vertically) nearby and breaks the blocks beneath them to bring them down.
- **Config**: `min-height-diff`, `horizontal-range`, `max-blocks-per-check`
