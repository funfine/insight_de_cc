package com.insight.anomalyDetection.util;

public enum IdGenerator {
    INSTANCE;
    private int curId = 0;
    public int getId() {
        return curId++;
    }
}
