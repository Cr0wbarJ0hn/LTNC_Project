package auction.test;

import auction.model.*;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

/**
 * Unit Test cho Auto-Bidding
 */
public class AutoBiddingTest {
    private AuctionManager auctionManager;
    private Auction testAuction;
    private String auctionId = "AUCTION-TEST-AUTOBID";
    private String bidderId = "BIDDER-001";

    @Before
    public void setUp() {
        AntiSnipingManager.resetInstance();
        AuctionManager.resetInstance();

        auctionManager = AuctionManager.getInstance();

        Item item = new Item("Laptop", "Dell XPS 13", 5000000);
        LocalDateTime endTime = LocalDateTime.now().plus(5, ChronoUnit.MINUTES);
        testAuction = new Auction(auctionId, item, LocalDateTime.now(), endTime);
        testAuction.setStatus(Auction.AuctionStatus.RUNNING);
        testAuction.setCurrentPrice(5000000);
        auctionManager.addAuction(testAuction);
    }

    @After
    public void tearDown() {
        AntiSnipingManager.resetInstance();
        AuctionManager.resetInstance();
    }

    @Test
    public void testAutoBidNotExceedMax() throws InterruptedException {
        double maxBid = 10000000;
        double increment = 500000;
        double initialPrice = testAuction.getCurrentPrice();

        auctionManager.registerAutoBid(auctionId, bidderId, maxBid, increment);

        Thread.sleep(1000);

        double currentPrice = testAuction.getCurrentPrice();
        assertTrue("Giá hiện tại phải cao hơn giá khởi điểm", currentPrice > initialPrice);
        assertTrue("Giá hiện tại không vượt maxBid", currentPrice <= maxBid);
    }

    @Test
    public void testMultipleAutoBidNoConflict() throws InterruptedException {
        auctionManager.registerAutoBid(auctionId, "BIDDER-001", 10000000, 500000);
        auctionManager.registerAutoBid(auctionId, "BIDDER-002", 12000000, 1000000);

        Thread.sleep(1500);

        String leadingBidder = testAuction.getLeadingBidder();
        assertNotNull("Phải có người đấu giá", leadingBidder);
        assertTrue("Leading bidder phải là BIDDER-001 hoặc BIDDER-002",
                leadingBidder.equals("BIDDER-001") || leadingBidder.equals("BIDDER-002"));

        double currentPrice = testAuction.getCurrentPrice();
        assertTrue("Giá hiện tại không được vượt 12000000", currentPrice <= 12000000);
    }

    @Test
    public void testAutoBidStopsAtMaxBid() throws InterruptedException {
        double maxBid = 5500000;
        double increment = 500000;
        double initialPrice = testAuction.getCurrentPrice();

        auctionManager.registerAutoBid(auctionId, bidderId, maxBid, increment);
        Thread.sleep(2000);

        double currentPrice = testAuction.getCurrentPrice();
        assertTrue("Giá hiện tại phải cao hơn giá khởi điểm", currentPrice > initialPrice);
        assertTrue("Giá hiện tại không vượt maxBid", currentPrice <= maxBid);
    }

    @Test
    public void testPriorityQueueByTime() throws InterruptedException {
        auctionManager.registerAutoBid(auctionId, "BIDDER-001", 10000000, 500000);
        Thread.sleep(100);
        auctionManager.registerAutoBid(auctionId, "BIDDER-002", 12000000, 1000000);

        Thread.sleep(1500);

        String leadingBidder = testAuction.getLeadingBidder();
        assertNotNull("Phải có leading bidder", leadingBidder);
    }
}