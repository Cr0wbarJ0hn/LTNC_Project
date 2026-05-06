package auction.model;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {
    private static final long serialVersionID = 1L;

    private double walletBalance;
    private List<String> wonAuctionIds;
    private List<String> activeBidsID;

    public Bidder(String email, String userName, String passWordHash) {
        super(email, userName, passWordHash, "Bidder");
        this.walletBalance = 0.0;
        this.wonAuctionIds = new ArrayList<>();
        this.activeBidsID = new ArrayList<>();
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }

    public double addFunds(double amount) {
        return walletBalance += amount;
    }

    public boolean deductFunds(double amount) {
        if (amount > walletBalance) {
            return false;
        }
        walletBalance -= amount;
        return true;
    }
    public List<String> getWonAuctionIds()       { return wonAuctionIds; }
    public List<String> getActiveBidIds()        { return activeBidsID; }
    public void addWonAuction(String auctionId)  { wonAuctionIds.add(auctionId); }
    public void addActiveBid(String bidId)       { activeBidsID.add(bidId); }

}