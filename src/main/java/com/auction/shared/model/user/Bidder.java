package com.auction.shared.model.user;

import com.auction.shared.observer.AuctionObserver;

//Bidder(Người đấu thầu)
public class Bidder extends User implements AuctionObserver {
    private double balance; // Số dư ví điện tử

    // 1. Constructor
    public Bidder(String id, String name, String username, String password, String email, double balance) {
        super(id, name, username, password, email);
        this.balance = balance;
    }

    // 2. Các hàm Getter và Setter
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    // ============================================================
    // 3. TRIỂN KHAI CÁC HÀM TỪ INTERFACE AUCTIONOBSERVER
    // ============================================================

    // Hàm này giúp giải quyết lỗi đỏ "does not override update(String)"
    @Override
    public void update(String msg) {
        System.out.println("\n[THÔNG BÁO MỚI ĐẾN BIDDER " + getName().toUpperCase() + "]:");
        System.out.println(" >> " + msg);
    }

    @Override
    public void updatePrice(String auctionId, double newPrice, String bidderName) {
        // Logic xử lý khi giá thay đổi (có thể dùng để cập nhật giao diện sau này)
        System.out.println("[Log]: Phiên " + auctionId + " đã đổi giá sang: " + newPrice);
    }

    @Override
    public void onAuctionFinished(String auctionId, String winnerName) {
        System.out.println("[Log]: Phiên " + auctionId + " kết thúc. Người thắng: " + winnerName);
    }


    @Override
    public String toString() {
        return String.format("Bidder[ID=%s, Name=%s, Balance=%,.2f]", id, name, balance);
    }
}
