package com.example.auctionapp.model;

import com.google.gson.JsonObject;

import java.io.PrintWriter;

public class Admin extends User {


    public Admin(String fullName, String phone, String email, String username, String password) {
        super(fullName, phone, email, username, password);

    }
    @Override
    public String getRole() {
        return "ADMIN";
    }

    public void requestDeleteAuction(int auctionId) {
        PrintWriter out = UserSession.getOut();
        if (out == null) return;

        JsonObject request = new JsonObject();
        request.addProperty("action", "ADMIN_DELETE_AUCTION");
        request.addProperty("auctionId", auctionId);

        out.println(request.toString());
        out.flush();
        System.out.println("⚙️ [OOP] Admin @" + getUsername() + " dispatched network request to delete auction ID: " + auctionId);
    }

    public void deleteUserAccount(String targetUsername) {
        PrintWriter out = UserSession.getOut();
        if (out != null) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "ADMIN_DELETE_USER");
            request.addProperty("targetUsername", targetUsername);

            out.println(request.toString());
            out.flush();
            System.out.println("⚙️ [OOP MODEL]: Admin @" + getUsername() + " dispatched deletion command for: " + targetUsername);
        }
    }

    public void deleteAuctionFromSystem(int auctionId) {
        PrintWriter out = UserSession.getOut();
        if (out != null) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "ADMIN_DELETE_AUCTION");
            request.addProperty("auctionId", auctionId);

            out.println(request.toString());
            out.flush();
            System.out.println(" [OOP MODEL]: Admin dispatched cascade delete packet for Auction #" + auctionId);
        }
    }
}


