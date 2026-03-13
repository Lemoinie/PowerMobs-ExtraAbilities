package com.test;

import com.powermobs.PowerMobsPlugin;
import com.test.abilities.*;
import com.test.config.ConfigManager;
import com.test.listeners.ReloadListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtraAbilities extends JavaPlugin {

    private ConfigManager configManager;

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public void onEnable() {
        // Find PowerMobs core plugin
        PowerMobsPlugin core = (PowerMobsPlugin) Bukkit.getPluginManager().getPlugin("PowerMobs");

        if (core != null) {
            // Initialize and inject configuration
            this.configManager = new ConfigManager(this);
            this.configManager.reload(core); // Ensure fresh load and injection
            
            // Register reload listener
            Bukkit.getPluginManager().registerEvents(new ReloadListener(this, core), this);

            // Register custom abilities
            core.getAbilityManager().registerAbility(new VoidPullAbility(core));
            core.getAbilityManager().registerAbility(new MountCrashAbility(core));
            core.getAbilityManager().registerAbility(new BerserkAbility(core));
            core.getAbilityManager().registerAbility(new VenomBiteAbility(core));
            core.getAbilityManager().registerAbility(new WitherTouchAbility(core));
            core.getAbilityManager().registerAbility(new LifeStealAbility(core));
            core.getAbilityManager().registerAbility(new StarvationCurseAbility(core));
            core.getAbilityManager().registerAbility(new DreadScreechAbility(core));
            core.getAbilityManager().registerAbility(new TenacityAbility(core));
            core.getAbilityManager().registerAbility(new QuicksilverAbility(core));
            core.getAbilityManager().registerAbility(new DomainExpansionAbility(core));
            core.getAbilityManager().registerAbility(new ExplosiveDeathAbility(core));
            core.getAbilityManager().registerAbility(new ArmorShatterAbility(core));
            core.getAbilityManager().registerAbility(new ShadowExtractionAbility(core));
            core.getAbilityManager().registerAbility(new FearAuraAbility(core));
            core.getAbilityManager().registerAbility(new RepulsionBurstAbility(core));
            core.getAbilityManager().registerAbility(new BloodPoolAbility(core));
            core.getAbilityManager().registerAbility(new CripplingStrikeAbility(core));
            core.getAbilityManager().registerAbility(new ChainLightningAbility(core));
            core.getAbilityManager().registerAbility(new CrushingBlowAbility(core));
            core.getAbilityManager().registerAbility(new DeathZoneAbility(core));
            core.getAbilityManager().registerAbility(new ParryAbility(core));
            core.getAbilityManager().registerAbility(new PotionImmunityAbility(core));
            core.getAbilityManager().registerAbility(new PotionReflectionAbility(core));
            core.getAbilityManager().registerAbility(new EffectPurgeAbility(core));
            core.getAbilityManager().registerAbility(new LavaWalkerAbility(core));
            core.getAbilityManager().registerAbility(new FallImmunityAbility(core));
            core.getAbilityManager().registerAbility(new AquaAnchorAbility(core));
            core.getAbilityManager().registerAbility(new SiegeBreakerAbility(core));
            core.getAbilityManager().registerAbility(new DisarmAbility(core));
            core.getAbilityManager().registerAbility(new ShieldBreakingAbility(core));
            core.getAbilityManager().registerAbility(new MeteorStrikeAbility(core));
            core.getAbilityManager().registerAbility(new MeteorShowerAbility(core));
            core.getAbilityManager().registerAbility(new AdvancedAggroAbility(core));
            // Keep it short and simple
            getLogger().info("[PM-ExtraAbilities] Successfully registered ExtraAbilities to PowerMobs!");
        } else {
            getLogger().severe("[PM-ExtraAbilities] Could not find PowerMobs core plugin! Abilities will not be registered.");
            getLogger().severe("[PM-ExtraAbilities] Please ensure PowerMobs is installed and enabled.");
        }

        getLogger().info("[PM-ExtraAbilities] PowerMobs-ExtraAbilities has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("[PM-ExtraAbilities] PowerMobs-ExtraAbilities has been disabled!");
    }
}
