package com.example.auctionapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemsTest {

    private Items testItem;

    @BeforeEach
    public void setUp() {
        // ID: 1, Type: "Electronics", Name: "Laptop", Condition: "New", Description: "Gaming laptop", Image: "path/to/image.jpg"
        testItem = new Items(1, "Electronics", "Laptop", "New", "Gaming laptop", "path/to/image.jpg");
    }

    @Test
    public void testItemConstructorAndGetters() {
        assertEquals(1, testItem.getId());
        assertEquals("Electronics", testItem.getType());
        assertEquals("Laptop", testItem.getName());
        assertEquals("New", testItem.getCondition());
        assertEquals("Gaming laptop", testItem.getDescription());
        assertEquals("path/to/image.jpg", testItem.getImagePath());
    }

    @Test
    public void testSettersUpdateValues() {
        testItem.setName("Desktop PC");
        testItem.setCondition("Used");

        assertEquals("Desktop PC", testItem.getName(), "Name should update after using setter");
        assertEquals("Used", testItem.getCondition(), "Condition should update after using setter");
    }

    @Test
    public void testEqualsMatchesById() {
        // Create a completely different item, but give it the SAME database ID (1)
        Items duplicateItem = new Items(1, "Furniture", "Chair", "Used", "Wooden", "chair.jpg");

        // Create an item with the same name, but a DIFFERENT database ID (2)
        Items differentItem = new Items(2, "Electronics", "Laptop", "New", "Gaming laptop", "path/to/image.jpg");

        // Assertions
        assertTrue(testItem.equals(duplicateItem), "Items with the same ID should be considered equal");
        assertFalse(testItem.equals(differentItem), "Items with different IDs should NOT be considered equal");
    }

    @Test
    public void testToStringFormat() {
        String expectedString = "Item [ID=1, Name=Laptop, Type=Electronics]";
        assertEquals(expectedString, testItem.toString(), "toString should output the custom readable format");
    }
}