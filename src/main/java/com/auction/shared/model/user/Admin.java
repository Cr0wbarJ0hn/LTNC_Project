package com.auction.shared.model.user;

//Admin(Quản trị hệ thống)
public class Admin extends User {
    public Admin(String id, String name, String username, String password, String email) {
        super(id, name, username, password, email);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
