package com.example.auctionapp.server;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.example.auctionapp.model.Items;
import java.sql.*;

public class DatabaseManager {
    // 1. UPDATED: Connection details for Supabase
    // Replace [YOUR-PASSWORD] with your actual Supabase database password
    private static final String URL = "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?user=postgres.dxhyrntoijscgblkjwys&password=Ekko2007@1410";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Ekko2007@1410";

    public static Connection getConnection() {
        try {
            // 2. UPDATED: Load the PostgreSQL Driver
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL Driver not found. Add the dependency to your pom.xml!");
            return null;
        } catch (SQLException e) {
            System.out.println("Cloud Database Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }

    public static void initializeTables() {
        // 3. UPDATED: SQL Syntax changes
        // MySQL 'INT AUTO_INCREMENT' becomes PostgreSQL 'SERIAL'
        // MySQL 'DATETIME' becomes PostgreSQL 'TIMESTAMP'

        String createMembersTable = "CREATE TABLE IF NOT EXISTS members (" +
                "email VARCHAR(100) UNIQUE, " +
                "username VARCHAR(50) PRIMARY KEY, " +
                "password VARCHAR(50) NOT NULL, " +
                "role VARCHAR(20) DEFAULT 'USER')";

        String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                "id SERIAL PRIMARY KEY, " + // SERIAL = Auto-increment
                "itemName VARCHAR(100) NOT NULL, " +
                "itemType VARCHAR(50), " +
                "itemCondition VARCHAR(50), " +
                "description TEXT, " +
                "imagePath VARCHAR(500))";

        String createAuctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                "id SERIAL PRIMARY KEY, " +
                "itemId INT NOT NULL, " +
                "seller VARCHAR(50) NOT NULL, " +
                "startingPrice DOUBLE PRECISION NOT NULL, " + // DOUBLE -> DOUBLE PRECISION
                "currentPrice DOUBLE PRECISION NOT NULL, " +
                "priceIncrement DOUBLE PRECISION NOT NULL, " +
                "endTime TIMESTAMP NOT NULL, " +              // DATETIME -> TIMESTAMP
                "active BOOLEAN DEFAULT TRUE, " +
                "FOREIGN KEY (itemId) REFERENCES items(id))";

        String createBidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                "id SERIAL PRIMARY KEY, " +
                "auctionId INT NOT NULL, " +
                "bidder VARCHAR(50) NOT NULL, " +
                "bidAmount DOUBLE PRECISION NOT NULL, " +
                "FOREIGN KEY (auctionId) REFERENCES auctions(id))";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createMembersTable);
            stmt.execute(createItemsTable);
            stmt.execute(createAuctionsTable);
            stmt.execute(createBidsTable);
            System.out.println("Supabase tables are ready to go");

        } catch (SQLException e) {
            System.out.println("Error creating Supabase tables!");
            e.printStackTrace();
        }
    }

    public static String fetchAuctionsByCategory(String category) {
        // 1. Create an empty JSON Array to hold all the items
        JsonArray jsonArray = new JsonArray();

        String query = "SELECT i.itemName, a.startingPrice, a.currentPrice, i.itemCondition, " +
                "i.imagePath, i.description, a.seller, a.endTime, a.priceIncrement " +
                "FROM items i " +
                "JOIN auctions a ON i.id = a.itemId " +
                "WHERE i.itemType = ? AND a.active = TRUE";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // 2. For every row in the database, create a new JSON Object
                JsonObject itemJson = new JsonObject();

                itemJson.addProperty("itemName", rs.getString("itemName"));
                itemJson.addProperty("startingPrice", rs.getDouble("startingPrice"));
                itemJson.addProperty("currentPrice", rs.getDouble("currentPrice"));
                itemJson.addProperty("itemCondition", rs.getString("itemCondition"));
                itemJson.addProperty("imagePath", rs.getString("imagePath"));
                itemJson.addProperty("description", rs.getString("description"));
                itemJson.addProperty("seller", rs.getString("seller"));
                itemJson.addProperty("endTime", rs.getTimestamp("endTime").getTime());
                itemJson.addProperty("priceIncrement", rs.getDouble("priceIncrement"));

                // 3. Add the object to our array
                jsonArray.add(itemJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 4. Return the beautifully formatted JSON string
        return jsonArray.toString();
    }

    public static void insertItemAndAuction(Items item, String seller, double initialPrice, double priceIncrement, java.sql.Timestamp endTime) {
        String insertItemSql = "INSERT INTO items (itemName, itemType, itemCondition, description, imagePath) VALUES (?, ?, ?, ?, ?)";
        String insertAuctionSql = "INSERT INTO auctions (itemId, seller, startingPrice, currentPrice, priceIncrement, endTime, active) VALUES (?, ?, ?, ?, ?, ?, TRUE)";

        try (Connection conn = getConnection()) {
            if (conn == null) return;

            // Step 1: Insert Item
            PreparedStatement itemStmt = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS);
            itemStmt.setString(1, item.getName());
            itemStmt.setString(2, item.getType());
            itemStmt.setString(3, item.getCondition());
            itemStmt.setString(4, item.getDescription());
            itemStmt.setString(5, item.getImagePath());
            itemStmt.executeUpdate();

            // Step 2: Get the generated ID
            ResultSet rs = itemStmt.getGeneratedKeys();
            if (rs.next()) {
                int generatedItemId = rs.getInt(1);

                // Step 3: Insert Auction
                PreparedStatement auctionStmt = conn.prepareStatement(insertAuctionSql);
                auctionStmt.setInt(1, generatedItemId);
                auctionStmt.setString(2, seller);
                auctionStmt.setDouble(3, initialPrice);
                auctionStmt.setDouble(4, initialPrice);
                auctionStmt.setDouble(5, priceIncrement);
                auctionStmt.setTimestamp(6, endTime);
                auctionStmt.executeUpdate();

                System.out.println("Success: Data saved to Supabase!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void registerUser(String username, String password, String email) throws SQLException {
        String query = "INSERT INTO members (username, password, email, role) VALUES (?, ?, ?, 'USER')";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            pstmt.executeUpdate();
        }
    }

    public static boolean verifyLogin(String username, String password) {
        String query = "SELECT * FROM members WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("Initializing Supabase Cloud Database...");
        initializeTables();
    }
}