package com.price.publiser.batch;

import com.price.publiser.bean.PriceRecord;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single price record upload batch.
 * Stores the latest price record until the batch is completed.
 * Batches only accept updates while IN_PROGRESS.
 */

public class Batch {

    /** Holds the latest price per instrument inside this batch (thread-safe). */
    private final Map<String, PriceRecord> latestPrices = new ConcurrentHashMap<>();
    /** Tracks the current lifecycle state of the batch. */
    private volatile BatchState state = BatchState.IN_PROGRESS;

    /**
     * Returns the current batch state (thread-safe).
     */
    public synchronized BatchState getStatus() {
        return state;
    }

    /**
     * Updates the batch state (thread-safe).
     */
    public synchronized void setStatus(BatchState status) {
        this.state = status;
    }

    /**
     *  True only while the batch is allowed to accept new records.
     */
    public boolean isActive() {
        return getStatus() == BatchState.IN_PROGRESS;
    }

    /**
     *  Returns all latest price records collected for this batch.
     */
    public Map<String, PriceRecord> getLatestPrices() {
        return latestPrices;
    }

    /**
     * Thread-safe update within the batch context
     * Adds or updates a price record in this batch.
     * Only allowed while IN_PROGRESS.
     * Throws IllegalStateException if the batch is completed or cancelled.
     */
    public synchronized void addOrUpdate(PriceRecord record) {
        if (state != BatchState.IN_PROGRESS)
            throw new IllegalStateException("Cannot add to batch: " + state);

        latestPrices.merge(record.getId(), record, (a, b) ->
                a.getAsOf().isAfter(b.getAsOf()) ? a : b
        );
    }
    /**
     *  Possible lifecycle states of a batch.
     */
        public enum BatchState {
            IN_PROGRESS, COMPLETED, CANCELLED
        }
}
