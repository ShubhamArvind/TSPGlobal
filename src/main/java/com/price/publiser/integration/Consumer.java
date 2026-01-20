package com.price.publiser.integration;

import com.price.publiser.bean.PriceRecord;
import com.price.publiser.service.PriceRecordConsumer;
import com.price.publiser.service.impl.ThreadBasedPriceService;

import java.util.Optional;
import java.util.UUID;

public class Consumer extends Thread{
    private final ThreadBasedPriceService consumerService;
    private final String priceRecordId;

    public Consumer(ThreadBasedPriceService consumerService, String priceRecordId) {
        this.consumerService = consumerService;
        this.priceRecordId = priceRecordId;
    }

    @Override
    public void run() {
            // Wait for all batches to be completed (simple example)
            consumerService.getActiveBatchIds().forEach(batchId-> {
                try {
                    consumerService.waitForBatch(batchId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            this.printLatestPrice();
    }


    /*
     Consumer reads the last prices record
     */
    private void printLatestPrice() {
        Optional<PriceRecord> record = consumerService.getLastPrice(priceRecordId);
        System.out.println(Thread.currentThread().getName() + " latest price for " +
                priceRecordId + " = " +
                record.map(payLoad -> payLoad.getPayLoad() + " (asOf: " + payLoad.getAsOf() + ")")
                        .orElse("No price found"));
    }
}
