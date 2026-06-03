package com.example.auctionapp.server;

import com.example.auctionapp.model.AuctionObserver;
import com.example.auctionapp.exception.AuctionClosedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionManagerTest {

    private AuctionManager manager;

    @BeforeEach
    public void setUp() {
        // Grab the single instance of the manager before every test
        manager = AuctionManager.getInstance();
    }

    @Test
    public void testSingletonBehavior() {
        // Act: Try to get a "second" manager
        AuctionManager secondManager = AuctionManager.getInstance();

        // Assert: They must be the exact same object in memory
        assertSame(manager, secondManager, "AuctionManager must follow strict Singleton pattern");
    }

    @Test
    public void testSubmitBidThrowsExceptionForMissingAuction() {
        // Act & Assert: Try to bid on an auction ID (999) that does not exist in the ConcurrentHashMap
        assertThrows(AuctionClosedException.class, () -> {
            manager.submitBid(999, "testUser", 150.0);
        }, "Bidding on a non-existent auction must throw an AuctionClosedException");
    }

    @Test
    public void testSubmitAutoBidThrowsExceptionForMissingAuction() {
        // Act & Assert: Try to auto-bid on an auction ID (888) that does not exist
        assertThrows(AuctionClosedException.class, () -> {
            manager.submitAutoBid(888, "testUser", 500.0);
        }, "Auto-Bidding on a non-existent auction must throw an AuctionClosedException");
    }

    @Test
    public void testObserverRegistrationAndRemoval() {
        // Arrange: Create a dummy client listener (Anonymous Class)
        AuctionObserver dummyClient = new AuctionObserver() {
            @Override
            public void onBidUpdated(int auctionId, double newPrice, String highestBidder) {}
            @Override
            public void onAuctionClosed(int auctionId, String itemName, String winner, double finalPrice) {}
            @Override
            public String getUsername() { return "dummyUser"; }
        };

        // Act & Assert: We can't directly check the private activeClients list,
        // but we can ensure adding/removing doesn't throw concurrency errors.
        assertDoesNotThrow(() -> {
            manager.registerObserver(dummyClient);
            manager.removeObserver(dummyClient);
        }, "Registering and removing observers should execute safely without exceptions");
    }
}