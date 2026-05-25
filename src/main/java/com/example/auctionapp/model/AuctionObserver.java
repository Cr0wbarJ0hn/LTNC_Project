package com.example.auctionapp.model;

public interface AuctionObserver {
    // The Subject calls this method to push new data to the Observer
    void onBidUpdated(int auctionId, double newPrice, String highestBidder);
}