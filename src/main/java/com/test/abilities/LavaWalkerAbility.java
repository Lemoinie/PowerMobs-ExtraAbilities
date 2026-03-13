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
 * Ability: Lava Walker
 * Mob is immune to fire and lava damage.
 */
public class LavaWalkerAbility extends AbstractAbility implements Listener {

    public LavaWalkerAbility(PowerMobsPlugin plugin) {
        super(plugin, "lava-walker");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FIRE || 
            cause == EntityDamageEvent.DamageCause.FIRE_TICK || 
            cause == EntityDamageEvent.DamageCause.LAVA || 
            cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            
            PowerMob pm = PowerMob.getFromEntity(plugin, victim);
            if (pm != null && pm.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id))) {
                event.setCancelled(true);
                victim.setFireTicks(0);
            }
        }
    }

    @Override
    public String getTitle() { return "Lava Walker"; }

    @Override
    public String getDescription() { return "Mob is immune to fire, lava, and magma damage."; }

    @Override
    public Material getMaterial() { return Material.LAVA_BUCKET; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        return Map.of();
    }
}
