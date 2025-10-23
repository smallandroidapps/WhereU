package com.whereu.whereu.activities;

public class NotificationItem {
    private String message;
    private String status;

    public NotificationItem(String message, String status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }
}