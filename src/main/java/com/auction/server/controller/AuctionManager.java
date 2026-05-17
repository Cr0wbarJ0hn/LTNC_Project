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
                    Comparator.comparing(AutoBidConfig::getMaxBid).reversed())).add(config);
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
        // 1. Lấy ra hàng đợi Robot của phòng đấu giá hiện tại
        PriorityQueue<AutoBidConfig> queue = autoBidConfigs.get(auction.getId());
        if (queue == null || queue.isEmpty()) return; // Nếu không có robot nào thì dừng lại luôn

        boolean robotActed;
        do {
            robotActed = false; // Mặc định coi như vòng này chưa có robot nào nâng giá

            // 2. Tạo một danh sách tạm thời để chứa các robot chưa đủ điều kiện ở vòng này
            List<AutoBidConfig> temporarilyRemoved = new ArrayList<>();
            AutoBidConfig bestRobot = null;

            // 3. Vòng lặp duyệt từ đỉnh Queue xuống (đỉnh luôn là thằng có maxBid cao nhất)
            while (!queue.isEmpty()) {
                AutoBidConfig top = queue.peek(); // peek(): Xem thử thằng đứng đầu hàng đợi (không lấy ra)

                // Điều kiện 1: Robot không tự đấu với chính mình (nếu nó đang là người thắng hiện tại)
                if (top.getUserId().equals(auction.getCurrentWinner())) {
                    temporarilyRemoved.add(queue.poll()); // poll(): Bốc nó ra khỏi queue tạm thời, tí trả lại sau
                    continue; // Nhảy sang kiểm tra thằng tiếp theo trong hàng đợi
                }

                // Điều kiện 2: Giá mới (Giá hiện tại + Bước giá) không vượt quá giá trần (maxBid) của robot
                if ((auction.getCurrentPrice() + top.getIncrement()) <= top.getMaxBid()) {
                    bestRobot = top; // Tìm thấy ông lớn nhất thỏa mãn rồi!
                    break; // Thoát xích, không cần xét các robot phía sau nữa
                } else {
                    // Robot không đủ tiền nâng mức giá tiếp theo -> Loại vĩnh viễn khỏi hàng đợi
                    queue.poll();
                }
            }

            // 4. Hoàn lại các robot tạm thời bị bỏ qua (đang giữ top 1) vào lại Queue cho các vòng sau
            if (!temporarilyRemoved.isEmpty()) {
                queue.addAll(temporarilyRemoved);
            }

            // 5. Nếu tìm thấy Robot tối ưu nhất, tiến hành nâng giá
            if (bestRobot != null) {
                double newPrice = auction.getCurrentPrice() + bestRobot.getIncrement(); // Tính giá mới
                updateWinner(auction, bestRobot.getUserId(), newPrice); // Cập nhật người thắng mới lên màn hình
                System.out.println("Robot [" + bestRobot.getUserId() + "] dùng PriorityQueue đã nâng giá lên: " + newPrice);
                robotActed = true; // Đánh dấu true để vòng do-while chạy tiếp, kiểm tra xem có robot khác "đè" tiếp không
            }

        } while (robotActed); // Vòng lặp dừng lại khi không còn ông Robot nào đủ tiền nâng giá nữa
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


