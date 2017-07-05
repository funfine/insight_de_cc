package com.insight.anomalyDetection.util;

import com.insight.anomalyDetection.model.PurchaseRecord;
import com.insight.anomalyDetection.model.User;

import java.util.*;

/**
 * Created by yfang on 7/1/2017.
 * This class includes four public methods which are called from StreamProcessor.
 * 1. addPurchase
 * 2. addFriend
 * 3. removeFriend
 * 4. buildInitialPurchaseHistoryOfFriends (only for the initialization phase)
 * For the initialization phase (determined by the boolean flagStream), the first three methods will not update the purchaseHistoryOfFriends in real time.
 * Method buildInitialPurchaseHistoryOfFriends will be called after the full initial file has been processed to update purchaseHistoryOfFriends for every user.
 */

public enum UserOperations {

    INSTANCE;

    // When a purchase record event is received, we do:
    // 1. Add the purchase to the selfPurchaseHistory
    // For the initialization phase, we return.
    // For the streaming phase, we further do:
    // 2. Determine if it is an anomaly purchase
    // 3. Send this record to the user's D-degree network to update their purchaseHistoryOfFriends
    public void addPurchase(Map<String, User> socialGraph, PurchaseRecord purchaseRecord, boolean flagStream) {
        User user = socialGraph.get(purchaseRecord.getId());

        insertRecord(user.getSelfPurchaseHistory(), purchaseRecord);

        if (!flagStream){
            return;
        }

        if (flagAnomaly(user, purchaseRecord)) {
            Printer.INSTANCE.print(user, purchaseRecord);
        }

        Queue<String> q = new LinkedList<>();
        HashSet<String> visited = new HashSet<>();
        q.offer(user.getId());
        visited.add(user.getId());

        for (int i = 0; i < User.D; i++) {
            int size = q.size();
            for (int j = 0; j < size; j++) {
                User cur = socialGraph.get(q.poll());
                for (User newFriend : cur.getFriends()) {
                    if(!visited.add(newFriend.getId())) {
                        continue;
                    }
                    q.offer(newFriend.getId());
                    insertRecord(newFriend.getPurchaseHistoryOfFriends(), purchaseRecord);
                }
            }
        }
    }

    // When a befriend event is received, we do:
    // 1. Add the relationship to the social graph
    // For the initialization phase, we return.
    // For the streaming phase, we further do:
    // 2.1 Update the purchaseHistoryOfFriends of userOne by aggregating the selfPurchaseHistory from userTwo's D-1 degree network
    // 2.2 Update the purchaseHistoryOfFriends of userTwo's D-1 degree network by merging userOne's selfPurchaseHistory
    // 3 Vice versa for userTwo
    public void addFriend(Map<String,User>socialGraph, String id1, String id2, boolean flagStream){
        User userOne = socialGraph.get(id1);
        User userTwo = socialGraph.get(id2);
        if (userOne.getFriends().contains(userTwo) || userTwo.getFriends().contains(userOne)){
            throw new IllegalArgumentException("You are be-friending users that already have relationship");
        }
        userOne.getFriends().add(userTwo);
        userTwo.getFriends().add(userOne);

        if (!flagStream){
            return;
        }
        bfsAddFriendDuringStreamingHelper(socialGraph, id1, id2, User.D);
        bfsAddFriendDuringStreamingHelper(socialGraph, id2, id1, User.D);
    }

    private void bfsAddFriendDuringStreamingHelper(Map<String,User>socialGraph, String id1, String id2, int depth) {
        User userOne = socialGraph.get(id1);
        Queue<String> q = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        q.offer(id2);
        visited.add(id1);
        visited.add(id2);

        for (int i = 0; i < depth; i++) {
            int size = q.size();
            for (int j = 0; j < size; j++) {
                User cur = socialGraph.get(q.poll());
                userOne.setPurchaseHistoryOfFriends(mergeList(userOne.getPurchaseHistoryOfFriends(), cur.getSelfPurchaseHistory()));
                cur.setPurchaseHistoryOfFriends(mergeList(userOne.getSelfPurchaseHistory(), cur.getPurchaseHistoryOfFriends()));
                if (i == depth - 1) {
                    continue;
                }
                for (User newFriend : cur.getFriends()){
                    if (!visited.add(newFriend.getId())) {
                        continue;
                    }
                    q.offer(newFriend.getId());
                }
            }
        }
    }

    // When an un-friend event is received, we do:
    // 1. Remove the relationship in the social graph
    // For the initialization phase, we return.
    // For the streaming phase, we further do:
    // 2. Clear and Update everyone's purchaseHistoryOfFriends in userOne and userTwo's D-degree network using their new relationships
    public void removeFriend(Map<String,User> socialGraph, String id1, String id2, boolean flagStream) {
        int depth = User.D;
        User userOne = socialGraph.get(id1);
        User userTwo = socialGraph.get(id2);

        if (!userOne.getFriends().contains(userTwo) || !userTwo.getFriends().contains(userOne)){
            throw new IllegalArgumentException("You are un-friending users that don't have relationship");
        }
        userOne.getFriends().remove(userTwo);
        userTwo.getFriends().remove(userOne);

        if (!flagStream){
            return;
        }

        Queue<String> q = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        q.offer(id1);
        q.offer(id2);
        visited.add(id1);
        visited.add(id2);

        for (int i = 0; i < depth; i++) {
            int size = q.size();
            for (int j = 0; j < size; j++) {
                User cur = socialGraph.get(q.poll());
                updatePurchaseHistoryOfFriends(socialGraph, cur.getId(), depth);
                if (i == depth - 1) {
                    continue;
                }
                for (User newFriend : cur.getFriends()) {
                    if (!visited.add(newFriend.getId())) {
                        continue;
                    }
                    q.offer(newFriend.getId());
                }
            }
        }
    }

    //For the initialization phase (flagged by flagStream = false), we first build the users and the relationships in the socialGraph and put the selfPurchaseHistory only.
    //After all events in the initial log have been processed, we start to build the purchaseHistoryOfFriends for each user in the following function.
    public void buildInitialPurchaseHistoryOfFriends(Map<String,User> socialGraph){
        for (String id : socialGraph.keySet()){
            UserOperations.INSTANCE.updatePurchaseHistoryOfFriends(socialGraph, id, User.D);
        }
    }

    //Update user's PurchaseHistoryOfFriends using the selfPurchaseHistory from user's depth-degree network
    private void updatePurchaseHistoryOfFriends(Map<String,User>socialGraph, String id, int depth) {
        User user = socialGraph.get(id);
        user.getPurchaseHistoryOfFriends().clear();
        Queue<String> q = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        q.offer(id);
        visited.add(user.getId());

        for (int i = 0; i < depth; i++) {
            int size = q.size();
            for (int j = 0; j < size; j++) {
                User cur = socialGraph.get(q.poll());
                for (User newFriend : cur.getFriends()) {
                    if (!visited.add(newFriend.getId())) {
                        continue;
                    }
                    q.offer(newFriend.getId());
                    user.setPurchaseHistoryOfFriends(mergeList(user.getPurchaseHistoryOfFriends(), newFriend.getSelfPurchaseHistory()));
                }
            }
        }
    }

    //Here, assume that the stream data arrive in order. The new record will always be added to the head of list.
    private void insertRecord(List<PurchaseRecord> list, PurchaseRecord purchaseRecord) {
        if (list.size() == User.T) {
            list.remove(User.T - 1);
        }
        list.add(0, purchaseRecord);
    }

    private boolean flagAnomaly(User user, PurchaseRecord purchaseRecord) {
        if(user.updateStatistics()) {
            return purchaseRecord.getAmount() > (user.getMean() + 3 * user.getStd());
        } else {
            return false;
        }
    }

    private List<PurchaseRecord> mergeList(List<PurchaseRecord> listOne, List<PurchaseRecord> listTwo){
        if (listOne == null || listOne.size() ==0) {
            return listTwo;
        }

        if (listTwo == null || listTwo.size() ==0) {
            return listOne;
        }

        List<PurchaseRecord> result = new LinkedList<PurchaseRecord>();

        Iterator<PurchaseRecord> it1 = listOne.iterator();
        Iterator<PurchaseRecord> it2 = listTwo.iterator();

        PurchaseRecord p1 = it1.hasNext() ? it1.next() : null;
        PurchaseRecord p2 = it2.hasNext() ? it2.next() : null;

        while (result.size() < User.T) {
            if(p1 != null && p2 != null) {
                int compare = p1.compareTo(p2);
                if(compare == 0) {
                    result.add(p1);
                    p1 = it1.hasNext() ? it1.next() : null;
                    p2 = it2.hasNext() ? it2.next() : null;
                } else if(compare < 0) {
                    result.add(p2);
                    p2 = it2.hasNext() ? it2.next() : null;
                } else {
                    result.add(p1);
                    p1 = it1.hasNext() ? it1.next() : null;
                }
            } else if(p1 != null) {
                result.add(p1);
                p1 = it1.hasNext() ? it1.next() : null;
            } else if(p2 != null) {
                result.add(p2);
                p2 = it2.hasNext() ? it2.next() : null;
            } else {
                return result;
            }
        }
        return result;
    }

    //when the data do not arrive in order, the record needs to be inserted based on timestamp which requires O(logT) time complexity.
    private void insertRecord(List<PurchaseRecord> list, PurchaseRecord purchaseRecord, boolean inOrder) {
        if (!inOrder) {
            int low = 0, high = list.size();
            while (low != high) {
                int mid = low + (high - low) / 2;
                if (list.get(mid).compareTo(purchaseRecord) <= 0) {
                    low = mid + 1;
                }
                else {
                    high = mid;
                }
            }
            list.add(low, purchaseRecord);
        } else {
            list.add(purchaseRecord);
        }
    }
}
