package com.price.publiser;


import com.price.publiser.batch.Batch;
import com.price.publiser.bean.PriceRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BatchTest {
    @Test
    void testInitialStateIsInProgress() {
        Batch batch = new Batch();

        // A new batch must always start IN_PROGRESS
        Assertions.assertEquals(Batch.BatchState.IN_PROGRESS, batch.getStatus());
        assertTrue(batch.isActive());
    }

    @Test
    void testAddOrUpdateStoresRecord() {
        Batch batch = new Batch();

        PriceRecord r1 = new PriceRecord("AAPL",
                Instant.parse("2024-01-01T10:00:00Z"), null);

        batch.addOrUpdate(r1);

        // Ensure record was stored correctly
        assertEquals(r1, batch.getLatestPrices().get("AAPL"));
    }

    @Test
    void testAddOrUpdateKeepsLatestByAsOf() {
        Batch batch = new Batch();

        PriceRecord record = new PriceRecord("MSFT",
                Instant.parse("2024-01-01T09:00:00Z"), null);
        PriceRecord newRecord = new PriceRecord("MSFT",
                Instant.parse("2024-01-01T11:00:00Z"), new PriceRecord("TEST", Instant.now(), null));

        batch.addOrUpdate(record);
        batch.addOrUpdate(newRecord);
        // Newer Object must override the older one
        assertEquals("TEST", batch.getLatestPrices().get("MSFT").getPayLoad().getId());
    }

    @Test
    void testSetStatusToCompleted() {
        Batch batch = new Batch();

        batch.setStatus(Batch.BatchState.COMPLETED);

        // Completed batches are inactive
        Assertions.assertEquals(Batch.BatchState.COMPLETED, batch.getStatus());
        assertFalse(batch.isActive());
    }

    @Test
    void testSetStatusToCancelled() {
        Batch batch = new Batch();

        batch.setStatus(Batch.BatchState.CANCELLED);

        // Cancelled batches should be inactive
        assertFalse(batch.isActive());
    }

    @Test
    void testAddingRecordsAfterCompletionThrowsException() {
        Batch batch = new Batch();
        batch.setStatus(Batch.BatchState.COMPLETED);

        PriceRecord record = new PriceRecord("IBM",
                Instant.parse("2024-01-01T12:00:00Z"), null);

        // Adding after completion should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> batch.addOrUpdate(record));
    }

    @Test
    void testThreadSafetyOnConcurrentUpdates() throws InterruptedException {
        Batch batch = new Batch();

        PriceRecord earlier = new PriceRecord("TSLA",
                Instant.parse("2024-01-01T10:00:00Z"), null);

        PriceRecord later = new PriceRecord("MSLA",
                Instant.parse("2024-01-01T12:00:00Z"), new PriceRecord("XYZE", Instant.now(),null));

        Thread t1 = new Thread(() -> batch.addOrUpdate(earlier));
        Thread t2 = new Thread(() -> batch.addOrUpdate(later));

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Later timestamp must win even with concurrent writes
        assertEquals("XYZE", batch.getLatestPrices().get("MSLA").getPayLoad().getId());
    }
}

