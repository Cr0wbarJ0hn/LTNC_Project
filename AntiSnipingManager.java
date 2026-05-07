package auction.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Anti-Sniping Manager: Gia hạn phiên khi có bid trong phút cuối
 */
public class AntiSnipingManager {
    private static final long ANTI_SNIPE_WINDOW = 60;
    private static final long EXTENSION_TIME = 300;
    private static final int MAX_EXTENSIONS_PER_AUCTION = 3;

    private final Map<String, SnipeData> snipeLog = new ConcurrentHashMap<>();
    private final List<AntiSnipingListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static AntiSnipingManager instance;

    private AntiSnipingManager() {}

    public static synchronized AntiSnipingManager getInstance() {
        if (instance == null) {
            instance = new AntiSnipingManager();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.lock.writeLock().lock();
            try {
                instance.snipeLog.clear();
                instance.listeners.clear();
            } finally {
                instance.lock.writeLock().unlock();
            }
        }
        instance = null;
    }

    public void addListener(AntiSnipingListener listener) {
        listeners.add(listener);
    }

    public boolean checkAndExtendAuction(String auctionId, String bidderId) {
        lock.writeLock().lock();
        try {
            Auction auction = AuctionManager.getInstance().getAuction(auctionId);
            if (auction == null) {
                return false;
            }

            LocalDateTime endTime = auction.getEndTime();
            LocalDateTime now = LocalDateTime.now();
            long secondsRemaining = ChronoUnit.SECONDS.between(now, endTime);

            SnipeData snipeData = snipeLog.computeIfAbsent(auctionId, k -> new SnipeData(auctionId));
            if (snipeData.getExtensionCount() >= MAX_EXTENSIONS_PER_AUCTION) {
                System.out.println("[Anti-Sniping] ⚠️ Phiên " + auctionId +
                        " đã đạt tối đa " + MAX_EXTENSIONS_PER_AUCTION + " lần gia hạn");
                return false;
            }

            if (secondsRemaining > 0 && secondsRemaining <= ANTI_SNIPE_WINDOW) {
                LocalDateTime newEndTime = endTime.plus(EXTENSION_TIME, ChronoUnit.SECONDS);
                auction.setEndTime(newEndTime);
                snipeData.addExtension(bidderId, now);

                System.out.println("[Anti-Sniping] ✅ Gia hạn phiên " + auctionId +
                        " thêm 5 phút (bid từ: " + bidderId + ")");
                System.out.println("[Anti-Sniping] Thời gian kết thúc mới: " + newEndTime);

                notifyExtensionEvent(auctionId, bidderId, newEndTime);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void notifyExtensionEvent(String auctionId, String bidderId, LocalDateTime newEndTime) {
        for (AntiSnipingListener listener : listeners) {
            listener.onAuctionExtended(auctionId, bidderId, newEndTime);
        }
    }

    public List<String> getExtensionHistory(String auctionId) {
        lock.readLock().lock();
        try {
            SnipeData data = snipeLog.get(auctionId);
            if (data != null) {
                return data.getExtensionLog();
            }
            return Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public interface AntiSnipingListener {
        void onAuctionExtended(String auctionId, String bidderId, LocalDateTime newEndTime);
    }

    private static class SnipeData {
        private final String auctionId;
        private final List<String> extensionLog;
        private int extensionCount;

        SnipeData(String auctionId) {
            this.auctionId = auctionId;
            this.extensionLog = new ArrayList<>();
            this.extensionCount = 0;
        }

        void addExtension(String bidderId, LocalDateTime time) {
            extensionCount++;
            String log = String.format("[Gia hạn #%d] Bidder: %s - Thời gian: %s",
                    extensionCount, bidderId, time);
            extensionLog.add(log);
        }

        List<String> getExtensionLog() {
            return new ArrayList<>(extensionLog);
        }

        int getExtensionCount() {
            return extensionCount;
        }
    }
}