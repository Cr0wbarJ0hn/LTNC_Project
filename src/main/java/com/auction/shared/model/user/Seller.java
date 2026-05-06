package com.auction.shared.model.user;

//Seller(Người bán hàng)
public class Seller extends User {
    public Seller(String id, String name, String username, String password, String email) {
        super(id, name, username, password, email);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }
}
