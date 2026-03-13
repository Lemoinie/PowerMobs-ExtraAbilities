package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.*;

/**
 * Ability that reduces the durability of the target's armor upon hit.
 */
public class ArmorShatterAbility extends AbstractAbility implements Listener {

    private final String title = "Armor Shatter";
    private final String description = "Attacks have a chance to damage the target's armor.";
    private final Material material = Material.ANVIL;

    private final int defaultDurabilityDamage = 15;
    private final double defaultChance = 0.3;

    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();
    private final Random random = new Random();

    public ArmorShatterAbility(PowerMobsPlugin plugin) {
        super(plugin, "armor-shatter");
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
    public void onHit(EntityDamageByEntityEvent event) {
        UUID attackerUuid = event.getDamager().getUniqueId();
        if (!monitoredMobs.containsKey(attackerUuid)) return;

        PowerMob powerMob = monitoredMobs.get(attackerUuid);
        if (!(event.getEntity() instanceof Player player)) return;

        double chance = ValidationUtils.getValidDouble(powerMob, this.id, "chance", this.defaultChance, 0.0, 1.0);
        if (random.nextDouble() > chance) return;

        int durabilityDamage = ValidationUtils.getValidInt(powerMob, this.id, "durability-damage", this.defaultDurabilityDamage, 1, 1000);

        ItemStack[] armor = player.getInventory().getArmorContents();
        List<ItemStack> pieces = new ArrayList<>();
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType() != Material.AIR) {
                pieces.add(piece);
            }
        }

        if (pieces.isEmpty()) return;

        // Pick a random piece to damage
        ItemStack targetPiece = pieces.get(random.nextInt(pieces.size()));
        if (targetPiece.getItemMeta() instanceof Damageable meta) {
            meta.setDamage(meta.getDamage() + durabilityDamage);
            targetPiece.setItemMeta(meta);

            // Check if it broke
            if (meta.getDamage() >= targetPiece.getType().getMaxDurability()) {
                targetPiece.setAmount(0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.5f);
            }
            
            player.sendMessage("§cYour armor was shattered by the impact!");
        }
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
        m.put("durability-damage", AbilityConfigField.integer("durability-damage", defaultDurabilityDamage, "Amount of durability to remove per trigger"));
        m.put("chance", AbilityConfigField.chance("chance", defaultChance, "Chance to trigger on hit (0.0 - 1.0)"));
        return m;
    }
}
