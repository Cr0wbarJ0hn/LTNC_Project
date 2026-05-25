package com.example.auctionapp.server;

import com.example.auctionapp.model.AuctionSession;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.example.auctionapp.model.Items;
import java.sql.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import com.example.auctionapp.exception.AuctionClosedException;
import com.example.auctionapp.exception.InvalidBidException;
import com.example.auctionapp.exception.SelfBiddingException;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Iterator;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres";
    private static final String USER = "postgres.dxhyrntoijscgblkjwys";
    private static final String PASSWORD = "Ekko2007@1410";

    public static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver missing!");
            return null;
        } catch (SQLException e) {
            System.err.println("--- CONNECTION FAILED ---");
            e.printStackTrace();
            return null;
        }
    }

    public static void initializeActiveAuctionsInMemory() {

        String query = "SELECT id, currentPrice, priceIncrement, endTime, " +
                "(SELECT bidder FROM bids WHERE auctionId = auctions.id ORDER BY bidAmount DESC LIMIT 1) AS highestBidder " +
                "FROM auctions WHERE endTime > NOW()";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                double price = rs.getDouble("currentPrice");
                double increment = rs.getDouble("priceIncrement");
                java.sql.Timestamp endTs = rs.getTimestamp("endTime");
                String topBidder = rs.getString("highestBidder");

                LocalDateTime endTime = endTs.toLocalDateTime();

                // Create the session object
                AuctionSession session = new AuctionSession(id, price, increment, endTime, topBidder);

                // Push it into the manager's memory
                AuctionManager.getInstance().addActiveAuction(session);
                count++;
            }
            System.out.println(" [SYSTEM] Successfully loaded " + count + " active auctions into server memory.");

        } catch (SQLException e) {
            System.err.println(" [SYSTEM ERROR] Failed to load active auctions into memory:");
            e.printStackTrace();
        }
    }

    public static byte[] compressAndResizeImage(byte[] originalBytes, int targetWidth) {
        try {
            // 1. Convert byte array back into a live BufferedImage
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (originalImage == null) {
                System.err.println("[Compression] Failed to read image bytes. Returning original.");
                return originalBytes;
            }

            // 2. Calculate proportional height so the image doesn't get stretched or squished
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // If the image is already smaller than our target width, don't upscale it!
            if (originalWidth <= targetWidth) {
                targetWidth = originalWidth;
            }
            int targetHeight = (originalHeight * targetWidth) / originalWidth;

            // 3. Create a new blank canvas with our downscaled dimensions
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();

            // Apply high-quality rendering hints so it doesn't look pixelated
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the old image onto the new small canvas
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            // 4. Compress the JPEG quality down to 75%
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) throw new IllegalStateException("No writers found for JPG");

            ImageWriter writer = writers.next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.75f); // 0.75 = 75% quality (sweet spot for web)
            }

            // Write the compressed file to our byte stream
            writer.write(null, new IIOImage(resizedImage, null, null), param);

            // Clean up resources
            writer.dispose();
            ios.close();

            System.out.println("[Compression] Success! Shrunk from " + (originalBytes.length / 1024) + "KB down to " + (baos.size() / 1024) + "KB");
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("[Compression] Error during image processing, using raw bytes: " + e.getMessage());
            return originalBytes;
        }
    }


    public static class SupabaseStorageManager {
        private static final String PROJECT_ID = "dxhyrntoijscgblkjwys";
        private static final String BUCKET_NAME = "auction-images";

        // --- FIX 2: REPLACE THIS with your "service_role" secret key from Supabase Dashboard ---
        // Go to Settings -> API -> Find "service_role" secret token.
        private static final String API_KEY = System.getenv("SUPABASE_SERVICE_KEY");

        public static String uploadImageToBucket(byte[] imageBytes, String fileName) {
            String uploadUrl = "https://" + PROJECT_ID + ".supabase.co/storage/v1/object/" + BUCKET_NAME + "/" + fileName;

            try {
                // --- NEW: COMPRESS AND RESIZE BEFORE UPLOADING ---
                // We set a max width boundary of 800px. Height scales automatically!
                System.out.println("[Storage] Optimizing image file asset parameters...");
                byte[] optimizedBytes = compressAndResizeImage(imageBytes, 800);

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uploadUrl))
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("apiKey", API_KEY)
                        .header("Content-Type", "image/jpeg")
                        .header("x-upsert", "true") // --- ADD THIS LINE HERE ---
                        .POST(HttpRequest.BodyPublishers.ofByteArray(optimizedBytes))
                        .build();

                System.out.println("[Storage] Sending compressed upload request to Supabase...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("[Storage] Supabase Response Code: " + response.statusCode());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    return "https://" + PROJECT_ID + ".supabase.co/storage/v1/object/public/" + BUCKET_NAME + "/" + fileName;
                } else {
                    System.err.println("[Storage] Error Response Body: " + response.body());
                    return null;
                }
            } catch (Exception e) {
                System.err.println("[Storage] Exception during upload:");
                e.printStackTrace();
                return null;
            }
        }
    }

    public static void initializeTables() {
        String createMembersTable = "CREATE TABLE IF NOT EXISTS members (" +
                "email VARCHAR(100) UNIQUE, " +
                "username VARCHAR(50) PRIMARY KEY, " +
                "password VARCHAR(50) NOT NULL, " +
                "role VARCHAR(20) DEFAULT 'USER')";

        String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                "id SERIAL PRIMARY KEY, " +
                "itemName VARCHAR(100) NOT NULL, " +
                "itemType VARCHAR(50), " +
                "itemCondition VARCHAR(50), " +
                "description TEXT, " +
                "imagePath VARCHAR(500))";

        String createAuctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                "id SERIAL PRIMARY KEY, " +
                "itemId INT NOT NULL, " +
                "seller VARCHAR(50) NOT NULL, " +
                "startingPrice DOUBLE PRECISION NOT NULL, " +
                "currentPrice DOUBLE PRECISION NOT NULL, " +
                "priceIncrement DOUBLE PRECISION NOT NULL, " +
                "endTime TIMESTAMP NOT NULL, " +
                "active BOOLEAN DEFAULT TRUE, " +
                "FOREIGN KEY (itemId) REFERENCES items(id))";

        String createBidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                "id SERIAL PRIMARY KEY, " +
                "auctionId INT NOT NULL, " +
                "bidder VARCHAR(50) NOT NULL, " +
                "bidAmount DOUBLE PRECISION NOT NULL, " +
                "bidTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " + // Tracks exactly when the bid arrived
                "FOREIGN KEY (auctionId) REFERENCES auctions(id), " +
                "FOREIGN KEY (bidder) REFERENCES members(username))"; // Prevents ghost bidders
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

    public static void executeSafeBidTransaction(int auctionId, String username, double proposedBid)
            throws AuctionClosedException, InvalidBidException, SelfBiddingException, SQLException {

        String query = "SELECT currentPrice, priceIncrement, endTime, seller FROM auctions WHERE id = ? FOR UPDATE";
        String update = "UPDATE auctions SET currentprice = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(query)) {
                checkStmt.setInt(1, auctionId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        double currentPrice = rs.getDouble("currentPrice");
                        double increment = rs.getDouble("priceIncrement");
                        java.sql.Timestamp endTime = rs.getTimestamp("endTime");
                        String seller = rs.getString("seller");

                        // 1. Validation Bouncers
                        if (endTime.getTime() < System.currentTimeMillis()) {
                            conn.rollback();
                            throw new AuctionClosedException("This auction has already ended!");
                        }

                        if (seller != null && seller.equalsIgnoreCase(username)) {
                            conn.rollback();
                            throw new SelfBiddingException("Security Warning: You cannot bid on your own auction item!");
                        }

                        double minimumRequired = currentPrice + increment;
                        if (proposedBid < minimumRequired) {
                            conn.rollback();
                            throw new InvalidBidException("Bid too low! Minimum required is $" + String.format("%.2f", minimumRequired));
                        }

                        // 🌟 2. THE FIX: Actually execute the update statement!
                        try (PreparedStatement updateStmt = conn.prepareStatement(update)) {
                            updateStmt.setDouble(1, proposedBid);  // Put the new bid price into the first '?'
                            updateStmt.setInt(2, auctionId);       // Put the auction ID into the second '?'
                            updateStmt.executeUpdate();            // Push it to the database table!
                            System.out.println("💾 [DB SUCCESS]: Written to disk. New price: " + proposedBid);
                        }

                        // 3. Commit the transaction safely now that the update ran
                        conn.commit();
                        return;

                    } else {
                        conn.rollback();
                        throw new InvalidBidException("Auction item target ID not found.");
                    }
                }
            } catch (Exception innerException) {
                conn.rollback();
                throw innerException;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    public static String fetchAuctionDetailById(int auctionId) {
        JsonObject auction = new JsonObject();

        // We use a subquery to grab the highest bidder directly from your 'bids' table!
        String sql = "SELECT a.id, i.itemname, a.startingprice, a.currentprice, " +
                "i.itemcondition, i.imagepath, i.description, a.seller, " +
                "a.endtime, a.priceincrement, " +
                "(SELECT bidder FROM bids WHERE auctionId = a.id ORDER BY bidAmount DESC LIMIT 1) AS leading_bidder " +
                "FROM auctions a " +
                "JOIN items i ON a.itemid = i.id " +
                "WHERE a.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    auction.addProperty("id", rs.getInt("id"));
                    auction.addProperty("itemName", rs.getString("itemname"));
                    auction.addProperty("startingPrice", rs.getDouble("startingprice"));
                    auction.addProperty("currentPrice", rs.getDouble("currentprice"));
                    auction.addProperty("itemCondition", rs.getString("itemcondition"));

                    String imgPath = rs.getString("imagepath");
                    auction.addProperty("imagePath", imgPath != null ? imgPath : "");
                    auction.addProperty("description", rs.getString("description"));
                    auction.addProperty("seller", rs.getString("seller"));

                    java.sql.Timestamp endTimeTs = rs.getTimestamp("endtime");
                    auction.addProperty("endTime", endTimeTs != null ? endTimeTs.getTime() : 0);
                    auction.addProperty("priceIncrement", rs.getDouble("priceincrement"));

                    // Extract the subquery result safely
                    String lastBidder = rs.getString("leading_bidder");
                    auction.addProperty("leadingBidder", lastBidder != null ? lastBidder : "No bids yet");

                    return auction.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("[DATABASE ERROR] Failed fetching details for auction ID: " + auctionId);
            e.printStackTrace();
        }
        return null;
    }

    public static String fetchAuctionsByBidder(String username) {
        JsonArray auctionsArray = new JsonArray();

        // Uses DISTINCT so an item only shows up once, even if the user bid on it 50 times
        String sql = "SELECT DISTINCT a.id, i.itemname, a.startingprice, a.currentprice, " +
                "i.itemcondition, i.imagepath, i.description, a.seller, " +
                "a.endtime, a.priceincrement " +
                "FROM auctions a " +
                "JOIN items i ON a.itemid = i.id " +
                "JOIN bids b ON a.id = b.auctionid " +
                "WHERE b.bidder = ? " +
                "ORDER BY a.endtime DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject auction = new JsonObject();

                    auction.addProperty("id", rs.getInt("id"));
                    auction.addProperty("itemName", rs.getString("itemname"));
                    auction.addProperty("startingPrice", rs.getDouble("startingprice"));
                    auction.addProperty("currentPrice", rs.getDouble("currentprice"));
                    auction.addProperty("itemCondition", rs.getString("itemcondition"));

                    String imgPath = rs.getString("imagepath");
                    auction.addProperty("imagePath", imgPath != null ? imgPath : "");

                    auction.addProperty("description", rs.getString("description"));
                    auction.addProperty("seller", rs.getString("seller"));

                    java.sql.Timestamp endTimeTs = rs.getTimestamp("endtime");
                    long endTimeMillis = (endTimeTs != null) ? endTimeTs.getTime() : 0;
                    auction.addProperty("endTime", endTimeMillis);

                    auction.addProperty("priceIncrement", rs.getDouble("priceincrement"));

                    auctionsArray.add(auction);
                }
            }

            return auctionsArray.size() > 0 ? auctionsArray.toString() : "NO_ITEMS";

        } catch (Exception e) {
            System.err.println("[DATABASE ERROR] Failed compiling participated bids for user context: " + username);
            e.printStackTrace();
            return "NO_ITEMS";
        }
    }



    public static boolean closeAuction(int auctionId) {
        String updateSQL = "UPDATE auctions SET active = false WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSQL)) {

            stmt.setInt(1, auctionId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Database: Auction " + auctionId + " is now CLOSED.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to close auction in database!");
            e.printStackTrace();
        }
        return false;
    }

    public static String fetchAuctionsByCategory(String category) {
        JsonArray jsonArray = new JsonArray();

        // 🌟 SQL STRINGS MANUALLY ADJUSTED TO MATCH YOUR INITIALIZETABLES SCHEMA EXACTLY
        String query = "SELECT a.id, i.itemName, a.startingPrice, a.currentPrice, i.itemCondition, " +
                "i.imagePath, i.description, a.seller, a.endTime, a.priceIncrement " +
                "FROM items i " +
                "JOIN auctions a ON i.id = a.itemId " +
                "WHERE i.itemType = ? AND a.active = TRUE";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                JsonObject itemJson = new JsonObject();

                // Extracted IDs mapped to UI expectations
                itemJson.addProperty("id", rs.getInt("id"));

                // 🌟 READ THE EXACT CASE TARGETED IN THE SQL SELECT QUERY STATEMENT ABOVE:
                itemJson.addProperty("itemName", rs.getString("itemName"));
                itemJson.addProperty("startingPrice", rs.getDouble("startingPrice"));
                itemJson.addProperty("currentPrice", rs.getDouble("currentPrice"));
                itemJson.addProperty("itemCondition", rs.getString("itemCondition"));
                itemJson.addProperty("imagePath", rs.getString("imagePath"));
                itemJson.addProperty("description", rs.getString("description"));
                itemJson.addProperty("seller", rs.getString("seller"));

                if (rs.getTimestamp("endTime") != null) {
                    itemJson.addProperty("endTime", rs.getTimestamp("endTime").getTime());
                } else {
                    itemJson.addProperty("endTime", System.currentTimeMillis());
                }

                itemJson.addProperty("priceIncrement", rs.getDouble("priceIncrement"));

                jsonArray.add(itemJson);
            }
        } catch (Exception e) {
            System.err.println("[SERVER ERROR] Failed to fetch auctions by category:");
            e.printStackTrace();
        }
        return jsonArray.toString();
    }

    public static String fetchAuctionsBySeller(String sellerName) {
        JsonArray auctionsArray = new JsonArray();

        // SQL uses standardized lowercase column profiles matching your existing Supabase structure
        String sql = "SELECT a.id, i.itemname, a.startingprice, a.currentprice, " +
                "i.itemcondition, i.imagepath, i.description, a.seller, " +
                "a.endtime, a.priceincrement " +
                "FROM auctions a " +
                "JOIN items i ON a.itemid = i.id " +
                "WHERE a.seller = ? " +
                "ORDER BY a.endtime DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sellerName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject auction = new JsonObject();

                    // Pack schema keys exactly as expected by your myAuctionController loop definitions
                    auction.addProperty("id", rs.getInt("id"));
                    auction.addProperty("itemName", rs.getString("itemname"));
                    auction.addProperty("startingPrice", rs.getDouble("startingprice"));
                    auction.addProperty("currentPrice", rs.getDouble("currentprice"));
                    auction.addProperty("itemCondition", rs.getString("itemcondition"));

                    String imgPath = rs.getString("imagepath");
                    auction.addProperty("imagePath", imgPath != null ? imgPath : "");

                    auction.addProperty("description", rs.getString("description"));
                    auction.addProperty("seller", rs.getString("seller"));

                    // Translate Postgres Timestamps safely into long epochs for your interface timer clocks
                    java.sql.Timestamp endTimeTs = rs.getTimestamp("endtime");
                    long endTimeMillis = (endTimeTs != null) ? endTimeTs.getTime() : 0;
                    auction.addProperty("endTime", endTimeMillis);

                    auction.addProperty("priceIncrement", rs.getDouble("priceincrement"));

                    auctionsArray.add(auction);
                }
            }

            return auctionsArray.size() > 0 ? auctionsArray.toString() : "NO_ITEMS";

        } catch (Exception e) {
            System.err.println("[DATABASE ERROR] Failed compiling hosted auctions for user payload context: " + sellerName);
            e.printStackTrace();
            return "NO_ITEMS";
        }
    }

    public static void insertItemAndAuction(Items item, String seller, double initialPrice, double priceIncrement, java.sql.Timestamp endTime) {
        String insertItemSql = "INSERT INTO items (itemName, itemType, itemCondition, description, imagePath) VALUES (?, ?, ?, ?, ?)";
        // Ensure table and column casings match your DB schema (PostgreSQL defaults to lower case unless quoted)
        String insertAuctionSql = "INSERT INTO auctions (itemId, seller, startingPrice, currentPrice, priceIncrement, endTime, active) VALUES (?, ?, ?, ?, ?, ?, TRUE)";

        try (Connection conn = getConnection()) {
            if (conn == null) return;

            String databaseImagePath = item.getImagePath();

            if (databaseImagePath != null && !databaseImagePath.startsWith("http")) {
                try {
                    // --- FIX 1: STRIP BASE64 DATA PREFIXES AND WHITESPACE ---
                    if (databaseImagePath.contains(",")) {
                        databaseImagePath = databaseImagePath.substring(databaseImagePath.indexOf(",") + 1);
                    }
                    databaseImagePath = databaseImagePath.replaceAll("\\s", "");

                    byte[] imageBytes = java.util.Base64.getDecoder().decode(databaseImagePath);
                    String uniqueFileName = "item_" + System.currentTimeMillis() + ".jpg";

                    String publicImageUrl = SupabaseStorageManager.uploadImageToBucket(imageBytes, uniqueFileName);

                    if (publicImageUrl != null) {
                        databaseImagePath = publicImageUrl;
                        System.out.println("Uploaded image successfully! URL: " + databaseImagePath);
                    } else {
                        System.err.println("[Warning] Storage upload returned null. Falling back to storing Base64 text in DB.");
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("CRITICAL: Failed to decode Base64 image data! Invalid characters detected. Saving raw text fallback.");
                    e.printStackTrace();
                }
            }

            // 1. Execute item insertion and fetch its generated key
            PreparedStatement itemStmt = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS);
            itemStmt.setString(1, item.getName());
            itemStmt.setString(2, item.getType());
            itemStmt.setString(3, item.getCondition());
            itemStmt.setString(4, item.getDescription());
            itemStmt.setString(5, databaseImagePath);
            itemStmt.executeUpdate();

            ResultSet rs = itemStmt.getGeneratedKeys();
            if (rs.next()) {
                int generatedItemId = rs.getInt(1);

                // 🌟 CHANGE A: Added Statement.RETURN_GENERATED_KEYS here so we can grab the generated auction ID
                PreparedStatement auctionStmt = conn.prepareStatement(insertAuctionSql, Statement.RETURN_GENERATED_KEYS);
                auctionStmt.setInt(1, generatedItemId);
                auctionStmt.setString(2, seller);
                auctionStmt.setDouble(3, initialPrice);
                auctionStmt.setDouble(4, initialPrice);
                auctionStmt.setDouble(5, priceIncrement);
                auctionStmt.setTimestamp(6, endTime);
                auctionStmt.executeUpdate();

                // 🌟 CHANGE B: Extract the newly created auction ID and build our live RAM Session
                ResultSet rsAuction = auctionStmt.getGeneratedKeys();
                if (rsAuction.next()) {
                    int generatedAuctionId = rsAuction.getInt(1);

                    // Convert the incoming java.sql.Timestamp cleanly over to java.time.LocalDateTime
                    LocalDateTime endTimeLocal = endTime.toLocalDateTime();

                    // Instantiate the session matching our system constraints
                    AuctionSession newSession = new AuctionSession(
                            generatedAuctionId,
                            initialPrice,      // Starting price acts as the baseline price
                            priceIncrement,
                            endTimeLocal,
                            null               // Brand new item has no highest bidder yet!
                    );

                    // 🌟 INJECT DIRECTLY INTO LIVE SERVER MEMORY:
                    // This makes the item immediately available for bidding across all client sockets!
                    AuctionManager.getInstance().addActiveAuction(newSession);

                    System.out.println("Success: Data saved to Supabase & synchronized to active server RAM! (Auction ID: " + generatedAuctionId + ")");
                }
            }
        } catch (SQLException e) {
            System.err.println(" Database operation failed inside insertItemAndAuction:");
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