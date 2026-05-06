package com.auction.shared.observer;

public interface AuctionObserver {
    // Gọi hàm này khi có giá mới được đặt
    void updatePrice(String auctionId, double newPrice, String bidderName);

    // Gọi hàm này khi phiên đấu giá kết thúc
    void onAuctionFinished(String auctionId, String winnerName);

    void update(String msg);
}
