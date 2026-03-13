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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Ability that applies stacking slowness to a target after multiple hits.
 */
public class CripplingStrikeAbility extends AbstractAbility implements Listener {

    private final String title = "Crippling Strike";
    private final String description = "Attacks apply stacking slowness. Multiple hits are required to increase the effect.";
    private final Material material = Material.TURTLE_HELMET;

    private final int defaultHitsPerStack = 3;
    private final int defaultMaxStacks = 5;
    private final int defaultDurationTicks = 100; // 5 seconds

    private final Map<UUID, Map<UUID, CrippleData>> cripplingData = new HashMap<>();

    public CripplingStrikeAbility(PowerMobsPlugin plugin) {
        super(plugin, "crippling-strike");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void apply(PowerMob powerMob) {}

    @Override
    public void remove(PowerMob powerMob) {
        cripplingData.remove(powerMob.getEntityUuid());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        PowerMob powerMob = PowerMob.getFromEntity(plugin, attacker);
        if (powerMob == null) return;
        
        boolean hasAbility = powerMob.getAbilities().stream().anyMatch(a -> a.getId().equals(this.id));
        if (!hasAbility) return;

        int hitsPerStack = ValidationUtils.getValidInt(powerMob, this.id, "hits-per-stack", defaultHitsPerStack, 1, 20);
        int maxStacks = ValidationUtils.getValidInt(powerMob, this.id, "max-stacks", defaultMaxStacks, 1, 10);
        int duration = ValidationUtils.getValidInt(powerMob, this.id, "duration-ticks", defaultDurationTicks, 20, 600);

        Map<UUID, CrippleData> targetMap = cripplingData.computeIfAbsent(attacker.getUniqueId(), k -> new HashMap<>());
        CrippleData data = targetMap.computeIfAbsent(player.getUniqueId(), k -> new CrippleData());

        data.hitCount++;
        int stack = data.hitCount / hitsPerStack;

        if (stack > 0) {
            int finalStack = Math.min(stack, maxStacks);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, finalStack - 1, false, true));
            
            if (finalStack > data.lastAppliedStack) {
                player.sendMessage("§8[§7!§8] §cCrippled: §7Your legs feel heavy (Stack " + finalStack + ")");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1.5f);
                data.lastAppliedStack = finalStack;
            }
        }
    }

    private static class CrippleData {
        int hitCount = 0;
        int lastAppliedStack = 0;
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
        m.put("hits-per-stack", AbilityConfigField.integer("hits-per-stack", defaultHitsPerStack, "Number of attacks required to increase slowness stack"));
        m.put("max-stacks", AbilityConfigField.integer("max-stacks", defaultMaxStacks, "Maximum slowness level (e.g., 5 = Slowness V)"));
        m.put("duration-ticks", AbilityConfigField.integer("duration-ticks", defaultDurationTicks, "Duration of the slowness effect in ticks"));
        return m;
    }
}
