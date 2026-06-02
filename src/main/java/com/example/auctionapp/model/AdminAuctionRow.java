package com.example.auctionapp.model;

public class AdminAuctionRow {
    private int id;
    private String itemName;
    private double currentPrice;
    private String seller;

    // Constructor
    public AdminAuctionRow(int id, String itemName, double currentPrice, String seller) {
        this.id = id;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.seller = seller;
    }

    // =========================================================================
    // 🌟 GETTERS (JavaFX PropertyValueFactory requires these to populate rows)
    // =========================================================================
    public int getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getSeller() {
        return seller;
    }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setSeller(String seller) { this.seller = seller; }
}