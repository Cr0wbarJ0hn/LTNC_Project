package com.example.auctionapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    private Auction auction;
    private Member seller;
    private Member bidder;
    private Items item; // (Assuming you successfully renamed 'Items' to 'Item')

    @BeforeEach
    public void setUp() {
        // Provide all 6 required strings for the Member constructor
        seller = new Member("Sam Seller", "555-0001", "sam@email.com", "Seller123", "password", "123 Sell St");
        bidder = new Member("Bob Bidder", "555-0002", "bob@email.com", "Bidder99", "password", "456 Buy Ave");

        // Provide all 6 required arguments for the Item constructor
        item = new Items(1, "Electronics", "Test Laptop", "New", "A nice laptop", "image.png");

        // Initialize an auction starting at $100.0, with a $10.0 increment, ending in 1 hour
        auction = new Auction(item, seller, 100.0, 10.0, LocalDateTime.now().plusHours(1));
    }

    @Test
    public void testSuccessfulValidBid() {
        // FIX: Removed the "Item1" string! Now it perfectly matches your class.
        BidTransaction validBid = new BidTransaction(bidder, 115.0);
        boolean result = auction.placeBid(validBid);

        assertTrue(result, "Bid should be accepted");
        assertEquals(115.0, auction.getCurrentHighestBid(), "Highest bid should update to 115.0");
        assertEquals(1, auction.getBidHistory().size(), "Bid history should contain 1 transaction");
    }

    @Test
    public void testBidRejectedIfTooLow() {
        // FIX: Removed the "Item1" string!
        BidTransaction weakBid = new BidTransaction(bidder, 105.0);
        boolean result = auction.placeBid(weakBid);

        assertFalse(result, "Bid should be rejected for being too low");
        assertEquals(100.0, auction.getCurrentHighestBid(), "Highest bid should remain 100.0");
        assertTrue(auction.getBidHistory().isEmpty(), "Bid history should remain empty");
    }

    @Test
    public void testBidRejectedIfBidderIsSeller() {
        // FIX: Removed the "Item1" string!
        BidTransaction selfBid = new BidTransaction(seller, 150.0);
        boolean result = auction.placeBid(selfBid);

        assertFalse(result, "Bid should be rejected because bidder is the seller");
        assertEquals(100.0, auction.getCurrentHighestBid(), "Price should not change");
    }

    @Test
    public void testBidRejectedIfAuctionEnded() {
        // Create an auction that ended 1 minute ago
        Auction expiredAuction = new Auction(item, seller, 100.0, 10.0, LocalDateTime.now().minusMinutes(1));

        // FIX: Removed the "Item1" string!
        BidTransaction validBid = new BidTransaction(bidder, 150.0);
        boolean result = expiredAuction.placeBid(validBid);

        assertFalse(result, "Bid should be rejected because the time has passed");
    }

    @Test
    public void testSellerCanCancelActiveAuction() {
        boolean result = auction.cancelAuction(seller);
        assertTrue(result, "Cancel request should be accepted");
        assertEquals("CANCELLED", auction.getStatus(), "Status should change to CANCELLED");
    }

    @Test
    public void testNonSellerCannotCancelAuction() {
        boolean result = auction.cancelAuction(bidder);
        assertFalse(result, "Cancel request should be rejected for unauthorized user");
        assertEquals("ACTIVE", auction.getStatus(), "Status should remain ACTIVE");
    }
}