package com.insight.anomalyDetection;

import com.insight.anomalyDetection.model.EventType;
import com.insight.anomalyDetection.model.PurchaseRecord;
import com.insight.anomalyDetection.model.User;
import com.insight.anomalyDetection.util.JsonBinder;
import com.insight.anomalyDetection.util.Printer;
import com.insight.anomalyDetection.util.UserOperations;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by yfang on 7/2/2017.
 */

@Slf4j
public class StreamProcessor {

    public static void streamFile(Map<String, User> socialGraph, String inputFile, boolean flagStream)
            throws IOException {
        Stream<String> inputStream = Files.lines(Paths.get(inputFile));
        inputStream.forEach(line ->
                processLine(line, socialGraph, flagStream)
        );

        if(!flagStream) {
            log.debug("building initial purchase history of friends");
            UserOperations.INSTANCE.buildInitialPurchaseHistoryOfFriends(socialGraph);
        }
    }

    private static void processLine(String line, Map<String, User> socialGraph, boolean flagStream) {
        if (line.isEmpty() || line.length() == 0) {
            return;
        }

        try {
            Map<String, String> jsonMap = JsonBinder.INSTANCE.readValue(line);
            String event = jsonMap.get("event_type");
            if(event == null) {
                return;
            }
            EventType eventType = EventType.valueOf(event);

            String timestamp = jsonMap.get("timestamp");

            switch (eventType) {

                case purchase:
                    String id = jsonMap.get("id");
                    socialGraph.putIfAbsent(id, new User(id));

                    PurchaseRecord purchaseRecord = new PurchaseRecord(timestamp, jsonMap.get("amount"), id);
                    UserOperations.INSTANCE.addPurchaseDuringStreaming(socialGraph, purchaseRecord, flagStream);
                    break;

                case befriend:
                    String id1 = jsonMap.get("id1");
                    socialGraph.putIfAbsent(id1, new User(id1));

                    String id2 = jsonMap.get("id2");
                    socialGraph.putIfAbsent(id2, new User(id2));

                    UserOperations.INSTANCE.addFriendDuringStreaming(socialGraph, id1, id2, flagStream);
                    break;

                case unfriend:
                    id1 = jsonMap.get("id1");
                    socialGraph.putIfAbsent(id1, new User(id1));

                    id2 = jsonMap.get("id2");
                    socialGraph.putIfAbsent(id2, new User(id2));

                    UserOperations.INSTANCE.removeFriendDuringStreaming(socialGraph, id1, id2, flagStream);
                    break;
            }
        } catch (IOException e) {
            log.error("processLine Exception: {}", e);
        }
    }


    public static void main(String[] args) throws IOException {

        String initialFileName = "log_input/batch_log.json";
        String streamFileName = "log_input/stream_log.json";
        String outputFileName = "log_output/flagged_purchases.json";
        Map<String, User> socialGraph = new HashMap<String, User>();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(initialFileName));
        Map<String, String> firstLineMap = JsonBinder
                .INSTANCE
                .readValue(bufferedReader.readLine());
        User.D = Integer.valueOf(firstLineMap.get("D"));
        User.T = Integer.valueOf(firstLineMap.get("T"));
        log.debug("D: " + User.D + ", T: " + User.T);
        bufferedReader.close();

        log.debug("Starting to read the initial file");
        StreamProcessor.streamFile(socialGraph, initialFileName, false);

        try {
            File outputFile = new File(outputFileName);
            outputFile.delete();
            outputFile.createNewFile();
            Printer.file = outputFile;
        } catch (IOException ex) {
            log.error("Failed to create output file: {}", ex);
            return;
        }
        log.debug("Starting to read the stream file");
        StreamProcessor.streamFile(socialGraph, streamFileName, true);
    }
}
