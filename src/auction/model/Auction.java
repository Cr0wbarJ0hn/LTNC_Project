package auction.model;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Auction: Phiên đấu giá
 */
public class Auction {
    public enum AuctionStatus {
        PENDING, RUNNING, ENDED, CANCELLED
    }

    private final String id;
    private final Item item;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private String leadingBidder;
    private AuctionStatus status;
    private final BidHistory bidHistory;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Auction(String id, Item item, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = item.getInitialPrice();
        this.leadingBidder = null;
        this.status = AuctionStatus.PENDING;
        this.bidHistory = new BidHistory(id);
    }

    public boolean placeBid(String bidderId, double amount) {
        lock.writeLock().lock();
        try {
            if (!isRunning()) {
                throw new IllegalStateException("Phiên đấu giá đã kết thúc");
            }
            if (amount <= currentPrice) {
                throw new IllegalArgumentException("Giá bid phải cao hơn giá hiện tại");
            }

            currentPrice = amount;
            leadingBidder = bidderId;
            bidHistory.recordBid(bidderId, amount, LocalDateTime.now());
            AntiSnipingManager.getInstance().checkAndExtendAuction(id, bidderId);
            notifyBidUpdate(bidderId, amount);

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public BidHistory getBidHistory() {
        return bidHistory;
    }

    public void setEndTime(LocalDateTime newEndTime) {
        lock.writeLock().lock();
        try {
            this.endTime = newEndTime;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public LocalDateTime getEndTime() {
        lock.readLock().lock();
        try {
            return endTime;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setCurrentPrice(double price) {
        lock.writeLock().lock();
        try {
            this.currentPrice = price;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getCurrentPrice() {
        lock.readLock().lock();
        try {
            return currentPrice;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getLeadingBidder() {
        lock.readLock().lock();
        try {
            return leadingBidder;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setLeadingBidder(String bidderId) {
        lock.writeLock().lock();
        try {
            this.leadingBidder = bidderId;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public AuctionStatus getStatus() {
        lock.readLock().lock();
        try {
            return status;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setStatus(AuctionStatus newStatus) {
        lock.writeLock().lock();
        try {
            this.status = newStatus;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public boolean isRunning() {
        lock.readLock().lock();
        try {
            return status == AuctionStatus.RUNNING && LocalDateTime.now().isBefore(endTime);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void notifyBidUpdate(String bidderId, double amount) {
        System.out.println("[Auction Update] " + id + " - " + bidderId + ": " + amount);
    }

    @Override
    public String toString() {
        return String.format("Auction{id='%s', item=%s, price=%.2f, bidder='%s', status=%s}",
                id, item, currentPrice, leadingBidder, status);
    }
}