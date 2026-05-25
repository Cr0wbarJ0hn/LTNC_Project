package com.example.auctionapp.model;

import com.google.gson.Gson;

public class NetworkMessage {
    private static final Gson gson = new Gson(); // Reusable Gson instance

    public String action;
    public String data;
    public boolean success;


    public NetworkMessage(String action, String data, boolean success) {
        this.action = action;
        this.data = data;
        this.success = success;
    }


    public String toJson() {
        return gson.toJson(this);
    }

    public static NetworkMessage fromJson(String jsonStr) {
        return gson.fromJson(jsonStr, NetworkMessage.class);
    }
}