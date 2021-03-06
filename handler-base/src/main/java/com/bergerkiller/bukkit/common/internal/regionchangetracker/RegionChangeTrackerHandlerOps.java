package com.bergerkiller.bukkit.common.internal.regionchangetracker;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.events.RegionChangeSource;

import java.util.Collection;

import org.bukkit.World;

/**
 * Operations a Handler is allowed to perform while
 * handling change events.
 */
public interface RegionChangeTrackerHandlerOps {

    /**
     * Gets the JavaPlugin instance that is hosting this region change tracker
     *
     * @return Region Change Tracker plugin instance
     */
    JavaPlugin getPlugin();

    /**
     * Checks whether a plugin by the name specified is enabled, or that a different
     * plugin is enabled that provided that same plugin's functionality. Plugins that
     * are in the process of disabling will always yield false, plugins that are
     * enabling will yield true.
     *
     * @param pluginName
     * @return Found plugin, null if not found or enabled
     */
    Plugin findPluginEnabledOrProvided(String pluginName);

    /**
     * Notifies changes have occurred inside one or more chunks
     *
     * @param source Source of the changes
     * @param world World where the changes occurred
     * @param chunks Chunk coordinates that changed
     */
    void notifyChanges(RegionChangeSource source, World world, Collection<RegionBlockChangeChunkCoordinate> chunks);
}
