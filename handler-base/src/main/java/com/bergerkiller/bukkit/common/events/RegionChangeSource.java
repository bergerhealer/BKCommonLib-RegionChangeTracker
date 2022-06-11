package com.bergerkiller.bukkit.common.events;

/**
 * Source that caused a region of blocks to change
 */
public enum RegionChangeSource {
    /** Fast Async Worldedit changed blocks */
    FASTASYNCWORLDEDIT(true),
    /** Standard Worldedit changed blocks */
    WORLDEDIT(true);

    private final boolean isWorldedit;

    private RegionChangeSource(boolean isWorldEdit) {
        this.isWorldedit = isWorldEdit;
    }

    /**
     * Gets whether the source is a type of Worldedit implementation
     *
     * @return True if a Worldedit operation caused the changes
     */
    public boolean isWorldedit() {
        return this.isWorldedit;
    }
}
