package com.example.auctionapp.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AdminTest {

    @Test
    public void testAdminRoleAndInheritance() {
        // Arrange
        Admin adminUser = new Admin("System Admin", "555-0000", "admin@system.com", "adminRoot", "superSecret");

        // Assert Inherited User properties
        assertEquals("adminRoot", adminUser.getUsername());
        assertEquals("admin@system.com", adminUser.getEmail());

        // Assert Polymorphic Role
        assertEquals("ADMIN", adminUser.getRole(), "Admin class must strictly return 'ADMIN' for security routing");
    }
}