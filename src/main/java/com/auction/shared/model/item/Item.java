package com.auction.shared.model.item;

import com.auction.shared.model.Entity;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startPrice;
    protected double currentPrice;



    public Item(String id, String name, double startPrice, double currentPrice,String description){
        super(id,name);
        this.startPrice=startPrice;
        this.currentPrice=currentPrice;
        this.description=description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    public abstract String getInfo();
    public abstract String getCategory();
    
}
