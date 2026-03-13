package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Ability that applies Wither effect to players hit by the mob.
 */
public class WitherTouchAbility extends AbstractAbility implements Listener {

    private final String title = "Wither Touch";
    private final String description = "Inflicts wither on hit.";
    private final Material material = Material.WITHER_SKELETON_SKULL;

    private final int defaultStrength = 3;
    private final int defaultDuration = 10;
    private final double defaultChance = 0.5;

    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();
    private final Random random = new Random();

    public WitherTouchAbility(PowerMobsPlugin plugin) {
        super(plugin, "wither-touch");
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
    public void onHit(EntityDamageByEntityEvent event) {
        UUID attackerUuid = event.getDamager().getUniqueId();
        if (!monitoredMobs.containsKey(attackerUuid)) return;

        PowerMob powerMob = monitoredMobs.get(attackerUuid);
        if (!(event.getEntity() instanceof Player player)) return;

        double chance = ValidationUtils.getValidDouble(powerMob, this.id, "chance", this.defaultChance, 0.0, 1.0);
        if (random.nextDouble() > chance) return;

        int strength = ValidationUtils.getValidInt(powerMob, this.id, "strength", this.defaultStrength, 1, 255);
        int duration = ValidationUtils.getValidInt(powerMob, this.id, "duration", this.defaultDuration, 1, 3600);

        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration * 20, Math.max(0, strength - 1)));
        player.sendMessage("§8You feel a withered touch from the mob!");
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
        m.put("strength", AbilityConfigField.integer("strength", defaultStrength, "Strength of the Wither effect"));
        m.put("duration", AbilityConfigField.integer("duration", defaultDuration, "Duration of the Wither effect in seconds"));
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to apply the effect (0.0 - 1.0)"));
        return m;
    }
}
