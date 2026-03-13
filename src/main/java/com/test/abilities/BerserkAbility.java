package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

/**
 * Ability that triggers when the mob is low on health, increasing damage and speed.
 */
public class BerserkAbility extends AbstractAbility implements Listener {

    private final String title = "Berserk";
    private final String description = "Increases damage and speed when health is low.";
    private final Material material = Material.REDSTONE;

    private final double defaultHpThreshold = 0.3; // 30% health
    private final double defaultDamageIncrease = 50.0; // 50% increase
    private final double defaultSpeedIncrease = 30.0; // 30% increase
    private final int defaultDuration = 10; // 10 seconds
    private final int defaultCooldown = 60; // 60 seconds

    private final Map<UUID, Long> activeUntil = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public BerserkAbility(PowerMobsPlugin plugin) {
        super(plugin, "berserk");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();

    @Override
    public void apply(PowerMob powerMob) {
        monitoredMobs.put(powerMob.getEntityUuid(), powerMob);
    }

    @Override
    public void remove(PowerMob powerMob) {
        UUID uuid = powerMob.getEntityUuid();
        monitoredMobs.remove(uuid);
        removeModifiers(powerMob.getEntity());
        activeUntil.remove(uuid);
        cooldownUntil.remove(uuid);
    }

    @EventHandler
    public void onMobDamage(EntityDamageEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (!monitoredMobs.containsKey(uuid)) return;

        PowerMob powerMob = monitoredMobs.get(uuid);
        if (!powerMob.isValid()) {
            remove(powerMob);
            return;
        }

        LivingEntity mob = powerMob.getEntity();
        double currentHealth = mob.getHealth() - event.getFinalDamage();
        double maxHealth = mob.getMaxHealth();
        
        double threshold = ValidationUtils.getValidDouble(powerMob, this.id, "hp-threshold", this.defaultHpThreshold, 0.01, 1000.0);
        
        // Check threshold (either percentage or absolute)
        boolean shouldTrigger = (threshold <= 1.0) ? (currentHealth / maxHealth <= threshold) : (currentHealth <= threshold);

        if (shouldTrigger && canTrigger(uuid)) {
            triggerBerserk(powerMob);
        }
    }

    private boolean canTrigger(UUID uuid) {
        long now = System.currentTimeMillis();
        return !activeUntil.containsKey(uuid) && now >= cooldownUntil.getOrDefault(uuid, 0L);
    }

    private void triggerBerserk(PowerMob powerMob) {
        LivingEntity mob = powerMob.getEntity();
        UUID uuid = mob.getUniqueId();
        
        double damageBoost = ValidationUtils.getValidDouble(powerMob, this.id, "damage-increase-percent", this.defaultDamageIncrease, 0.0, 1000.0) / 100.0;
        double speedBoost = ValidationUtils.getValidDouble(powerMob, this.id, "speed-increase-percent", this.defaultSpeedIncrease, 0.0, 500.0) / 100.0;
        int duration = ValidationUtils.getValidInt(powerMob, this.id, "duration", this.defaultDuration, 1, 600);
        int cooldown = ValidationUtils.getValidInt(powerMob, this.id, "cooldown", this.defaultCooldown, 1, 3600);

        applyModifier(mob, Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.attack_damage")), "berserk_damage", damageBoost);
        applyModifier(mob, Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.movement_speed")), "berserk_speed", speedBoost);

        long now = System.currentTimeMillis();
        activeUntil.put(uuid, now + (duration * 1000L));
        
        mob.sendMessage("§c§lBERSERK! §7Mob has entered a rage!");
        // Visual effects could be added here
        
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (mob.isValid()) {
                removeModifiers(mob);
                activeUntil.remove(uuid);
                cooldownUntil.put(uuid, System.currentTimeMillis() + (cooldown * 1000L));
                mob.sendMessage("§7Berserk state has ended.");
            }
        }, duration * 20L);
    }

    private void applyModifier(LivingEntity entity, Attribute attribute, String key, double amount) {
        if (attribute == null) return;
        NamespacedKey nsKey = new NamespacedKey(this.plugin, key);
        AttributeModifier modifier = new AttributeModifier(nsKey, amount, AttributeModifier.Operation.ADD_SCALAR);
        if (entity.getAttribute(attribute) != null) {
            entity.getAttribute(attribute).addModifier(modifier);
        }
    }

    private void removeModifiers(LivingEntity entity) {
        if (entity == null) return;
        removeModifier(entity, Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.attack_damage")), "berserk_damage");
        removeModifier(entity, Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.movement_speed")), "berserk_speed");
    }

    private void removeModifier(LivingEntity entity, Attribute attribute, String key) {
        if (attribute == null || entity.getAttribute(attribute) == null) return;
        NamespacedKey nsKey = new NamespacedKey(this.plugin, key);
        entity.getAttribute(attribute).removeModifier(nsKey);
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
        m.put("hp-threshold", AbilityConfigField.dbl("hp-threshold", defaultHpThreshold, "Health threshold to trigger (ratio or absolute)"));
        m.put("damage-increase-percent", AbilityConfigField.dbl("damage-increase-percent", defaultDamageIncrease, "Percentage increase in damage"));
        m.put("speed-increase-percent", AbilityConfigField.dbl("speed-increase-percent", defaultSpeedIncrease, "Percentage increase in speed"));
        m.put("duration", AbilityConfigField.integer("duration", defaultDuration, "Duration in seconds"));
        m.put("cooldown", AbilityConfigField.integer("cooldown", defaultCooldown, "Cooldown in seconds"));
        return m;
    }
}
