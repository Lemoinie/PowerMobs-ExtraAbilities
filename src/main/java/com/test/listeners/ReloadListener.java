package com.test.listeners;

import com.powermobs.PowerMobsPlugin;
import com.test.ExtraAbilities;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

/**
 * Listener to catch /powermob reload and sync the extra abilities config.
 */
public class ReloadListener implements Listener {

    private final ExtraAbilities plugin;
    private final PowerMobsPlugin powerMobs;

    public ReloadListener(ExtraAbilities plugin, PowerMobsPlugin powerMobs) {
        this.plugin = plugin;
        this.powerMobs = powerMobs;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isReloadCommand(event.getMessage())) {
            plugin.getConfigManager().reload(powerMobs);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (isReloadCommand(event.getCommand())) {
            plugin.getConfigManager().reload(powerMobs);
        }
    }

    private boolean isReloadCommand(String command) {
        String cmd = command.toLowerCase().trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        
        // PowerMobs uses /powermob reload. /pm is a common alias for plugins but let's be thorough.
        // We check if it starts with powermob reload or pm reload (if we want to support aliases)
        return cmd.equals("powermob reload") || cmd.startsWith("powermob reload ") ||
               cmd.equals("pm reload") || cmd.startsWith("pm reload ");
    }
}
