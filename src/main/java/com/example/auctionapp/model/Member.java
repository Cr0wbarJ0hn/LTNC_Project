package com.example.auctionapp.model;

public class Member extends User {
    private String address;
    public Member (String fullName, String phone, String email, String username, String password, String address){
        super(fullName, phone, email, username, password);
        this.address = address;

}

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    } @Override
    public String getRole(){
        return "Member";
    }

}
