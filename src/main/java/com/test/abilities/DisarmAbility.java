package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Ability: Disarm
 * Attacks have a chance to make the player drop their main hand item.
 */
public class DisarmAbility extends AbstractAbility implements Listener {

    private final double defaultChance = 0.05;
    private final Random random = new Random();

    public DisarmAbility(PowerMobsPlugin plugin) {
        super(plugin, "disarm");
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

        PowerMob pm = PowerMob.getFromEntity(plugin, attacker);
        if (pm == null || pm.getAbilities().stream().noneMatch(a -> a.getId().equals(this.id))) return;

        double chance = ValidationUtils.getValidDouble(pm, this.id, "chance", defaultChance, 0.0, 1.0);
        if (random.nextDouble() <= chance) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            
            if (mainHand != null && mainHand.getType() != Material.AIR) {
                // Drop the item
                player.getInventory().setItemInMainHand(null);
                Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), mainHand);
                dropped.setPickupDelay(40); // 2 second delay before they can pick it up again
                
                player.sendMessage("§8[§7!§8] §cYou have been disarmed!");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            }
        }
    }

    @Override
    public String getTitle() { return "Disarm"; }

    @Override
    public String getDescription() { return "Attacks have a chance to make the player drop their weapon."; }

    @Override
    public Material getMaterial() { return Material.IRON_SWORD; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to disarm the player on hit"));
        return m;
    }
}
