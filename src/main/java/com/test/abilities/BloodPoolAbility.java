package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Ability that summons a blood pool on the ground. Players standing in it for too long take heavy damage.
 */
public class BloodPoolAbility extends AbstractAbility implements Listener {

    private final String title = "Blood Pool";
    private final String description = "Summons a pool of blood. Staying inside for too long deals massive damage.";
    private final Material material = Material.REDSTONE_BLOCK;

    private final double defaultSummonChance = 0.1;
    private final double defaultRadius = 3.0;
    private final int defaultStayTimeSeconds = 3;
    private final double defaultDamagePercent = 0.2; // 20%

    private final List<BloodPool> activePools = new ArrayList<>();
    private BukkitTask processingTask;
    private final Random random = new Random();

    public BloodPoolAbility(PowerMobsPlugin plugin) {
        super(plugin, "blood-pool");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startProcessing();
    }

    private void startProcessing() {
        processingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<BloodPool> it = activePools.iterator();
            while (it.hasNext()) {
                BloodPool pool = it.next();
                if (pool.isExpired()) {
                    it.remove();
                    continue;
                }
                pool.tick();
            }
        }, 0, 5); // Tick every 5 ticks (0.25s) for smooth tracking
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        PowerMob powerMob = PowerMob.getFromEntity(plugin, attacker);
        if (powerMob == null) return;
        
        boolean hasAbility = powerMob.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id));
        if (!hasAbility) return;

        double chance = ValidationUtils.getValidDouble(powerMob, this.id, "summon-chance", defaultSummonChance, 0.0, 1.0);
        if (random.nextDouble() <= chance) {
            summonPool(powerMob, attacker);
        }
    }

    private void summonPool(PowerMob powerMob, LivingEntity mob) {
        Location front = mob.getLocation().add(mob.getLocation().getDirection().setY(0).normalize().multiply(2));
        // Ensure it's on the ground
        front = findGround(front);

        double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", defaultRadius, 1.0, 10.0);
        int stayTimeTicks = ValidationUtils.getValidInt(powerMob, this.id, "stay-time-seconds", defaultStayTimeSeconds, 1, 60) * 20;
        double damagePct = ValidationUtils.getValidDouble(powerMob, this.id, "damage-percent", defaultDamagePercent, 0.01, 1.0);

        activePools.add(new BloodPool(front, radius, stayTimeTicks, damagePct));
        mob.getWorld().playSound(front, Sound.ITEM_BUCKET_EMPTY_LAVA, 1.0f, 0.5f);
    }

    private Location findGround(Location loc) {
        Location l = loc.clone();
        for (int i = 0; i < 5; i++) {
            if (l.getBlock().getType().isSolid()) break;
            l.add(0, -1, 0);
        }
        return l;
    }

    private class BloodPool {
        private final Location center;
        private final double radius;
        private final int requiredTicks;
        private final double damagePct;
        private final long startTime;
        private final int durationTicks = 200; // Pool lasts 10 seconds
        private final Map<UUID, Integer> playerTicks = new HashMap<>();

        public BloodPool(Location center, double radius, int requiredTicks, double damagePct) {
            this.center = center;
            this.radius = radius;
            this.requiredTicks = requiredTicks;
            this.damagePct = damagePct;
            this.startTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > durationTicks * 50L;
        }

        public void tick() {
            spawnParticles();
            
            for (Player player : center.getWorld().getPlayers()) {
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
                
                if (player.getLocation().distance(center) <= radius && Math.abs(player.getLocation().getY() - center.getY()) < 2) {
                    int ticks = playerTicks.getOrDefault(player.getUniqueId(), 0) + 5;
                    playerTicks.put(player.getUniqueId(), ticks);

                    if (ticks >= requiredTicks) {
                        applyDamage(player);
                        playerTicks.put(player.getUniqueId(), 0); // Reset after damage
                    }
                } else {
                    playerTicks.remove(player.getUniqueId());
                }
            }
        }

        private void applyDamage(Player player) {
            double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            double damage = maxHp * damagePct;
            
            // Bypass armor/defense by directly modifying health
            double newHealth = Math.max(0, player.getHealth() - damage);
            player.setHealth(newHealth);
            
            player.sendMessage("§4Your blood boils within the pool!");
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 1, 0), 20, 0.2, 0.5, 0.2, Material.REDSTONE_BLOCK.createBlockData());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
        }

        private void spawnParticles() {
            World world = center.getWorld();
            if (world == null) return;

            // Disk particles
            for (int i = 0; i < 20; i++) {
                double r = Math.sqrt(random.nextDouble()) * radius;
                double angle = random.nextDouble() * 2 * Math.PI;
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                Location pLoc = new Location(world, x, center.getY() + 0.1, z);
                world.spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(Color.RED, 1.0f));
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
        m.put("summon-chance", AbilityConfigField.chance("summon-chance", defaultSummonChance, "Chance to summon pool on attack"));
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius of the blood pool"));
        m.put("stay-time-seconds", AbilityConfigField.integer("stay-time-seconds", defaultStayTimeSeconds, "Time player must stay in to take damage"));
        m.put("damage-percent", AbilityConfigField.dbl("damage-percent", defaultDamagePercent, "Percent of max HP to deal (0.2 = 20%)"));
        return m;
    }
}
