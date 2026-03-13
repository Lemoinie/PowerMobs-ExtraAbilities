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
 * Ability: Meteor Strike
 * Calls a meteor from the sky targeting the lowest HP player in range.
 */
public class MeteorStrikeAbility extends AbstractAbility {

    private final Map<UUID, Integer> activeTasks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private final int defaultCooldown = 180;
    private final double defaultSearchRadius = 40.0;
    private final int defaultWarningTicks = 40; // 2 seconds
    private final double defaultDamage = 10.0;
    private final double defaultKnockback = 1.5;

    public MeteorStrikeAbility(PowerMobsPlugin plugin) {
        super(plugin, "meteor-strike");
    }

    @Override
    public void apply(PowerMob powerMob) {
        if (activeTasks.containsKey(powerMob.getEntityUuid())) return;

        // Run a check task
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

            // Find target (lowest HP player)
            double radius = ValidationUtils.getValidDouble(powerMob, this.id, "search-radius", defaultSearchRadius, 5.0, 100.0);
            Player target = findLowestHpPlayer(mob, radius);

            if (target != null) {
                cooldowns.put(powerMob.getEntityUuid(), now);
                triggerMeteor(powerMob, mob, target);
            }

        }, 100, 100).getTaskId(); // Check every 5 seconds

        activeTasks.put(powerMob.getEntityUuid(), taskId);
    }

    private Player findLowestHpPlayer(LivingEntity mob, double radius) {
        Player lowest = null;
        double minHp = Double.MAX_VALUE;

        for (Entity e : mob.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p && p.getGameMode() == GameMode.SURVIVAL) {
                if (p.getHealth() < minHp) {
                    minHp = p.getHealth();
                    lowest = p;
                }
            }
        }
        return lowest;
    }

    private void triggerMeteor(PowerMob powerMob, LivingEntity mob, Player target) {
        Location targetLoc = target.getLocation();
        int warningTicks = ValidationUtils.getValidInt(powerMob, this.id, "warning-ticks", defaultWarningTicks, 10, 100);

        // Warning phase
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= warningTicks) {
                    spawnMeteor(powerMob, mob, targetLoc);
                    this.cancel();
                    return;
                }

                // Ground circle particles
                for (int i = 0; i < 8; i++) {
                    double angle = (ticks + i * 5) * 0.2;
                    double x = 2 * Math.cos(angle);
                    double z = 2 * Math.sin(angle);
                    targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0.02);
                }
                
                if (ticks % 10 == 0) {
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void spawnMeteor(PowerMob powerMob, LivingEntity mob, Location landingLoc) {
        Location startLoc = landingLoc.clone().add(5, 30, 5);
        Vector direction = landingLoc.toVector().subtract(startLoc.toVector()).normalize();
        double speed = 1.5;
        
        double damage = ValidationUtils.getValidDouble(powerMob, this.id, "damage", defaultDamage, 0, 100);
        double kbPower = ValidationUtils.getValidDouble(powerMob, this.id, "knockback", defaultKnockback, 0, 5);

        new BukkitRunnable() {
            Location current = startLoc.clone();
            int steps = 0;

            @Override
            public void run() {
                if (steps > 40 || current.getY() <= landingLoc.getY() || current.getBlock().getType().isSolid()) {
                    explode(powerMob, mob, current, damage, kbPower);
                    this.cancel();
                    return;
                }

                // Meteor "Tail"
                current.getWorld().spawnParticle(Particle.LARGE_SMOKE, current, 10, 0.2, 0.2, 0.2, 0.05);
                current.getWorld().spawnParticle(Particle.FLAME, current, 20, 0.3, 0.3, 0.3, 0.1);
                current.getWorld().spawnParticle(Particle.LAVA, current, 2, 0.1, 0.1, 0.1, 0.05);
                
                current.add(direction.clone().multiply(speed));
                steps++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void explode(PowerMob powerMob, LivingEntity mob, Location loc, double damage, double kb) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 1, 1, 1, 0.1);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 100, 2, 2, 2, 0.2);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            if (e instanceof LivingEntity victim && !e.equals(mob)) {
                victim.damage(damage, mob);
                victim.setFireTicks(60);
                
                Vector dir = victim.getLocation().toVector().subtract(loc.toVector()).normalize();
                dir.setY(0.5);
                victim.setVelocity(dir.multiply(kb));
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
    public String getTitle() { return "Meteor Strike"; }

    @Override
    public String getDescription() { return "Summons a meteor from the sky targeting the weakest nearby player."; }

    @Override
    public Material getMaterial() { return Material.MAGMA_BLOCK; }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("cooldown-seconds", AbilityConfigField.integer("cooldown-seconds", defaultCooldown, "Cooldown between meteor strikes"));
        m.put("search-radius", AbilityConfigField.dbl("search-radius", defaultSearchRadius, "Radius to search for players"));
        m.put("warning-ticks", AbilityConfigField.integer("warning-ticks", defaultWarningTicks, "Delay between warning and meteor fall (20 = 1s)"));
        m.put("damage", AbilityConfigField.dbl("damage", defaultDamage, "Damage dealt by the impact"));
        m.put("knockback", AbilityConfigField.dbl("knockback", defaultKnockback, "Strength of the explosion knockback"));
        return m;
    }

    @Override
    public List<String> getStatus() { return List.of(); }
}
