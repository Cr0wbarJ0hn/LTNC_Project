package com.example.auctionapp.server;
import com.google.gson.Gson;
import com.example.auctionapp.model.NetworkMessage;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * AutoBidManager — Engine xử lý Auto-Bidding phía server.
 *
 * Chức năng chính:
 *   1. Đăng ký / hủy rule auto-bid của user (lưu vào DB + cache bộ nhớ).
 *   2. Sau mỗi bid thành công, gọi triggerAutoBid() để kiểm tra xem
 *      có auto-bidder nào cần phản hồi ngay không.
 *   3. Tự động dừng khi user chạm trần maxBid hoặc hết số vòng (maxRounds).
 *   4. Broadcast sự kiện AUTO_BID_UPDATE đến tất cả client để UI cập nhật giá real-time.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 **/
public class AutoBidManager {

    // ── Thread pool xử lý auto-bid bất đồng bộ ───────────────────────────────
    // dùng daemon thread để tự tắt khi server tắt
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AutoBid-Worker");
        t.setDaemon(true);
        return t;
    });

    private static final Gson gson = new Gson();

    // ════════════════════════════════════════════════════════════════════════
    // API CÔNG KHAI — ClientHandler gọi các method này
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Đăng ký hoặc cập nhật rule auto-bid cho 1 user trên 1 phiên đấu giá.
     *
     * @param auctionId       ID phiên đấu giá
     * @param username        Tên người dùng
     * @param maxBid          Giá tối đa user chịu trả (trần giá)
     * @param customIncrement Bước tăng tùy chỉnh; truyền 0 để dùng increment của phiên
     * @param maxRounds       Số lần tối đa được auto-bid; truyền 999 = vô hạn
     * @return "SUCCESS" hoặc chuỗi mô tả lỗi
     */
    public static String registerAutoBid(int auctionId, String username,
            double maxBid, double customIncrement,
            int maxRounds) {
        // Kiểm tra đầu vào cơ bản
        if (maxBid <= 0)    return "Maximum price must be greater than 0.";
        if (maxRounds <= 0) return "Maximum rounds must be at least 1.";

        // Lấy giá hiện tại của phiên đấu giá
        double currentPrice = getCurrentPrice(auctionId);
        if (currentPrice < 0) return "Auction does not exist or has already ended.";
        if (maxBid <= currentPrice)
            return "Maximum price ($" + maxBid + ") must be higher than the current price ($" + currentPrice + ").";

        // Không cho phép người bán tự bid vật phẩm của mình
        if (isSeller(auctionId, username))
            return "You cannot place a bid on your own auction.";

        // Lưu rule vào database
        String dbResult = upsertAutoBidRule(auctionId, username, maxBid, customIncrement, maxRounds);
        if (!"SUCCESS".equals(dbResult)) return dbResult;

        // Ngay sau khi đăng ký, thử đặt giá luôn nếu user chưa đang thắng
        executor.submit(() -> processTrigger(auctionId, username, currentPrice));

        return "SUCCESS";
    }

    /**
     * Hủy rule auto-bid đang chạy của một user.
     *
     * @return "SUCCESS" hoặc chuỗi mô tả lỗi
     */
    public static String cancelAutoBid(int auctionId, String username) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return "Could not connect to the database.";

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE auto_bids SET is_active = FALSE " +
                            "WHERE auction_id = ? AND username = ? AND is_active = TRUE"
            );
            ps.setInt(1, auctionId);
            ps.setString(2, username);
            int rows = ps.executeUpdate();

            if (rows == 0) return "No active auto-bid rule found.";
            return "SUCCESS";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Kích hoạt chuỗi auto-bid sau mỗi lần có bid thành công.
     * Gọi method này từ ClientHandler.handleBid() sau khi bid được chấp nhận.
     *
     * @param auctionId  Phiên đấu giá vừa có bid mới
     * @param lastBidder Username của người vừa bid (để bỏ qua họ trong vòng này)
     * @param newPrice   Giá mới sau bid vừa rồi
     */
    public static void triggerAutoBid(int auctionId, String lastBidder, double newPrice) {
        // Chạy trên thread riêng để không block ClientHandler
        executor.submit(() -> processTrigger(auctionId, lastBidder, newPrice));
    }

    /**
     * Truy vấn trạng thái auto-bid hiện tại của một user trên một phiên.
     *
     * @return Chuỗi JSON: { active, maxBid, increment, maxRounds, roundsUsed }
     */
    public static String getAutoBidStatus(int auctionId, String username) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return "{\"error\":\"No database connection\"}";

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT max_bid, bid_increment, max_rounds, rounds_used, is_active " +
                            "FROM auto_bids WHERE auction_id = ? AND username = ?"
            );
            ps.setInt(1, auctionId);
            ps.setString(2, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return "{\"active\":false}";

            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("active",     rs.getBoolean("is_active"));
            obj.addProperty("maxBid",     rs.getDouble("max_bid"));
            obj.addProperty("increment",  rs.getDouble("bid_increment"));
            obj.addProperty("maxRounds",  rs.getInt("max_rounds"));
            obj.addProperty("roundsUsed", rs.getInt("rounds_used"));
            return obj.toString();

        } catch (SQLException e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOGIC NỘI BỘ
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Xử lý trigger: tìm auto-bidder đủ điều kiện và đặt giá cho họ.
     * Chỉ bid cho 1 người mỗi lần kích hoạt (người có maxBid cao nhất được ưu tiên).
     */
    private static void processTrigger(int auctionId, String lastBidder, double newPrice) {
        List<AutoBidRule> rules = loadActiveRules(auctionId);
        if (rules.isEmpty()) return;

        for (AutoBidRule rule : rules) {
            // Bỏ qua người vừa bid (tránh tự bid lại chính mình)
            if (rule.username.equals(lastBidder)) continue;

            // Kiểm tra còn vòng không
            if (rule.roundsUsed >= rule.maxRounds) {
                deactivateRule(auctionId, rule.username, "maximum rounds reached");
                continue;
            }

            // Tính số tiền cần bid
            double auctionIncrement  = getAuctionIncrement(auctionId);
            double effectiveIncrement = (rule.customIncrement > 0) ? rule.customIncrement : auctionIncrement;
            double autoBidAmount      = newPrice + effectiveIncrement;

            // Kiểm tra có vượt trần maxBid không
            if (autoBidAmount > rule.maxBid) {
                deactivateRule(auctionId, rule.username, "maximum price ceiling reached");
                // Gửi thông báo đến đúng user đó
                notifyUser(rule.username, "AUTO_BID_STOPPED",
                        "Auto-bid stopped: your maximum price of $" + rule.maxBid +
                                " has been reached for auction #" + auctionId);
                continue;
            }

            // Thực hiện bid qua transaction DB hiện có
            String result = DatabaseManager.executeSafeBidTransaction(auctionId, rule.username, autoBidAmount);

            if ("SUCCESS".equals(result)) {
                incrementRoundsUsed(auctionId, rule.username);
                // Broadcast đến tất cả client để UI cập nhật giá
                broadcastAutoBidUpdate(auctionId, rule.username, autoBidAmount);
                System.out.printf("[AutoBid] %s auto-bid $%.2f on auction #%d (round %d/%d)%n",
                        rule.username, autoBidAmount, auctionId,
                        rule.roundsUsed + 1, rule.maxRounds);
                // Chỉ bid 1 người mỗi trigger — dừng vòng lặp
                break;
            } else {
                System.out.printf("[AutoBid] Bid failed for %s on auction #%d: %s%n",
                        rule.username, auctionId, result);
            }
        }
    }

    /** Đọc danh sách rule đang active từ DB, sắp xếp theo maxBid giảm dần (ưu tiên cao nhất). */
    private static List<AutoBidRule> loadActiveRules(int auctionId) {
        List<AutoBidRule> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return list;

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT username, max_bid, bid_increment, max_rounds, rounds_used " +
                            "FROM auto_bids " +
                            "WHERE auction_id = ? AND is_active = TRUE " +
                            "ORDER BY max_bid DESC"   // Người trả cao nhất được xét trước
            );
            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new AutoBidRule(
                        rs.getString("username"),
                        rs.getDouble("max_bid"),
                        rs.getDouble("bid_increment"),
                        rs.getInt("max_rounds"),
                        rs.getInt("rounds_used")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Thêm mới hoặc cập nhật rule trong DB (dùng UPSERT để tránh duplicate). */
    private static String upsertAutoBidRule(int auctionId, String username,
            double maxBid, double customIncrement, int maxRounds) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return "Could not connect to the database.";

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO auto_bids (auction_id, username, max_bid, bid_increment, max_rounds, rounds_used, is_active) " +
                            "VALUES (?, ?, ?, ?, ?, 0, TRUE) " +
                            "ON CONFLICT (auction_id, username) DO UPDATE SET " +
                            "  max_bid = EXCLUDED.max_bid, " +
                            "  bid_increment = EXCLUDED.bid_increment, " +
                            "  max_rounds = EXCLUDED.max_rounds, " +
                            "  rounds_used = 0, " +      // reset lại vòng khi user cập nhật rule
                            "  is_active = TRUE"
            );
            ps.setInt(1, auctionId);
            ps.setString(2, username);
            ps.setDouble(3, maxBid);
            ps.setDouble(4, customIncrement);
            ps.setInt(5, maxRounds);
            ps.executeUpdate();
            return "SUCCESS";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    /** Tăng rounds_used lên 1 sau mỗi lần auto-bid thành công. */
    private static void incrementRoundsUsed(int auctionId, String username) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return;
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE auto_bids SET rounds_used = rounds_used + 1 WHERE auction_id = ? AND username = ?"
            );
            ps.setInt(1, auctionId);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Vô hiệu hóa rule khi không còn dùng được (hết vòng hoặc chạm trần). */
    private static void deactivateRule(int auctionId, String username, String lyDo) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return;
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE auto_bids SET is_active = FALSE WHERE auction_id = ? AND username = ?"
            );
            ps.setInt(1, auctionId);
            ps.setString(2, username);
            ps.executeUpdate();
            System.out.printf("[AutoBid] Rule deactivated for %s on auction #%d: %s%n", username, auctionId, lyDo);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Lấy giá hiện tại của phiên đấu giá. Trả về -1 nếu phiên không hợp lệ. */
    private static double getCurrentPrice(int auctionId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return -1;
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT current_highest_bid FROM auctions WHERE auction_id = ? AND status = 'ACTIVE'"
            );
            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("current_highest_bid");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    /** Lấy bước giá tăng (priceIncrement) của phiên đấu giá. */
    private static double getAuctionIncrement(int auctionId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return 1.0;
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT price_increment FROM auctions WHERE auction_id = ?"
            );
            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("price_increment");
        } catch (SQLException e) { e.printStackTrace(); }
        return 1.0;
    }

    /** Kiểm tra user có phải người bán của phiên đấu giá không. */
    private static boolean isSeller(int auctionId, String username) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return false;
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT seller_username FROM auctions WHERE auction_id = ?"
            );
            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return username.equals(rs.getString("seller_username"));
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * Broadcast sự kiện AUTO_BID_UPDATE đến toàn bộ client đang kết nối.
     * Tất cả UI sẽ nhận được và cập nhật giá hiển thị.
     */
    private static void broadcastAutoBidUpdate(int auctionId, String bidder, double amount) {
        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("auctionId", auctionId);
        payload.addProperty("bidder",    bidder);
        payload.addProperty("amount",    amount);
        payload.addProperty("isAuto",    true);

        String msg = gson.toJson(new NetworkMessage("AUTO_BID_UPDATE", payload.toString(), true));

        // Gửi đến tất cả ClientHandler đang mở
        for (ClientHandler client : AuctionServer.activeClients) {
            client.sendMessage(msg);
        }
    }

    /**
     * Gửi thông báo riêng đến 1 user cụ thể (ví dụ: báo auto-bid đã dừng).
     * Nếu user đó đã ngắt kết nối thì bỏ qua.
     */
    private static void notifyUser(String username, String action, String message) {
        String msg = gson.toJson(new NetworkMessage(action, message, false));
        for (ClientHandler client : AuctionServer.activeClients) {
            // Cần thêm getter getUsername() vào ClientHandler — xem hướng dẫn bên dưới
            if (username.equals(client.getUsername())) {
                client.sendMessage(msg);
                return;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // INNER CLASS — lưu thông tin rule trong bộ nhớ (chỉ dùng nội bộ)
    // ════════════════════════════════════════════════════════════════════════
    private static class AutoBidRule {
        String username;
        double maxBid;
        double customIncrement;
        int    maxRounds;
        int    roundsUsed;

        AutoBidRule(String username, double maxBid, double customIncrement, int maxRounds, int roundsUsed) {
            this.username        = username;
            this.maxBid          = maxBid;
            this.customIncrement = customIncrement;
            this.maxRounds       = maxRounds;
            this.roundsUsed      = roundsUsed;
        }
    }
}