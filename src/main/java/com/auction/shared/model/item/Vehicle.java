package com.auction.shared.model.item;

//Vehicle(Phuong tien)
//mileage(quang duong da di)
public class Vehicle extends Item {
    private int mileage;

    public Vehicle(String id, String name, double startPrice, double currentPrice, String description, int mileage) {
        super(id, name, startPrice, currentPrice, description);
        this.mileage = mileage;
    }

    @Override
    public String getCategory() {
        return "Vehicle";
    }

    @Override
    public String getInfo() {
        return "Mileage: " + mileage + " km";
    }
}
