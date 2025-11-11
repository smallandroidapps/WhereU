package com.whereu.whereu.models;

public class SavedCard {
    private String masked; // e.g., **** **** **** 1234
    private String brand; // e.g., VISA, MasterCard
    private Integer expMonth; // 1-12
    private Integer expYear; // e.g., 2027

    public SavedCard() {}

    public String getMasked() { return masked; }
    public void setMasked(String masked) { this.masked = masked; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public Integer getExpMonth() { return expMonth; }
    public void setExpMonth(Integer expMonth) { this.expMonth = expMonth; }

    public Integer getExpYear() { return expYear; }
    public void setExpYear(Integer expYear) { this.expYear = expYear; }
}

