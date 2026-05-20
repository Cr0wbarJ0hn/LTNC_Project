package com.example.auctionapp.model;

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
    private AuctionStatus status; // Changed to Enum
    private List<BidTransaction> bidHistory;

    public enum AuctionStatus {
        ACTIVE, CANCELLED, COMPLETED
    }

    public Auction(Items item, Member seller, double startingPrice, double priceIncrement, LocalDateTime endTime) {
        this.item = item;
        this.seller = seller;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.priceIncrement = priceIncrement; // Now properly initialized
        this.endTime = endTime;
        this.status = AuctionStatus.ACTIVE;
        this.bidHistory = new ArrayList<>();
    }

    public synchronized void placeBid(BidTransaction newBid) throws AuctionException {
        if (status != AuctionStatus.ACTIVE) {
            throw new AuctionException("Bid rejected: This auction is no longer active.");
        }
        if (LocalDateTime.now().isAfter(endTime)) {
            this.status = AuctionStatus.COMPLETED;
            throw new AuctionException("Bid rejected: This auction has ended.");
        }
        if (newBid.getBidder().getUsername().equals(seller.getUsername())) {
            throw new AuctionException("Bid rejected: You cannot bid on your own item.");
        }

        double minimumRequiredBid = currentHighestBid + priceIncrement;
        if (newBid.getBidAmount() >= minimumRequiredBid) {
            bidHistory.add(newBid);
            currentHighestBid = newBid.getBidAmount();
        } else {
            throw new AuctionException("Bid rejected: Bid price must be at least: " + minimumRequiredBid);
        }
    }

    public synchronized void cancelAuction(Member user) throws AuctionException {
        if (status != AuctionStatus.ACTIVE) {
            throw new AuctionException("Cancel rejected: This auction is no longer active.");
        }
        if (LocalDateTime.now().isAfter(endTime)) {
            throw new AuctionException("Cancel rejected: This auction has already ended.");
        }
        if (user.getUsername().equals(seller.getUsername())) {
            this.status = AuctionStatus.CANCELLED;
        } else {
            throw new AuctionException("Cancel rejected: You are not the seller of this auction.");
        }
    }

    public synchronized void completeAuction() {
        if (LocalDateTime.now().isAfter(endTime) && status == AuctionStatus.ACTIVE) {
            this.status = AuctionStatus.COMPLETED;
        }
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public Items getItem() { return item; }


    public Member getSeller() { return seller; }
    
    public double getStartingPrice() { return startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }

    public double getPriceIncrement() { return priceIncrement; }

    public void setPriceIncrement(double priceIncrement) { this.priceIncrement = priceIncrement; }

    public LocalDateTime getEndTime() { return endTime; }

    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; } // Might want to restrict this too!

    public AuctionStatus getStatus() { return status; }

    public List<BidTransaction> getBidHistory() { return new ArrayList<>(bidHistory); }

}