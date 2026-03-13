package com.test.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbstractAbility;
import com.test.utils.ValidationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Ability: Siege Breaker
 * Detects if a player is pillaring up and breaks the blocks beneath them.
 */
public class SiegeBreakerAbility extends AbstractAbility {

    private final Map<UUID, Integer> activeTasks = new HashMap<>();
    
    private final int defaultIntervalTicks = 20;
    private final double defaultMinHeightDiff = 2.0;
    private final double defaultMaxHorizontalRange = 5.0;
    private final int defaultMaxBlocksToBreak = 3;

    public SiegeBreakerAbility(PowerMobsPlugin plugin) {
        super(plugin, "siege-breaker");
    }

    @Override
    public void apply(PowerMob powerMob) {
        if (activeTasks.containsKey(powerMob.getEntityUuid())) return;

        int interval = ValidationUtils.getValidInt(powerMob, this.id, "check-interval-ticks", defaultIntervalTicks, 5, 200);

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LivingEntity mob = powerMob.getEntity();
            if (mob == null || !mob.isValid()) {
                remove(powerMob);
                return;
            }

            double minHeight = ValidationUtils.getValidDouble(powerMob, this.id, "min-height-diff", defaultMinHeightDiff, 1.0, 10.0);
            double maxRange = ValidationUtils.getValidDouble(powerMob, this.id, "horizontal-range", defaultMaxHorizontalRange, 1.0, 20.0);
            int maxBreak = ValidationUtils.getValidInt(powerMob, this.id, "max-blocks-per-check", defaultMaxBlocksToBreak, 1, 10);

            for (Player player : mob.getWorld().getPlayers()) {
                Location mobLoc = mob.getLocation();
                Location playerLoc = player.getLocation();

                // Check vertical height difference
                double yDiff = playerLoc.getY() - mobLoc.getY();
                if (yDiff < minHeight) continue;

                // Check horizontal distance
                double dx = playerLoc.getX() - mobLoc.getX();
                double dz = playerLoc.getZ() - mobLoc.getZ();
                double horizontalDistSq = dx * dx + dz * dz;

                if (horizontalDistSq > maxRange * maxRange) continue;

                // Detect "Pillar" - check blocks directly under the player
                breakPillar(playerLoc, (int) Math.floor(yDiff), maxBreak, mob);
            }
        }, interval, interval).getTaskId();

        activeTasks.put(powerMob.getEntityUuid(), taskId);
    }

    private void breakPillar(Location playerLoc, int heightDiff, int maxBreak, LivingEntity mob) {
        int brokenCount = 0;
        Location checkLoc = playerLoc.clone().subtract(0, 1, 0);

        for (int i = 0; i < heightDiff && brokenCount < maxBreak; i++) {
            Block block = checkLoc.getBlock();
            
            if (block.getType() != Material.AIR && block.getType().isSolid() && !isUnbreakable(block.getType())) {
                // To avoid breaking terrain randomly, check if it's likely a pillar.
                // A simple check: is it thin? (air around it)
                if (islikelyPillar(block)) {
                    block.breakNaturally();
                    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
                    brokenCount++;
                }
            }
            checkLoc.subtract(0, 1, 0);
        }
    }

    private boolean islikelyPillar(Block block) {
        // A block is considered part of a "pillar" if it has air in at least 3 horizontal directions 
        // OR if the user just wants us to break whatever is under the player when they are high up.
        // Let's go with a moderate check: at least 2 adjacent blocks are air/non-solid.
        int solidNeighbors = 0;
        if (block.getRelative(1, 0, 0).getType().isSolid()) solidNeighbors++;
        if (block.getRelative(-1, 0, 0).getType().isSolid()) solidNeighbors++;
        if (block.getRelative(0, 0, 1).getType().isSolid()) solidNeighbors++;
        if (block.getRelative(0, 0, -1).getType().isSolid()) solidNeighbors++;
        
        return solidNeighbors <= 2; // If it's a wall (3 or 4 solid neighbors), don't break it.
    }

    private boolean isUnbreakable(Material material) {
        return material == Material.BEDROCK || material == Material.BARRIER || 
               material == Material.END_PORTAL_FRAME || material == Material.COMMAND_BLOCK;
    }

    @Override
    public void remove(PowerMob powerMob) {
        Integer taskId = activeTasks.remove(powerMob.getEntityUuid());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    @Override
    public String getTitle() { return "Siege Breaker"; }

    @Override
    public String getDescription() { return "Breaks blocks under players who try to pillar up."; }

    @Override
    public Material getMaterial() { return Material.IRON_PICKAXE; }

    @Override
    public List<String> getStatus() { return List.of(); }

    @Override
    public Map<String, AbilityConfigField> getConfigSchema() {
        Map<String, AbilityConfigField> m = new LinkedHashMap<>();
        m.put("check-interval-ticks", AbilityConfigField.integer("check-interval-ticks", defaultIntervalTicks, "How often to check for pillaring players"));
        m.put("min-height-diff", AbilityConfigField.dbl("min-height-diff", defaultMinHeightDiff, "Minimum vertical height to trigger"));
        m.put("horizontal-range", AbilityConfigField.dbl("horizontal-range", defaultMaxHorizontalRange, "Maximum horizontal distance for detection"));
        m.put("max-blocks-per-check", AbilityConfigField.integer("max-blocks-per-check", defaultMaxBlocksToBreak, "Max blocks to break at once"));
        return m;
    }
}
