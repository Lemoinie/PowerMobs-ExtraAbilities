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
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

/**
 * Ability that reduces incoming damage when the mob is below a certain health threshold.
 */
public class TenacityAbility extends AbstractAbility implements Listener {

    private final String title = "Tenacity";
    private final String description = "Reduces incoming damage when low on health.";
    private final Material material = Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE;

    private final double defaultHpThreshold = 0.3; // 30%
    private final double defaultDamageReduction = 0.5; // 50%

    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();

    public TenacityAbility(PowerMobsPlugin plugin) {
        super(plugin, "tenacity");
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (!monitoredMobs.containsKey(uuid)) return;

        PowerMob powerMob = monitoredMobs.get(uuid);
        if (!powerMob.isValid()) {
            remove(powerMob);
            return;
        }

        LivingEntity mob = powerMob.getEntity();
        double currentHp = mob.getHealth();
        double maxHp = mob.getMaxHealth();
        
        double threshold = ValidationUtils.getValidDouble(powerMob, this.id, "hp-threshold", this.defaultHpThreshold, 0.0, 1.0);
        
        if (currentHp / maxHp <= threshold) {
            double reduction = ValidationUtils.getValidDouble(powerMob, this.id, "damage-reduction", this.defaultDamageReduction, 0.0, 1.0);
            double multiplier = 1.0 - reduction;
            
            event.setDamage(event.getDamage() * multiplier);
            
            // Visual feedback
            mob.getWorld().spawnParticle(Particle.CRIT, mob.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
            // Low volume click/thud sound
            mob.getWorld().playSound(mob.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.5f, 0.8f);
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
        m.put("hp-threshold", AbilityConfigField.dbl("hp-threshold", defaultHpThreshold, "Health threshold to activate (ratio)"));
        m.put("damage-reduction", AbilityConfigField.dbl("damage-reduction", defaultDamageReduction, "Percentage of damage to reduce (0.0 - 1.0)"));
        return m;
    }
}
