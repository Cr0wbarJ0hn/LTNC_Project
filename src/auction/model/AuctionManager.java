package auction.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuctionManager: Quản lý tất cả phiên đấu giá
 * Singleton pattern
 */
public class AuctionManager {
    private static AuctionManager instance;
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final AutoBidder autoBidder;

    private AuctionManager() {
        this.autoBidder = new AutoBidder();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.autoBidder.shutdown();
            instance.auctions.clear();
        }
        instance = null;
    }

    public void addAuction(Auction auction) {
        if (auction != null) {
            auctions.put(auction.getId(), auction);
            System.out.println("[AuctionManager] Thêm phiên: " + auction.getId());
        }
    }

    public Auction getAuction(String auctionId) {
        return auctions.get(auctionId);
    }

    public Collection<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }

    public void removeAuction(String auctionId) {
        auctions.remove(auctionId);
    }

    public void registerAutoBid(String auctionId, String bidderId,
                                double maxBid, double increment) {
        autoBidder.registerAutoBid(auctionId, bidderId, maxBid, increment);
    }

    public void shutdown() {
        autoBidder.shutdown();
        auctions.clear();
        System.out.println("[AuctionManager] Shutdown hoàn thành");
    }

    public int getAuctionCount() {
        return auctions.size();
    }

    @Override
    public String toString() {
        return String.format("AuctionManager{totalAuctions=%d}", auctions.size());
    }
}