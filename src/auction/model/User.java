package auction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class User implements Serializable{
    private static final long serialVersionUID = 1L;

    private String id;
    private String userName;
    private String email;
    private String passWordHash;
    private String role; // "Admin, Bidder, Seller"
    private boolean isActive;
    private LocalDateTime createdAt;

    public User(String email, String userName, String passWordHash, String role){
        this.userName = userName;
        this.email = email;
        this.passWordHash = passWordHash;
        this.role = role;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    protected User() {
    }

    // getter setter
    public String getId(){return id;};
    public void setUserName(String userName){this.userName = userName;}
    public String getUserName(){return userName;}
    public void setEmail(String email){this.email = email;}
    public String getEmail(){return email;}
    public void setPassWordHash(String passWordHash){this.passWordHash = passWordHash;}
    public String getPassWordHash(){return passWordHash;}
    public String getRole(){return role;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public boolean isActive(){return isActive;}
    public void setActive(boolean isActive){this.isActive = isActive;}



}