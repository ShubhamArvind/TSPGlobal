package com.price.publiser.integration;

import com.price.publiser.bean.PriceRecord;
import com.price.publiser.service.PriceRecordPublisher;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Producer extends Thread{
    private final PriceRecordPublisher publisherService;

    public Producer(PriceRecordPublisher publisherService) {
        this.publisherService = publisherService;
    }

    @Override
    public void run() {
        // Producer publishes a batch
        this.publishSampleBatch();
    }

    private void publishSampleBatch() {
        // Start a new batch
        UUID batchId = publisherService.startBatch();
        System.out.println(Thread.currentThread().getName() + " started batch: " + batchId);

        // Create first chunk of 1000 or fewer records (example 3 records)
        List<PriceRecord> chunk1 = List.of(
                new PriceRecord("AAPL", Instant.parse("2025-01-01T10:00:00Z"), new PriceRecord("AAPL",Instant.now(),null)),
                new PriceRecord("GOOG", Instant.parse("2025-01-01T10:00:00Z"), null)
        );
        publisherService.uploadChunk(batchId, chunk1);
        System.out.println("Uploaded chunk 1 with " + chunk1.size() + " records");

        // Second chunk (optional)
        List<PriceRecord> chunk2 = List.of(
                new PriceRecord("MSFT", Instant.parse("2025-01-01T10:00:00Z"), new PriceRecord("KFG", Instant.now(),null))
        );
        publisherService.uploadChunk(batchId, chunk2);
        System.out.println("Uploaded chunk 2 with " + chunk2.size() + " records");

        // Complete the batch
        publisherService.completeBatch(batchId);
        System.out.println("Completed batch: " + batchId);
        System.out.println(Thread.currentThread().getName() + " completed batch: " + batchId);
    }
}
