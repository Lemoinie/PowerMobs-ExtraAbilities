package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

/**
 * Ability that creates a protective domain for players but makes the mob and nearby monsters vulnerable.
 */
public class DomainExpansionAbility extends AbstractAbility implements Listener {

    private final String title = "Domain Expansion";
    private final String description = "Creates a zone where players take less damage but the mob takes more.";
    private final Material material = Material.BEACON;

    private final double defaultRadius = 10.0;
    private final double defaultMobVulnerability = 0.5; // +50%
    private final double defaultPlayerProtection = 0.5; // -50%
    private final double defaultChance = 0.05;
    private final int defaultCooldown = 120;
    private final int defaultDuration = 15;

    private final Map<UUID, Long> activeUntil = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();
    private final Random random = new Random();

    public DomainExpansionAbility(PowerMobsPlugin plugin) {
        super(plugin, "domain-expansion");
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
        activeUntil.remove(uuid);
        cooldowns.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onTrigger(EntityDamageEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (!monitoredMobs.containsKey(uuid)) return;

        PowerMob powerMob = monitoredMobs.get(uuid);
        if (!powerMob.isValid()) {
            remove(powerMob);
            return;
        }

        long now = System.currentTimeMillis();
        if (activeUntil.containsKey(uuid) || now < cooldowns.getOrDefault(uuid, 0L)) return;

        double chance = ValidationUtils.getValidDouble(powerMob, this.id, "chance", this.defaultChance, 0.0, 1.0);
        if (random.nextDouble() < chance) {
            triggerDomain(powerMob);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageInDomain(EntityDamageEvent event) {
        for (Map.Entry<UUID, Long> entry : activeUntil.entrySet()) {
            UUID mobUuid = entry.getKey();
            if (System.currentTimeMillis() > entry.getValue()) continue;

            PowerMob powerMob = monitoredMobs.get(mobUuid);
            if (powerMob == null || !powerMob.isValid()) continue;

            LivingEntity mob = powerMob.getEntity();
            Location domainCenter = mob.getLocation();
            double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", this.defaultRadius, 1.0, 50.0);

            Entity victim = event.getEntity();
            if (victim.getWorld().equals(domainCenter.getWorld()) && victim.getLocation().distance(domainCenter) <= radius) {
                
                if (victim.getUniqueId().equals(mobUuid) || victim instanceof Monster) {
                    // Increase damage to mob and "summons" (Monsters)
                    double vuln = ValidationUtils.getValidDouble(powerMob, this.id, "damage-increase", this.defaultMobVulnerability, 0.0, 5.0);
                    event.setDamage(event.getDamage() * (1.0 + vuln));
                } else if (victim instanceof Player) {
                    // Decrease damage to players
                    double prot = ValidationUtils.getValidDouble(powerMob, this.id, "damage-reduction", this.defaultPlayerProtection, 0.0, 1.0);
                    event.setDamage(event.getDamage() * (1.0 - prot));
                }
            }
        }
    }

    private void triggerDomain(PowerMob powerMob) {
        LivingEntity mob = powerMob.getEntity();
        UUID uuid = mob.getUniqueId();
        
        int duration = ValidationUtils.getValidInt(powerMob, this.id, "duration", this.defaultDuration, 1, 300);
        int cooldown = ValidationUtils.getValidInt(powerMob, this.id, "cooldown", this.defaultCooldown, 0, 3600);

        activeUntil.put(uuid, System.currentTimeMillis() + (duration * 1000L));
        cooldowns.put(uuid, System.currentTimeMillis() + ((duration + cooldown) * 1000L));

        mob.sendMessage("§b§lDOMAIN EXPANSION!");
        mob.getWorld().playSound(mob.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);
        
        // Visual boundary task
        double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", this.defaultRadius, 1.0, 50.0);
        new TimerTask(powerMob, duration, radius).runTaskTimer(this.plugin, 0, 10);
    }

    private class TimerTask extends org.bukkit.scheduler.BukkitRunnable {
        private final PowerMob mob;
        private int ticksLeft;
        private final double radius;

        public TimerTask(PowerMob mob, int seconds, double radius) {
            this.mob = mob;
            this.ticksLeft = seconds * 2; // Every 10 ticks = 2 per second
            this.radius = radius;
        }

        @Override
        public void run() {
            if (ticksLeft-- <= 0 || !mob.isValid() || !activeUntil.containsKey(mob.getEntityUuid())) {
                this.cancel();
                if (mob.isValid()) {
                    mob.getEntity().sendMessage("§7The domain has collapsed.");
                    mob.getEntity().getWorld().playSound(mob.getEntity().getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
                    activeUntil.remove(mob.getEntityUuid());
                }
                return;
            }

            // Draw circle with particles
            Location loc = mob.getEntity().getLocation();
            for (int i = 0; i < 360; i += 10) {
                double angle = Math.toRadians(i);
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                loc.add(x, 0.1, z);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 1, new Particle.DustOptions(Color.AQUA, 1.5f));
                loc.subtract(x, 0.1, z);
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
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius of the domain"));
        m.put("damage-increase", AbilityConfigField.dbl("damage-increase", defaultMobVulnerability, "Increase in damage taken by the mob and monsters (ratio, e.g. 0.5 = +50%)"));
        m.put("damage-reduction", AbilityConfigField.dbl("damage-reduction", defaultPlayerProtection, "Reduction in damage taken by players (ratio, e.g. 0.5 = -50%)"));
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to trigger on damage"));
        m.put("cooldown", AbilityConfigField.integer("cooldown", defaultCooldown, "Cooldown in seconds after expiration"));
        m.put("duration", AbilityConfigField.integer("duration", defaultDuration, "Duration in seconds"));
        return m;
    }
}
