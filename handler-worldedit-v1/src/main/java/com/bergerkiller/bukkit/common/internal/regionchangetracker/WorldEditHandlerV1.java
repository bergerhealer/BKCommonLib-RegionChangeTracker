package com.bergerkiller.bukkit.common.internal.regionchangetracker;

import java.lang.reflect.Method;

import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.events.RegionChangeSource;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.eventbus.Subscribe;

final class WorldEditHandlerV1 implements RegionChangeTrackerHandler {

    @Override
    public boolean isSupported(RegionChangeTrackerHandlerOps ops) {
        // A plugin must exist on the server that is, or provides, WorldEdit
        Plugin plugin = ops.findPluginEnabledOrProvided("WorldEdit");
        if (plugin == null) {
            return false;
        }

        // These classes must exist
        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class.forName("com.sk89q.worldedit.extent.AbstractDelegateExtent", false, loader);
            Class.forName("com.sk89q.worldedit.Vector", false, loader);
            Class.forName("com.sk89q.worldedit.blocks.BaseBlock", false, loader);
        } catch (ClassNotFoundException ex) {
            return false;
        }

        // Should not be used on FAWE
        if (plugin.getName().equalsIgnoreCase("FastAsyncWorldEdit")) {
            return false;
        }

        return true;
    }

    @Override
    public String name() {
        return "WorldEdit (<= v6.0.0)";
    }

    @Override
    public HandlerInstance<?> enable(RegionChangeTrackerHandlerOps ops) throws Exception, Error {
        return new WEV1HandlerInstance(this, ops);
    }

    private static final class WEV1HandlerInstance extends HandlerInstance<WorldEditHandlerV1> {
        private final Method adaptWorldMethod;
        private final EventBus eventBus;

        public WEV1HandlerInstance(WorldEditHandlerV1 handler, RegionChangeTrackerHandlerOps ops) throws Exception {
            super(handler, ops);

            // On very old versions of WorldEdit the BukkitAdapter.adapt(world) method is not public,
            // because the class does not have public scope. This is a workaround for that.
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World");
            adaptWorldMethod = bukkitAdapter.getDeclaredMethod("adapt", worldClass);
            adaptWorldMethod.setAccessible(true);

            eventBus = WorldEdit.getInstance().getEventBus();
            eventBus.register(this);
        }

        @Override
        public void disable() {
            eventBus.unregister(this);
        }

        @Subscribe
        public void onEditSession(EditSessionEvent event) {
            if (event.getStage() == EditSession.Stage.BEFORE_CHANGE) {
                org.bukkit.World world;
                try {
                    world = (org.bukkit.World) adaptWorldMethod.invoke(null, event.getWorld());
                } catch (Throwable t) {
                    notifyError("Failed to get WorldEdit EditSessionEvent world", t);
                    return;
                }
                event.setExtent(new ChangeTrackerAdapter(event.getExtent(), ops, world));
            }
        }

        // Main adapter, supports latest worldedit version
        public static class ChangeTrackerAdapter extends AbstractDelegateExtent {
            private final RegionChangeTrackerHandlerChunkDebouncer _debouncer;

            public ChangeTrackerAdapter(Extent extent, RegionChangeTrackerHandlerOps ops, org.bukkit.World world) {
                super(extent);
                this._debouncer = new RegionChangeTrackerHandlerChunkDebouncer(RegionChangeSource.WORLDEDIT, world, ops);
            }

            @Override
            public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
                if (super.setBlock(location, block)) {
                    this._debouncer.addBlock(location.getBlockX(), location.getBlockZ());
                    return true;
                }
                return false;
            }
        }
    }
}
