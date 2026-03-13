package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Ability that extracts the shadow of a fallen mob, turning it into a summon.
 */
public class ShadowExtractionAbility extends AbstractAbility implements Listener {

    private final String title = "Shadow Extraction";
    private final String description = "Chances to resurrect mobs that die nearby as loyal shadows.";
    private final Material material = Material.WITHER_SKELETON_SKULL;

    private final double defaultChance = 0.3;
    private final double defaultSummonHp = 5.0;
    private final double defaultSummonDamage = 3.0;
    private final int defaultRadius = 8;

    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();
    private final Random random = new Random();
    private final NamespacedKey summonKey;

    public ShadowExtractionAbility(PowerMobsPlugin plugin) {
        super(plugin, "shadow-extraction");
        this.summonKey = new NamespacedKey(plugin, "is_shadow_summon");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {
        monitoredMobs.put(powerMob.getEntityUuid(), powerMob);
    }

    @Override
    public void remove(PowerMob powerMob) {
        monitoredMobs.remove(powerMob.getEntityUuid());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        
        // Don't extract shadows from players or already summoned shadows/mobs
        if (victim instanceof Player || victim.getPersistentDataContainer().has(summonKey, PersistentDataType.BOOLEAN)) {
            return;
        }

        Location deathLoc = victim.getLocation();
        
        for (PowerMob master : monitoredMobs.values()) {
            if (!master.isValid()) continue;
            
            LivingEntity masterEntity = master.getEntity();
            if (!masterEntity.getWorld().equals(deathLoc.getWorld())) continue;

            int radius = ValidationUtils.getValidInt(master, this.id, "radius", defaultRadius, 1, 50);
            if (masterEntity.getLocation().distance(deathLoc) <= radius) {
                
                double chance = ValidationUtils.getValidDouble(master, this.id, "chance", defaultChance, 0.0, 1.0);
                if (random.nextDouble() <= chance) {
                    summonShadow(master, victim);
                    break; // Only one master extracts the shadow
                }
            }
        }
    }

    private void summonShadow(PowerMob master, LivingEntity victim) {
        EntityType type = victim.getType();
        Location loc = victim.getLocation();
        
        LivingEntity shadow = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        
        double hp = ValidationUtils.getValidDouble(master, this.id, "summon-hp", defaultSummonHp, 1.0, 1000.0);
        double damage = ValidationUtils.getValidDouble(master, this.id, "summon-damage", defaultSummonDamage, 1.0, 1000.0);

        // Configure Shadow
        shadow.getPersistentDataContainer().set(summonKey, PersistentDataType.BOOLEAN, true);
        shadow.setCustomName(ChatColor.DARK_GRAY + "Shadow " + ChatColor.GRAY + victim.getName());
        shadow.setCustomNameVisible(true);
        
        // Base Attributes
        if (shadow.getAttribute(Attribute.MAX_HEALTH) != null) {
            shadow.getAttribute(Attribute.MAX_HEALTH).setBaseValue(hp);
            shadow.setHealth(hp);
        }
        
        if (shadow.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            shadow.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(damage);
        }

        // Register as a temporary PowerMob so it doesn't drop loot or clash
        PowerMob shadowPm = new PowerMob(this.plugin, shadow, "shadow-summon");
        this.plugin.getPowerMobManager().registerPowerMob(shadowPm);

        // Visuals
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 20, 0.5, 1, 0.5, 0.05);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 30, 0.5, 1, 0.5, 0.02);
        loc.getWorld().playSound(loc, Sound.ENTITY_VEX_AMBIENT, 1.0f, 0.5f);
        
        // Target Logic
        if (shadow instanceof Mob mob) {
             // Target whatever master is targeting, if possible
             if (master.getEntity() instanceof Mob masterMob && masterMob.getTarget() != null) {
                 mob.setTarget(masterMob.getTarget());
             } else {
                 // Or just find nearest player
                 Player nearest = findNearestPlayer(loc);
                 if (nearest != null) mob.setTarget(nearest);
             }
        }
    }

    private Player findNearestPlayer(Location loc) {
        Player nearest = null;
        double bestDist = 30.0;
        for (Player p : loc.getWorld().getPlayers()) {
            double d = p.getLocation().distance(loc);
            if (d < bestDist) {
                bestDist = d;
                nearest = p;
            }
        }
        return nearest;
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
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to extract shadow (0.0 - 1.0)"));
        m.put("radius", AbilityConfigField.integer("radius", defaultRadius, "Radius to monitor for deaths"));
        m.put("summon-hp", AbilityConfigField.dbl("summon-hp", defaultSummonHp, "Health of the summoned shadow"));
        m.put("summon-damage", AbilityConfigField.dbl("summon-damage", defaultSummonDamage, "Damage of the summoned shadow"));
        return m;
    }
}
