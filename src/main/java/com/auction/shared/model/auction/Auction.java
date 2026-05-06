package com.auction.shared.model.auction;


import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.Entity;
import com.auction.shared.observer.AuctionObserver;

import java.util.ArrayList;
import java.util.List;
//Auction = Quản lý trung tâm đấu giá
public class Auction extends Entity {
    private Item item;  // Sản phẩm
    private Seller seller;   //Người bán
    private List<BidTransaction> bids; //Danh sách các lượt đặt giá
    private AuctionStatus status; //Trạng thái(Sử dụng Enum)
    private List<AuctionObserver> observers = new ArrayList<>();

    public Auction(String id, String name, Item item, Seller seller) {
        super(id, name);
        this.item = item;
        this.seller = seller;
        this.bids = new ArrayList<>();
        this.status = AuctionStatus.OPEN; // Mặc định là OPEN
    }
    //Thêm một giao dịch đặt giá mới
    public void addBid(BidTransaction bid) throws Exception{
        // 1. Kiểm tra tính hợp lệ của giá đấu
        if (bid.getBidAmount() <= item.getCurrentPrice()) {
            throw new Exception("Giá đặt phải cao hơn giá hiện tại!");
        }

        // 2. Kiểm tra trạng thái phiên
        if (this.status != AuctionStatus.RUNNING) {
            throw new Exception("Phiên đấu giá không trong trạng thái cho phép đặt giá!");
        }

        // 3. Nếu hợp lệ, cập nhật
        this.bids.add(bid);
        this.item.setCurrentPrice(bid.getBidAmount());
        notifyObservers("Giá mới: " + bid.getBidAmount() + " bởi " + bid.getBidderId());
    }
    public void addObserver(AuctionObserver obs) { observers.add(obs); }

    private void notifyObservers(String msg) {
        for (AuctionObserver obs : observers) { obs.update(msg); }
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public List<BidTransaction> getBids() {
        return bids;
    }

    public void setBids() {
        setBids(null);
    }

    public void setBids(List<BidTransaction> bids) {
        this.bids = bids;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
    @Override
    public String toString(){
        String winnerName;
        if(this.bids.isEmpty()){
            winnerName ="Empty";
        }else{
            //Nếu có người đặt giá,lấy giao dịch cuối cùng của danh sách
            int lastIndex = this.bids.size()-1;
            BidTransaction lastBid = this.bids.get(lastIndex);
            winnerName = lastBid.getBidderId();
        }
        return String.format(
                "========= PHIÊN ĐẤU GIÁ [%s] =========\n" +
                        "| Sản phẩm: %s\n" +
                        "| Người bán: %s\n" +
                        "| Giá hiện tại: %,.2f VNĐ\n" +
                        "| Người dẫn đầu: %s\n" +
                        "| Trạng thái: [%s]\n" +
                        "| Số lượt bid: %d\n" +
                        "======================================",
                id,
                item.getName(),
                seller.getName(),
                item.getCurrentPrice(),
                winnerName,
                status,
                bids.size()
        );
    }
}
