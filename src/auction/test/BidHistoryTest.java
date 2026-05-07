package auction.test;

import auction.model.BidHistory;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit Test cho Bid History
 */
public class BidHistoryTest {
    private BidHistory bidHistory;
    private String auctionId = "AUCTION-TEST-HISTORY";

    @Before
    public void setUp() {
        bidHistory = new BidHistory(auctionId);
    }

    @Test
    public void testRecordBid() {
        LocalDateTime now = LocalDateTime.now();
        bidHistory.recordBid("BIDDER-001", 5000000, now);
        bidHistory.recordBid("BIDDER-002", 5500000, now.plusSeconds(5));
        bidHistory.recordBid("BIDDER-001", 6000000, now.plusSeconds(10));

        List<BidHistory.BidRecord> records = bidHistory.getAllRecords();
        assertEquals("Phải có 3 records", 3, records.size());
    }

    @Test
    public void testGetRecordsInRange() {
        LocalDateTime start = LocalDateTime.now();

        bidHistory.recordBid("BIDDER-001", 5000000, start);
        bidHistory.recordBid("BIDDER-002", 5500000, start.plusSeconds(10));
        bidHistory.recordBid("BIDDER-001", 6000000, start.plusSeconds(20));

        LocalDateTime rangeStart = start.plusSeconds(5);
        LocalDateTime rangeEnd = start.plusSeconds(15);

        List<BidHistory.BidRecord> filtered = bidHistory.getRecordsInRange(rangeStart, rangeEnd);

        assertEquals("Phải có 1 record trong range", 1, filtered.size());
        assertEquals("Record phải có giá 5500000", 5500000, filtered.get(0).getPrice(), 0.01);
    }

    @Test
    public void testRecordsOrderedByTime() {
        LocalDateTime now = LocalDateTime.now();
        bidHistory.recordBid("B1", 1000, now);
        bidHistory.recordBid("B2", 2000, now.plusSeconds(10));
        bidHistory.recordBid("B3", 3000, now.plusSeconds(5));

        List<BidHistory.BidRecord> records = bidHistory.getAllRecords();

        assertEquals("Record 1 phải có giá 1000", 1000, records.get(0).getPrice(), 0.01);
        assertEquals("Record 2 phải có giá 2000", 2000, records.get(1).getPrice(), 0.01);
        assertEquals("Record 3 phải có giá 3000", 3000, records.get(2).getPrice(), 0.01);
    }
}