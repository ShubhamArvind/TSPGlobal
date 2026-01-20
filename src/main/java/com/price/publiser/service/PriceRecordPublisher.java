package com.price.publiser.service;

import com.price.publiser.bean.PriceRecord;

import java.util.List;
import java.util.UUID;

/**
 *  Publisher interface to manage batches of price records.
 */
public interface PriceRecordPublisher {
    /**
     *  Starts a new batch and returns its unique batch ID.
     */
    UUID startBatch();
    /**
     * Uploads a chunk of price records to the specified batch.
     */
    void uploadChunk(UUID batchId, List<PriceRecord> chunk);
    /**
     * Completes the batch and publishes all collected records.
     */
    void completeBatch(UUID batchId);
    /**
     *  Cancels the batch and discards all collected records.
     */
    void cancelBatch(UUID batchId);
}
