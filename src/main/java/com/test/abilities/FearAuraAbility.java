package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Ability that emits a fear aura, de-buffing nearby players and showing a black particle circle.
 */
public class FearAuraAbility extends AbstractAbility {

    private final String title = "Fear Aura";
    private final String description = "Surrounds the mob with an aura of fear, debuffing nearby players.";
    private final Material material = Material.WEEPING_VINES;

    private final double defaultRadius = 8.0;
    private final int defaultSlownessAmp = 0;
    private final int defaultWeaknessAmp = 0;
    private final int defaultDurationTicks = 100; // 5 seconds

    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    public FearAuraAbility(PowerMobsPlugin plugin) {
        super(plugin, "fear-aura");
    }

    @Override
    public void apply(PowerMob powerMob) {
        if (activeTasks.containsKey(powerMob.getEntityUuid())) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!powerMob.isValid()) {
                remove(powerMob);
                return;
            }

            double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", defaultRadius, 1.0, 50.0);
            Location loc = powerMob.getEntity().getLocation();

            // Visual Effect: Black Circle
            spawnCircle(loc, radius);

            // Debuffs
            for (Player player : loc.getWorld().getPlayers()) {
                if (player.getLocation().distance(loc) <= radius) {
                    applyDebuffs(powerMob, player);
                }
            }
        }, 0, 20); // Every second

        activeTasks.put(powerMob.getEntityUuid(), task);
    }

    private void applyDebuffs(PowerMob powerMob, Player player) {
        int slownessAmp = ValidationUtils.getValidInt(powerMob, this.id, "slowness-amplifier", defaultSlownessAmp, 0, 10);
        int weaknessAmp = ValidationUtils.getValidInt(powerMob, this.id, "weakness-amplifier", defaultWeaknessAmp, 0, 10);
        int duration = ValidationUtils.getValidInt(powerMob, this.id, "duration-ticks", defaultDurationTicks, 20, 1200);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, slownessAmp, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, weaknessAmp, false, true));
    }

    private void spawnCircle(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;

        double increment = (2 * Math.PI) / 30; // 30 points in the circle
        for (int i = 0; i < 30; i++) {
            double angle = i * increment;
            double x = center.getX() + (radius * Math.cos(angle));
            double z = center.getZ() + (radius * Math.sin(angle));
            Location particleLoc = new Location(world, x, center.getY() + 0.1, z);
            
            // Using DUST with black color
            world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.BLACK, 1.5f));
        }
    }

    @Override
    public void remove(PowerMob powerMob) {
        BukkitTask task = activeTasks.remove(powerMob.getEntityUuid());
        if (task != null) {
            task.cancel();
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
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius of the fear aura"));
        m.put("slowness-amplifier", AbilityConfigField.integer("slowness-amplifier", defaultSlownessAmp, "Amplifier for Slowness (0 = Level 1)"));
        m.put("weakness-amplifier", AbilityConfigField.integer("weakness-amplifier", defaultWeaknessAmp, "Amplifier for Weakness (0 = Level 1)"));
        m.put("duration-ticks", AbilityConfigField.integer("duration-ticks", defaultDurationTicks, "Duration of effects in ticks (20 ticks = 1s)"));
        return m;
    }
}
