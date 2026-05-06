package com.auction.shared.model.auction;

import com.auction.shared.model.Entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//BidTransaction = giao dịch đặt giá
public class BidTransaction extends Entity {
    private String auctionId; //ID của phiên đấu giá
    private String bidderId; //ID của người đặt giá
    private double bidAmount; //Số tiền đặt giá
    private LocalDateTime bidTime; //Thời điểm đặt giá
    public BidTransaction(String id, String name, String auctionId, String bidderId, double bidAmount) {
        super(id, name);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now(); // Tự động ghi nhận thời gian hiện tại
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }
    @Override
    public String toString(){
        // Tạo định dạng thời gian: Giờ:Phút:Giây Ngày/Tháng/Năm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        String formattedTime = bidTime.format(formatter);
        //String.format() -> tạo chuỗi có chèn dữ liệu vào
        return String.format(
                "--- GIAO DỊCH ĐẶT GIÁ [%s] ---\n" +
                        "| Mã phiên: %s\n" +
                        "| Người đặt: %s\n" +
                        "| Số tiền: %,.2f VNĐ\n" +
                        "| Thời gian: %s\n" +
                        "------------------------------",
                id, auctionId, bidderId, bidAmount, formattedTime
        );
    }
}
