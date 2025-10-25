package com.whereu.whereu.models;

public class FrequentlyRequestedModel {
    private String receiverId;
    private int requestCount;
    private long lastRequested;

    public FrequentlyRequestedModel() {
        // Default constructor required for calls to DataSnapshot.getValue(FrequentlyRequestedModel.class)
    }

    public FrequentlyRequestedModel(String receiverId, int requestCount, long lastRequested) {
        this.receiverId = receiverId;
        this.requestCount = requestCount;
        this.lastRequested = lastRequested;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public long getLastRequested() {
        return lastRequested;
    }

    public void setLastRequested(long lastRequested) {
        this.lastRequested = lastRequested;
    }
}