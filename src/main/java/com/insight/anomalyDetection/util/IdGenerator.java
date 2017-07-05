package com.insight.anomalyDetection.util;

/**
 * Created by yfang on 7/2/2017.
 * This is to create a logid for every purchase record, otherwise the order will be lost when two events have the same timestamp.
 * logid is an integer type, which can support 2^31-1 records, roughly corresponding to 200GB data (calculated from the sample data where 44MB data has 500,000 records).
 * In the real use case, when it approaches the limit of int32, we can change this to an int64 type or we can traverse all the saved purchase record and subtract the minimum.
 */

public enum IdGenerator {
    INSTANCE;
    private int curId = 0;
    public int getId() {
        return curId++;
    }
}
