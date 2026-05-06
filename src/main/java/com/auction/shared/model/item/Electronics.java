package com.auction.shared.model.item;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;
    public Electronics(String id,String name,double startPrice,double currentPrice,String description,String brand,int warrantyMonths){
        super(id,name,startPrice,currentPrice,description);
        this.brand=brand;
        this.warrantyMonths=warrantyMonths;
    }
    @Override
    public String getCategory(){
        return "Electronics";
    }

    @Override
    public String getInfo() {
        return "Brand: "+this.brand+"/"+"Warranty: "+this.warrantyMonths;
    }
}
