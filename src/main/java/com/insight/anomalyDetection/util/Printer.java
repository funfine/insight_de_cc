package com.insight.anomalyDetection.util;

import com.insight.anomalyDetection.model.Anomaly;
import com.insight.anomalyDetection.model.PurchaseRecord;
import com.insight.anomalyDetection.model.User;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;

/**
 * Created by yfang on 7/2/2017.
 * This is to print the output.
 */

@Slf4j
public enum Printer {

    INSTANCE;

    public static File file;

    public void print(User user, PurchaseRecord purchaseRecord) {

        if (file == null) {
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("YYYY.MM.dd HH:mm:ss");

        Anomaly anomalyPurchase = new Anomaly(
                "purchase",
                String.valueOf(df.format(purchaseRecord.getTimestamp())),
                purchaseRecord.getId(),
                String.valueOf(purchaseRecord.getAmount()),
                String.format("%.2f", user.getMean()),
                String.format("%.2f", user.getStd())
        );

        try {
            FileWriter fw = new FileWriter(file, true);
            String output = JsonBinder.INSTANCE.getObjectMapper().writeValueAsString(anomalyPurchase) + "\n";
            log.info(output);
            fw.write(output);
            fw.flush();
            fw.close();
        } catch (Exception ex) {
            log.error("Failed to write the record to output file {}", ex);
        }
    }

}
