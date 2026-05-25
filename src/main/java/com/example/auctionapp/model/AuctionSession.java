package com.example.auctionapp.model;

import com.example.auctionapp.exception.AuctionClosedException;
import com.example.auctionapp.exception.InvalidBidException;
import com.example.auctionapp.exception.SelfBiddingException;
import com.example.auctionapp.server.DatabaseManager;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionSession {
    private final int auctionId;
    private double currentPrice;
    private final double priceIncrement;
    private final LocalDateTime endTime;
    private String highestBidder;

    // This lock satisfies your Week 7 requirement for multi-client Concurrency Handling
    private final ReentrantLock lock = new ReentrantLock();

    public AuctionSession(int auctionId, double startingPrice, double priceIncrement, LocalDateTime endTime, String highestBidder) {
        this.auctionId = auctionId;
        this.currentPrice = startingPrice;
        this.priceIncrement = priceIncrement;
        this.endTime = endTime;
        this.highestBidder = highestBidder;
    }


    public void processIncomingBid(String bidderName, double bidAmount)
            throws AuctionClosedException, InvalidBidException, SelfBiddingException {


        lock.lock();
        try {
            // Check A: Has the auction expired?
            if (LocalDateTime.now().isAfter(endTime)) {
                throw new AuctionClosedException("Bidding has already ended for Auction ID " + auctionId);
            }

            // Check B: Does the bid meet the current minimum increment requirement?
            double minimumRequiredBid = currentPrice + priceIncrement;
            if (bidAmount < minimumRequiredBid) {
                throw new InvalidBidException("Bid is too low! Minimum required is $" + minimumRequiredBid);
            }


            if (bidderName.equals(highestBidder)) {
                throw new InvalidBidException("You are already the highest bidder!");
            }

            // 🌟 FIX FOR java.sql.SQLException & REMOVED String dbResult
            // 2. Attempt the database transaction FIRST before changing local server memory values.
            try {
                // If this method fails, it will automatically throw an exception matching its rules
                DatabaseManager.executeSafeBidTransaction(this.auctionId, bidderName, bidAmount);
            } catch (java.sql.SQLException e) {
                // Catch the unhandled SQLException and wrap it cleanly so the client handler can print it
                throw new InvalidBidException("Database synchronization failed: " + e.getMessage());
            }

            // 🌟 3. Crucial Concurrency Rule: Only change server memory values IF the database approved it!
            this.currentPrice = bidAmount;
            this.highestBidder = bidderName;

            System.out.println("Success: " + bidderName + " is now the highest bidder for Auction " + auctionId + " at $" + bidAmount);

        } finally {
            // 4. Always unlock in a 'finally' block so other waiting threads can take their turn
            lock.unlock();
        }
    }

    // Getters so your Server network threads can read data to send back to clients
    public int getAuctionId() { return auctionId; }
    public double getCurrentPrice() { return currentPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getHighestBidder() { return (highestBidder != null) ? highestBidder : "No bids yet"; }
}