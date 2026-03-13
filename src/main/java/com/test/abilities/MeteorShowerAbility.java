package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Ability: Meteor Shower
 * Summons multiple meteors in an area around players. 
 * Increases in intensity based on the number of players nearby.
 */
public class MeteorShowerAbility extends AbstractAbility {

    private final Map<UUID, Integer> activeTasks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();

    private final int defaultCooldown = 180;
    private final double defaultSearchRadius = 30.0;
    private final int defaultBaseCount = 10;
    private final double defaultSpreadRadius = 10.0;
    private final int defaultDelayTicks = 10; // 0.5s
    private final int defaultIncreasePerPlayer = 5;
    private final double defaultDamage = 8.0;

    public MeteorShowerAbility(PowerMobsPlugin plugin) {
        super(plugin, "meteor-shower");
    }

    @Override
    public void apply(PowerMob powerMob) {
        if (activeTasks.containsKey(powerMob.getEntityUuid())) return;

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LivingEntity mob = powerMob.getEntity();
            if (mob == null || !mob.isValid()) {
                remove(powerMob);
                return;
            }

            long now = System.currentTimeMillis();
            int cooldown = ValidationUtils.getValidInt(powerMob, this.id, "cooldown-seconds", defaultCooldown, 10, 600);
            
            if (cooldowns.containsKey(powerMob.getEntityUuid())) {
                if (now - cooldowns.get(powerMob.getEntityUuid()) < cooldown * 1000L) return;
            }

            double radius = ValidationUtils.getValidDouble(powerMob, this.id, "search-radius", defaultSearchRadius, 5.0, 100.0);
            List<Player> nearbyPlayers = new ArrayList<>();
            for (Entity e : mob.getNearbyEntities(radius, radius, radius)) {
                if (e instanceof Player p && p.getGameMode() == GameMode.SURVIVAL) {
                    nearbyPlayers.add(p);
                }
            }

            if (!nearbyPlayers.isEmpty()) {
                cooldowns.put(powerMob.getEntityUuid(), now);
                startShower(powerMob, mob, nearbyPlayers);
            }

        }, 200, 200).getTaskId();

        activeTasks.put(powerMob.getEntityUuid(), taskId);
    }

    private void startShower(PowerMob powerMob, LivingEntity mob, List<Player> players) {
        int baseCount = ValidationUtils.getValidInt(powerMob, this.id, "base-count", defaultBaseCount, 1, 50);
        int increase = ValidationUtils.getValidInt(powerMob, this.id, "increase-per-player", defaultIncreasePerPlayer, 0, 20);
        int delay = ValidationUtils.getValidInt(powerMob, this.id, "delay-between-ticks", defaultDelayTicks, 1, 100);
        double spread = ValidationUtils.getValidDouble(powerMob, this.id, "spread-radius", defaultSpreadRadius, 1.0, 30.0);
        double damage = ValidationUtils.getValidDouble(powerMob, this.id, "damage", defaultDamage, 0.0, 50.0);

        int totalMeteors = baseCount + (players.size() - 1) * increase;
        
        // Pick a focal point (one of the players)
        Player focalPlayer = players.get(random.nextInt(players.size()));
        Location center = focalPlayer.getLocation();

        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        if (focalPlayer != null) {
            focalPlayer.sendMessage("§c§lA METEOR SHOWER IS APPROACHING!");
        }

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= totalMeteors || !mob.isValid()) {
                    this.cancel();
                    return;
                }

                // Pick a random spot in the spread
                double xOff = (random.nextDouble() * 2 - 1) * spread;
                double zOff = (random.nextDouble() * 2 - 1) * spread;
                Location impactLoc = center.clone().add(xOff, 0, zOff);
                // Adjust Y to floor
                impactLoc.setY(impactLoc.getWorld().getHighestBlockYAt(impactLoc) + 0.1);

                spawnSingleMeteor(mob, impactLoc, damage);
                
                count++;
            }
        }.runTaskTimer(plugin, 40, delay); // Start after 2s warning
    }

    private void spawnSingleMeteor(LivingEntity mob, Location impactLoc, double damage) {
        Location startLoc = impactLoc.clone().add(random.nextInt(10) - 5, 30, random.nextInt(10) - 5);
        Vector direction = impactLoc.toVector().subtract(startLoc.toVector()).normalize();
        
        new BukkitRunnable() {
            Location current = startLoc.clone();
            int steps = 0;

            @Override
            public void run() {
                if (steps > 60 || current.getY() <= impactLoc.getY() || current.getBlock().getType().isSolid()) {
                    explode(mob, current, damage);
                    this.cancel();
                    return;
                }

                current.getWorld().spawnParticle(Particle.FLAME, current, 5, 0.1, 0.1, 0.1, 0.05);
                current.getWorld().spawnParticle(Particle.LARGE_SMOKE, current, 2, 0.05, 0.05, 0.05, 0.02);
                
                current.add(direction.clone().multiply(1.5));
                steps++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void explode(LivingEntity mob, Location loc, double damage) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 30, 1, 1, 1, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof LivingEntity victim && !e.equals(mob)) {
                victim.damage(damage, mob);
                victim.setFireTicks(40);
            }
        }
    }

    @Override
    public void remove(PowerMob powerMob) {
        Integer taskId = activeTasks.remove(powerMob.getEntityUuid());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        cooldowns.remove(powerMob.getEntityUuid());
    }

    @Override
    public String getTitle() { return "Meteor Shower"; }

    @Override
    public String getDescription() { return "Summons a barrage of meteors. Intensity scales with the number of nearby players."; }

    @Override
    public Material getMaterial() { return Material.FIRE_CHARGE; }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("cooldown-seconds", AbilityConfigField.integer("cooldown-seconds", defaultCooldown, "Cooldown between showers"));
        m.put("search-radius", AbilityConfigField.dbl("search-radius", defaultSearchRadius, "Range to find players to target"));
        m.put("base-count", AbilityConfigField.integer("base-count", defaultBaseCount, "Base number of meteors"));
        m.put("spread-radius", AbilityConfigField.dbl("spread-radius", defaultSpreadRadius, "Area spread of the shower"));
        m.put("delay-between-ticks", AbilityConfigField.integer("delay-between-ticks", defaultDelayTicks, "Delay between each meteor fall (20 = 1s)"));
        m.put("increase-per-player", AbilityConfigField.integer("increase-per-player", defaultIncreasePerPlayer, "Extra meteors for each additional player"));
        m.put("damage", AbilityConfigField.dbl("damage", defaultDamage, "Damage per meteor impact"));
        return m;
    }

    @Override
    public List<String> getStatus() { return List.of(); }
}
