package com.price.publiser.service;

import com.price.publiser.bean.PriceRecord;

import java.util.Optional;

/**
 * Consumer interface to fetch the latest price for a given instrument.
 */
public interface PriceRecordConsumer {
    /**
     *  Returns the last published price for a given price record ID.
     */
    Optional<PriceRecord> getLastPrice(String priceRecordId);
}
