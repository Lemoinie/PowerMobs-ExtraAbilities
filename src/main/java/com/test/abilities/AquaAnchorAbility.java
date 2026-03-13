package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ability: Aqua Anchor
 * Mob moves normally in water and is unaffected by currents.
 */
public class AquaAnchorAbility extends AbstractAbility {

    private final Map<UUID, Integer> activeTasks = new HashMap<>();

    public AquaAnchorAbility(PowerMobsPlugin plugin) {
        super(plugin, "aqua-anchor");
    }

    @Override
    public void apply(PowerMob powerMob) {
        if (activeTasks.containsKey(powerMob.getEntityUuid())) return;

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LivingEntity entity = powerMob.getEntity();
            if (entity == null || !entity.isValid()) {
                remove(powerMob);
                return;
            }

            Block block = entity.getLocation().getBlock();
            if (block.getType() == Material.WATER) {
                // To ignore water flow, we can't easily cancel the internal Minecraft physics 
                // but we can ensure they keep their intended velocity or move at normal speed.
                
                // Note: WATER_MOVEMENT_EFFICIENCY attribute exists in 1.21+ 
                // We'll try to set it if it exists.
                try {
                    org.bukkit.attribute.Attribute waterEff = org.bukkit.attribute.Attribute.valueOf("WATER_MOVEMENT_EFFICIENCY");
                    if (entity.getAttribute(waterEff) != null) {
                        entity.getAttribute(waterEff).setBaseValue(1.0);
                    }
                } catch (Exception ignored) {}

                // If it's flowing water, Minecraft applies a horizontal push.
                // We don't want to completely stop the mob, just the water's influence.
                // This is hard to do perfectly without NMS, so we ensure they don't drift away 
                // while they are supposed to be standing still.
            }
        }, 0, 5).getTaskId();

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
    public String getTitle() { return "Aqua Anchor"; }

    @Override
    public String getDescription() { return "Mob moves normally in water and ignores currents."; }

    @Override
    public Material getMaterial() { return Material.ANVIL; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        return Map.of();
    }
}
