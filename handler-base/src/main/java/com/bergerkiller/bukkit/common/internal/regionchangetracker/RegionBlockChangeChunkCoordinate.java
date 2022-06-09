package com.bergerkiller.bukkit.common.internal.regionchangetracker;

import org.bukkit.Chunk;

/**
 * Stores the chunk x/z coordinates part of a region block change event.
 * Mutable (for performance reasons).
 */
public class RegionBlockChangeChunkCoordinate {
    public int x, z;

    public RegionBlockChangeChunkCoordinate(Chunk chunk) {
        this(chunk.getX(), chunk.getZ());
    }

    public RegionBlockChangeChunkCoordinate(final int x, final int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public int hashCode() {
        long i = (long) this.x & 4294967295L | ((long) this.z & 4294967295L) << 32;
        int j = (int) i;
        int k = (int) (i >> 32);
        return j ^ k;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (object instanceof RegionBlockChangeChunkCoordinate) {
            RegionBlockChangeChunkCoordinate iv3 = (RegionBlockChangeChunkCoordinate) object;
            return iv3.x == this.x && iv3.z == this.z;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "{" + x + ", " + z + "}";
    }
}
