package auction.test;

import auction.model.*;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

/**
 * Unit Test cho Anti-Sniping
 */
public class AntiSnipingTest {
    private AntiSnipingManager antiSnipingManager;
    private AuctionManager auctionManager;
    private Auction testAuction;
    private String auctionId = "AUCTION-TEST-ANTI-SNIPE";

    @Before
    public void setUp() {
        AntiSnipingManager.resetInstance();
        AuctionManager.resetInstance();

        antiSnipingManager = AntiSnipingManager.getInstance();
        auctionManager = AuctionManager.getInstance();

        Item item = new Item("Art", "Bức tranh Picasso", 50000000);
        LocalDateTime endTime = LocalDateTime.now().plus(30, ChronoUnit.SECONDS);
        testAuction = new Auction(auctionId, item, LocalDateTime.now(), endTime);
        testAuction.setStatus(Auction.AuctionStatus.RUNNING);
        auctionManager.addAuction(testAuction);
    }

    @After
    public void tearDown() {
        AntiSnipingManager.resetInstance();
        AuctionManager.resetInstance();
    }

    @Test
    public void testBidInFinalMinuteTriggersExtension() {
        LocalDateTime originalEndTime = testAuction.getEndTime();

        boolean extended = antiSnipingManager.checkAndExtendAuction(auctionId, "BIDDER-001");

        assertTrue("Phải gia hạn khi bid trong 60 giây cuối", extended);
        assertNotEquals("Thời gian kết thúc phải thay đổi", originalEndTime, testAuction.getEndTime());
        assertTrue("Thời gian kết thúc phải sau thời gian ban đầu", testAuction.getEndTime().isAfter(originalEndTime));
    }

    @Test
    public void testExtensionHistoryRecorded() {
        boolean extended1 = antiSnipingManager.checkAndExtendAuction(auctionId, "BIDDER-001");
        assertTrue("Lần 1 phải gia hạn", extended1);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newEndTime = now.plus(30, ChronoUnit.SECONDS);
        testAuction.setEndTime(newEndTime);

        boolean extended2 = antiSnipingManager.checkAndExtendAuction(auctionId, "BIDDER-002");
        assertTrue("Lần 2 phải gia hạn", extended2);

        var history = antiSnipingManager.getExtensionHistory(auctionId);

        assertEquals("Phải có 2 extensions", 2, history.size());
        assertTrue("Extension 1 phải chứa BIDDER-001", history.get(0).contains("BIDDER-001"));
        assertTrue("Extension 2 phải chứa BIDDER-002", history.get(1).contains("BIDDER-002"));
    }

    @Test
    public void testBidOutsideFinalMinuteNoExtension() {
        Item item = new Item("Test", "Test Item", 1000000);
        LocalDateTime farFutureEnd = LocalDateTime.now().plus(10, ChronoUnit.MINUTES);
        Auction farAuction = new Auction("AUCTION-FAR", item, LocalDateTime.now(), farFutureEnd);
        farAuction.setStatus(Auction.AuctionStatus.RUNNING);
        auctionManager.addAuction(farAuction);

        boolean extended = antiSnipingManager.checkAndExtendAuction("AUCTION-FAR", "BIDDER-001");

        assertFalse("Không nên gia hạn nếu còn > 60 giây", extended);
        auctionManager.removeAuction("AUCTION-FAR");
    }

    @Test
    public void testMultipleAuctionsIndependent() {
        Item item1 = new Item("Item1", "Item 1", 1000000);
        LocalDateTime endTime1 = LocalDateTime.now().plus(30, ChronoUnit.SECONDS);
        Auction auction1 = new Auction("AUCTION-1", item1, LocalDateTime.now(), endTime1);
        auction1.setStatus(Auction.AuctionStatus.RUNNING);
        auctionManager.addAuction(auction1);

        Item item2 = new Item("Item2", "Item 2", 2000000);
        LocalDateTime endTime2 = LocalDateTime.now().plus(30, ChronoUnit.SECONDS);
        Auction auction2 = new Auction("AUCTION-2", item2, LocalDateTime.now(), endTime2);
        auction2.setStatus(Auction.AuctionStatus.RUNNING);
        auctionManager.addAuction(auction2);

        antiSnipingManager.checkAndExtendAuction("AUCTION-1", "BIDDER-001");
        antiSnipingManager.checkAndExtendAuction("AUCTION-2", "BIDDER-001");

        var history1 = antiSnipingManager.getExtensionHistory("AUCTION-1");
        assertEquals("Auction 1 phải có 1 extension", 1, history1.size());

        var history2 = antiSnipingManager.getExtensionHistory("AUCTION-2");
        assertEquals("Auction 2 phải có 1 extension", 1, history2.size());

        auctionManager.removeAuction("AUCTION-1");
        auctionManager.removeAuction("AUCTION-2");
    }
}