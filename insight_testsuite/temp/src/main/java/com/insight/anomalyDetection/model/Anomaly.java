package com.insight.anomalyDetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@JsonPropertyOrder({"event_type", "timestamp", "id", "amount", "mean", "std"})
public class Anomaly {
    @JsonProperty("event_type")
    String eventType;
    @JsonProperty("timestamp")
    String timestamp;
    @JsonProperty("id")
    String id;
    @JsonProperty("amount")
    String amount;
    @JsonProperty("mean")
    String mean;
    @JsonProperty("std")
    String std;
}
