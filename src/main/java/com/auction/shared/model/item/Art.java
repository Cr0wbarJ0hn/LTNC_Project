package com.auction.shared.model.item;

public class Art extends Item {
    private String artist;

    public Art(String id, String name, double startPrice, double currentPrice, String description, String artist) {
        super(id, name, startPrice, currentPrice, description);
        this.artist = artist;
    }
    @Override
    public String getCategory() {
        return "Art";
    }

    @Override
    public String getInfo() {
        return "Artist: " + artist;
    }
}
