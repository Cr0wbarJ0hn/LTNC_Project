package com.example.auctionapp.server;

import com.example.auctionapp.model.Items;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/auction_app";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // XAMPP default is a blank password

    // This method hands out a connection whenever the Server needs to talk to the database
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Database Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }

    public static void initializeTables() {
        String createMembersTable = "CREATE TABLE IF NOT EXISTS members (" +
                "email VARCHAR(100) UNIQUE, " +
                "username VARCHAR(50) PRIMARY KEY, " +
                "password VARCHAR(50) NOT NULL, " +
                "role VARCHAR(20) DEFAULT 'USER')";

        String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "itemName VARCHAR(100) NOT NULL, " +
                "itemType VARCHAR(50), " +
                "itemCondition VARCHAR(50), " +
                "description TEXT, " +
                "imagePath VARCHAR(500))";

        String createAuctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "itemId INT NOT NULL, " +                     // <-- THE LINK!
                "seller VARCHAR(50) NOT NULL, " +
                "startingPrice DOUBLE NOT NULL, " +           // <-- NEW: Keeps the original price safe!
                "currentPrice DOUBLE NOT NULL, " +
                "priceIncrement DOUBLE NOT NULL, " +
                "endTime DATETIME NOT NULL, " +
                "active BOOLEAN DEFAULT TRUE, " +
                "FOREIGN KEY (itemId) REFERENCES items(id))";


        String createBidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "auctionId INT NOT NULL, " +
                "bidder VARCHAR(50) NOT NULL, " +
                "bidAmount DOUBLE NOT NULL, " +
                "FOREIGN KEY (auctionId) REFERENCES auctions(id))";

        try (Connection conn = getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            stmt.execute(createMembersTable);
            stmt.execute(createItemsTable);   // MUST BE BEFORE AUCTIONS

            // 2. Dependent tables second
            stmt.execute(createAuctionsTable); // Needs the items table to exist!

            // 3. Most dependent table last
            stmt.execute(createBidsTable);
            System.out.println("Database tables are ready to go");

        } catch (SQLException e) {
            System.out.println("Error creating tables!");
            e.printStackTrace();
        }

    }

    public static String fetchAuctionsByCategory(String category) {
        StringBuilder result = new StringBuilder();

        // ADDED a.priceIncrement to the SELECT list
        String query = "SELECT i.itemName, a.startingPrice, a.currentPrice, i.itemCondition, " +
                "i.imagePath, i.description, a.seller, a.endTime, a.priceIncrement, " +
                "(SELECT COUNT(*) FROM bids b WHERE b.auctionId = a.id) as totalBids " +
                "FROM items i " +
                "JOIN auctions a ON i.id = a.itemId " +
                "WHERE i.itemType = ? AND a.active = 1";

        try (java.sql.Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, category);
            java.sql.ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("itemName");
                double startPrice = rs.getDouble("startingPrice");
                double currentPrice = rs.getDouble("currentPrice");
                String condition = rs.getString("itemCondition");
                String imgPath = rs.getString("imagePath");
                String description = rs.getString("description");
                String seller = rs.getString("seller");
                java.sql.Timestamp endTime = rs.getTimestamp("endTime");

                // --- NEW: Grab the increment from the result set ---
                double increment = rs.getDouble("priceIncrement");

                // --- IMPORTANT: Build the string in the EXACT order BrowseController expects ---
                // Order: name~start~current~condition~img~desc~seller~time~INCREMENT
                result.append(name).append("~")
                        .append(startPrice).append("~")
                        .append(currentPrice).append("~")
                        .append(condition).append("~")
                        .append(imgPath).append("~")
                        .append(description).append("~")
                        .append(seller).append("~")
                        .append(endTime.getTime()).append("~")
                        .append(increment).append("|"); // Changed from totalBids to increment
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    public static void insertItemAndAuction(Items item, String seller, double initialPrice, double priceIncrement, java.sql.Timestamp endTime) {
        String insertItemSql = "INSERT INTO items (itemName, itemType, itemCondition, description, imagePath) VALUES (?, ?, ?, ?, ?)";

        String insertAuctionSql = "INSERT INTO auctions (itemId, seller, startingPrice, currentPrice, priceIncrement, endTime, active) VALUES (?, ?, ?, ?, ?, ?, TRUE)";

        Connection conn = getConnection();
        if (conn == null) return;

        try {

            java.sql.PreparedStatement itemStmt = conn.prepareStatement(insertItemSql, java.sql.Statement.RETURN_GENERATED_KEYS);
            itemStmt.setString(1, item.getName());
            itemStmt.setString(2, item.getType());
            itemStmt.setString(3, item.getCondition());
            itemStmt.setString(4, item.getDescription());
            itemStmt.setString(5, item.getImagePath());

            itemStmt.executeUpdate();


            java.sql.ResultSet rs = itemStmt.getGeneratedKeys();
            int generatedItemId = -1;
            if (rs.next()) {
                generatedItemId = rs.getInt(1); // This is the ID MySQL just created!
            }

            // STEP 3: Save the Auction, linking it to the Item ID we just grabbed
            if (generatedItemId != -1) {
                java.sql.PreparedStatement auctionStmt = conn.prepareStatement(insertAuctionSql);
                auctionStmt.setInt(1, generatedItemId);
                auctionStmt.setString(2, seller);
                auctionStmt.setDouble(3, initialPrice);  // startingPrice
                auctionStmt.setDouble(4, initialPrice);  // currentPrice (Starts at the same value)
                auctionStmt.setDouble(5, priceIncrement);
                auctionStmt.setTimestamp(6, endTime);

                auctionStmt.executeUpdate();
                System.out.println("Success: Item and Auction saved completely separately!");
                auctionStmt.close();
            }

            itemStmt.close();
            conn.close();

        } catch (java.sql.SQLException e) {
            System.out.println("Error saving separated Item and Auction!");
            e.printStackTrace();
        }
    }

    public static void registerUser(String username, String password, String email) throws SQLException {
        String query = "INSERT INTO members (username, password, email, role) VALUES (?, ?, ?, 'USER')";

        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);

            pstmt.executeUpdate();

        }

    }

    public static boolean verifyLogin(String username, String password) {
        String query = "SELECT * FROM members WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Fill in the ? placeholders
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            // Execute the search
            java.sql.ResultSet rs = pstmt.executeQuery();

            // If rs.next() is true, it means we found a matching row!
            if (rs.next()) {
                return true;
            } else {
                System.out.println("Login failed: Incorrect username or password for '" + username + "'.");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("Database error during login.");
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("Setting up Database");
        initializeTables();
    }
}
