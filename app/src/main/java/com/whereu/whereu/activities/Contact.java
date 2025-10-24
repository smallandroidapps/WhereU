package com.whereu.whereu.activities;

public class Contact {
    private String name;
    private String phoneNumber;
    private String status; // e.g., "not_requested", "pending", "approved", "denied", "expired"

    public Contact(String name, String phoneNumber, String status) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getStatus() {
        return status;
    }
}