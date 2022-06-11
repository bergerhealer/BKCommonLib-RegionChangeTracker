package com.bergerkiller.bukkit.common.internal.regionchangetracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Tracks large-scale block changes that occur because of other plugin's fill/regen operations.
 * Supports WorldEdit/FastAsyncWorldEdit and possibly others in the future.
 * Must be implemented to override the {@link #notifyChanges(World, Collection)} method.
 */
public abstract class RegionChangeTracker implements RegionChangeTrackerHandlerOps {
    private final JavaPlugin plugin;
    private final Handler[] all_handlers;
    private Plugin pluginCurrentlyDisabling = null;

    public RegionChangeTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        List<Handler> handlers = new ArrayList<>();
        initHandler(handlers, WorldEditHandlerV1::new);
        initHandler(handlers, WorldEditHandlerV2::new);
        initHandler(handlers, FastAsyncWorldEditHandlerV1::new);
        initHandler(handlers, FastAsyncWorldEditHandlerV2::new);
        this.all_handlers = handlers.toArray(new Handler[handlers.size()]);
    }

    private void initHandler(List<Handler> handlers, Supplier<RegionChangeTrackerHandler> supplier) {
        try {
            handlers.add(new Handler(supplier.get()));
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "[RegionChangeTracker] Failed to register a handler", t);
        }
    }

    /**
     * Enables this region change tracker. From now on, region block changes will result in
     * events being generated.<br>
     * <br>
     * Should be called in plugin onEnable.
     */
    public void enable() {
        refreshHandlers();
        Bukkit.getPluginManager().registerEvents(new Listener() {
            // Fires AFTER a plugin is enabled
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPluginAfterEnabled(PluginEnableEvent event) {
                refreshHandlers();
            }

            // Fires BEFORE a plugin is disabled
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPluginBeforeDisable(PluginDisableEvent event) {
                pluginCurrentlyDisabling = event.getPlugin();
                refreshHandlers();
                pluginCurrentlyDisabling = null;
            }
        }, plugin);
    }

    /**
     * Disables this region change tracker. From now on no more events will be fired.<br>
     * <br>
     * Should be called in plugin onDisable.
     */
    public void disable() {
        for (Handler handler : all_handlers) {
            handler.disable(false);
        }
    }

    private void refreshHandlers() {
        for (Handler handler : all_handlers) {
            handler.refresh();
        }
    }

    @Override
    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public Plugin findPluginEnabledOrProvided(String pluginName) {
        // The plugin itself
        {
            Plugin p = Bukkit.getPluginManager().getPlugin(pluginName);
            if (p != null && p != pluginCurrentlyDisabling && p.isEnabled()) {
                return p;
            }
        }

        // Plugins that substitute the plugin (provides)
        try {
            for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
                if (p != pluginCurrentlyDisabling && p.isEnabled()) {
                    for (String provide : p.getDescription().getProvides()) {
                        if (pluginName.equalsIgnoreCase(provide)) {
                            return p;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            /* Ignore - probably missing provides api */
        }
        return null;
    }

    private class Handler {
        public final RegionChangeTrackerHandler handler;
        public RegionChangeTrackerHandler.HandlerInstance<?> instance;
        private boolean enableFailed;

        public Handler(RegionChangeTrackerHandler handler) {
            this.handler = handler;
            this.instance = null;
            this.enableFailed = false;
        }

        public void refresh() {
            if (handler.isSupported(RegionChangeTracker.this)) {
                if (instance == null && !enableFailed) {
                    try {
                        instance = handler.enable(RegionChangeTracker.this);
                        plugin.getLogger().info("[RegionChangeTracker] Region block changes will be notified from " + handler.name());
                    } catch (Throwable t) {
                        enableFailed = true;
                        plugin.getLogger().log(Level.SEVERE, "[RegionChangeTracker] Failed to enable handler for " + handler.name(), t);
                    }
                }
            } else {
                disable(true);
            }
        }

        public void disable(boolean logDisabled) {
            enableFailed = false;
            if (instance != null) {
                try {
                    instance.disable();
                    if (logDisabled) {
                        plugin.getLogger().info("[RegionChangeTracker] Region block changes will no longer be notified from " + handler.name());
                    }
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "[RegionChangeTracker] Failed to disable handler for " + handler.name(), t);
                }
                instance = null;
            }
        }
    }
}
