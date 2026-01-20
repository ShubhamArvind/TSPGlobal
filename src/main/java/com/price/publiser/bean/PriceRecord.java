package com.price.publiser.bean;

import java.time.Instant;

public class PriceRecord {
    private String id;
    private Instant asOf;
    private PriceRecord payLoad;

    public PriceRecord(String id, Instant asOf, PriceRecord payLoad) {
        this.id = id;
        this.asOf = asOf;
        this.payLoad = payLoad;
    }

    @Override
    public String toString() {
        return "PriceRecord{" +
                "id='" + id + '\'' +
                ", asOf=" + asOf +
                ", payLoad=" + payLoad +
                '}';
    }

    public String getId() {
        return id;
    }

    public Instant getAsOf() {
        return asOf;
    }

    public PriceRecord getPayLoad() {
        return payLoad;
    }
}
