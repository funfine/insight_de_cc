package com.insight.anomalyDetection.util;

import com.insight.anomalyDetection.model.PurchaseRecord;
import com.insight.anomalyDetection.model.User;

import java.util.*;

/**
 * Created by yfang on 7/2/2017.
 * This class includes four public function which are called from StreamProcessor.
 * 1. buildInitialPurchaseHistoryOfFriends (only for the initialization phase)
 * 2. addPurchase
 * 3. addFriend
 * 4. removeFriend
 */

public enum UserOperations {

    INSTANCE;

    //For the initialization phase (flagged by flagStream = false), we first build the users and the relations in the socialGraph and put the selfPurchaseHistory only.
    //After all events in the initial log have been processed, we start to build the purchaseHistoryOfFriends for each user in the following function.
    public void buildInitialPurchaseHistoryOfFriends(Map<String,User> socialGraph){
        for (String id : socialGraph.keySet()){
            UserOperations.INSTANCE.updatePurchaseHistoryOfFriends(socialGraph, id, User.D);
        }
    }

    // When receive a purchase record event during streaming, we do
    // 1. Add the purchase to the selfPurchaseHistory
    // 2. Determine if it is anomaly purchase
    // 3. Send this record to the user's D-degree network to update their purchaseHistoryOfFriends
    public void addPurchaseDuringStreaming(Map<String, User> socialGraph, PurchaseRecord purchaseRecord, boolean flagStream) {
        User user = socialGraph.get(purchaseRecord.getId());

        addToSelfHistory(user, purchaseRecord);

        //For the initialization phase, we return here.
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
                    addToFriendHistory(newFriend, purchaseRecord);
                }
            }
        }
    }

    // When receive an befriend event, we do
    // 1. Add the relationships in the social graph
    // 2.1 Update the purchaseHistoryOfFriends of userOne by aggregating the selfPurchaseHistory from userTwo's D-1 degree network
    // 2.2 Update the purchaseHistoryOfFriends of userTwo's D-1 degree network by merging userOne's selfPurchaseHistory
    // 3 Vice versa for userTwo
    public void addFriendDuringStreaming(Map<String,User>socialGraph, String id1, String id2, boolean flagStream){
        User userOne = socialGraph.get(id1);
        User userTwo = socialGraph.get(id2);
        if (userOne.getFriends().contains(userTwo) || userTwo.getFriends().contains(userOne)){
            throw new IllegalArgumentException("You are be-friending users that already have relations");
        }
        userOne.getFriends().add(userTwo);
        userTwo.getFriends().add(userOne);

        //For the initialization phase, we return here.
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

        for (int i = 0; i < depth; i++) {
            int size = q.size();
            for (int j = 0; j < size; j++) {
                User cur = socialGraph.get(q.poll());
                userOne.setPurchaseHistoryOfFriends(mergeList(userOne.getPurchaseHistoryOfFriends(), cur.getSelfPurchaseHistory()));
                cur.setPurchaseHistoryOfFriends(mergeList(userOne.getSelfPurchaseHistory(), cur.getPurchaseHistoryOfFriends()));
                if (i == depth - 1) {
                    break;
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

    // When receive a un-friend event, we do
    // 1. Remove the relationships in the social graph
    // 2. Clear and Update everyone's purchaseHistoryOfFriends in userOne and userTwo's D-degree network using their new relationships
    public void removeFriendDuringStreaming(Map<String,User> socialGraph, String id1, String id2, boolean flagStream) {
        int depth = User.D;
        User userOne = socialGraph.get(id1);
        User userTwo = socialGraph.get(id2);

        if (!userOne.getFriends().contains(userTwo) || !userTwo.getFriends().contains(userOne)){
            throw new IllegalArgumentException("You are un-friending users that don't have relations");
        }
        userOne.getFriends().remove(userTwo);
        userTwo.getFriends().remove(userOne);

        //For the initialization phase, we return here.
        if (!flagStream){
            return;
        }

        Queue<String> q = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        q.offer(id1);
        q.offer(id2);
        visited.add(userOne.getId());
        visited.add(userTwo.getId());

        for (int i = 0; i < depth; i++) {
            int size = q.size();
            for (int j = 0; j < size; j++) {
                User cur = socialGraph.get(q.poll());
                updatePurchaseHistoryOfFriends(socialGraph, cur.getId(), depth);
                if (i == depth - 1) {
                    break;
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

    private void addToSelfHistory(User user, PurchaseRecord purchaseRecord){
        if (user.getSelfPurchaseHistory().size() < User.T) {
            insertRecord(user.getSelfPurchaseHistory(), purchaseRecord);
        } else if (user.getSelfPurchaseHistory().get(User.T - 1).compareTo(purchaseRecord) <= 0) {
            user.getSelfPurchaseHistory().remove(User.T - 1);
            insertRecord(user.getSelfPurchaseHistory(), purchaseRecord);
        }
    }

    private void addToFriendHistory(User user, PurchaseRecord purchaseRecord) {
        if (user.getPurchaseHistoryOfFriends().size() < User.T) {
            insertRecord(user.getPurchaseHistoryOfFriends(), purchaseRecord);
        } else if (user.getPurchaseHistoryOfFriends().get(User.T - 1).compareTo(purchaseRecord) <= 0){
            user.getPurchaseHistoryOfFriends().remove(User.T - 1);
            insertRecord(user.getPurchaseHistoryOfFriends(), purchaseRecord);
        }
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

    //Here, assume the stream data arrives in order. The new record always added to the first record.
    private void insertRecord(List<PurchaseRecord> list, PurchaseRecord purchaseRecord) {
        list.add(0, purchaseRecord);
    }

    //when the data don't arrive in order, needs to insert the record based on timestamp which require O(logT)
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
