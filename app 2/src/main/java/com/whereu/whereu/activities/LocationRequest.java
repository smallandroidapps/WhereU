package com.whereu.whereu.activities;

public class LocationRequest {
    private String requestId;
    private String fromUserId;
    private String senderDisplayName;
    private String toUserId;
    private long timestamp;
    private String status; // e.g., "pending", "accepted", "rejected"
    private double latitude;
    private double longitude;
    private long approvedAt;
    private String areaName;

    public LocationRequest() {
        // Required for Firestore deserialization
    }

    public LocationRequest(String requestId, String fromUserId, String senderDisplayName, String toUserId) {
        this.requestId = requestId;
        this.fromUserId = fromUserId;
        this.senderDisplayName = senderDisplayName;
        this.toUserId = toUserId;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.approvedAt = 0;
        this.areaName = "";
    }

    // Getters
    public String getRequestId() {
        return requestId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public String getToUserId() {
        return toUserId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getApprovedAt() {
        return approvedAt;
    }

    public String getAreaName() {
        return areaName;
    }

    // Setters (optional, depending on whether you want to update these fields directly)
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setApprovedAt(long approvedAt) {
        this.approvedAt = approvedAt;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }
}