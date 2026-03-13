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
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Ability that pulls nearby players toward the mob
 */
public class VoidPullAbility extends AbstractAbility {

    private final String title = "Void Pull";
    private final String description = "Pulls nearby players toward the mob.";
    private final Material material = Material.ENDER_EYE;
    
    private final double defaultRadius = 5.0;
    private final double defaultStrength = 0.5;
    private final int defaultTickRate = 10;
    private final boolean defaultUseAttackRange = false;

    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public VoidPullAbility(PowerMobsPlugin plugin) {
        super(plugin, "void-pull");
    }

    @Override
    public void apply(PowerMob powerMob) {
        UUID entityUuid = powerMob.getEntityUuid();

        final double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", this.defaultRadius, 0.1, 50.0);
        final double strength = ValidationUtils.getValidDouble(powerMob, this.id, "strength", this.defaultStrength, 0.0, 5.0);
        final int tickRate = ValidationUtils.getValidInt(powerMob, this.id, "tick-rate", this.defaultTickRate, 1, 1200);
        final boolean useAttackRange = powerMob.getAbilityBoolean(this.id, "use-attack-range", this.defaultUseAttackRange);

        // Cancel existing task if it exists
        if (this.tasks.containsKey(entityUuid)) {
            this.tasks.get(entityUuid).cancel();
        }

        // Create a new task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!powerMob.isValid()) {
                remove(powerMob);
                return;
            }

            LivingEntity mob = powerMob.getEntity();
            Location mobLoc = mob.getLocation();
            
            // Calculate effective radius
            double effectiveRadius = radius;
            if (useAttackRange) {
                 Attribute attackRange = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.attack_range"));
                 if (attackRange != null && mob.getAttribute(attackRange) != null) {
                     effectiveRadius = mob.getAttribute(attackRange).getValue();
                 }
            }

            // Get entities in radius
            for (Entity entity : mobLoc.getWorld().getNearbyEntities(mobLoc, effectiveRadius, effectiveRadius, effectiveRadius)) {
                if (entity instanceof Player player && player.getUniqueId() != entityUuid) {
                    
                    // Check distance and line of sight/targeting rules
                    if (player.getLocation().distance(mobLoc) <= effectiveRadius && 
                        MobTargetingUtil.shouldAllowTargeting(this.plugin, mob, player)) {
                        
                        // Calculate pull vector
                        Vector pullDir = mobLoc.toVector().subtract(player.getLocation().toVector()).normalize();
                        
                        // Apply strength
                        pullDir.multiply(strength);
                        
                        // Add upward nudge to prevent players from getting stuck on floor
                        pullDir.setY(pullDir.getY() + 0.1);

                        // Apply velocity
                        player.setVelocity(pullDir);
                        
                        // Play effects
                        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }

            // Show particles around mob
            mobLoc.getWorld().spawnParticle(
                    Particle.SMOKE,
                    mobLoc.clone().add(0, 1, 0),
                    30,
                    effectiveRadius / 2,
                    1.0,
                    effectiveRadius / 2,
                    0.02
            );

        }, 0, tickRate);

        this.tasks.put(entityUuid, task);
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
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius to pull players from"));
        m.put("strength", AbilityConfigField.dbl("strength", defaultStrength, "Strength of the pull velocity"));
        m.put("tick-rate", AbilityConfigField.integer("tick-rate", defaultTickRate, "How often to pull (in ticks)"));
        m.put("use-attack-range", AbilityConfigField.bool("use-attack-range", defaultUseAttackRange, "If true, uses the mob's attack_range attribute instead of radius"));
        return m;
    }
}
