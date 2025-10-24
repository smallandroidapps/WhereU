package com.whereu.whereu.models;

public class LocationRequest {
    private String senderUserId;
    private String senderDisplayName;
    private String targetUserId;

    // Default constructor required for calls to DataSnapshot.getValue(LocationRequest.class)
    public LocationRequest() {
    }

    public LocationRequest(String senderUserId, String senderDisplayName, String targetUserId) {
        this.senderUserId = senderUserId;
        this.senderDisplayName = senderDisplayName;
        this.targetUserId = targetUserId;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }
}
