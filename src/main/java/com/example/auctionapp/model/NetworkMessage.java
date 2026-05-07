package com.example.auctionapp.model;
import com.google.gson.Gson;

public class NetworkMessage {
    public String action;
    public String data;
    public boolean success;

    // Constructor for making a new message
    public NetworkMessage(String action, String data, boolean success) {
        this.action = action;
        this.data = data;
        this.success = success;
    }
}