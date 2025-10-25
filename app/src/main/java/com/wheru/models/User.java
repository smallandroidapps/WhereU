package com.wheru.models;

import java.util.Date;

public class User {
    private String userId;
    private String displayName;
    private String email;
    private String mobileNumber;
    private String profilePhotoUrl;
    private String accountType;
    private Date registeredAt;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String userId, String displayName, String email, String mobileNumber, String profilePhotoUrl, String accountType, Date registeredAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.profilePhotoUrl = profilePhotoUrl;
        this.accountType = accountType;
        this.registeredAt = registeredAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Date getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Date registeredAt) {
        this.registeredAt = registeredAt;
    }
}