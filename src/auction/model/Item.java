package auction.model;

/**
 * Item: Mục được đấu giá
 */
public class Item {
    private final String category;
    private final String name;
    private final double initialPrice;

    public Item(String category, String name, double initialPrice) {
        this.category = category;
        this.name = name;
        this.initialPrice = initialPrice;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public double getInitialPrice() {
        return initialPrice;
    }

    @Override
    public String toString() {
        return String.format("Item{category='%s', name='%s', initialPrice=%.2f}",
                category, name, initialPrice);
    }
}