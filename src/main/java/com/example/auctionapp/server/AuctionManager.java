package com.example.auctionapp.server;

import com.example.auctionapp.model.AuctionObserver;
import com.example.auctionapp.model.AuctionSession;
import com.example.auctionapp.exception.AuctionClosedException;
import com.example.auctionapp.exception.InvalidBidException;
import com.example.auctionapp.exception.SelfBiddingException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionManager {
    private static AuctionManager instance;

    // Stores all currently running auctions in server memory
    private final ConcurrentHashMap<Integer, AuctionSession> activeAuctions = new ConcurrentHashMap<>();

    // A background thread to act as the clock referee
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private AuctionManager() {
        startAutoCloseWorker();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    private final List<AuctionObserver> activeClients = new CopyOnWriteArrayList<>();
    public void registerObserver(AuctionObserver client) {
        activeClients.add(client);
    }

    // 🌟 3. Method to Unsubscribe (Detach)
    public void removeObserver(AuctionObserver client) {
        activeClients.remove(client);
    }


    public void broadcastPriceUpdate(int auctionId, double newPrice, String highestBidder) {
        for (AuctionObserver client : activeClients) {
            client.onBidUpdated(auctionId, newPrice, highestBidder);
        }
        System.out.println("📡 [BROADCAST] Sent live update for Auction " + auctionId + " to all clients.");
    }



    public void submitBid(int auctionId, String username, double amount)
            throws AuctionClosedException, InvalidBidException, SelfBiddingException {

        // Look up the active item in the server's running concurrent memory map
        AuctionSession session = activeAuctions.get(auctionId);

        // If the auction isn't in memory, it has already expired or doesn't exist!
        if (session == null) {
            throw new AuctionClosedException("This auction item is no longer active or does not exist!");
        }

        // Pass the bid details into the individual item's thread lock thread for processing.
        // (This method now internally handles all broadcasting for both manual and auto-bids!)
        session.processIncomingBid(username, amount);
    }

    public void submitAutoBid(int auctionId, String username, double maxBudget)
            throws AuctionClosedException, InvalidBidException, SelfBiddingException {

        // Look up the active item in the server's running concurrent memory map
        AuctionSession session = activeAuctions.get(auctionId);

        // If the auction isn't in memory, it has already expired or doesn't exist!
        if (session == null) {
            throw new AuctionClosedException("This auction item is no longer active or does not exist!");
        }

        // Pass configuration down into the individual item's isolated thread lock for safe execution
        session.processIncomingAutoBidRegistration(username, maxBudget);
    }

    // Add an auction to the server's watch list
    public void addActiveAuction(AuctionSession session) {
        activeAuctions.put(session.getAuctionId(), session);
    }


    private void startAutoCloseWorker() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();

            // Loop through all active auctions
            for (AuctionSession session : activeAuctions.values()) {

                // If the current time has passed the auction's end time
                if (now.isAfter(session.getEndTime())) {

                    // 🌟 Step 1: Thread-safe lock during closure to prevent late incoming bids
                    session.getLock().lock();
                    try {
                        int auctionId = session.getAuctionId();
                        String item = session.getItemName();
                        String seller = session.getSellerName();
                        String winner = session.getHighestBidder();
                        double price = session.getCurrentPrice();

                        // 1. Update the database state to active = false
                        DatabaseManager.closeAuction(auctionId);

                        // 2. 🌟 PERSISTENT DATABASE NOTIFICATION ENGINE (Polymorphic Types)
                        if (winner.equals("No bids yet") || winner == null) {
                            // Notify Seller it expired empty
                            DatabaseManager.createNotification(
                                    seller, "ITEM_EXPIRED", "Auction Expired",
                                    "Your listing '" + item + "' has ended with no valid bids.", auctionId
                            );
                        } else {
                            // Notify Seller it sold successfully
                            DatabaseManager.createNotification(
                                    seller, "ITEM_SOLD", "Item Sold!",
                                    "Congratulations! Your item '" + item + "' was sold to " + winner + " for $" + price, auctionId
                            );

                            // Notify Winner they won
                            DatabaseManager.createNotification(
                                    winner, "AUCTION_WON", "You Won!",
                                    "Success! You won the auction for '" + item + "' with a final bid of $" + price, auctionId
                            );
                        }

                        // Notify all Losers (everyone who bid/auto-bid but isn't the winner)
                        for (String participant : session.getParticipants()) {
                            if (!participant.equals(winner)) {
                                DatabaseManager.createNotification(
                                        participant, "AUCTION_LOST", "Auction Ended",
                                        "The auction for '" + item + "' has closed. Better luck next time!", auctionId
                                );
                            }
                        }

                        for (AuctionObserver client : activeClients) {
                            String connectedUser = client.getUsername();

                            if (connectedUser != null &&
                                    (connectedUser.equals(seller) || session.getParticipants().contains(connectedUser))) {

                                // Send targeted network push message to their client listener
                                client.onAuctionClosed(auctionId, item, winner, price);
                            }
                        }

                        // 4. Remove it from the server's active memory list
                        activeAuctions.remove(auctionId);

                        System.out.println("🔔 [SYSTEM CLEANUP] Auction " + auctionId + " successfully terminated and logged.");

                    } finally {
                        // 🌟 Always unlock in a finally block to prevent thread deadlocks
                        session.getLock().unlock();
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS); // Delay 0, repeat every 1 second
    }
}
