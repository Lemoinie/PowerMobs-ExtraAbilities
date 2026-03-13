package com.test.config;

import com.powermobs.PowerMobsPlugin;
import com.test.ExtraAbilities;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

public class ConfigManager {

    private final ExtraAbilities plugin;
    private final File configFile;
    private FileConfiguration config;

    public ConfigManager(ExtraAbilities plugin) {
        this.plugin = plugin;
        // User requested the file to be right in the PowerMobs folder
        File powerMobsFolder = new File(plugin.getDataFolder().getParentFile(), "PowerMobs");
        this.configFile = new File(powerMobsFolder, "extraabilitiesconfig.yml");
        
        saveDefaultConfig();
        reloadConfig();
    }

    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (InputStream in = plugin.getResource("extraabilitiesconfig.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save default extraabilitiesconfig.yml", e);
            }
        } else {
            // If file exists, merge missing defaults
            try (InputStream in = plugin.getResource("extraabilitiesconfig.yml")) {
                if (in != null) {
                    YamlConfiguration internalConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in));
                    YamlConfiguration externalConfig = YamlConfiguration.loadConfiguration(configFile);
                    
                    boolean changed = false;
                    ConfigurationSection internalAbilities = internalConfig.getConfigurationSection("abilities");
                    if (internalAbilities != null) {
                        for (String key : internalAbilities.getKeys(false)) {
                            if (!externalConfig.contains("abilities." + key)) {
                                externalConfig.set("abilities." + key, internalAbilities.get(key));
                                changed = true;
                                plugin.getLogger().info("Detected new ability '" + key + "', adding to config.");
                            }
                        }
                    }
                    
                    if (changed) {
                        externalConfig.save(configFile);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not merge new defaults into extraabilitiesconfig.yml", e);
            }
        }
    }

    public void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Complete reload process: reloads file then re-injects into PowerMobs
     */
    public void reload(PowerMobsPlugin powerMobs) {
        reloadConfig();
        injectIntoPowerMobs(powerMobs);
        plugin.getLogger().info("[PM-ExtraAbilities] Extra abilities configuration reloaded and re-injected.");
    }

    public FileConfiguration getConfig() {
        if (config == null) reloadConfig();
        return config;
    }

    /**
     * Injects the configurations from extraabilitiesconfig.yml into PowerMobs' main abilities config
     * so that the GUI can see the defaults.
     */
    public void injectIntoPowerMobs(PowerMobsPlugin powerMobs) {
        if (powerMobs == null) return;
        
        FileConfiguration powerMobsAbilitiesConfig = powerMobs.getConfigManager().getAbilitiesConfigManager().getConfig();
        ConfigurationSection extraAbilities = config.getConfigurationSection("abilities");
        
        if (extraAbilities != null) {
            for (String key : extraAbilities.getKeys(false)) {
                ConfigurationSection abilitySection = extraAbilities.getConfigurationSection(key);
                if (abilitySection != null) {
                    // We set it in PowerMobs' memory configuration.
                    // This makes AbilityConfigGUIPage see it as a default.
                    powerMobsAbilitiesConfig.set("abilities." + key, abilitySection.getValues(true));
                    plugin.getLogger().info("Injected defaults for extra ability: " + key);
                }
            }
        }
    }
}
