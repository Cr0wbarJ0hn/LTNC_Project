package com.auction.shared.model.item;

public class ItemFactory {
    // Định nghĩa hằng số để tránh lỗi gõ sai (Hard-coding)
    public static final String ELECTRONICS = "ELECTRONICS";
    public static final String ART = "ART";
    public static final String VEHICLE = "VEHICLE";

    /**
     * Phương thức khởi tạo Item dựa trên loại sản phẩm
     * args: thứ tự các tham số phụ tùy thuộc vào từng loại Item
     */
    public static Item createItem(String type, String id, String name, double startPrice, String description, Object... args) {
        if (type == null) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống.");
        }

        return switch (type.toUpperCase()) {
            case ELECTRONICS -> {
                // Giả sử Electronics cần: Brand (String), Warranty (int)
                String brand = (String) args[0];
                int warranty = (int) args[1];
                yield new Electronics(id, name, startPrice, startPrice, description, brand, warranty);
            }
            case ART -> {
                // Giả sử Art cần: Artist (String)
                String artist = (String) args[0];
                yield new Art(id, name, startPrice, startPrice, description, artist);
            }
            case VEHICLE -> {
                // Giả sử Vehicle cần: Model (String), Year (int)
                String model = (String) args[0];
                int year = (int) args[1];
                yield new Vehicle(id, name, startPrice, startPrice, description, year);
            }
            default -> throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        };
    }
}
