package com.example.auctionapp.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AdminAuctionRowTest {

    @Test
    public void testJavaFXPropertyGetters() {
        AdminAuctionRow row = new AdminAuctionRow(101, "Vintage Camera", 250.0, "SellerBob");

        assertEquals(101, row.getId(), "Table ID mismatch");
        assertEquals("Vintage Camera", row.getItemName(), "Table Item Name mismatch");
        assertEquals(250.0, row.getCurrentPrice(), "Table Price mismatch");

        assertEquals("SellerBob", row.getSeller(), "Table Seller mismatch");
    }
}