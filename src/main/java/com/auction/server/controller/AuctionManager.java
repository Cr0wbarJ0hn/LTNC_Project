package com.auction.server.controller;

import com.auction.shared.model.auction.Auction;
import com.auction.shared.model.autobid.AutoBidConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    // Lưu trữ danh sách các phiên đấu giá đang diễn ra
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    // Lưu trữ danh sách cấu hình Auto-Bid sử dụng PriorityQueue
    private final Map<String, PriorityQueue<AutoBidConfig>> autoBidConfigs = new ConcurrentHashMap<>();

    // Quản lý ổ khóa (Lock) riêng biệt cho từng phòng đấu giá để tối ưu hiệu năng
    private final Map<String, Object> auctionLocks = new ConcurrentHashMap<>();

    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
        auctionLocks.putIfAbsent(auction.getId(), new Object());
    }

    /**
     * Đăng ký cấu hình AutoBid
     */
    public void registerAutoBid(AutoBidConfig config) {
        Object lock = auctionLocks.computeIfAbsent(config.getAuctionId(), k -> new Object());

        // Đồng bộ hóa dựa trên lock của từng phòng để bảo vệ PriorityQueue (vì nó không thread-safe)
        synchronized (lock) {
            autoBidConfigs.computeIfAbsent(config.getAuctionId(), k -> new PriorityQueue<>(
                    // Sắp xếp giảm dần theo maxBid
                    Comparator.comparing(AutoBidConfig::getMaxBid).reversed()
            )).add(config);
        }
        System.out.println("Server: Cấu hình Đặt giá tự động cho người dùng đã được nhận. " + config.getUserId());
    }

    private void broadcastNewPrice(Auction auction) {
        System.out.println("Thông báo: Giá mới cho " + auction.getId() + " là " + auction.getCurrentPrice());
    }

    private void updateWinner(Auction auction, String userId, double price) {
        auction.setCurrentPrice(price);
        auction.setCurrentWinner(userId);
        broadcastNewPrice(auction);
    }

    /**
     * Hàm này chạy bên trong block synchronized(lock) nên không sợ tranh chấp dữ liệu
     */
    private void handleAutoBidding(Auction auction) {
        PriorityQueue<AutoBidConfig> queue = autoBidConfigs.get(auction.getId());
        if (queue == null || queue.isEmpty()) return;

        boolean robotActed;
        do {
            robotActed = false;
            List<AutoBidConfig> temporarilyRemoved = new ArrayList<>();
            AutoBidConfig bestRobot = null;

            // Duyệt PriorityQueue để tìm robot tối ưu nhất
            while (!queue.isEmpty()) {
                AutoBidConfig top = queue.peek();

                // Điều kiện 1: Không tự đấu với chính mình
                if (top.getUserId().equals(auction.getCurrentWinner())) {
                    temporarilyRemoved.add(queue.poll());
                    continue;
                }

                // Điều kiện 2: Giá mới không vượt quá giá trần (maxBid)
                if ((auction.getCurrentPrice() + top.getIncrement()) <= top.getMaxBid()) {
                    bestRobot = top;
                    break;
                } else {
                    // Robot không đủ tiền nâng mức giá tiếp theo -> Loại vĩnh viễn khỏi hàng đợi
                    queue.poll();
                }
            }

            // Hoàn lại các robot hợp lệ nhưng tạm thời bị bỏ qua (do đang là người thắng) vào lại Queue
            if (!temporarilyRemoved.isEmpty()) {
                queue.addAll(temporarilyRemoved);
            }

            if (bestRobot != null) {
                double newPrice = auction.getCurrentPrice() + bestRobot.getIncrement();
                updateWinner(auction, bestRobot.getUserId(), newPrice);
                System.out.println("Robot [" + bestRobot.getUserId() + "] dùng PriorityQueue đã nâng giá lên: " + newPrice);
                robotActed = true;
            }

        } while (robotActed);
    }

    /**
     * Chỉ lock đúng phòng đấu giá đang có người đặt giá.
     */
    public void processBid(String auctionId, String bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        Object lock = auctionLocks.computeIfAbsent(auctionId, k -> new Object());

        // Lock riêng biệt từng phòng đấu giá (Phòng A đặt giá không làm ảnh hưởng đến phòng B)
        synchronized (lock) {
            if (bidAmount > auction.getCurrentPrice()) {
                updateWinner(auction, bidderId, bidAmount);
                handleAutoBidding(auction);
            }
        }
    }
}


