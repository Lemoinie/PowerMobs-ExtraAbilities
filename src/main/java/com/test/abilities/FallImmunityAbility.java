package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;
import java.util.Map;

/**
 * Ability: Fall Immunity
 * Mob takes no damage from falling.
 */
public class FallImmunityAbility extends AbstractAbility implements Listener {

    public FallImmunityAbility(PowerMobsPlugin plugin) {
        super(plugin, "fall-immunity");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            PowerMob pm = PowerMob.getFromEntity(plugin, victim);
            if (pm != null && pm.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id))) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getTitle() { return "Fall Immunity"; }

    @Override
    public String getDescription() { return "Mob takes no damage from falling."; }

    @Override
    public Material getMaterial() { return Material.FEATHER; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        return Map.of();
    }
}
