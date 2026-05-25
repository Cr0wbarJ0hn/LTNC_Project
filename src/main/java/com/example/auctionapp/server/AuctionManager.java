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


    private void broadcastPriceUpdate(int auctionId, double newPrice, String highestBidder) {
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

        // Pass the bid details into the individual item's thread lock thread for processing
        session.processIncomingBid(username, amount);
        broadcastPriceUpdate(auctionId, session.getCurrentPrice(), session.getHighestBidder());
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

                    // 1. Update the database to active = false
                    DatabaseManager.closeAuction(session.getAuctionId());

                    // 2. Remove it from the server's active memory list
                    activeAuctions.remove(session.getAuctionId());

                    // 3. Announce the winner in the console
                    System.out.println("🔔 AUCTION ENDED: Item " + session.getAuctionId() +
                            " sold to " + session.getHighestBidder() +
                            " for $" + session.getCurrentPrice());


                }
            }
        }, 0, 1, TimeUnit.SECONDS); // Delay 0, repeat every 1 second
    }
}
