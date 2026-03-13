package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.powermobs.utils.MobTargetingUtil;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Ability that slams the ground, dismounting players from horses, camels, or nautiluses
 */
public class MountCrashAbility extends AbstractAbility {

    private final String title = "Mount Crash";
    private final String description = "Slams the ground, dismounting players on mounts.";
    private final Material material = Material.SADDLE;

    private final double defaultRadius = 8.0;
    private final int defaultSlownessDuration = 5;
    private final int defaultTickRate = 100; // 5 seconds

    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public MountCrashAbility(PowerMobsPlugin plugin) {
        super(plugin, "mount-crash");
    }

    @Override
    public void apply(PowerMob powerMob) {
        UUID entityUuid = powerMob.getEntityUuid();

        final double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", this.defaultRadius, 0.1, 50.0);
        final int slownessDuration = ValidationUtils.getValidInt(powerMob, this.id, "slowness-duration", this.defaultSlownessDuration, 1, 600);
        final int tickRate = ValidationUtils.getValidInt(powerMob, this.id, "tick-rate", this.defaultTickRate, 1, 1200);

        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!powerMob.isValid()) {
                remove(powerMob);
                return;
            }

            Location loc = powerMob.getEntity().getLocation();
            boolean triggered = false;

            for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (entity instanceof Player player && player.getUniqueId() != entityUuid) {
                    if (player.getLocation().distance(loc) <= radius && 
                        MobTargetingUtil.shouldAllowTargeting(this.plugin, powerMob.getEntity(), player)) {
                        
                        Entity vehicle = player.getVehicle();
                        if (vehicle != null && (isMount(vehicle))) {
                            // Dismount player
                            player.leaveVehicle();
                            
                            // Apply Slowness III (amplifier 2)
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration * 20, 2));
                            
                            // Visuals for the player
                            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                            player.sendMessage("§cYour mount was crashed by a Power Mob!");
                            triggered = true;
                        }
                    }
                }
            }

            if (triggered) {
                // Ground slam effects
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 5, 1.0, 0.1, 1.0, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.5f);
            }

        }, 0, tickRate);

        this.tasks.put(entityUuid, task);
    }

    private boolean isMount(Entity entity) {
        // Core mounts
        if (entity instanceof AbstractHorse || entity instanceof Camel) {
            return true;
        }
        
        // Handle 1.21.11 Nautilus or other new mounts via class name check if not explicitly in API
        String className = entity.getClass().getSimpleName();
        return className.contains("Nautilus");
    }

    @Override
    public void remove(PowerMob powerMob) {
        UUID entityUuid = powerMob.getEntityUuid();
        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
            this.tasks.remove(entityUuid);
        }
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public Material getMaterial() {
        return this.material;
    }

    @Override
    public List<String> getStatus() {
        return List.of();
    }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius to crash mounts"));
        m.put("slowness-duration", AbilityConfigField.integer("slowness-duration", defaultSlownessDuration, "Duration of Slowness III in seconds"));
        m.put("tick-rate", AbilityConfigField.integer("tick-rate", defaultTickRate, "Cooldown between checks (in ticks)"));
        return m;
    }
}
