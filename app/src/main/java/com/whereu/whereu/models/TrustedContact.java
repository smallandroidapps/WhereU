package com.whereu.whereu.models;

import java.io.Serializable;

public class TrustedContact implements Serializable {
    private String uid;
    private String displayName;
    private String phoneNumber;
    private String profilePhotoUrl;
    private String status;
    private String requestId;

    public TrustedContact() {
        // Default constructor required for calls to DataSnapshot.getValue(TrustedContact.class)
    }

    public TrustedContact(String uid, String displayName, String phoneNumber) {
        this.uid = uid;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
    }

    public TrustedContact(String displayName, String phoneNumber, String uid, String profilePhotoUrl, String status) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.uid = uid;
        this.profilePhotoUrl = profilePhotoUrl;
        this.status = status;
    }

    public TrustedContact(String displayName, String phoneNumber, String uid, String profilePhotoUrl, String status, String requestId) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.uid = uid;
        this.profilePhotoUrl = profilePhotoUrl;
        this.status = status;
        this.requestId = requestId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
}