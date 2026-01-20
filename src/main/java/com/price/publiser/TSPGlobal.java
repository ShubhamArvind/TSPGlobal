package com.price.publiser;

import com.price.publiser.integration.Consumer;
import com.price.publiser.integration.Producer;
import com.price.publiser.provider.PriceDataFactory;
import com.price.publiser.service.PriceRecordServiceAPI;
import com.price.publiser.service.impl.ThreadBasedPriceService;

public class TSPGlobal {

    public static void main(String[] args) {
        TSPGlobal tspGlobal = new TSPGlobal();
        try {
            tspGlobal.callBatchCycle();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void callBatchCycle() throws InterruptedException{
        // Initialize the service
        PriceRecordServiceAPI service = PriceDataFactory.getService();
        ThreadBasedPriceService threadBasedPriceService = ThreadBasedPriceService.getInstance();

        // Create producer

        Producer producer1 = new Producer(service);
        Producer producer2 = new Producer(service);
        // activate the thread
        producer1.start();
        producer2.start();

        // Create consumer
        Consumer consumerAAPL = new Consumer(threadBasedPriceService, "AAPL");
        Consumer consumerGOOG = new Consumer(threadBasedPriceService, "GOOG");
        Consumer consumerMSFT = new Consumer(threadBasedPriceService, "MSFT");

        // activate the thread
        consumerAAPL.start();
        consumerGOOG.start();
        consumerMSFT.start();

        // applying locking
        producer1.join();
        producer2.join();
        consumerAAPL.join();
        consumerGOOG.join();
        consumerMSFT.join();
    }
}