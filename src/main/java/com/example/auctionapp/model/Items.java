 package com.example.auctionapp.model;

public class Items {
    private String imagePath;
    private int id;
    private String type;
    private String name;
    private String condition;
    private String description;
    public Items(int id, String type, String name, String condition, String description, String imagePath){
        this.id = id;
        this.type = type;
        this.name = name;
        this.condition = condition;
        this.description = description;
        this.imagePath = imagePath;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getCondition() {
        return condition;
    }
    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Items items = (Items) o;
        return id == items.id;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Item [ID=" + id + ", Name=" + name + ", Type=" + type + "]";
    }
}
