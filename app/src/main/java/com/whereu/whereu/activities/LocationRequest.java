package com.whereu.whereu.activities;

public class LocationRequest {
    private String senderUserId;
    private String senderDisplayName;
    private String targetUserId;
    private long timestamp;
    private String status; // e.g., "pending", "accepted", "rejected"

    public LocationRequest() {
        // Required for Firestore deserialization
    }

    public LocationRequest(String senderUserId, String senderDisplayName, String targetUserId) {
        this.senderUserId = senderUserId;
        this.senderDisplayName = senderDisplayName;
        this.targetUserId = targetUserId;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
    }

    // Getters
    public String getSenderUserId() {
        return senderUserId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    // Setters (optional, depending on whether you want to update these fields directly)
    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}