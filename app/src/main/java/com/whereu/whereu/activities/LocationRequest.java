package com.whereu.whereu.activities;

public class LocationRequest {
    private String senderUserId;
    private String senderDisplayName;
    private String targetUserId;
    private long timestamp;
    private String status; // e.g., "pending", "accepted", "rejected"
    private double latitude;
    private double longitude;
    private long approvedAt;
    private String areaName;

    public LocationRequest() {
        // Required for Firestore deserialization
    }

    public LocationRequest(String senderUserId, String senderDisplayName, String targetUserId) {
        this.senderUserId = senderUserId;
        this.senderDisplayName = senderDisplayName;
        this.targetUserId = targetUserId;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.approvedAt = 0;
        this.areaName = "";
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