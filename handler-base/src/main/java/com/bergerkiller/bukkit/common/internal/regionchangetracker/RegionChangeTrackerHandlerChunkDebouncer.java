package com.bergerkiller.bukkit.common.internal.regionchangetracker;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.events.RegionChangeSource;

/**
 * Temporarily holds chunk coordinates that have changed. Then periodically
 * notifies the changed chunks to the downstream tracker.
 * Not multithread-safe. If called from another thread, must externally synchronize
 * on this class instance before calling addBlock/addChunk to avoid corruption.
 */
class RegionChangeTrackerHandlerChunkDebouncer {
    private final RegionChangeSource source;
    private final World world;
    private final RegionChangeTrackerHandlerOps ops;
    private final AtomicInteger ticksOfNoChanges = new AtomicInteger();
    private int commitEarlyTaskId;
    private int commitLateTaskId;
    private Set<RegionBlockChangeChunkCoordinate> values = new HashSet<>();
    private RegionBlockChangeChunkCoordinate lookupCoord = new RegionBlockChangeChunkCoordinate(0, 0);

    public RegionChangeTrackerHandlerChunkDebouncer(RegionChangeSource source, World world, RegionChangeTrackerHandlerOps ops) {
        this.source = source;
        this.world = world;
        this.ops = ops;
    }

    public boolean addBlock(int blockX, int blockZ) {
        return addChunk(blockX >> 4, blockZ >> 4);
    }

    public boolean addChunk(int chunkX, int chunkZ) {
        Set<RegionBlockChangeChunkCoordinate> values = this.values;
        if (values.isEmpty()) {
            // First time adding: start task to auto-notify these changes
            ticksOfNoChanges.set(0);
            commitEarlyTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(ops.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    if (ticksOfNoChanges.incrementAndGet() >= 5) {
                        commit();
                    }
                }
            }, 1, 1);
            commitLateTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(ops.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    commitLateTaskId = -1;
                    commit();
                }
            }, 100); // At most 5s later

            values.add(new RegionBlockChangeChunkCoordinate(chunkX, chunkZ));
            return true;
        } else {
            // Second or further times adding
            lookupCoord.x = chunkX;
            lookupCoord.z = chunkZ;
            if (values.add(lookupCoord)) {
                lookupCoord = new RegionBlockChangeChunkCoordinate(0, 0);
                ticksOfNoChanges.set(0);
                return true;
            } else {
                return false;
            }
        }
    }

    public synchronized void commit() {
        if (commitEarlyTaskId != -1) {
            Bukkit.getScheduler().cancelTask(commitEarlyTaskId);
            commitEarlyTaskId = -1;
        }
        if (commitLateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(commitLateTaskId);
            commitLateTaskId = -1;
        }
        if (!values.isEmpty()) {
            Set<RegionBlockChangeChunkCoordinate> tmp = values;
            values = new HashSet<>();
            ops.notifyChanges(source, world, tmp);
        }
    }
}
