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
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;

import java.util.List;
import java.util.Map;

/**
 * Ability: Potion Immunity
 * Mob is immune to effects from splash and lingering potions.
 */
public class PotionImmunityAbility extends AbstractAbility implements Listener {

    public PotionImmunityAbility(PowerMobsPlugin plugin) {
        super(plugin, "potion-immunity");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        event.getAffectedEntities().removeIf(entity -> {
            if (!(entity instanceof LivingEntity victim)) return false;
            PowerMob pm = PowerMob.getFromEntity(plugin, victim);
            return pm != null && pm.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLingeringPotion(AreaEffectCloudApplyEvent event) {
        event.getAffectedEntities().removeIf(entity -> {
            if (!(entity instanceof LivingEntity victim)) return false;
            PowerMob pm = PowerMob.getFromEntity(plugin, victim);
            return pm != null && pm.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id));
        });
    }

    @Override
    public String getTitle() { return "Potion Immunity"; }

    @Override
    public String getDescription() { return "Mob is immune to splash and lingering potions."; }

    @Override
    public Material getMaterial() { return Material.GLASS_BOTTLE; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        return Map.of();
    }
}
