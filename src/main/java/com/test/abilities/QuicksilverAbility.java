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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Ability that gives the mob a temporary speed boost when damaged.
 */
public class QuicksilverAbility extends AbstractAbility implements Listener {

    private final String title = "Quicksilver";
    private final String description = "Temporary speed boost when damaged.";
    private final Material material = Material.SUGAR;

    private final int defaultLevel = 2;
    private final int defaultDuration = 5;
    private final double defaultChance = 0.1;
    private final int defaultCooldown = 60;

    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();

    public QuicksilverAbility(PowerMobsPlugin plugin) {
        super(plugin, "quicksilver");
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
        cooldowns.remove(uuid);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (!monitoredMobs.containsKey(uuid)) return;

        PowerMob powerMob = monitoredMobs.get(uuid);
        if (!powerMob.isValid()) {
            remove(powerMob);
            return;
        }

        // Cooldown check
        long now = System.currentTimeMillis();
        if (now < cooldowns.getOrDefault(uuid, 0L)) return;

        double chance = ValidationUtils.getValidDouble(powerMob, this.id, "chance", this.defaultChance, 0.0, 1.0);
        if (random.nextDouble() < chance) {
            triggerQuicksilver(powerMob);
            
            int cooldownSec = ValidationUtils.getValidInt(powerMob, this.id, "cooldown", this.defaultCooldown, 0, 3600);
            cooldowns.put(uuid, now + (cooldownSec * 1000L));
        }
    }

    private void triggerQuicksilver(PowerMob powerMob) {
        LivingEntity mob = powerMob.getEntity();
        
        int level = ValidationUtils.getValidInt(powerMob, this.id, "level", this.defaultLevel, 1, 10);
        int duration = ValidationUtils.getValidInt(powerMob, this.id, "duration", this.defaultDuration, 1, 600);

        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, Math.max(0, level - 1)));
        
        // Effects
        mob.getWorld().spawnParticle(Particle.CLOUD, mob.getLocation(), 10, 0.2, 0.1, 0.2, 0.05);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);
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
        m.put("level", AbilityConfigField.integer("level", defaultLevel, "Level of Speed effect"));
        m.put("duration", AbilityConfigField.integer("duration", defaultDuration, "Duration in seconds"));
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to trigger on damage"));
        m.put("cooldown", AbilityConfigField.integer("cooldown", defaultCooldown, "Cooldown in seconds"));
        return m;
    }
}
