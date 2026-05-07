package auction.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * BidHistory: Lưu trữ lịch sử bid
 */
public class BidHistory {
    private final String auctionId;
    private final List<BidRecord> records;
    private final Object lock = new Object();

    public BidHistory(String auctionId) {
        this.auctionId = auctionId;
        this.records = new CopyOnWriteArrayList<>();
    }

    public void recordBid(String bidderId, double price, LocalDateTime timestamp) {
        synchronized (lock) {
            BidRecord record = new BidRecord(bidderId, price, timestamp);
            records.add(record);
        }
    }

    public List<BidRecord> getAllRecords() {
        return new ArrayList<>(records);
    }

    public List<BidRecord> getRecordsInRange(LocalDateTime start, LocalDateTime end) {
        synchronized (lock) {
            List<BidRecord> filtered = new ArrayList<>();
            for (BidRecord record : records) {
                if (!record.getTimestamp().isBefore(start) &&
                        !record.getTimestamp().isAfter(end)) {
                    filtered.add(record);
                }
            }
            return filtered;
        }
    }

    public static class BidRecord {
        private final String bidderId;
        private final double price;
        private final LocalDateTime timestamp;

        public BidRecord(String bidderId, double price, LocalDateTime timestamp) {
            this.bidderId = bidderId;
            this.price = price;
            this.timestamp = timestamp;
        }

        public String getBidderId() { return bidderId; }
        public double getPrice() { return price; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("%s | Bidder: %s | Price: %.2f",
                    timestamp, bidderId, price);
        }
    }
}