package com.whereu.whereu.models;

public class TrustedContact {
    private String uid;
    private String displayName;
    private String phoneNumber;

    public TrustedContact() {
        // Default constructor required for calls to DataSnapshot.getValue(TrustedContact.class)
    }

    public TrustedContact(String uid, String displayName, String phoneNumber) {
        this.uid = uid;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
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
}