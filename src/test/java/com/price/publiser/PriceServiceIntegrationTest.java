package com.price.publiser;
import com.price.publiser.bean.PriceRecord;
import com.price.publiser.integration.Consumer;
import com.price.publiser.integration.Producer;
import com.price.publiser.service.impl.PriceRecordService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class PriceServiceIntegrationTest {
    // Initialize the service
    private PriceRecordService service = new PriceRecordService();

    @Test
    void testConcurrentProducerConsumer() throws InterruptedException, ExecutionException {
        // Create producer and consumer
        Producer producer = new Producer(service);
        Consumer consumer = new Consumer(null, "AAPL");
        Consumer consumer1 = new Consumer(null, "GOOG");
        Consumer consumer2 = new Consumer(null, "MSFT");


        // Use a thread pool for concurrency
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Submit producer task
        Callable<UUID> producerTask = () -> {
            UUID batchId = service.startBatch();

            // Chunk 1
            List<PriceRecord> chunk1 = List.of(
                    new PriceRecord("AAPL", Instant.now(), null),
                    new PriceRecord("GOOG", Instant.now(), null)
            );
            service.uploadChunk(batchId, chunk1);

            // Chunk 2
            List<PriceRecord> chunk2 = List.of(
                    new PriceRecord("MSFT", Instant.now(), null)
            );
            service.uploadChunk(batchId, chunk2);

            // Complete the batch
            service.completeBatch(batchId);

            return batchId;
        };

        // Submit multiple producer tasks concurrently
        Future<UUID> future1 = executor.submit(producerTask);
        Future<UUID> future2 = executor.submit(producerTask);

        // Wait for producers to finish
        UUID batch1 = future1.get();
        UUID batch2 = future2.get();

        // Consumers reading concurrently
        Runnable consumerTask = () -> {
            // Access the service inside appleConsumer via reflection
            try {
                printPriceViaReflection(consumer);
                printPriceViaReflection(consumer1);
                printPriceViaReflection(consumer2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // Submit multiple consumers
        executor.submit(consumerTask);
        executor.submit(consumerTask);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Assertions: check that the last prices exist
        Optional<PriceRecord> aapl = service.getLastPrice("AAPL");
        Optional<PriceRecord> goog = service.getLastPrice("GOOG");
        Optional<PriceRecord> msft = service.getLastPrice("MSFT");

        assertTrue(aapl.isPresent());
        assertTrue(goog.isPresent());
        assertTrue(msft.isPresent());

    }

    public void printPriceViaReflection(Consumer consumer) throws Exception {
        // 1. Get the private 'priceRecordId' field from the Consumer class
        Field serviceField = Consumer.class.getDeclaredField("priceRecordId");
        serviceField.setAccessible(true);

        // 2. Extract the service instance from our consumer object
        Object serviceInstance = serviceField.get(consumer);

        // 3. Find the 'getLastPrice' method on the service object
        Method getLastPriceMethod = serviceInstance.getClass()
                .getMethod("printLatestPrice", null);

        // 4. Invoke the method and cast the result
        getLastPriceMethod.invoke(serviceInstance);
    }
}

