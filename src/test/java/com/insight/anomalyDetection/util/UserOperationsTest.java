package com.insight.anomalyDetection.util;

import com.insight.anomalyDetection.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Created by yfang on 7/3/2017.
 */
public class UserOperationsTest {
    private User userOne;
    private User userTwo;
    private User userThree;
    private User userFour;
    private Map<String, User> socialGraph;

    private PurchaseRecord purchaseRecordOne;
    private PurchaseRecord purchaseRecordTwo;
    private PurchaseRecord purchaseRecordThree;
    private PurchaseRecord purchaseRecordFour;

    @Before
    public void init(){
        User.D = 2;
        User.T = 10;
        userOne = new User("1");
        userTwo = new User ("2");
        userThree = new User ("3");
        userFour = new User ("4");

        purchaseRecordOne = new PurchaseRecord("2017-06-13 11:33:01", "16.83", "1");
        purchaseRecordTwo = new PurchaseRecord("2017-06-13 11:33:01", "59.28", "2");
        purchaseRecordThree = new PurchaseRecord("2017-06-13 11:33:01", "11.20", "3");
        purchaseRecordFour = new PurchaseRecord("2017-06-13 11:33:02", "1601.83", "4");

        userOne.getSelfPurchaseHistory().add(purchaseRecordOne);
        userTwo.getSelfPurchaseHistory().add(purchaseRecordTwo);
        userThree.getSelfPurchaseHistory().add(purchaseRecordThree);
        userFour.getSelfPurchaseHistory().add(purchaseRecordFour);
    }

    @Test
    public void buildInitialFriendsHistory() throws Exception {
        Map<String, User> socialGraph = new HashMap<String, User>();
        socialGraph.put("1", userOne);
        socialGraph.put("2", userTwo);
        socialGraph.put("3", userThree);
        socialGraph.put("4", userFour);

        userOne.getFriends().add(userTwo);
        userTwo.getFriends().add(userOne);
        userTwo.getFriends().add(userThree);
        userThree.getFriends().add(userTwo);
        userOne.getFriends().add(userThree);
        userThree.getFriends().add(userOne);
        userFour.getFriends().add(userThree);
        userThree.getFriends().add(userFour);

        UserOperations.INSTANCE.buildInitialPurchaseHistoryOfFriends(socialGraph);

        assertThat(userOne.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordFour)));
        assertThat(userOne.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordThree)));
        assertThat(userOne.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordTwo)));

        assertThat(userTwo.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordFour)));
        assertThat(userTwo.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordThree)));
        assertThat(userTwo.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordOne)));

        assertThat(userThree.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordFour)));
        assertThat(userThree.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordTwo)));
        assertThat(userThree.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordOne)));

        assertThat(userFour.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordThree)));
        assertThat(userFour.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordTwo)));
        assertThat(userFour.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordOne)));
   }

    @Test
    public void addPurchaseDuringStreaming() throws Exception {
        Map<String, User> socialGraph = new HashMap<String, User>();

        socialGraph.put("1", userOne);
        socialGraph.put("2", userTwo);
        socialGraph.put("3", userThree);
        socialGraph.put("4", userFour);

        userOne.getFriends().add(userTwo);
        userTwo.getFriends().add(userOne);
        userTwo.getFriends().add(userThree);
        userThree.getFriends().add(userTwo);
        userOne.getFriends().add(userThree);
        userThree.getFriends().add(userOne);
        userFour.getFriends().add(userThree);
        userThree.getFriends().add(userFour);
        UserOperations.INSTANCE.buildInitialPurchaseHistoryOfFriends(socialGraph);

        PurchaseRecord purchaseRecordFive = new PurchaseRecord("2017-06-13 11:33:02", "161.83", "4");
        UserOperations.INSTANCE.addPurchase(socialGraph, purchaseRecordFive,true);

        assertThat(userOne.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordFive)));
        assertThat(userOne.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordFour)));
        assertThat(userOne.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordThree)));
        assertThat(userOne.getPurchaseHistoryOfFriends().get(3), is(equalTo(purchaseRecordTwo)));

        assertThat(userTwo.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordFive)));
        assertThat(userTwo.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordFour)));
        assertThat(userTwo.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordThree)));
        assertThat(userTwo.getPurchaseHistoryOfFriends().get(3), is(equalTo(purchaseRecordOne)));

        assertThat(userThree.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordFive)));
        assertThat(userThree.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordFour)));
        assertThat(userThree.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordTwo)));
        assertThat(userThree.getPurchaseHistoryOfFriends().get(3), is(equalTo(purchaseRecordOne)));

        assertThat(userFour.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordThree)));
        assertThat(userFour.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordTwo)));
        assertThat(userFour.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordOne)));
    }

    @Test
    public void addFriendDuringStreaming() throws Exception {
        Map<String, User> socialGraph = new HashMap<String, User>();

        socialGraph.put("1", userOne);
        socialGraph.put("2", userTwo);
        socialGraph.put("3", userThree);
        socialGraph.put("4", userFour);

        userOne.getFriends().add(userTwo);
        userTwo.getFriends().add(userOne);
        userTwo.getFriends().add(userThree);
        userThree.getFriends().add(userTwo);
        userOne.getFriends().add(userThree);
        userThree.getFriends().add(userOne);

        UserOperations.INSTANCE.buildInitialPurchaseHistoryOfFriends(socialGraph);
        UserOperations.INSTANCE.addFriend(socialGraph, "3", "4", true);

        assertThat(userOne.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordFour)));
        assertThat(userOne.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordThree)));
        assertThat(userOne.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordTwo)));

        assertThat(userTwo.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordFour)));
        assertThat(userTwo.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordThree)));
        assertThat(userTwo.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordOne)));

        assertThat(userThree.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordFour)));
        assertThat(userThree.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordTwo)));
        assertThat(userThree.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordOne)));

        assertThat(userFour.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordThree)));
        assertThat(userFour.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordTwo)));
        assertThat(userFour.getPurchaseHistoryOfFriends().get(2), is(equalTo(purchaseRecordOne)));
    }

    @Test
    public void removeFriendDuringStreaming() throws Exception {
        Map<String, User> socialGraph = new HashMap<String, User>();
        socialGraph.put("1", userOne);
        socialGraph.put("2", userTwo);
        socialGraph.put("3", userThree);
        socialGraph.put("4", userFour);

        userOne.getFriends().add(userTwo);
        userTwo.getFriends().add(userOne);
        userTwo.getFriends().add(userThree);
        userThree.getFriends().add(userTwo);
        userOne.getFriends().add(userThree);
        userThree.getFriends().add(userOne);
        userFour.getFriends().add(userThree);
        userThree.getFriends().add(userFour);

        UserOperations.INSTANCE.buildInitialPurchaseHistoryOfFriends(socialGraph);
        UserOperations.INSTANCE.removeFriend(socialGraph, "3", "4", true);

        assertThat(userOne.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordThree)));
        assertThat(userOne.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordTwo)));

        assertThat(userTwo.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordThree)));
        assertThat(userTwo.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordOne)));

        assertThat(userThree.getPurchaseHistoryOfFriends().get(0), is(equalTo(purchaseRecordTwo)));
        assertThat(userThree.getPurchaseHistoryOfFriends().get(1), is(equalTo(purchaseRecordOne)));

        assertThat(userFour.getPurchaseHistoryOfFriends().size(), is(equalTo(0)));

    }
}