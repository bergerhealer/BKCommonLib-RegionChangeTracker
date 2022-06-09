package com.bergerkiller.bukkit.common.internal.regionchangetracker;

import java.util.logging.Level;

/**
 * Base implementation for a handler
 */
interface RegionChangeTrackerHandler {
    /**
     * Gets a name identifying this type of region change tracker handler.
     * Is logged when the handler enables/disables.
     *
     * @return Handler name
     */
    String name();

    /**
     * Checks whether currently this handler is supported on the server,
     * and so should be chosen as a handler.
     *
     * @param ops Provides the operations handlers can perform
     * @return True if this handler is supported
     */
    boolean isSupported(RegionChangeTrackerHandlerOps ops);

    /**
     * Initializes a new instance of this handler. Is called if
     * {@link #isSupported()} returns true.
     *
     * @param ops Provides the operations handlers can perform
     * @return New instance
     * @throws Exception
     * @throws Error
     */
    HandlerInstance<?> enable(RegionChangeTrackerHandlerOps ops) throws Exception, Error;

    /**
     * An instance of the handler that was initialized
     */
    public static abstract class HandlerInstance<H extends RegionChangeTrackerHandler> {
        protected final H handler;
        protected final RegionChangeTrackerHandlerOps ops;

        protected HandlerInstance(H handler, RegionChangeTrackerHandlerOps ops) {
            this.handler = handler;
            this.ops = ops;
        }

        /**
         * Notifies an error occurred inside a handler implementation
         *
         * @param message Message indicating where/what happened
         * @param error Error that occurred
         */
        public void notifyError(String message, Throwable error) {
            ops.getPlugin().getLogger().log(Level.SEVERE, "[RegionChangeTracker] An error occurred in handler for " +
                    handler.name() + ": " + message, error);
        }

        /**
         * Disables the instance again, stopping any tasks that were
         * started and cleaning it up
         */
        public abstract void disable() throws Exception, Error;
    }
}
