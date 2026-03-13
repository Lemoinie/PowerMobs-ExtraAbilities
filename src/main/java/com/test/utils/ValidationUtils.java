package com.test.utils;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.PowerMob;
import java.util.logging.Level;

public class ValidationUtils {

    /**
     * Gets a validated integer from ability settings.
     */
    public static int getValidInt(PowerMob mob, String abilityId, String key, int defaultValue, int min, int max) {
        int val = mob.getAbilityInt(abilityId, key, defaultValue);
        if (val < min || val > max) {
            int clamped = Math.max(min, Math.min(val, max));
            logWarning(mob.getPlugin(), abilityId, key, val, clamped);
            return clamped;
        }
        return val;
    }

    /**
     * Gets a validated double from ability settings.
     */
    public static double getValidDouble(PowerMob mob, String abilityId, String key, double defaultValue, double min, double max) {
        double val = mob.getAbilityDouble(abilityId, key, defaultValue);
        if (val < min || val > max) {
            double clamped = Math.max(min, Math.min(val, max));
            logWarning(mob.getPlugin(), abilityId, key, val, clamped);
            return clamped;
        }
        return val;
    }

    private static void logWarning(PowerMobsPlugin plugin, String abilityId, String key, Object invalid, Object clamped) {
        plugin.getLogger().log(Level.WARNING, 
            String.format("[ExtraAbilities] Invalid config for '%s'. Key '%s' was %s, clamped to %s", 
                abilityId, key, invalid, clamped));
    }
}
