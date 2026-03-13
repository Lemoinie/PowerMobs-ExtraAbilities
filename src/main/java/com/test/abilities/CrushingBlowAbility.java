package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Ability: Crushing Blow
 * Mob charges for 2 seconds, then rushes target with high speed for a massive knockback attack.
 * Deals damage only if player hits a block during knockback.
 */
public class CrushingBlowAbility extends AbstractAbility implements Listener {

    private final String title = "Crushing Blow";
    private final String description = "Charges up a massive blow. High damage if knocked into a wall.";
    private final Material material = Material.NETHERITE_AXE;

    private final double defaultChance = 0.1;
    private final int defaultChargeTicks = 40;
    private final double defaultApproachSpeed = 0.5; // Base is usually ~0.3
    private final double defaultImpactDamage = 10.0;
    private final double defaultKnockbackStrength = 2.5;
    private final double defaultWallDamage = 15.0;
    private final int defaultCooldown = 15;

    private final Set<UUID> chargingMobs = new HashSet<>();
    private final Set<UUID> rushingMobs = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CrushingBlowAbility(PowerMobsPlugin plugin) {
        super(plugin, "crushing-blow");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {
        chargingMobs.remove(powerMob.getEntityUuid());
        rushingMobs.remove(powerMob.getEntityUuid());
        cooldowns.remove(powerMob.getEntityUuid());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        
        // Prevent attacking while charging
        if (chargingMobs.contains(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        PowerMob powerMob = PowerMob.getFromEntity(plugin, attacker);
        if (powerMob == null) return;
        
        boolean hasAbility = powerMob.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id));
        if (!hasAbility) return;

        // If currently rushing, handle the impact
        if (rushingMobs.contains(attacker.getUniqueId()) && event.getEntity() instanceof Player player) {
            rushingMobs.remove(attacker.getUniqueId());
            event.setCancelled(true);
            handleImpact(powerMob, attacker, player);
            return;
        }

        // Check chance to start charging
        double chance = ValidationUtils.getValidDouble(powerMob, this.id, "chance", defaultChance, 0.0, 1.0);
        long now = System.currentTimeMillis();
        int cooldown = ValidationUtils.getValidInt(powerMob, this.id, "cooldown", defaultCooldown, 0, 300);

        if (cooldowns.containsKey(attacker.getUniqueId())) {
            if (now - cooldowns.get(attacker.getUniqueId()) < cooldown * 1000L) return;
        }

        if (new Random().nextDouble() <= chance) {
            startCharging(powerMob, attacker);
        }
    }

    private void startCharging(PowerMob powerMob, LivingEntity mob) {
        UUID uuid = mob.getUniqueId();
        chargingMobs.add(uuid);
        cooldowns.put(uuid, System.currentTimeMillis());

        // Visuals
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
        
        // Slow down mob to "stand still"
        double originalSpeed = mob.getAttribute(Attribute.ATTACK_DAMAGE).getBaseValue(); // Using as temp storage or just set speed
        if (mob.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            mob.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0);
        }

        int chargeTicks = ValidationUtils.getValidInt(powerMob, this.id, "charge-ticks", defaultChargeTicks, 10, 200);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!mob.isValid()) {
                chargingMobs.remove(uuid);
                return;
            }

            chargingMobs.remove(uuid);
            rushingMobs.add(uuid);

            // Rush
            double speed = ValidationUtils.getValidDouble(powerMob, this.id, "approach-speed", defaultApproachSpeed, 0.1, 2.0);
            if (mob.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                mob.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
            }

            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.5f);
            mob.getWorld().spawnParticle(Particle.CLOUD, mob.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

            // Duration for the rush phase before it expires
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (rushingMobs.remove(uuid)) {
                    // Reset speed if missed
                    if (mob.isValid() && mob.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                        mob.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.25); // Approximate default
                    }
                }
            }, 100L); // 5 seconds to hit someone

        }, chargeTicks);
    }

    private void handleImpact(PowerMob powerMob, LivingEntity mob, Player player) {
        // Reset speed
        if (mob.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            mob.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.25);
        }

        double damage = ValidationUtils.getValidDouble(powerMob, this.id, "impact-damage", defaultImpactDamage, 0, 100);
        double kbPower = ValidationUtils.getValidDouble(powerMob, this.id, "knockback-strength", defaultKnockbackStrength, 0, 10);
        double wallDamage = ValidationUtils.getValidDouble(powerMob, this.id, "wall-collision-damage", defaultWallDamage, 0, 100);

        // Standard hit damage - only if player is alive
        if (!player.isDead()) {
            player.damage(damage, mob);
        }

        // Knockback
        Vector dir = player.getLocation().toVector().subtract(mob.getLocation().toVector()).normalize();
        dir.setY(0.4); // Pop them up a bit
        player.setVelocity(dir.multiply(kbPower));

        mob.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 0.8f);

        // Task to detect wall hit
        new BukkitTaskWallCheck(player, wallDamage, mob).runTaskTimer(plugin, 1, 1);
    }

    private class BukkitTaskWallCheck extends org.bukkit.scheduler.BukkitRunnable {
        private final Player player;
        private final double wallDamage;
        private final LivingEntity source;
        private int ticks = 0;

        public BukkitTaskWallCheck(Player player, double wallDamage, LivingEntity source) {
            this.player = player;
            this.wallDamage = wallDamage;
            this.source = source;
        }

        @Override
        public void run() {
            if (ticks++ > 40 || !player.isValid()) {
                this.cancel();
                return;
            }

            // Check for collision
            if (isCollidingWithBlock(player)) {
                if (!player.isDead() && player.isValid()) {
                    player.damage(wallDamage, source);
                    player.sendMessage(ChatColor.RED + "You slammed into a wall!");
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
                    player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                }
                this.cancel();
            }
        }

        private boolean isCollidingWithBlock(Player p) {
            Location loc = p.getLocation();
            Vector v = p.getVelocity();
            
            // Check in the direction of velocity
            Location head = loc.clone().add(0, 1.5, 0);
            Location feet = loc.clone().add(0, 0.2, 0);
            
            return head.add(v.clone().multiply(0.5)).getBlock().getType().isSolid() ||
                   feet.add(v.clone().multiply(0.5)).getBlock().getType().isSolid();
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
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to trigger on hit"));
        m.put("charge-ticks", AbilityConfigField.integer("charge-ticks", defaultChargeTicks, "Ticks to stand still (20 = 1s)"));
        m.put("approach-speed", AbilityConfigField.dbl("approach-speed", defaultApproachSpeed, "Move speed during rush phase"));
        m.put("impact-damage", AbilityConfigField.dbl("impact-damage", defaultImpactDamage, "Damage on direct hit"));
        m.put("knockback-strength", AbilityConfigField.dbl("knockback-strength", defaultKnockbackStrength, "Force of the knockback"));
        m.put("wall-collision-damage", AbilityConfigField.dbl("wall-collision-damage", defaultWallDamage, "Damage if player hits a block"));
        m.put("cooldown", AbilityConfigField.integer("cooldown", defaultCooldown, "Cooldown in seconds"));
        return m;
    }
}
