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
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Ability: Potion Reflection
 * Thrown potions hitting the mob are reflected back to the thrower.
 */
public class PotionReflectionAbility extends AbstractAbility implements Listener {

    private final double defaultChance = 1.0;
    private final Random random = new Random();

    public PotionReflectionAbility(PowerMobsPlugin plugin) {
        super(plugin, "potion-reflection");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {}

    @EventHandler(priority = EventPriority.LOWEST) // Run before immunity so we can reflect
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        if (!(potion.getShooter() instanceof LivingEntity shooter)) return;

        boolean reflected = false;
        
        for (LivingEntity affected : event.getAffectedEntities()) {
            PowerMob pm = PowerMob.getFromEntity(plugin, affected);
            if (pm == null) continue;
            
            boolean hasAbility = pm.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id));
            if (!hasAbility) continue;

            double chance = ValidationUtils.getValidDouble(pm, this.id, "chance", defaultChance, 0.0, 1.0);
            if (random.nextDouble() <= chance) {
                reflected = true;
                // Remove effect from the mob
                event.setIntensity(affected, 0);
            }
        }

        if (reflected) {
            // Apply all potion effects to the shooter
            for (PotionEffect effect : potion.getEffects()) {
                shooter.addPotionEffect(effect);
            }
            shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_WITCH_THROW, 1.0f, 2.0f);
            if (shooter instanceof org.bukkit.entity.Player player) {
                player.sendMessage("§8[§7!§8] §cYour potion was reflected!");
            }
        }
    }

    @Override
    public String getTitle() { return "Potion Reflection"; }

    @Override
    public String getDescription() { return "Thrown potions are reflected back to the thrower."; }

    @Override
    public Material getMaterial() { return Material.SPLASH_POTION; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to reflect the potion"));
        return m;
    }
}
