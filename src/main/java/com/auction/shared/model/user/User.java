package com.auction.shared.model.user;

import com.auction.shared.model.Entity;

public abstract class User extends Entity {
    protected String username;// Tên đăng nhập(duy nhất)
    protected String password;
    protected String email;


    public User(String id, String name, String username, String password, String email){
        super(id,name);
        this.username=username;
        this.password=password;
        this.email=email;
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    // Phương thức trừu tuợnng để lấy vai trò(Phục vụ tính đa hình)
    //Role(Vai trò)
    public abstract String getRole();
}
