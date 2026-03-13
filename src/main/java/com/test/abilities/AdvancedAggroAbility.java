package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.powermobs.utils.MobTargetingUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Advanced Aggro System
 * Uses threat and priority gauges to determine mob targets.
 */
public class AdvancedAggroAbility extends AbstractAbility implements Listener {

    private final String title = "Advanced Aggro System";
    private final String description = "Uses a complex threat and priority model for targeting.";
    private final Material material = Material.RECOVERY_COMPASS;

    // Default Modifiers
    private final double defThreatRange = 10.0;
    private final double defThreatMelee = 5.0;
    private final double defThreatHeal = 6.0;
    private final double defThreatClosest = 2.0;
    private final double defThreatHitByMob = 4.0;
    
    private final double defPriorityDist = 40.0;
    private final double defPriorityRunAway = 30.0;
    private final double defPriorityLowHp = 50.0;
    private final double defPriorityPotion = 20.0;
    private final double defPriorityLowArmorEnchant = 50.0;

    private final double defDecayThreat = 3.0;
    private final double defDecayPriority = 5.0;

    // Per-mob instances configuration
    private static class MobConfig {
        double threatRange, threatMelee, threatHeal, threatClosest, threatHitByMob;
        double priorityDist, priorityRunAway, priorityLowHp, priorityPotion, priorityLowArmorEnchant;
        double decayThreat, decayPriority;
    }

    private final Map<UUID, Map<UUID, AggroData>> mobAggroData = new HashMap<>();
    private final Map<UUID, MobConfig> mobConfigs = new HashMap<>();
    private final Map<UUID, Long> playerLastPotionUse = new HashMap<>();
    private BukkitTask mainTask;

    public AdvancedAggroAbility(PowerMobsPlugin plugin) {
        super(plugin, "advanced-aggro");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startTask();
    }

    private void startTask() {
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, Map<UUID, AggroData>>> mobIter = mobAggroData.entrySet().iterator();
            while (mobIter.hasNext()) {
                Map.Entry<UUID, Map<UUID, AggroData>> entry = mobIter.next();
                UUID mobUuid = entry.getKey();
                Entity entity = Bukkit.getEntity(mobUuid);
                if (!(entity instanceof org.bukkit.entity.Mob mob)) {
                    mobIter.remove();
                    mobConfigs.remove(mobUuid);
                    continue;
                }
                
                PowerMob powerMob = PowerMob.getFromEntity(plugin, mob);

                if (powerMob == null || !powerMob.isValid()) {
                    mobIter.remove();
                    mobConfigs.remove(mobUuid);
                    continue;
                }

                Map<UUID, AggroData> playerScores = entry.getValue();
                MobConfig config = mobConfigs.get(mobUuid);
                if (config == null) continue;

                // Recalculate priority and handle decay
                updateMobAggro(mob, playerScores, config);

                // Target selection
                selectBestTarget(powerMob, mob, playerScores);
            }
        }, 0, 40); // Recalculate every 2 seconds (40 ticks)
    }

    private void updateMobAggro(org.bukkit.entity.Mob mob, Map<UUID, AggroData> playerScores, MobConfig config) {
        long now = System.currentTimeMillis();
        Location mobLoc = mob.getLocation();

        // Find closest player for threat bonus
        Player closestPlayer = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Player player : mob.getWorld().getPlayers()) {
            if (!MobTargetingUtil.shouldAllowTargeting(plugin, mob, player)) {
                playerScores.remove(player.getUniqueId());
                continue;
            }

            double distSq = player.getLocation().distanceSquared(mobLoc);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestPlayer = player;
            }

            AggroData data = playerScores.computeIfAbsent(player.getUniqueId(), k -> new AggroData());
            
            // Priority recalculation
            double priority = 0;
            double dist = Math.sqrt(distSq);

            if (dist > 12) priority += config.priorityDist;
            
            // Running away check
            if (data.lastLoc != null && dist > data.lastDist) {
                priority += config.priorityRunAway;
            }
            data.lastLoc = player.getLocation().clone();
            data.lastDist = dist;

            // HP < 25% check
            if (player.getHealth() / player.getAttribute(Attribute.MAX_HEALTH).getValue() < 0.25) {
                priority += config.priorityLowHp;
            }

            // Potion usage recently (5s)
            if (now - playerLastPotionUse.getOrDefault(player.getUniqueId(), 0L) < 5000) {
                priority += config.priorityPotion;
            }

            // Low armor/enchantment check
            if (isLowArmorOrEnchantment(player)) {
                priority += config.priorityLowArmorEnchant;
            }

            data.priority = Math.min(100, priority);

            // Decay logic (if no gain for 5s)
            if (now - data.lastGainTime > 5000) {
                data.threat = Math.max(0, data.threat - (config.decayThreat * 2.0)); // 3/sec, task is 2sec
                data.priority = Math.max(0, data.priority - (config.decayPriority * 2.0)); // 5/sec, task is 2sec
            }
        }

        // Closest player threat bonus
        if (closestPlayer != null) {
            AggroData data = playerScores.get(closestPlayer.getUniqueId());
            if (data != null) {
                addThreat(data, config.threatClosest * 2.0, closestPlayer.getUniqueId()); // 2/sec, task is 2sec
            }
        }
    }

    private void selectBestTarget(PowerMob pm, org.bukkit.entity.Mob mob, Map<UUID, AggroData> playerScores) {
        Player bestTarget = null;
        double bestScore = -1;

        for (Map.Entry<UUID, AggroData> entry : playerScores.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            AggroData data = entry.getValue();
            double score = data.threat + data.priority;

            if (score > bestScore) {
                bestScore = score;
                bestTarget = player;
            } else if (score == bestScore && score > 0 && bestTarget != null) {
                // Tie-breaker: lowest HP ratio
                double currentRatio = player.getHealth() / player.getAttribute(Attribute.MAX_HEALTH).getValue();
                double bestRatio = bestTarget.getHealth() / bestTarget.getAttribute(Attribute.MAX_HEALTH).getValue();
                if (currentRatio < bestRatio) {
                    bestTarget = player;
                }
            }
        }

        if (bestTarget != null && (mob.getTarget() == null || !mob.getTarget().getUniqueId().equals(bestTarget.getUniqueId()))) {
            mob.setTarget(bestTarget);
        }
    }

    private boolean isLowArmorOrEnchantment(Player player) {
        double armorValue = player.getAttribute(Attribute.ARMOR).getValue();
        if (armorValue < 10) return true; // Low base armor

        int protLevels = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                protLevels += armor.getEnchantmentLevel(Enchantment.PROTECTION);
            }
        }
        return protLevels < 4; // Low protection enchantments
    }

    @Override
    public void apply(PowerMob powerMob) {
        UUID uuid = powerMob.getEntityUuid();
        if (!powerMob.getAbilityBoolean(this.id, "active", false)) {
            mobAggroData.remove(uuid);
            mobConfigs.remove(uuid);
        } else {
            mobAggroData.putIfAbsent(uuid, new HashMap<>());
            
            MobConfig config = new MobConfig();
            config.threatRange = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "threat-range", defThreatRange, 0, 100);
            config.threatMelee = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "threat-melee", defThreatMelee, 0, 100);
            config.threatHeal = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "threat-heal", defThreatHeal, 0, 100);
            config.threatClosest = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "threat-closest-per-sec", defThreatClosest, 0, 100);
            config.threatHitByMob = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "threat-hit-by-mob", defThreatHitByMob, 0, 100);
            
            config.priorityDist = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "priority-distance-large", defPriorityDist, 0, 100);
            config.priorityRunAway = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "priority-run-away", defPriorityRunAway, 0, 100);
            config.priorityLowHp = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "priority-low-hp", defPriorityLowHp, 0, 100);
            config.priorityPotion = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "priority-potion-used", defPriorityPotion, 0, 100);
            config.priorityLowArmorEnchant = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "priority-weak-armor", defPriorityLowArmorEnchant, 0, 100);
            
            config.decayThreat = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "decay-threat-per-sec", defDecayThreat, 0, 100);
            config.decayPriority = com.test.utils.ValidationUtils.getValidDouble(powerMob, this.id, "decay-priority-per-sec", defDecayPriority, 0, 100);
            
            mobConfigs.put(uuid, config);
        }
    }

    @Override
    public void remove(PowerMob powerMob) {
        mobAggroData.remove(powerMob.getEntityUuid());
        mobConfigs.remove(powerMob.getEntityUuid());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // Player deals damage to mob
        if (victim instanceof LivingEntity target && mobAggroData.containsKey(target.getUniqueId())) {
            Player player = null;
            boolean isRange = false;

            if (damager instanceof Player p) {
                player = p;
            } else if (damager instanceof Arrow arrow && arrow.getShooter() instanceof Player p) {
                player = p;
                isRange = true;
            }

            if (player != null) {
                MobConfig config = mobConfigs.get(target.getUniqueId());
                if (config != null) {
                    Map<UUID, AggroData> scores = mobAggroData.get(target.getUniqueId());
                    AggroData data = scores.computeIfAbsent(player.getUniqueId(), k -> new AggroData());
                    addThreat(data, isRange ? config.threatRange : config.threatMelee, player.getUniqueId());
                }
            }
        }

        // Mob deals damage to player
        if (damager instanceof LivingEntity mob && mobAggroData.containsKey(mob.getUniqueId()) && victim instanceof Player player) {
            MobConfig config = mobConfigs.get(mob.getUniqueId());
            if (config != null) {
                Map<UUID, AggroData> scores = mobAggroData.get(mob.getUniqueId());
                AggroData data = scores.computeIfAbsent(player.getUniqueId(), k -> new AggroData());
                addThreat(data, config.threatHitByMob, player.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        for (Map.Entry<UUID, Map<UUID, AggroData>> entry : mobAggroData.entrySet()) {
            Entity mob = Bukkit.getEntity(entry.getKey());
            if (mob instanceof LivingEntity livingMob && livingMob.getWorld().equals(player.getWorld())) {
                if (livingMob.getLocation().distanceSquared(player.getLocation()) < 100) { // 10 blocks range
                    MobConfig config = mobConfigs.get(entry.getKey());
                    if (config != null) {
                        AggroData data = entry.getValue().computeIfAbsent(player.getUniqueId(), k -> new AggroData());
                        addThreat(data, config.threatHeal, player.getUniqueId());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPotionConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.POTION || event.getItem().getType() == Material.SPLASH_POTION || event.getItem().getType() == Material.LINGERING_POTION) {
            playerLastPotionUse.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        mobAggroData.remove(event.getEntity().getUniqueId());
        mobConfigs.remove(event.getEntity().getUniqueId());
    }

    private void addThreat(AggroData data, double amount, UUID playerUuid) {
        data.threat = Math.min(100, data.threat + amount);
        data.lastGainTime = System.currentTimeMillis();
    }

    @Override
    public String getTitle() { return ChatColor.GOLD + title; }

    @Override
    public String getDescription() { return description; }

    @Override
    public Material getMaterial() { return material; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("active", AbilityConfigField.bool("active", false, "Enable advanced aggro system"));
        
        m.put("threat-range", AbilityConfigField.dbl("threat-range", defThreatRange, "Threat gain per ranged hit"));
        m.put("threat-melee", AbilityConfigField.dbl("threat-melee", defThreatMelee, "Threat gain per melee hit"));
        m.put("threat-heal", AbilityConfigField.dbl("threat-heal", defThreatHeal, "Threat gain for healing near mob"));
        m.put("threat-closest-per-sec", AbilityConfigField.dbl("threat-closest-per-sec", defThreatClosest, "Threat gain per second for being closest"));
        m.put("threat-hit-by-mob", AbilityConfigField.dbl("threat-hit-by-mob", defThreatHitByMob, "Threat gain for getting hit by the mob"));
        
        m.put("priority-distance-large", AbilityConfigField.dbl("priority-distance-large", defPriorityDist, "Priority bonus if distance > 12"));
        m.put("priority-run-away", AbilityConfigField.dbl("priority-run-away", defPriorityRunAway, "Priority bonus for running away"));
        m.put("priority-low-hp", AbilityConfigField.dbl("priority-low-hp", defPriorityLowHp, "Priority bonus if HP < 25%"));
        m.put("priority-potion-used", AbilityConfigField.dbl("priority-potion-used", defPriorityPotion, "Priority bonus if used potion recently"));
        m.put("priority-weak-armor", AbilityConfigField.dbl("priority-weak-armor", defPriorityLowArmorEnchant, "Priority bonus for low armor/enchants"));
        
        m.put("decay-threat-per-sec", AbilityConfigField.dbl("decay-threat-per-sec", defDecayThreat, "Threat decay per second after 5s idle"));
        m.put("decay-priority-per-sec", AbilityConfigField.dbl("decay-priority-per-sec", defDecayPriority, "Priority decay per second after 5s idle"));

        return m;
    }

    private static class AggroData {
        double threat = 0;
        double priority = 0;
        long lastGainTime = System.currentTimeMillis();
        Location lastLoc;
        double lastDist = 0;
    }
}
