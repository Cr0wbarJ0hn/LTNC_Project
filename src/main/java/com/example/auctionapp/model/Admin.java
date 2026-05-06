package com.example.auctionapp.model;

public class Admin extends User {


    public Admin(String fullName, String phone, String email, String username, String password) {
        super(fullName, phone, email, username, password);

    }
    @Override
    public String getRole() {
        return "ADMIN";
    }
}

