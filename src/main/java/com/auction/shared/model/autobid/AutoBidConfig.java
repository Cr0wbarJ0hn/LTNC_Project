package com.auction.shared.model.autobid;

import java.io.Serializable;

public class AutoBidConfig implements Serializable {
    private String userId;
    private String auctionId;
    private double maxBid;
    private double increment;

    public AutoBidConfig (String userId,String auctionId,double maxBid,double increment){
        this.userId=userId;
        this.auctionId=auctionId;
        this.maxBid=maxBid;
        this.increment=increment;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public double getIncrement() {
        return increment;
    }
}
