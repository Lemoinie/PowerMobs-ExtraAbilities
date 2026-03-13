package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Ability: Shield Breaking
 * Chance to disable a player's shield, similar to how an axe works.
 */
public class ShieldBreakingAbility extends AbstractAbility implements Listener {

    private final double defaultChance = 0.2;
    private final int defaultDisableTimeTicks = 100; // 5 seconds
    private final Random random = new Random();

    public ShieldBreakingAbility(PowerMobsPlugin plugin) {
        super(plugin, "shield-breaking");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if player is actually using a shield
        if (!player.isBlocking()) return;

        PowerMob pm = PowerMob.getFromEntity(plugin, attacker);
        if (pm == null || pm.getAbilities().stream().noneMatch(a -> a.getId().equals(this.id))) return;

        double chance = ValidationUtils.getValidDouble(pm, this.id, "chance", defaultChance, 0.0, 1.0);
        if (random.nextDouble() <= chance) {
            int disableTime = ValidationUtils.getValidInt(pm, this.id, "disable-time-ticks", defaultDisableTimeTicks, 20, 400);
            
            // Disable shield
            player.setCooldown(Material.SHIELD, disableTime);
            
            // Note: setCooldown alone doesn't stop the current block immediately, 
            // but Minecraft will stop the block on the next tick if a cooldown is present.
            // We can play the sound and particles to make it obvious.
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
            player.sendMessage("§8[§7!§8] §cYour shield has been disabled!");
        }
    }

    @Override
    public String getTitle() { return "Shield Breaking"; }

    @Override
    public String getDescription() { return "Chance to disable the player's shield on hit."; }

    @Override
    public Material getMaterial() { return Material.IRON_AXE; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to disable the shield"));
        m.put("disable-time-ticks", AbilityConfigField.integer("disable-time-ticks", defaultDisableTimeTicks, "How long the shield remains disabled (20 = 1s)"));
        return m;
    }
}
