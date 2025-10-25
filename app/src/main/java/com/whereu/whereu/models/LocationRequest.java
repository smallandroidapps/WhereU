package com.whereu.whereu.models;

import android.os.Parcel;
import android.os.Parcelable;

public class LocationRequest implements Parcelable {
    private String requestId;
    private String fromUserId;
    private String toUserId;
    private String status; // pending, approved, rejected
    private double latitude;
    private double longitude;
    private String areaName;
    private long timestamp;
    private long approvedTimestamp;
    private String userName;

    public LocationRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(LocationRequest.class)
    }

    // Constructor for creating a new request
    public LocationRequest(String fromUserId, String toUserId) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
        this.approvedTimestamp = 0;
        this.latitude = 0;
        this.longitude = 0;
        this.areaName = "";
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getApprovedTimestamp() {
        return approvedTimestamp;
    }

    public void setApprovedTimestamp(long approvedTimestamp) {
        this.approvedTimestamp = approvedTimestamp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    // Parcelable implementation
    protected LocationRequest(Parcel in) {
        requestId = in.readString();
        fromUserId = in.readString();
        toUserId = in.readString();
        status = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        areaName = in.readString();
        timestamp = in.readLong();
        approvedTimestamp = in.readLong();
        userName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(requestId);
        dest.writeString(fromUserId);
        dest.writeString(toUserId);
        dest.writeString(status);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(areaName);
        dest.writeLong(timestamp);
        dest.writeLong(approvedTimestamp);
        dest.writeString(userName);
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
