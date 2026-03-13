package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Ability: Parry
 * Mob has a chance to block melee attacks, negating damage and knocking back the attacker.
 */
public class ParryAbility extends AbstractAbility implements Listener {

    private final String title = "Parry";
    private final String description = "Has a chance to parry melee attacks, negating damage and reflecting knockback.";
    private final Material material = Material.SHIELD;

    private final double defaultChance = 0.2;
    private final double defaultKnockbackStrength = 1.0;
    private final Random random = new Random();

    public ParryAbility(PowerMobsPlugin plugin) {
        super(plugin, "parry");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        PowerMob powerMob = PowerMob.getFromEntity(plugin, victim);
        if (powerMob == null) return;

        boolean hasAbility = powerMob.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id));
        if (!hasAbility) return;

        // Ensure it's a melee attack (not a projectile)
        if (event.getCause() != EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK &&
            event.getCause() != EntityDamageByEntityEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }

        double chance = ValidationUtils.getValidDouble(powerMob, this.id, "chance", defaultChance, 0.0, 1.0);
        if (random.nextDouble() <= chance) {
            // Parry!
            event.setDamage(0);
            
            // Knockback attacker
            double kbStrength = ValidationUtils.getValidDouble(powerMob, this.id, "knockback-strength", defaultKnockbackStrength, 0.1, 5.0);
            Vector dir = attacker.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize();
            dir.setY(0.3); // Slight lift
            attacker.setVelocity(dir.multiply(kbStrength));

            // Visuals/Audio
            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
            victim.getWorld().spawnParticle(org.bukkit.Particle.CRIT, victim.getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.1);
            
            if (attacker instanceof org.bukkit.entity.Player player) {
                player.sendMessage("§8[§7!§8] §cYour attack was parried!");
            }
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
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to parry melee attacks"));
        m.put("knockback-strength", AbilityConfigField.dbl("knockback-strength", defaultKnockbackStrength, "Strength of the knockback on the attacker"));
        return m;
    }
}
