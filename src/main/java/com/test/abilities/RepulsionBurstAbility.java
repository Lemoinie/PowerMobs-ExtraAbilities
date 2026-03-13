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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Ability that triggers a repulsion burst when the mob's health drops below a certain threshold.
 */
public class RepulsionBurstAbility extends AbstractAbility implements Listener {

    private final String title = "Repulsion Burst";
    private final String description = "Triggers a shockwave that pushes nearby players away when health is low.";
    private final Material material = Material.BEACON;

    private final double defaultHpThreshold = 0.3; // 30%
    private final double defaultRadius = 5.0;
    private final double defaultKnockbackStrength = 2.0;
    private final double defaultDamage = 3.0;
    private final int defaultSlownessAmp = 9; // Level 10
    private final int defaultRegenAmp = 4; // Level 5
    private final int defaultEffectDuration = 40; // 2s
    private final int defaultCooldown = 120;

    private final Map<UUID, Long> lastUsed = new HashMap<>();

    public RepulsionBurstAbility(PowerMobsPlugin plugin) {
        super(plugin, "repulsion-burst");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {
        // Event-based
    }

    @Override
    public void remove(PowerMob powerMob) {
        lastUsed.remove(powerMob.getEntityUuid());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        PowerMob powerMob = PowerMob.getFromEntity(plugin, victim);
        if (powerMob == null) return;
        
        boolean hasAbility = powerMob.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id));
        if (!hasAbility) return;

        // Check if HP threshold is reached AFTER this damage
        double maxHp = victim.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHp = victim.getHealth() - event.getFinalDamage();
        double threshold = ValidationUtils.getValidDouble(powerMob, this.id, "hp-threshold", defaultHpThreshold, 0.01, 1.0);

        if (currentHp / maxHp <= threshold) {
            triggerBurst(powerMob, victim);
        }
    }

    private void triggerBurst(PowerMob powerMob, LivingEntity mob) {
        UUID uuid = mob.getUniqueId();
        long now = System.currentTimeMillis();
        int cooldown = ValidationUtils.getValidInt(powerMob, this.id, "cooldown", defaultCooldown, 0, 3600);

        if (lastUsed.containsKey(uuid)) {
            if (now - lastUsed.get(uuid) < cooldown * 1000L) return;
        }

        double radius = ValidationUtils.getValidDouble(powerMob, this.id, "radius", defaultRadius, 1.0, 50.0);
        double knockback = ValidationUtils.getValidDouble(powerMob, this.id, "knockback-strength", defaultKnockbackStrength, 0.1, 10.0);
        double damage = ValidationUtils.getValidDouble(powerMob, this.id, "damage", defaultDamage, 0.0, 100.0);

        List<Player> targets = new ArrayList<>();
        for (Player p : mob.getWorld().getPlayers()) {
            if (p.getLocation().distance(mob.getLocation()) <= radius) {
                targets.add(p);
            }
        }

        if (targets.isEmpty()) return;

        // Trigger effects
        lastUsed.put(uuid, now);
        mob.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, mob.getLocation().add(0, 1, 0), 1);
        mob.getWorld().spawnParticle(Particle.SONIC_BOOM, mob.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.0f);

        for (Player p : targets) {
            p.damage(damage, mob);
            Vector push = p.getLocation().toVector().subtract(mob.getLocation().toVector()).normalize().multiply(knockback).setY(0.5);
            p.setVelocity(push);
            p.sendMessage("§cYou were repelled by the shockwave!");
        }

        // Buff mob
        int slownessAmp = ValidationUtils.getValidInt(powerMob, this.id, "mob-slowness-amp", defaultSlownessAmp, 0, 255);
        int regenAmp = ValidationUtils.getValidInt(powerMob, this.id, "mob-regen-amp", defaultRegenAmp, 0, 255);
        int duration = ValidationUtils.getValidInt(powerMob, this.id, "effect-duration-ticks", defaultEffectDuration, 1, 1200);

        mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, slownessAmp));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, regenAmp));
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
        m.put("hp-threshold", AbilityConfigField.dbl("hp-threshold", defaultHpThreshold, "HP percentage to trigger burst (0.01 - 1.0)"));
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Radius of the shockwave"));
        m.put("knockback-strength", AbilityConfigField.dbl("knockback-strength", defaultKnockbackStrength, "Strength of the repulsion push"));
        m.put("damage", AbilityConfigField.dbl("damage", defaultDamage, "Damage dealt to nearby players"));
        m.put("mob-slowness-amp", AbilityConfigField.integer("mob-slowness-amp", defaultSlownessAmp, "Slowness level for the mob (9 = Level 10)"));
        m.put("mob-regen-amp", AbilityConfigField.integer("mob-regen-amp", defaultRegenAmp, "Regen level for the mob (4 = Level 5)"));
        m.put("effect-duration-ticks", AbilityConfigField.integer("effect-duration-ticks", defaultEffectDuration, "Duration of buffs/debuffs on mob"));
        m.put("cooldown", AbilityConfigField.integer("cooldown", defaultCooldown, "Cooldown in seconds"));
        return m;
    }
}
