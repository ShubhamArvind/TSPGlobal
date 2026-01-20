package com.price.publiser.provider;

import com.price.publiser.service.PriceRecordServiceAPI;
import com.price.publiser.service.impl.PriceRecordService;

/*
Factory to provide instances of the price record service like PriceRecordConsumer, PriceRecordPublisher
 */
public class PriceDataFactory {

    /*
    Returns a new instance of the price record service API
     */
    public static PriceRecordServiceAPI getService() {
        return new PriceRecordService();
    }

}
