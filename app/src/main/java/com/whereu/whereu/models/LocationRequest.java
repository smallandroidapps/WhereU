package com.whereu.whereu.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Date;

public class LocationRequest implements Parcelable {
    private String requestId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName;
    private String receiverPhoneNumber;
    private String receiverProfilePhotoUrl;
    private String status;
    private Date createdAt;
    private Date approvedAt;
    private Date expiresAt;
    private double latitude;
    private double longitude;
    private String areaName;

    public LocationRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(LocationRequest.class)
    }

    public LocationRequest(String senderId, String receiverId, String receiverPhoneNumber, String receiverProfilePhotoUrl) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.receiverProfilePhotoUrl = receiverProfilePhotoUrl;
        this.status = "pending";
        this.createdAt = new Date();
        this.expiresAt = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    }

    public LocationRequest(String senderId, String senderName, String receiverId, String receiverName, String receiverPhoneNumber, String receiverProfilePhotoUrl, long createdAt, String status, double latitude, double longitude, String areaName) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.receiverProfilePhotoUrl = receiverProfilePhotoUrl;
        this.createdAt = new Date(createdAt);
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaName = areaName;
        this.expiresAt = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhoneNumber() {
        return receiverPhoneNumber;
    }

    public void setReceiverPhoneNumber(String receiverPhoneNumber) {
        this.receiverPhoneNumber = receiverPhoneNumber;
    }

    public String getReceiverProfilePhotoUrl() {
        return receiverProfilePhotoUrl;
    }

    public void setReceiverProfilePhotoUrl(String receiverProfilePhotoUrl) {
        this.receiverProfilePhotoUrl = receiverProfilePhotoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    protected LocationRequest(Parcel in) {
        requestId = in.readString();
        senderId = in.readString();
        senderName = in.readString();
        receiverId = in.readString();
        receiverName = in.readString();
        receiverPhoneNumber = in.readString();
        receiverProfilePhotoUrl = in.readString();
        status = in.readString();
        long tmpCreatedAt = in.readLong();
        createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        long tmpApprovedAt = in.readLong();
        approvedAt = tmpApprovedAt == -1 ? null : new Date(tmpApprovedAt);
        long tmpExpiresAt = in.readLong();
        expiresAt = tmpExpiresAt == -1 ? null : new Date(tmpExpiresAt);
        latitude = in.readDouble();
        longitude = in.readDouble();
        areaName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(requestId);
        dest.writeString(senderId);
        dest.writeString(senderName);
        dest.writeString(receiverId);
        dest.writeString(receiverName);
        dest.writeString(receiverPhoneNumber);
        dest.writeString(receiverProfilePhotoUrl);
        dest.writeString(status);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1L);
        dest.writeLong(approvedAt != null ? approvedAt.getTime() : -1L);
        dest.writeLong(expiresAt != null ? expiresAt.getTime() : -1L);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(areaName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LocationRequest> CREATOR = new Creator<LocationRequest>() {
        @Override
        public LocationRequest createFromParcel(Parcel in) {
            return new LocationRequest(in);
        }

        @Override
        public LocationRequest[] newArray(int size) {
            return new LocationRequest[size];
        }
    };
}
