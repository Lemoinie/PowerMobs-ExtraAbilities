package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

/**
 * Ability that strikes lightning on nearby players. 
 * The number of strikes scales with the number of players nearby.
 */
public class ChainLightningAbility extends AbstractAbility implements Listener {

    private final String title = "Chain Lightning";
    private final String description = "Strikes lightning on all nearby players. Strikes scale with group size.";
    private final Material material = Material.LIGHTNING_ROD;

    private final double defaultChance = 0.1;
    private final double defaultRadius = 10.0;
    private final double defaultDamageMultiplier = 0.2; // 20% increase per strike
    private final double baseLightningDamage = 5.0; // Minecraft default lightning damage is approx 5

    private final Random random = new Random();

    public ChainLightningAbility(PowerMobsPlugin plugin) {
        super(plugin, "chain-lightning");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        PowerMob powerMob = PowerMob.getFromEntity(plugin, attacker);
        if (powerMob == null) return;

        boolean hasAbility = powerMob.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id));
        if (!hasAbility) return;

        double chance = ValidationUtils.getValidDouble(powerMob, this.id, "chance", defaultChance, 0.0, 1.0);
        if (random.nextDouble() > chance) return;

        double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", defaultRadius, 1.0, 50.0);
        double multiplier = ValidationUtils.getValidDouble(powerMob, this.id, "damage-multiplier", defaultDamageMultiplier, 0.0, 5.0);

        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player p : attacker.getWorld().getPlayers()) {
            if (p.getLocation().distance(attacker.getLocation()) <= radius) {
                nearbyPlayers.add(p);
            }
        }

        if (nearbyPlayers.isEmpty()) return;

        int strikeCount = nearbyPlayers.size();

        // Process each player
        for (Player target : nearbyPlayers) {
            triggerChainStrikes(target, strikeCount, attacker, multiplier);
        }
    }

    private void triggerChainStrikes(Player target, int count, LivingEntity source, double multiplier) {
        // Space strikes out slightly for visual effect and to bypass NoDamageTicks
        for (int i = 0; i < count; i++) {
            final int strikeIndex = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isValid()) return;

                Location loc = target.getLocation();
                loc.getWorld().strikeLightningEffect(loc);

                // Calculate damage: base * (1 + (index * multiplier))
                // Strike 0: Base
                // Strike 1: Base * (1.2)
                // Strike 2: Base * (1.4)
                double finalDamage = baseLightningDamage * (1.0 + (strikeIndex * multiplier));
                
                // Allow damage to accumulate even if they just got hit (bypass NoDamageTicks)
                target.setNoDamageTicks(0);
                target.damage(finalDamage, source);
                
            }, i * 5L); // 5 ticks between strikes
        }
    }

    @Override
    public String getTitle() { return title; }

    @Override
    public String getDescription() { return description; }

    @Override
    public Material getMaterial() { return material; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to trigger on attack"));
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius to find nearby players"));
        m.put("damage-multiplier", AbilityConfigField.dbl("damage-multiplier", defaultDamageMultiplier, "Damage increase per sequential strike (0.2 = 20%)"));
        return m;
    }
}
