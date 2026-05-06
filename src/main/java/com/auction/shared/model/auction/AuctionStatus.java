package com.auction.shared.model.auction;

//AuctionStatus(Trạng thái)
public enum AuctionStatus {
    OPEN,//Vừa tạo, chưa bắt đầu
    RUNNING,//Đang diễn ra, cho phép đặt giá
    FINISHED,//Đã kết thúc
    PAID,//Đã thanh toán
    CANCELED//Đã hủy
}
