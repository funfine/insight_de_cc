package com.insight.anomalyDetection.model;

import com.insight.anomalyDetection.util.IdGenerator;
import lombok.Getter;

import java.sql.Timestamp;

/**
 * Created by yfang on 7/1/2017.
 * This class saves the purchase record. All variables are immutable and declared as final.
 * The compareTo method compares the timestamp first. If two timestamps are the same, it will rely on the logId to determine order.
 */

@Getter
public class PurchaseRecord implements Comparable<PurchaseRecord> {

    private final Timestamp timestamp;
    private final Double amount;
    private final String id;
    private final int logId;


    public PurchaseRecord(String timestamp, String amount, String id) {
        this.timestamp = Timestamp.valueOf(timestamp);
        this.amount = Double.valueOf(amount);
        this.id = id;

        // This ID is saved to handle the case when the timestamp is the same.
        // logId is the int type and can handle the data up to ~200GB (given that the sample data has 500,000 records and took 45MB)
        logId = IdGenerator.INSTANCE.getId();
    }

    @Override
    public int compareTo(PurchaseRecord another) {
        int compare = timestamp.compareTo(another.timestamp);
        if (compare == 0) {
            compare = logId - another.getLogId();
            return compare;
        }
        return compare;
    }
}
