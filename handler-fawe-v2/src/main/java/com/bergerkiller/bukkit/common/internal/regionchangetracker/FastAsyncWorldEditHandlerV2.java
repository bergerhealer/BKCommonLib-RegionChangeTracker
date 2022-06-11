package com.bergerkiller.bukkit.common.internal.regionchangetracker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import com.bergerkiller.bukkit.common.events.RegionChangeSource;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.eventbus.Subscribe;

/**
 * Uses the experimental (beta) API of FastAsyncWorldEdit to
 * detect chunks that change.
 */
final class FastAsyncWorldEditHandlerV2 implements RegionChangeTrackerHandler {

    @Override
    public boolean isSupported(RegionChangeTrackerHandlerOps ops) {
        // This plugin must exist and be enabled
        if (ops.findPluginEnabledOrProvided("FastAsyncWorldEdit") == null) {
            return false;
        }

        // These classes must exist
        try {
            Class.forName("com.fastasyncworldedit.core.queue.IBatchProcessor");
            Class.forName("com.fastasyncworldedit.core.queue.IChunk");
        } catch (ClassNotFoundException ex) {
            return false;
        }

        return true;
    }

    @Override
    public String name() {
        return "FastAsyncWorldEdit (>= v1.17)";
    }

    @Override
    public HandlerInstance<?> enable(RegionChangeTrackerHandlerOps ops) {
        return new FAWEV2HandlerInstance(this, ops);
    }

    private static final class FAWEV2HandlerInstance extends HandlerInstance<FastAsyncWorldEditHandlerV2> {
        private final EventBus eventBus;

        public FAWEV2HandlerInstance(FastAsyncWorldEditHandlerV2 handler, RegionChangeTrackerHandlerOps ops) {
            super(handler, ops);
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
                final org.bukkit.World world = BukkitAdapter.adapt(event.getWorld());

                final RegionChangeTrackerHandlerChunkDebouncer debouncer = new RegionChangeTrackerHandlerChunkDebouncer(RegionChangeSource.FASTASYNCWORLDEDIT, world, ops);
                event.getExtent().addProcessor(new IBatchProcessor() {
                    @Override
                    public Extent construct(Extent extent) {
                        return extent;
                    }

                    @Override
                    public IChunkSet processSet(IChunk chunk, IChunkGet chunkGet, IChunkSet chunkSet) {
                        synchronized (debouncer) {
                            debouncer.addChunk(chunk.getX(), chunk.getZ());
                        }
                        return chunkSet;
                    }

                    @Override
                    public Future<IChunkSet> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
                        return CompletableFuture.completedFuture(set);
                    }
                });
            }
        }
    }
}
