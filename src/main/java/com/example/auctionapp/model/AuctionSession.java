package com.example.auctionapp.model; // Adjust package name to match your server layout

import com.example.auctionapp.exception.AuctionClosedException;
import com.example.auctionapp.exception.InvalidBidException;
import com.example.auctionapp.server.DatabaseManager; // Adjust to your actual DatabaseManager import

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

    /**
     * Thread-safe method to process an incoming bid from a client handler.
     */
    public void processIncomingBid(String bidderName, double bidAmount)
            throws AuctionClosedException, InvalidBidException {

        // 1. Lock the session so no two clients can modify this auction at the exact same millisecond
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

            // Check C: Prevent a user from outbidding themselves (Optional business rule)
            if (bidderName.equals(highestBidder)) {
                throw new InvalidBidException("You are already the highest bidder!");
            }

            // 2. If it passes all checks, update the memory values
            this.currentPrice = bidAmount;
            this.highestBidder = bidderName;

            // 3. Write it permanently to Supabase via the DatabaseManager transaction we built earlier
            String dbResult = DatabaseManager.executeSafeBidTransaction(this.auctionId, bidderName, bidAmount);
            if (!"SUCCESS".equals(dbResult)) {
                // Pass the actual database error message (e.g., "Bid too low!") up to the client handler
                throw new InvalidBidException("Database synchronization failed: " + dbResult);
            }

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
    public String getHighestBidder() { return highestBidder; }
}