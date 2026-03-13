package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Ability that triggers a loud screech when damaged, applying various debuffs to nearby players.
 */
public class DreadScreechAbility extends AbstractAbility implements Listener {

    private final String title = "Dread Screech";
    private final String description = "Screeches when damaged, debuffing nearby players.";
    private final Material material = Material.GHAST_TEAR;

    private final double defaultThreshold = 0.1; // 10%
    private final double defaultRadius = 5.0;
    private final double defaultChanceAbove = 0.05; // 5%
    private final double defaultChanceBelow = 0.0;
    private final int defaultDuration = 5; // 5 seconds
    private final int defaultSlownessLevel = 3;

    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();
    private final Set<UUID> thresholdTriggered = new HashSet<>();
    private final Random random = new Random();

    public DreadScreechAbility(PowerMobsPlugin plugin) {
        super(plugin, "dread-screech");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {
        monitoredMobs.put(powerMob.getEntityUuid(), powerMob);
    }

    @Override
    public void remove(PowerMob powerMob) {
        UUID uuid = powerMob.getEntityUuid();
        monitoredMobs.remove(uuid);
        thresholdTriggered.remove(uuid);
    }

    @EventHandler
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
        double nextHp = currentHp - event.getFinalDamage();
        
        double threshold = ValidationUtils.getValidDouble(powerMob, this.id, "hp-threshold", this.defaultThreshold, 0.0, 1.0);
        double chanceAbove = ValidationUtils.getValidDouble(powerMob, this.id, "chance-above-threshold", this.defaultChanceAbove, 0.0, 1.0);
        double chanceBelow = ValidationUtils.getValidDouble(powerMob, this.id, "chance-below-threshold", this.defaultChanceBelow, 0.0, 1.0);

        boolean triggered = false;

        // Threshold check (guaranteed once)
        if (!thresholdTriggered.contains(uuid) && nextHp / maxHp <= threshold) {
            thresholdTriggered.add(uuid);
            triggered = true;
        } else if (currentHp / maxHp > threshold) {
            // Chance above threshold
            if (random.nextDouble() < chanceAbove) {
                triggered = true;
            }
        } else {
            // Chance below threshold
            if (random.nextDouble() < chanceBelow) {
                triggered = true;
            }
        }

        if (triggered) {
            triggerScreech(powerMob);
        }
    }

    private void triggerScreech(PowerMob powerMob) {
        LivingEntity mob = powerMob.getEntity();
        Location loc = mob.getLocation();
        
        double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", this.defaultRadius, 1.0, 50.0);
        int duration = ValidationUtils.getValidInt(powerMob, this.id, "duration", this.defaultDuration, 1, 60);
        int slownessLevel = ValidationUtils.getValidInt(powerMob, this.id, "slowness-level", this.defaultSlownessLevel, 1, 10);

        // Sound: Ghast scream, high pitch, loud
        loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SCREAM, 2.0f, 1.5f);
        
        // Particles: Warden Sonic Boom effect
        loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc.add(0, 1, 0), 1);

        for (Entity entity : mob.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player player) {
                // Apply effects
                int ticks = duration * 20;
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, ticks, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, Math.max(0, slownessLevel - 1)));
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, 0));
                
                // Deafness (if available)
                PotionEffectType deafness = PotionEffectType.getByName("deafness");
                if (deafness != null) {
                    player.addPotionEffect(new PotionEffect(deafness, ticks, 0));
                }

                player.sendMessage("§8§lA blood-curdling screech rings in your ears!");
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
        m.put("hp-threshold", AbilityConfigField.dbl("hp-threshold", defaultThreshold, "Health threshold for guaranteed trigger (ratio)"));
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius of the screech"));
        m.put("chance-above-threshold", AbilityConfigField.dbl("chance-above-threshold", defaultChanceAbove, "Chance to trigger when above threshold"));
        m.put("chance-below-threshold", AbilityConfigField.dbl("chance-below-threshold", defaultChanceBelow, "Chance to trigger when below threshold"));
        m.put("duration", AbilityConfigField.integer("duration", defaultDuration, "Duration of debuffs (seconds)"));
        m.put("slowness-level", AbilityConfigField.integer("slowness-level", defaultSlownessLevel, "Level of Slowness to apply"));
        return m;
    }
}
