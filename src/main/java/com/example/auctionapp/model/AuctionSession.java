package com.example.auctionapp.model;

import com.example.auctionapp.exception.AuctionClosedException;
import com.example.auctionapp.exception.InvalidBidException;
import com.example.auctionapp.exception.SelfBiddingException;
import com.example.auctionapp.server.AuctionManager;
import com.example.auctionapp.server.DatabaseManager;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionSession {
    private final int auctionId;
    private final String itemName; // 🌟 ADDED: Needed for notifications
    private final String sellerName; // The host
    private final double priceIncrement;

    // 🌟 REMOVED 'final' from endTime so Anti-Sniping can extend it!
    private LocalDateTime endTime;
    private double currentPrice;
    private String highestBidder;

    // A thread-safe set to hold unique usernames of everyone who participates
    private final Set<String> participants = ConcurrentHashMap.newKeySet();


    private final ReentrantLock lock = new ReentrantLock();

    // 🌟 FIXED CONSTRUCTOR: Now accepts itemName and sellerName!
    public AuctionSession(int auctionId, String itemName, double startingPrice, double priceIncrement, LocalDateTime endTime, String highestBidder, String sellerName) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = startingPrice;
        this.priceIncrement = priceIncrement;
        this.endTime = endTime;
        this.highestBidder = highestBidder;
        this.sellerName = sellerName;
    }

    public void processIncomingBid(String bidderName, double bidAmount)
            throws AuctionClosedException, InvalidBidException, SelfBiddingException {

        lock.lock();
        try {
            if (java.time.LocalDateTime.now().isAfter(endTime)) {
                throw new AuctionClosedException("Bidding has already ended for Auction ID " + auctionId);
            }

            double minimumRequiredBid = currentPrice + priceIncrement;
            if (bidAmount < minimumRequiredBid) {
                throw new InvalidBidException("Bid is too low! Minimum required is $" + minimumRequiredBid);
            }

            if (bidderName.equals(highestBidder)) {
                throw new InvalidBidException("You are already the highest bidder!");
            }

            // 🌟 TRACK PARTICIPANT: Add them to the notification mailing list!
            participants.add(bidderName);

            try {
                // Step 1: Manual Bid
                DatabaseManager.executeSafeBidTransaction(this.auctionId, bidderName, bidAmount);

                long secondsRemaining = java.time.temporal.ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
                if (secondsRemaining < 120) {
                    this.endTime = this.endTime.plusMinutes(1); // Add 1 minute
                    DatabaseManager.updateAuctionEndTime(this.auctionId, this.endTime);
                    System.out.println("🛡️ [ANTI-SNIPE]: Auction " + auctionId + " extended by 1 minute to stop sniping!");
                }

                // Sync memory and Broadcast the human bid!
                refreshLocalMemoryFromDatabase();
                AuctionManager.getInstance().broadcastPriceUpdate(this.auctionId, this.currentPrice, this.highestBidder);
                System.out.println("👤 [MANUAL BID]: " + this.highestBidder + " bid $" + this.currentPrice);

                // Step 2: Auto-Bid War
                boolean autoBidTriggered;
                do {
                    autoBidTriggered = DatabaseManager.executeAutoBidCounterTransaction(this.auctionId);

                    if (autoBidTriggered) {
                        refreshLocalMemoryFromDatabase();
                        AuctionManager.getInstance().broadcastPriceUpdate(this.auctionId, this.currentPrice, this.highestBidder);
                    }
                } while (autoBidTriggered);

            } catch (java.sql.SQLException e) {
                throw new InvalidBidException("Database synchronization failed: " + e.getMessage());
            }

        } finally {
            lock.unlock();
        }
    }
    public java.util.concurrent.locks.ReentrantLock getLock() {
        return lock;
    }

    public void processIncomingAutoBidRegistration(String username, double maxBudget)
            throws AuctionClosedException, InvalidBidException, SelfBiddingException {

        lock.lock();
        try {
            if (java.time.LocalDateTime.now().isAfter(endTime)) {
                throw new AuctionClosedException("Bidding has already ended for Auction ID " + auctionId);
            }

            double minimumRequiredBid = currentPrice + priceIncrement;
            if (maxBudget < minimumRequiredBid) {
                throw new InvalidBidException("Your maximum budget must be at least $" + minimumRequiredBid + " to join.");
            }

            // 🌟 TRACK PARTICIPANT: Add them to the notification mailing list!
            participants.add(username);

            try {
                DatabaseManager.registerAutoBid(this.auctionId, username, maxBudget);
                System.out.println("🤖 [AUTO-BID LOCKED]: " + username + " successfully registered proxy budget at $" + maxBudget);

                boolean autoBidTriggered;
                do {
                    autoBidTriggered = DatabaseManager.executeAutoBidCounterTransaction(this.auctionId);

                    if (autoBidTriggered) {
                        refreshLocalMemoryFromDatabase();
                        AuctionManager.getInstance().broadcastPriceUpdate(this.auctionId, this.currentPrice, this.highestBidder);
                        System.out.println("🤖 [AUTO-BID TRIGGER]: " + this.highestBidder + " jumped to $" + this.currentPrice);
                    }
                } while (autoBidTriggered);

            } catch (java.sql.SQLException e) {
                throw new InvalidBidException("Database synchronization failed: " + e.getMessage());
            }

        } finally {
            lock.unlock();
        }
    }

    private void refreshLocalMemoryFromDatabase() throws java.sql.SQLException {
        String sql = "SELECT currentPrice, last_bidder FROM auctions WHERE id = ?";
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, this.auctionId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    this.currentPrice = rs.getDouble("currentPrice");
                    this.highestBidder = rs.getString("last_bidder");
                }
            }
        }
    }

    // 🌟 ADDED ALL GETTERS NEEDED BY THE MANAGER
    public int getAuctionId() { return auctionId; }
    public String getItemName() { return itemName; }
    public String getSellerName() { return sellerName; }
    public Set<String> getParticipants() { return participants; }
    public double getCurrentPrice() { return currentPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getHighestBidder() { return (highestBidder != null) ? highestBidder : "No bids yet"; }
}