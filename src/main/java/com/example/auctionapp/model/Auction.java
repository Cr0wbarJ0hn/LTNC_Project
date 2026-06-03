package com.example.auctionapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private int auctionId;
    private Items item;
    private Member seller;
    private double startingPrice;
    private double currentHighestBid;
    private double priceIncrement;
    private LocalDateTime endTime;
    private String status;
    private List<BidTransaction> bidHistory;
    public Auction(Items item, Member seller, double startingPrice, double priceIncrement, LocalDateTime endTime){
        this.item = item;
        this.seller = seller;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.priceIncrement = priceIncrement;
        this.endTime = endTime;
        this.status = "ACTIVE";
        this.bidHistory = new ArrayList<>();
        this.priceIncrement = priceIncrement;
    }
    public boolean placeBid(BidTransaction newBid) {
        if (!status.equals("ACTIVE")) {
            System.out.println("Bid rejected: This auction is no longer active.");
            return false;
        }
        if (LocalDateTime.now().isAfter(endTime)) {
            System.out.println("Bid rejected: This auction has ended.");
            return false;
        }
        if (newBid.getBidder().getUsername().equals(seller.getUsername())) {
            System.out.println("Bid rejected: You cannot bid on your own item");
            return false;
        }
        if (newBid.getBidAmount() >= currentHighestBid + priceIncrement) {
            bidHistory.add(newBid);
            currentHighestBid = newBid.getBidAmount();
            return true;
        } else {
            System.out.println("Bid rejected: Bid price must be higher than:" + (currentHighestBid + priceIncrement));
            return false;

        }
    } public boolean cancelAuction(Member user) {
        if (!status.equals("ACTIVE")) {
            System.out.println("Cancel rejected: This auction is no longer active");
            return false;
        } if (LocalDateTime.now().isAfter(endTime)){
            System.out.println("Cancel rejected: This auction has already ended");
            return false;
        } if (user.getUsername().equals(seller.getUsername())){
            System.out.println("Cancel request accepted: The auction have been canceled");
            this.status = "CANCELLED";
            return true;
        } else {
            System.out.println("Cancel rejected: You are not the seller of this auction");
            return false;
        }
    }
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public Items getItem() { return item; }
    public void setItem(Items item) { this.item = item; }

    public Member getSeller() { return seller; }
    public void setSeller(Member seller) { this.seller = seller; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public double getPriceIncrement() {
        return priceIncrement;
    }

    public void setPriceIncrement(double priceIncrement) {
        this.priceIncrement = priceIncrement;
    }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public void setBidHistory(List<BidTransaction> bidHistory) { this.bidHistory = bidHistory; }
}



