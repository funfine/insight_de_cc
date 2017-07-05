package com.insight.anomalyDetection.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * Created by yfang on 7/1/2017.
 */

@Getter
@RequiredArgsConstructor
public class User {
    public static int T;
    public static int D;

    private final String id;

    // The insertion and deletion of friends happens rarely and need to access random elements in the friend list.
    // Thus the ArrayList is chosen.
    private List<User> friends = new ArrayList<>();

    // The insertion and deletion always happens at the head and tail of the list of PurchaseRecords.
    // Merge List and calculate mean and statistics can be achieved with an iterator.
    // Thus the LinkedList is chosen.
    private List<PurchaseRecord> selfPurchaseHistory = new LinkedList<>();
    @Setter
    private List<PurchaseRecord> purchaseHistoryOfFriends = new LinkedList<>();
    private Double mean = 0.0;
    private Double std = 0.0;

    public boolean updateStatistics() {
        //
        if(purchaseHistoryOfFriends == null || purchaseHistoryOfFriends.size() < 2 ) {
            return false;
        }

        double sum = purchaseHistoryOfFriends.stream()
                .mapToDouble(PurchaseRecord::getAmount)
                .sum();

        mean = sum / purchaseHistoryOfFriends.size();

        double sumOfSquare = purchaseHistoryOfFriends.stream()
                .mapToDouble(PurchaseRecord::getAmount)
                .map(amount -> Math.pow(amount - mean, 2))
                .sum();

        std = Math.sqrt((sumOfSquare / purchaseHistoryOfFriends.size()));

        return true;
    }
}
