package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Ability: Death Zone
 * Summons a black zone. Staying inside for a long time results in instant death.
 */
public class DeathZoneAbility extends AbstractAbility implements Listener {

    private final String title = "Death Zone";
    private final String description = "Creates a dark zone. Staying inside for too long is fatal.";
    private final Material material = Material.NETHER_STAR;

    private final double defaultSummonChance = 0.05;
    private final double defaultRadius = 5.0;
    private final int defaultDeadlyTimeSeconds = 15;
    private final int defaultZoneDurationSeconds = 30;

    private final List<DeathZone> activeZones = new ArrayList<>();
    private BukkitTask processingTask;
    private final Random random = new Random();

    public DeathZoneAbility(PowerMobsPlugin plugin) {
        super(plugin, "death-zone");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startProcessing();
    }

    private void startProcessing() {
        processingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<DeathZone> it = activeZones.iterator();
            while (it.hasNext()) {
                DeathZone zone = it.next();
                if (zone.isExpired()) {
                    it.remove();
                    continue;
                }
                zone.tick();
            }
        }, 0, 10); // Tick every 10 ticks (0.5s)
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
            summonZone(powerMob, attacker);
        }
    }

    private void summonZone(PowerMob powerMob, LivingEntity mob) {
        Location loc = mob.getLocation();
        double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", defaultRadius, 1.0, 20.0);
        int deadlyTimeTicks = ValidationUtils.getValidInt(powerMob, this.id, "deadly-time-seconds", defaultDeadlyTimeSeconds, 1, 300) * 20;
        int durationTicks = ValidationUtils.getValidInt(powerMob, this.id, "zone-duration-seconds", defaultZoneDurationSeconds, 5, 600) * 20;

        activeZones.add(new DeathZone(loc, radius, deadlyTimeTicks, durationTicks));
        mob.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
    }

    private class DeathZone {
        private final Location center;
        private final double radius;
        private final int requiredTicks;
        private final int durationTicks;
        private final long startTime;
        private final Map<UUID, Integer> playerTicks = new HashMap<>();

        public DeathZone(Location center, double radius, int requiredTicks, int durationTicks) {
            this.center = center;
            this.radius = radius;
            this.requiredTicks = requiredTicks;
            this.durationTicks = durationTicks;
            this.startTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > durationTicks * 50L;
        }

        public void tick() {
            spawnParticles();
            
            for (Player player : center.getWorld().getPlayers()) {
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
                
                if (player.getLocation().distance(center) <= radius && Math.abs(player.getLocation().getY() - center.getY()) < 3) {
                    int ticks = playerTicks.getOrDefault(player.getUniqueId(), 0) + 10;
                    playerTicks.put(player.getUniqueId(), ticks);

                    if (ticks % 20 == 0) { // Every second
                        int secondsLeft = (requiredTicks - ticks) / 20;
                        if (secondsLeft > 0 && secondsLeft <= 5) {
                            player.sendMessage(ChatColor.DARK_RED + "DEATH IN " + secondsLeft + "...");
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 2.0f);
                        }
                    }

                    if (ticks >= requiredTicks) {
                        applyDeath(player);
                        playerTicks.put(player.getUniqueId(), 0); 
                    }
                } else {
                    playerTicks.remove(player.getUniqueId());
                }
            }
        }

        private void applyDeath(Player player) {
            // Instant Death
            player.setHealth(0);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "THE VOID HAS CLAIMED YOU.");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
            player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 1);
        }

        private void spawnParticles() {
            World world = center.getWorld();
            if (world == null) return;

            // Outer ring
            for (int i = 0; i < 40; i++) {
                double angle = i * Math.PI * 2 / 40;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                world.spawnParticle(Particle.DUST, x, center.getY() + 0.1, z, 1, new Particle.DustOptions(Color.BLACK, 2.0f));
            }
            
            // Random interior bits
            for (int i = 0; i < 10; i++) {
                double r = random.nextDouble() * radius;
                double angle = random.nextDouble() * Math.PI * 2;
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                world.spawnParticle(Particle.SQUID_INK, x, center.getY() + 0.5, z, 1, 0, 0, 0, 0.02);
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
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("summon-chance", AbilityConfigField.chance("summon-chance", defaultSummonChance, "Chance to summon zone on attack"));
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius of the death zone"));
        m.put("deadly-time-seconds", AbilityConfigField.integer("deadly-time-seconds", defaultDeadlyTimeSeconds, "Seconds a player can stay inside before dying"));
        m.put("zone-duration-seconds", AbilityConfigField.integer("zone-duration-seconds", defaultZoneDurationSeconds, "How long the zone exists on the ground"));
        return m;
    }

    @Override
    public List<String> getStatus() { return List.of(); }
}
