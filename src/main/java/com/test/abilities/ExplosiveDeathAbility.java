package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;

/**
 * Ability that causes the mob to explode upon death.
 */
public class ExplosiveDeathAbility extends AbstractAbility implements Listener {

    private final String title = "Explosive Death";
    private final String description = "Causes a powerful explosion when the mob dies.";
    private final Material material = Material.TNT;

    private final double defaultRadius = 4.0;
    private final double defaultDamage = 8.0; // Explosion power
    private final boolean defaultBreakBlocks = false;

    private final Map<UUID, PowerMob> monitoredMobs = new HashMap<>();

    public ExplosiveDeathAbility(PowerMobsPlugin plugin) {
        super(plugin, "explosive-death");
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (!monitoredMobs.containsKey(uuid)) return;

        PowerMob powerMob = monitoredMobs.get(uuid);
        triggerExplosion(powerMob);
        remove(powerMob);
    }

    private void triggerExplosion(PowerMob powerMob) {
        LivingEntity mob = powerMob.getEntity();
        Location loc = mob.getLocation();

        // We'll use 'damage' as the explosion power/size in Minecraft terms
        // 'radius' is often used interchangeably in loose requests, but Minecraft createExplosion takes a float power.
        // We'll use the 'damage' value as the primary power, or fallback to radius.
        double power = ValidationUtils.getValidDouble(powerMob, this.id, "damage", this.defaultDamage, 1.0, 50.0);
        boolean breakBlocks = powerMob.getAbilityBoolean(this.id, "break-blocks", this.defaultBreakBlocks);

        // Minecraft createExplosion(Location, float power, boolean setFire, boolean breakBlocks)
        loc.getWorld().createExplosion(loc, (float) power, false, breakBlocks);
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
        m.put("radius", AbilityConfigField.dbl("radius", defaultRadius, "Visual radius of the explosion (visual/internal)"));
        m.put("damage", AbilityConfigField.dbl("damage", defaultDamage, "Power of the explosion (affects damage and reach)"));
        m.put("break-blocks", AbilityConfigField.bool("break-blocks", defaultBreakBlocks, "Whether the explosion should break blocks"));
        return m;
    }
}
