package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Ability: Effect Purge
 * Periodically removes all negative potion effects from the mob.
 */
public class EffectPurgeAbility extends AbstractAbility {

    private final int defaultIntervalTicks = 100; // 5 seconds
    private final Map<UUID, Integer> activeTasks = new HashMap<>();

    private final Set<PotionEffectType> negativeEffects = new HashSet<>(Arrays.asList(
        PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.INSTANT_DAMAGE,
        PotionEffectType.NAUSEA, PotionEffectType.BLINDNESS, PotionEffectType.HUNGER,
        PotionEffectType.WEAKNESS, PotionEffectType.POISON, PotionEffectType.WITHER,
        PotionEffectType.GLOWING, PotionEffectType.LEVITATION, PotionEffectType.DARKNESS,
        PotionEffectType.UNLUCK, PotionEffectType.BAD_OMEN
    ));

    public EffectPurgeAbility(PowerMobsPlugin plugin) {
        super(plugin, "effect-purge");
    }

    @Override
    public void apply(PowerMob powerMob) {
        if (activeTasks.containsKey(powerMob.getEntityUuid())) return;

        int interval = ValidationUtils.getValidInt(powerMob, this.id, "interval-ticks", defaultIntervalTicks, 20, 1200);
        
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LivingEntity entity = powerMob.getEntity();
            if (entity == null || !entity.isValid()) {
                remove(powerMob);
                return;
            }

            boolean purged = false;
            for (PotionEffect effect : entity.getActivePotionEffects()) {
                if (negativeEffects.contains(effect.getType())) {
                    entity.removePotionEffect(effect.getType());
                    purged = true;
                }
            }

            if (purged) {
                entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 2.0f);
            }

        }, interval, interval).getTaskId();

        activeTasks.put(powerMob.getEntityUuid(), taskId);
    }

    @Override
    public void remove(PowerMob powerMob) {
        Integer taskId = activeTasks.remove(powerMob.getEntityUuid());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    @Override
    public String getTitle() { return "Effect Purge"; }

    @Override
    public String getDescription() { return "Periodically removes negative potion effects."; }

    @Override
    public Material getMaterial() { return Material.MILK_BUCKET; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("interval-ticks", AbilityConfigField.integer("interval-ticks", defaultIntervalTicks, "How often to purge effects (20 = 1s)"));
        return m;
    }
}
