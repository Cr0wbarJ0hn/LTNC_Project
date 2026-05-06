package com.example.auctionapp.model;
import com.google.gson.Gson;

public class NetworkMessage {
    public String action;  // What do we want to do? e.g., "GET_CATEGORY" or "PLACE_BID"
    public String data;    // The actual payload (Category name, or Item JSON)
    public boolean success; // Did it work?

    // Constructor for making a new message
    public NetworkMessage(String action, String data, boolean success) {
        this.action = action;
        this.data = data;
        this.success = success;
    }
}