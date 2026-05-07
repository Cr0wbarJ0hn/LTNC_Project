package auction.model;
import java.util.ArrayList;
import java.util.List;

public class Seller extends User{
    private static final long serialVersionID = 1L;

    private String storeName;
    private double rating;    // 0.0 - 6.0
    private List<String> itemIDs;
    private boolean isVerified;

    public Seller(String userName, String email, String passWordHash, String storeName){
        super(email, userName, passWordHash, "Seller");
        this.storeName = storeName;
        this.rating = rating;
        this.itemIDs = new ArrayList<>();
        this.isVerified = false;
    }

    public String getStoreName(){return storeName;}
    public void setStoreName(String storeName){this.storeName = storeName;}
    public double getRating(){return rating;}
    public void setRating(double rating){this.rating = rating;}
    public boolean isVerified(){return isVerified;}
    public void setVerified(boolean v){this.isVerified = v;}
    public List<String> getItemIDs(){return itemIDs;}
    public void addItemIDs(String itemID){itemIDs.add(itemID);}


}
