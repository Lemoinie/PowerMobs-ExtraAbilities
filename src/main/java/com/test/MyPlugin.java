package com.test;

import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("PowerMobs-ExtraAbilities has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("PowerMobs-ExtraAbilities has been disabled!");
    }
}
