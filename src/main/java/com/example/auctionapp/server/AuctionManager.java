package com.example.auctionapp.server;

import com.example.auctionapp.model.AuctionSession;
import com.example.auctionapp.exception.AuctionClosedException;
import com.example.auctionapp.exception.InvalidBidException;

import java.time.LocalDateTime;
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

    // Add an auction to the server's watch list
    public void addActiveAuction(AuctionSession session) {
        activeAuctions.put(session.getAuctionId(), session);
    }

    /**
     * This background thread runs every 1 second.
     * It checks all active auctions to see if their time is up.
     */
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

                    // TODO: Notify all connected clients over the socket that this auction is over!
                }
            }
        }, 0, 1, TimeUnit.SECONDS); // Delay 0, repeat every 1 second
    }
}
