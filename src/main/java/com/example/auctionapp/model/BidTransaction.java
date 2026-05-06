package com.example.auctionapp.model;

import java.time.LocalDateTime;

public class BidTransaction {
    private int id;
    private Member bidder;
    private double bidAmount;
    private LocalDateTime time;

    public BidTransaction(Member bidder, double bidAmount) {
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.time = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Member getBidder() { return bidder; }
    public void setBidder(Member bidder) { this.bidder = bidder; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
}

