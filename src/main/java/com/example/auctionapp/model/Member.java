package com.example.auctionapp.model;

import com.google.gson.JsonObject;
import java.io.PrintWriter;

public class Member extends User {
    private String address;

    public Member(String fullName, String phone, String email, String username, String password, String address) {
        super(fullName, phone, email, username, password);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getRole() {
        return "USER";
    }
    public void editItemListing(int itemId, String name, String type, String condition, String description) {
        PrintWriter out = UserSession.getOut();
        if (out != null) {
            JsonObject req = new JsonObject();
            req.addProperty("action", "SELLER_UPDATE_ITEM");
            req.addProperty("itemId", itemId);
            req.addProperty("itemName", name);
            req.addProperty("itemType", type);
            req.addProperty("itemCondition", condition);
            req.addProperty("description", description);

            out.println(req.toString());
            out.flush();
            System.out.println("📤 [OOP Domain]: Member '" + getUsername() + "' dispatched update for item ID " + itemId);
        } else {
            System.err.println("🚨 Network Error: No active connection stream inside UserSession cache.");
        }
    }
}