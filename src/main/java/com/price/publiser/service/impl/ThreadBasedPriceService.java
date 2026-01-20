package com.price.publiser.service.impl;

import com.price.publiser.batch.Batch;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
/*
Working on Price Record via help of thread
 */
public class ThreadBasedPriceService extends PriceRecordService{

    private static final ThreadBasedPriceService threadBasedPriceService =new ThreadBasedPriceService();

    // Private constructor prevents instantiation from other classes
    private ThreadBasedPriceService(){}
    /*
       Get Singleton Object via Eagerly initialize Object
  */
    public static ThreadBasedPriceService getInstance(){
        return threadBasedPriceService;
    }

    /**
     *  Wait until a batch is completed
     */
    public void waitForBatch(UUID batchId) throws InterruptedException {
        Batch batch = batches.get(batchId);
        if (batch == null) return;

        synchronized (batch) {
            while (batch.isActive()) {
                batch.wait();
            }
        }
    }

    /**
     * Returns the set of active batch IDs.
     * Only batches which are currently IN_PROGRESS are returned.
     */
    public Set<UUID> getActiveBatchIds() {
        // Filter batches that are still in progress
        return Collections.unmodifiableSet(
                batches.entrySet().stream()
                        .filter(entry -> entry.getValue().isActive())
                        .map(entry -> entry.getKey())
                        .collect(Collectors.toSet())
        );
    }
}
