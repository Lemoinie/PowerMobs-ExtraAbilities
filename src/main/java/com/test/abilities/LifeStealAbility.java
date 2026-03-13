package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

/**
 * Ability that heals the mob when it deals damage to a target.
 */
public class LifeStealAbility extends AbstractAbility implements Listener {

    private final String title = "Life Steal";
    private final String description = "Heals the mob when dealing damage.";
    private final Material material = Material.RED_DYE;

    private final double defaultHealAmount = 3.0;
    private final double defaultChance = 0.3;

    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();
    private final Random random = new Random();

    public LifeStealAbility(PowerMobsPlugin plugin) {
        super(plugin, "life-steal");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {
        monitoredMobs.put(powerMob.getEntityUuid(), powerMob);
    }

    @Override
    public void remove(PowerMob powerMob) {
        monitoredMobs.remove(powerMob.getEntityUuid());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDealDamage(EntityDamageByEntityEvent event) {
        UUID attackerUuid = event.getDamager().getUniqueId();
        if (!monitoredMobs.containsKey(attackerUuid)) return;

        PowerMob powerMob = monitoredMobs.get(attackerUuid);
        if (!powerMob.isValid()) {
            remove(powerMob);
            return;
        }

        double chance = ValidationUtils.getValidDouble(powerMob, this.id, "chance", this.defaultChance, 0.0, 1.0);
        if (random.nextDouble() > chance) return;

        double healAmount = ValidationUtils.getValidDouble(powerMob, this.id, "heal-amount", this.defaultHealAmount, 0.1, 100.0);
        
        LivingEntity mob = powerMob.getEntity();
        double currentHealth = mob.getHealth();
        double maxHealth = mob.getMaxHealth();
        
        if (currentHealth < maxHealth) {
            double newHealth = Math.min(maxHealth, currentHealth + healAmount);
            mob.setHealth(newHealth);
            
            // Visual/Sound effects
            mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WITCH_DRINK, 1.0f, 1.2f);
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
        m.put("heal-amount", AbilityConfigField.dbl("heal-amount", defaultHealAmount, "Amount of HP to heal per trigger"));
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to steal life (0.0 - 1.0)"));
        return m;
    }
}
