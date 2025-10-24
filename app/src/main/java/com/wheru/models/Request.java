package com.wheru.models;

import java.util.Date;

public class Request {
    private String requestId;
    private String senderId;
    private String receiverId;
    private String senderName;
    private RequestStatus status;
    private Date timestamp;

    public Request() {
        // Default constructor required for Firebase
    }

    public Request(String requestId, String senderId, String receiverId, String senderName, RequestStatus status, Date timestamp) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderName = senderName;
        this.status = status;
        this.timestamp = timestamp;
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

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}