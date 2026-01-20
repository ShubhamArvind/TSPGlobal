package com.price.publiser;

import com.price.publiser.bean.PriceRecord;
import com.price.publiser.service.PriceRecordConsumer;
import com.price.publiser.service.PriceRecordPublisher;
import com.price.publiser.service.impl.PriceRecordService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PriceServiceTest {
    private PriceRecordService service = new PriceRecordService();

    @Test
    public void testBatchUploadAndComplete() {

        UUID bid = service.startBatch();
        // Upload two records in a batch
        service.uploadChunk(bid, List.of(
                new PriceRecord("IBM", Instant.parse("2024-01-01T10:00:00Z"), new PriceRecord("IBM1", Instant.now(), null)),
                new PriceRecord("AAPL", Instant.parse("2024-01-01T11:00:00Z"), new PriceRecord("AAPL123", Instant.now(), null))
        ));

        // Before completion → no data visible
        assertTrue(service.getLastPrice("IBM").isEmpty());

        service.completeBatch(bid);
        // After completion → latest prices visible
        assertEquals("IBM1", service.getLastPrice("IBM").get().getPayLoad().getId());
        assertEquals("AAPL123", service.getLastPrice("AAPL").get().getPayLoad().getId());
    }

    @Test
    public void testBatchCancel() {

        UUID bid = service.startBatch();

        service.uploadChunk(bid, List.of(
                new PriceRecord("TSLA", Instant.now(), new PriceRecord("TSLA1", Instant.now(), null))
        ));
        // Cancelled batch → no prices visible
        service.cancelBatch(bid);

        assertTrue(service.getLastPrice("TSLA").isEmpty());
    }

    @Test
    public void testLatestPriceWins() {

        UUID bid = service.startBatch();
        // Upload two records for same instrument with different asOf
        service.uploadChunk(bid, List.of(
                new PriceRecord("GOOG", Instant.parse("2024-01-01T10:00:00Z"), new PriceRecord("IBM1", Instant.now(), null)),
                new PriceRecord("GOOG", Instant.parse("2024-01-01T12:00:00Z"), new PriceRecord("AAPL123", Instant.now(), null))
        ));
        // Last asOf wins
        service.completeBatch(bid);

        assertEquals("AAPL123", service.getLastPrice("GOOG").get().getPayLoad().getId());
    }

    @Test
    public void testIncorrectOrderDoesNotCrash() {

        // Completing unknown batch → should not throw exceptions
        service.completeBatch(UUID.randomUUID());
    }
}
