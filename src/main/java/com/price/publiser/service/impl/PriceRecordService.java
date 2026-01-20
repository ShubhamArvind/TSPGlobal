package com.price.publiser.service.impl;

import com.price.publiser.batch.Batch;
import com.price.publiser.bean.PriceRecord;
import com.price.publiser.service.PriceRecordServiceAPI;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that manages batches of price records and exposes publisher/consumer operations.
 */

public class PriceRecordService implements PriceRecordServiceAPI {

    /**
     *  Active batches keyed by batch ID.
     */
    protected final Map<UUID, Batch> batches = new ConcurrentHashMap<>();

    /**
     *  Stores the last committed price Record (thread-safe).
     */
    private final ConcurrentHashMap<String, PriceRecord> commitedPrice = new ConcurrentHashMap<>();

    /**
     *  Starts a new batch and returns its unique ID.
     */
    @Override
    public UUID startBatch() {
        UUID id = UUID.randomUUID();
        batches.put(id, new Batch());
        return id;
    }

    /**
     *  Uploads a chunk of price records into an active batch.
     */
    @Override
    public void uploadChunk(UUID batchId, List<PriceRecord> chunks) {
        Batch batch = batches.get(batchId);
        if (batch == null || !batch.isActive())
            return;
        synchronized (batch) {
            if (!batch.isActive())
                return;
            chunks.forEach(record -> {
                batch.addOrUpdate(record);
            });
        }
    }

    /**
     *  Completes a batch and publishes all collected price record updates atomically.
     */
    @Override
    public void completeBatch(UUID batchId) {
        Batch batch = batches.get(batchId);
        if (batch == null)
            return;

        synchronized (batch) {
            if (!batch.isActive())
                return;
            batch.setStatus(Batch.BatchState.COMPLETED);
            // Publish all last records to the global provider
            batch.getLatestPrices().forEach((key,value) -> {
                commitedPrice.merge(key, value,
                        (existing, incoming) -> incoming.getAsOf().isAfter(existing.getAsOf()) ? incoming : existing
                );
            });
            // Notify all threads waiting on this batch
            batch.notifyAll();
        }
        batches.remove(batchId);
    }
    /**
     *  Cancels a batch and discards all collected data.
     */
    @Override
    public void cancelBatch(UUID batchId) {
        Batch batch = batches.get(batchId);
        if (batch == null)
            return;

        synchronized (batch) {
            if (!batch.isActive())
                return;
            batch.setStatus(Batch.BatchState.CANCELLED);
            // Notify all threads waiting on this batch
            batch.notifyAll();
        }

        batches.remove(batchId);
    }
    /**
     *  Retrieves the last published price for an price record.
     */
    @Override
    public Optional<PriceRecord> getLastPrice(String priceRecordId) {
        return Optional.ofNullable(commitedPrice.get(priceRecordId));
    }

}
