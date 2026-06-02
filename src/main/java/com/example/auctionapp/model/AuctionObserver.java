package com.example.auctionapp.model;

public interface AuctionObserver {
    // Fired when someone places a higher bid
    void onBidUpdated(int auctionId, double newPrice, String highestBidder);

    // Fired when the background timer closes an auction
    void onAuctionClosed(int auctionId, String itemName, String winner, double finalPrice);

    // Used by the manager to identify which user owns this socket connection
    String getUsername();
}