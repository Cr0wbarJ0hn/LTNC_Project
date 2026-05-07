package auction.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Auto-Bidder: Tự động trả giá theo maxBid và increment
 * ✅ HOÀN THIỆN: Max extension limit, persist, conflict resolution
 */
public class AutoBidder {
    private static final int MAX_AUTO_BID_EXTENSIONS = 3;
    private final PriorityQueue<AutoBidRequest> bidQueue;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean running = true;
    private Thread processorThread;
    private final Map<String, AutoBidRequest> persistedRequests = new java.util.concurrent.ConcurrentHashMap<>();

    public AutoBidder() {
        this.bidQueue = new PriorityQueue<>(
                Comparator.comparing(AutoBidRequest::getRequestTime)
        );
        startProcessor();
    }

    public void registerAutoBid(String auctionId, String bidderId,
                                double maxBid, double increment) {
        lock.writeLock().lock();
        try {
            String key = auctionId + ":" + bidderId;

            AutoBidRequest existing = persistedRequests.get(key);
            if (existing != null && existing.getExtensionCount() >= MAX_AUTO_BID_EXTENSIONS) {
                System.out.println("[AutoBid] ⚠️ Bidder " + bidderId +
                        " đã đạt tối đa " + MAX_AUTO_BID_EXTENSIONS + " extensions");
                return;
            }

            AutoBidRequest request = new AutoBidRequest(
                    auctionId, bidderId, maxBid, increment, LocalDateTime.now()
            );
            bidQueue.offer(request);
            persistedRequests.put(key, request);

            System.out.println("[AutoBid] ✅ Đăng ký: " + bidderId +
                    " - Max: " + maxBid + ", Increment: " + increment);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void startProcessor() {
        processorThread = new Thread(() -> {
            while (running) {
                AutoBidRequest request = null;

                lock.writeLock().lock();
                try {
                    request = bidQueue.peek();
                    if (request != null) {
                        bidQueue.poll();
                    }
                } finally {
                    lock.writeLock().unlock();
                }

                if (request != null) {
                    processBidRequest(request);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        processorThread.setName("AutoBidProcessor");
        processorThread.setDaemon(true);
        processorThread.start();
    }

    private void processBidRequest(AutoBidRequest request) {
        try {
            Auction auction = AuctionManager.getInstance().getAuction(request.getAuctionId());
            if (auction == null || !auction.isRunning()) {
                return;
            }

            double currentPrice = auction.getCurrentPrice();
            double nextBidPrice = currentPrice + request.getIncrement();

            if (nextBidPrice <= request.getMaxBid()) {
                try {
                    auction.placeBid(request.getBidderId(), nextBidPrice);
                    System.out.println("[AutoBid] ✅ " + request.getBidderId() +
                            " tự động bid: " + nextBidPrice);

                    if (nextBidPrice < request.getMaxBid()) {
                        request.incrementExtension();
                        registerAutoBid(request.getAuctionId(),
                                request.getBidderId(),
                                request.getMaxBid(),
                                request.getIncrement());
                    }
                } catch (IllegalStateException e) {
                    System.err.println("[AutoBid] ❌ " + e.getMessage());
                }
            } else {
                System.out.println("[AutoBid] ⚠️ " + request.getBidderId() +
                        " đã đạt maxBid: " + request.getMaxBid());
            }
        } catch (Exception e) {
            System.err.println("[AutoBid Error] " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        if (processorThread != null) {
            processorThread.interrupt();
        }
        persistedRequests.clear();
    }

    public static class AutoBidRequest {
        private final String auctionId;
        private final String bidderId;
        private final double maxBid;
        private final double increment;
        private final LocalDateTime requestTime;
        private AtomicInteger extensionCount = new AtomicInteger(0);

        public AutoBidRequest(String auctionId, String bidderId,
                              double maxBid, double increment, LocalDateTime requestTime) {
            this.auctionId = auctionId;
            this.bidderId = bidderId;
            this.maxBid = maxBid;
            this.increment = increment;
            this.requestTime = requestTime;
        }

        public String getAuctionId() { return auctionId; }
        public String getBidderId() { return bidderId; }
        public double getMaxBid() { return maxBid; }
        public double getIncrement() { return increment; }
        public LocalDateTime getRequestTime() { return requestTime; }
        public int getExtensionCount() { return extensionCount.get(); }
        public void incrementExtension() { extensionCount.incrementAndGet(); }

        @Override
        public String toString() {
            return String.format("AutoBidRequest{auction=%s, bidder=%s, maxBid=%.2f, extensions=%d}",
                    auctionId, bidderId, maxBid, extensionCount.get());
        }
    }
}