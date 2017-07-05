package com.insight.anomalyDetection.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * Created by yfang on 7/1/2017.
 * D and T are declared as the static variables.
 * For each user: id, friends, selfPurchaseHistory and purchaseHistoryOfFriends are saved.
 * public method UpdateStatistics will calculate the mean and standard deviation from the purchaseHistoryOfFriends.
 */

@Getter
@RequiredArgsConstructor
public class User {
    public static int T;
    public static int D;

    private final String id;

    // The insertion and deletion of friends happen rarely and need to access random elements in the friend list.
    // Thus the ArrayList is chosen.
    private List<User> friends = new ArrayList<>();

    // The insertion and deletion always happen at the head and tail of the list of PurchaseRecords.
    // Merging list and calculating mean and standard deviation can be achieved with an iterator.
    // Thus the LinkedList is chosen.
    private List<PurchaseRecord> selfPurchaseHistory = new LinkedList<>();
    @Setter
    private List<PurchaseRecord> purchaseHistoryOfFriends = new LinkedList<>();
    private Double mean = 0.0;
    private Double std = 0.0;

    public boolean updateStatistics() {
        // If a user's social network has less than 2 purchases, purchases won't be considered anomalous at that point.
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
