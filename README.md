# Table of Contents
1. [Working Environment](README.md#working-environment)
2. [Summary of Approach](README.md#details-of-implementation)
3. [Explanation of Source Code](README.md#explanation-of-source-code)
4. [Optimization and Discussion](README.md#optimization-and-discussion)

# 1. Working Environment
**1.1** Java 1.8 was used for this challenge. Apache Maven 3.5 was used to compile and execute the java source code.

**1.2** jackson 2.9.0.pr4 was used for JSON parsing. lombok 1.16.16 was used for cleaner code. junit 4.12 was used for Unit testing. log4j and slf4j were used to log information. All the dependency were specified in the maven file pom.xml. The run.sh will automatically copy the pom.xml from src folder to the root folder and start to compile and execute. In the rare case the maven is not installed, the executable jar file which was compiled on my machine can directly called as "java -jar target/anomalyDetection-1.0-jar-with-dependencies.jar".

# 2. Summary of Approach
A graph built with user as vertex and relationship as edges was used. A BFS algorithm was used to traverse every user in userOne's D-degree network. In addition to user's own T latest purchase records, T latest purchase records from user's D-degree network were also saved for every user. 
 
# 3. Explanation of Source Code
The source code consists of a main class called StreamerProcessor and two packages (model and util). 

**3.1** The main method of StreamProcessor will read the log file and parsing the JSON object line by line. All users were saved to a HashMap with the ID as the key. Depending on the event_type, different methods in the UserOpreations class will be called.   

**3.2** There are four classes in the model package, User, PurchaseRecord, Anomaly and EventType. The first two are the major types. Each User will have the id, a list of friends, a list of self's T latest purchase records, and a list of the T latest purchase records from user's D-level social network. PurchaseRecord will save the purchase amount, timestamp, user id and a logid. Anomaly is for the output purpose, and EventType is a eNum class.

**3.3** There are also four classes in the util package, IdGenerator, JsonBinder, Printer, UserOperations. They are all singleton classes. IdGenerator is a counter to create the logid for the PurchaseRecord. JsonBinder is to parse the JSON object for input/output files. Printer is to print the output file. UserOperations have 4 public methods that can be called. 

***3.3.1*** addPurchase will add the purchase to the selfPurchaseHistory, and return in the case of initialization phase. In the streaming phase, it will further determine if the purchase is anomalous, and send the purchase to the user's D-degree network to update their purchaseHistoryOfFriends.

***3.3.2*** addFriend will add the relationship to the social graph, and return in the case of initialization phase. In the streaming phase, it will further update the purchaseHistoryOfFriends of userOne by aggregating the selfPurchaseHistory from userTwo's D-1 degree network and update the purchaseHistoryOfFriends of userTwo's D-1 degree network by merging userOne's selfPurchaseHistory. The above process will be repeated for userTwo.

***3.3.3*** removeFriend will remove the relationship in the social graph, and return in the case of initialization phase. In the streaming phase, it will further clear and rebuild the purchaseHistoryOfFriends of everyone in userOne and userTwo's D-degree network using their new relationships.

***3.3.4*** buildInitialPurchaseHistoryOfFriends will only be called in the initialization phase after all records in the initial file have been processed. it will build the purchaseHistoryOfFriends for each user.

**3.4** Four Junit tests were designed for the four public methods in the UserOperations to validate the implementation. 

# 4. Optimization and discussion
### 4.1 Time complexity during streaming
It was observed from the sample data that the majority of the data is the purchase type, and only a small percentage is the befriend type. Very rarely it is the unfriend type. This also coincides with the daily experience. By saving the purchaseHistoryOfFriends, the anomaly purchase can be flagged immediately and send the current record to user's D-degree network will cost O(K) time, where K is the size of user's D-degree network. When two users become friend, the purchaseHistoryOfFriends will need to be updated and everyone in the other user's D-1 degree network will also need to update their purchaseHistoryOfFriends. This can be done in one BFS operation and thus has the time complexity of O (K*T). When two user un-friend, everyone in these two user's D-degree network will need to rebuild the purchaseHistoryOfFriends. This is an O(K^2*T) operation.

If the frequency of each type had changed significantly, for instance the majority of records become be-friend and un-friend type, then other implementation would be desirable, such as only traverse the last T purchases from user's D-degree social network when a purchase record is received (an O(K*T) operation).

### 4.2 Initialization phase
Due to high cost of updating the purchaseHistoryOfFriends, it was not designed to update the purchaseHistoryOfFriends in real-time for the initialization phase. A specific method to build it was called, after the full initial file was read and all user and their relationships and selfPurchaseHistory were put into the Map. 

### 4.3 ArrayList and LinkedList
The insertion and deletion of friends happen not so often and need to access random elements in the friend list. Thus the ArrayList is chosen for the list of friend. On the other hand, when a purchase record is received, the insertion and deletion always happen at the head and tail of the list of PurchaseRecords, and thus the LinkedList is chosen for the list of PurchaseRecords. To accommodate this, merging list and calculating mean and standard deviation was implemented without calling list.get(n) method which is inefficient for LinkedList. This implementation can also change if the nature of the data changes. 

### 4.4 Data order
It is assumed here the data arrives in order and when two events have the same timestamp, the one listed first is considered the earlier one. Hence, a logid was created to save the order if the two timestamp is the same. The logid is implemented with int32 type. It can support 2^31-1 records, roughly corresponding to 200GB data (calculated from the sample data where 44MB data have 500,000 records). In the real use case, when the record number approaches the limit of int32, we can change this to an int64 type or we can traverse all the saved purchase record and subtract the minimum.

A insertion method using binary search algorithm was also written in the source code in case the data didn't arrive in order due to network delay, machine glitch, etc. It can be called with a boolean flag.

### 4.5 Lazy and eager implementation for the calculation of mean and standard deviation 
Here, the lazy implementation is used, i.e. the calculation only happens when it is needed to determine if a purchase is anomalous. The eager implementation would be to calculate it everytime the list was updated. This was also designed based on the assumed use case that we wouldn't receive the large amount of consecutive records from the same user. If this had happen, an eager implementation will be more desirable.
 
