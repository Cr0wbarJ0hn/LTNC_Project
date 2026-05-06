package auction.model;

public class Admin extends User {
    private static final long serialVersionID = 1l;

    private String adminCode;

    public Admin(String email, String userName, String passWordHash, String adminCode){
        super(email,userName,passWordHash,"Admin");
        this.adminCode = adminCode;
    }

    public String getAdminCode(){return adminCode;}
    public void setAdminCode(String adminCode){this.adminCode = adminCode;}
}
