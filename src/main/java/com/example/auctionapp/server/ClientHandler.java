package com.example.auctionapp.server;

import com.example.auctionapp.model.AuctionObserver;
import com.example.auctionapp.model.Items;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import com.example.auctionapp.exception.AuctionClosedException;
import com.example.auctionapp.exception.InvalidBidException;
import com.example.auctionapp.exception.SelfBiddingException;
import java.net.Socket;
import java.util.List;

import com.google.gson.Gson;
import com.example.auctionapp.model.NetworkMessage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClientHandler implements Runnable, AuctionObserver {
    private Socket clientSocket;
    private BufferedReader in;
    private String role;
    private PrintWriter out;
    private String username;
    private Gson gson = new Gson(); // Keep one instance for the whole class
    private Socket socket;


    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        AuctionManager.getInstance().registerObserver(this);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("SERVER LOG: Received: " + clientMessage);

                JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                String command = request.get("action").getAsString();

                // The switch now just "dispatches" to the right method
                switch (command) {
                    case "LOGIN" -> handleLogin(request);
                    case "ADMIN_FETCH_USERS" -> handleFetchUser(request);
                    case "REGISTER" -> handleRegister(request);
                    case "SUBMIT_AUCTION" -> handleSubmitAuction(request);
                    case "BID" -> handleBid(request);
                    case "GET_CATEGORY" -> handleGetCategory(request);
                    case "GET_MY_BIDS" -> handleGetMyBids(request);
                    case "GET_MY_AUCTIONS" -> handleGetMyAuction(request);
                    case "GET_AUCTION_DETAIL" -> handleGetAuctionDetail(request);
                    case "REGISTER_AUTOBID" -> handleRegisterAutoBid(request);
                    case "FETCH_NOTIF_HISTORY" -> handleFetchNotifHistory(request); // 🌟 Added this line!
                    case "ADMIN_DELETE_AUCTION" -> handleDeleteAuctions(request);
                    case "ADMIN_FETCH_ALL_AUCTIONS" -> handleFetchAuction(request);
                    case "LOGOUT" -> handleLogout();
                    case "ADMIN_DELETE_USER" -> handleDeleteUser(request);
                    case "SELLER_UPDATE_ITEM" -> handelUpdateItem(request);
                    default -> sendMessage(gson.toJson(new NetworkMessage("ERROR", "Unknown command", false)));
                }
            }
        } catch (Exception e) {
            System.out.println("User connection lost or error occurred.");
        } finally {
            closeConnection();
        }
    }






    private void handleLogin(JsonObject request) {
        String loginUser = request.get("username").getAsString();
        String loginPass = request.get("password").getAsString();

        // 1. Capture the role string ("USER", "ADMIN", or null if invalid)
        String userRole = DatabaseManager.verifyLoginAndGetRole(loginUser, loginPass);

        if (userRole != null) {
            this.username = loginUser;

            // 🌟 CRUCIAL: Save the role to this specific client socket thread instance!
            // This stops sneaky clients from modifying their local code to bypass admin checks.
            this.role = userRole;

            AuctionManager.getInstance().registerObserver(this);

            // 2. Build a comprehensive success payload containing the role
            JsonObject response = new JsonObject();
            response.addProperty("action", "LOGIN_SUCCESS");
            response.addProperty("message", "Welcome back!");
            response.addProperty("success", true);
            response.addProperty("username", loginUser);
            response.addProperty("role", userRole);

            sendMessage(response.toString());
            System.out.println("🔐 [SERVER]: @" + loginUser + " authenticated successfully as role: [" + userRole + "]");

        } else {
            // Keep your original custom network message format for errors!
            sendMessage(gson.toJson(new NetworkMessage("LOGIN_ERROR", "Incorrect credentials.", false)));
        }
    }

    private void handelUpdateItem(JsonObject request){
        int itemId = request.get("itemId").getAsInt();
        String name = request.get("itemName").getAsString();
        String type = request.get("itemType").getAsString();
        String condition = request.get("itemCondition").getAsString();
        String description = request.get("description").getAsString();

        // Call the database manager to actually save the changes!
        // Note: 'this.username' ensures they can only edit their own items
        boolean success = DatabaseManager.updateItemDetails(itemId, this.username, name, type, condition, description);


        JsonObject response = new JsonObject();
        response.addProperty("action", "SELLER_UPDATE_ITEM_RESPONSE");
        response.addProperty("success", success);

        sendMessage(response.toString());
    }

    private void handleFetchAuction(JsonObject request){
        if (!"ADMIN".equalsIgnoreCase(this.role)) {
            System.err.println("Unauthorized access.");
            return;
        }

        // 2. Fetch the data from the database
        JsonArray auctionsList = DatabaseManager.getAllAuctionsForAdmin();

        // 3. Send the payload back to the client
        JsonObject response = new JsonObject();
        response.addProperty("action", "ADMIN_FETCH_ALL_AUCTIONS_RESPONSE");
        response.addProperty("success", true);
        response.add("data", auctionsList);

        sendMessage(response.toString());
        System.out.println("📤 [SERVER]: Sent all active auctions to Admin.");    }

    private void handleDeleteAuctions(JsonObject request){
        if (!"ADMIN".equalsIgnoreCase(this.role)) {
            System.err.println("Security Exception: Unauthorized auction deletion attempt!");
            return;
        }

        int auctionId = request.get("auctionId").getAsInt();
        boolean success = DatabaseManager.hardDeleteAuction(auctionId);

        JsonObject response = new JsonObject();
        response.addProperty("action", "ADMIN_DELETE_AUCTION_RESPONSE");
        response.addProperty("success", success);
        response.addProperty("auctionId", auctionId);

        sendMessage(response.toString());
    }


    private void handleDeleteUser(JsonObject request){
        if (!"ADMIN".equalsIgnoreCase(this.role)) {
            System.err.println("🚨 Security Warning: Non-admin tried to execute a deletion command!");
            return;
        }
        String targetUser = request.get("targetUsername").getAsString();

        // Run the SQL command
        boolean dbSuccess = DatabaseManager.deleteUser(targetUser);

        JsonObject reply = new JsonObject();
        reply.addProperty("action", "ADMIN_DELETE_USER_RESPONSE");
        reply.addProperty("success", dbSuccess);
        reply.addProperty("message", dbSuccess ? "User deleted successfully." : "Database failed to drop row.");

        sendMessage(reply.toString());
    }

    private void handleFetchUser(JsonObject request){
        if (!"ADMIN".equalsIgnoreCase(this.role)) {
            System.err.println("❌ Security Warning: Unauthorized attempt to fetch structural user rows!");
            return;
        }

        // Call your database manager to fetch the actual rows
        com.google.gson.JsonArray userDatabaseData = DatabaseManager.getAllRegisteredUsers();

        // Bundle it up into a response packet
        com.google.gson.JsonObject reply = new com.google.gson.JsonObject();
        reply.addProperty("action", "ADMIN_FETCH_USERS_RESPONSE");
        reply.add("users", userDatabaseData);

        sendMessage(reply.toString());
    }
    /**
     * Handles the client's request to pull their notification history from the database.
     */
    private void handleFetchNotifHistory(JsonObject request) {
        try {
            String targetUser = request.get("username").getAsString();

            // 1. Pull the rows straight from the database table
            List<JsonObject> notifications = DatabaseManager.getNotificationsForUser(targetUser);

            // 2. Wrap them neatly into a response payload array
            JsonObject responsePacket = new JsonObject();
            responsePacket.addProperty("action", "NOTIF_HISTORY_RESPONSE");

            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            if (notifications != null) {
                for (JsonObject notif : notifications) {
                    array.add(notif);
                }
            }
            responsePacket.add("data", array);

            // 3. Send it back down the socket stream to this specific client
            sendMessage(responsePacket.toString());

            System.out.println("📤 [SERVER]: Successfully streamed notification history back to @" + targetUser);

        } catch (Exception e) {
            System.err.println("❌ [SERVER ERROR] Failed to fetch notification history: " + e.getMessage());
            sendMessage(gson.toJson(new NetworkMessage("ERROR", "Failed to load notifications", false)));
        }
    }

    private void handleRegisterAutoBid(JsonObject request) {
        System.out.println("🚨 [TRACER]: handleRegisterAutoBid was just triggered!");
        // 🌟 REMOVED: Direct database mutations from the network layer.
        // Thread safety and sequential operations are now handled directly inside the memory lock pipeline.

        String registrantName = "Unknown"; // Declared outside try block for visibility in catch scopes

        try {
            // 1. 🌟 FIX: Extract the nested JsonObject directly! No string conversion needed.
            JsonObject autoBidPayload = request.getAsJsonObject("data");

            int auctionId = autoBidPayload.get("auctionId").getAsInt();
            double maxBudget = autoBidPayload.get("maxBudget").getAsDouble();

            // Establish identity
            registrantName = (this.username != null) ? this.username : autoBidPayload.get("username").getAsString();

            // 2. Route to manager
            AuctionManager.getInstance().submitAutoBid(auctionId, registrantName, maxBudget);

            // 3. Respond
            sendMessage(gson.toJson(new NetworkMessage("AUTOBID_RESPONSE", "Auto-bid configured successfully!", true)));
        } catch (SelfBiddingException e) {
            // Handle Policy Violation individually
            System.err.println("⚠️ [POLICY VIOLATION] User '" + registrantName + "' attempted to set an auto-bid on their own item!");
            sendMessage(gson.toJson(new NetworkMessage("AUTOBID_RESPONSE", e.getMessage(), false)));

        } catch (AuctionClosedException | InvalidBidException e) {
            // Catch standard operational exceptions and send their specific errors back to the UI
            sendMessage(gson.toJson(new NetworkMessage("AUTOBID_RESPONSE", e.getMessage(), false)));

        } catch (Exception e) {
            // Failsafe catch-all block to prevent client-handler threads from crashing unexpectedly
            System.err.println("[SERVER ERROR] Unexpected exception thrown inside handleRegisterAutoBid execution stream:");
            e.printStackTrace();
            sendMessage(gson.toJson(new NetworkMessage("AUTOBID_RESPONSE", "Server processing error encountered.", false)));
        }
    }

    private void handleGetAuctionDetail(JsonObject request) {
        try {
            int targetId = Integer.parseInt(request.get("data").getAsString());

            // Invoke our custom subquery database engine method
            String jsonPayload = DatabaseManager.fetchAuctionDetailById(targetId);

            String responseData = (jsonPayload != null) ? jsonPayload : "ERROR";
            sendMessage(gson.toJson(new NetworkMessage("AUCTION_DETAIL_RESPONSE", responseData, true)));

        } catch (Exception e) {
            System.err.println("[SERVER ERROR] Failed handling incoming detailed view packet query:");
            e.printStackTrace();
            sendMessage(gson.toJson(new NetworkMessage("AUCTION_DETAIL_RESPONSE", "ERROR", false)));
        }
    }



    private void handleRegister(JsonObject request) {
        String newUsername = request.get("username").getAsString();
        String newPassword = request.get("password").getAsString();
        String newEmail = request.get("email").getAsString();

        if (!newEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            sendMessage(gson.toJson(new NetworkMessage("REGISTER_ERROR", "Invalid email format.", false)));
            return;
        }

        try {
            DatabaseManager.registerUser(newUsername, newPassword, newEmail);
            sendMessage(gson.toJson(new NetworkMessage("REGISTER_SUCCESS", "Welcome!", true)));
        } catch (Exception e) {
            String msg = e.getMessage().toLowerCase().contains("email") ? "Email taken" : "Username taken";
            sendMessage(gson.toJson(new NetworkMessage("REGISTER_ERROR", msg, false)));
        }
    }

    private void handleGetMyBids(JsonObject request) {
        try {
            // 🌟 FIX 1: Safely handle both payload styles (depending on if client sent "data" or "username")
            String bidderUser = "";
            if (request.has("data")) {
                bidderUser = request.get("data").getAsString();
            } else if (request.has("username")) {
                bidderUser = request.get("username").getAsString();
            }

            String data = DatabaseManager.fetchAuctionsByBidder(bidderUser);

            // 🌟 FIX 2: Return "[]" (Empty JSON Array) instead of "NO_ITEMS" so the client parser doesn't crash!
            String response = (data == null || data.isEmpty()) ? "[]" : data;
            sendMessage(gson.toJson(new NetworkMessage("MY_BIDS_RESPONSE", response, true)));

        } catch (Exception e) {
            System.err.println("[SERVER ERROR] Exception inside handleGetMyBids:");
            e.printStackTrace();
            // Send an empty array on error to safely clear the skeletons
            sendMessage(gson.toJson(new NetworkMessage("MY_BIDS_RESPONSE", "[]", false)));
        }
    }

    private void handleSubmitAuction(JsonObject request) {
        try {
            String rawImagePath = request.get("imagePath").getAsString();
            String finalDatabasePath = rawImagePath; // Fallback to raw base64 if upload fails

            // Check if the image path looks like a Base64 string instead of a local file path/URL
            if (rawImagePath != null && (rawImagePath.startsWith("/9j/") || rawImagePath.contains(","))) {
                try {
                    System.out.println("[SERVER LOG] Base64 image detected. Attempting Supabase upload...");

                    // 1. Clean the base64 string if it contains a data URI prefix
                    String cleanBase64 = rawImagePath;
                    if (cleanBase64.contains(",")) {
                        cleanBase64 = cleanBase64.split(",")[1];
                    }

                    // 2. Decode the string into a byte array
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(cleanBase64.trim());
                    System.out.println("[SERVER LOG] Successfully decoded image bytes: " + imageBytes.length);

                    // 3. Generate a unique name for the file in the bucket
                    String uniqueFileName = "auction_" + System.currentTimeMillis() + ".jpg";

                    // 4. Upload it using your utility class


                    String publicUrl = DatabaseManager.SupabaseStorageManager.uploadImageToBucket(imageBytes, uniqueFileName);

                    if (publicUrl != null && !publicUrl.isEmpty()) {
                        finalDatabasePath = publicUrl;
                        System.out.println("[SERVER LOG] Upload successful! Public URL: " + finalDatabasePath);
                    } else {
                        System.err.println("[SERVER WARNING] Upload method returned an empty URL. Falling back to text.");
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("[SERVER ERROR] Base64 decoding failed: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("[SERVER ERROR] Supabase storage upload sequence failed:");
                    e.printStackTrace();
                }
            }

            // Create the item using the resulting path (either the public cloud URL or fallback text)
            Items newItem = new Items(
                    0,
                    request.get("itemType").getAsString(),
                    request.get("itemName").getAsString(),
                    request.get("itemCondition").getAsString(),
                    request.get("description").getAsString(),
                    finalDatabasePath
            );

            DatabaseManager.insertItemAndAuction(
                    newItem,
                    request.get("seller").getAsString(),
                    request.get("price").getAsDouble(),
                    request.get("increment").getAsDouble(),
                    new java.sql.Timestamp(request.get("endTime").getAsLong())
            );

            sendMessage(gson.toJson(new NetworkMessage("SUBMIT_AUCTION_RESPONSE", "Posted!", true)));
            System.out.println("✨ [SERVER]: Successfully inserted auction and sent confirmation to client.");

        } catch (Exception e) {
            System.err.println("[SERVER ERROR] Error handling SUBMIT_AUCTION request:");
            e.printStackTrace();

            // 🌟 FIX: Change "SUBMIT_ERROR" to "SUBMIT_AUCTION_RESPONSE" so errors unlock the UI too!
            sendMessage(gson.toJson(new NetworkMessage("SUBMIT_AUCTION_RESPONSE", "Server error: " + e.getMessage(), false)));
        }
    }


    private void handleBid(JsonObject request) {
        // 🌟 REMOVED: synchronized (DatabaseManager.class)
        // Thread safety is now handled elegantly by individual item locks inside AuctionSession
        // and 'FOR UPDATE' row states in SQL. No more global server-wide bottlenecks!

        String bidderName = "Unknown"; // Declared outside try block for visibility in catch scopes

        try {
            // 1. Extract the inner string data envelope and decode the parameters
            String innerJsonData = request.get("data").getAsString();
            JsonObject bidPayload = JsonParser.parseString(innerJsonData).getAsJsonObject();

            int auctionId = bidPayload.get("auctionId").getAsInt();
            double bidAmount = bidPayload.get("bidAmount").getAsDouble();

            // Establish the identity of who is placing this bid
            bidderName = (this.username != null) ? this.username : bidPayload.get("username").getAsString();

            // 2. 🌟 ROUTE THROUGH THE MANAGER: Route the bid to AuctionManager instead of direct DB mutations
            // This ensures the server's running active memory and locks are respected!
            AuctionManager.getInstance().submitBid(auctionId, bidderName, bidAmount);

            // 3. If no exceptions were thrown above, the bid is officially approved!
            JsonObject replyPayload = new JsonObject();
            replyPayload.addProperty("action", "BID_RESPONSE");
            replyPayload.addProperty("success", true);
            replyPayload.addProperty("message", "Bid accepted successfully!");
            replyPayload.addProperty("bidAmount", bidAmount);

            sendMessage(replyPayload.toString());

        } catch (SelfBiddingException e) {
            // 🌟 Handle Policy Violation individually
            System.err.println("⚠️ [POLICY VIOLATION] User '" + bidderName + "' attempted to bid on their own item!");
            sendMessage(gson.toJson(new NetworkMessage("BID_RESPONSE", e.getMessage(), false)));

        } catch (AuctionClosedException | InvalidBidException e) {
            // 🌟 Catch standard operational exceptions and send their specific errors back to the UI
            sendMessage(gson.toJson(new NetworkMessage("BID_RESPONSE", e.getMessage(), false)));

        } catch (Exception e) {
            // Failsafe catch-all block to prevent client-handler threads from crashing unexpectedly
            System.err.println("[SERVER ERROR] Unexpected exception thrown inside handleBid execution stream:");
            e.printStackTrace();
            sendMessage(gson.toJson(new NetworkMessage("BID_RESPONSE", "Server processing error encountered.", false)));
        }
    }

    private void handleGetMyAuction(JsonObject request) {
        try {
            // 🌟 FIX 1: Safe payload extraction
            String targetUser = "";
            if (request.has("data")) {
                targetUser = request.get("data").getAsString();
            } else if (request.has("username")) {
                targetUser = request.get("username").getAsString();
            }

            String data = DatabaseManager.fetchAuctionsBySeller(targetUser);

            // 🌟 FIX 2: Empty JSON Array fallback
            String response = (data == null || data.isEmpty()) ? "[]" : data;
            sendMessage(gson.toJson(new NetworkMessage("MY_AUCTIONS_RESPONSE", response, true)));

        } catch (Exception e) {
            System.err.println("[SERVER ERROR] Exception processing GET_MY_AUCTIONS execution thread:");
            e.printStackTrace();
            sendMessage(gson.toJson(new NetworkMessage("MY_AUCTIONS_RESPONSE", "[]", false)));
        }
    }

    private void handleGetCategory(JsonObject request) {
        String cat = request.get("data").getAsString();
        String data = DatabaseManager.fetchAuctionsByCategory(cat);

        String response = (data == null || data.isEmpty()) ? "NO_ITEMS" : data;
        sendMessage(gson.toJson(new NetworkMessage("CATEGORY_RESPONSE", response, true)));
    }

    private void handleLogout() {
        System.out.println(" [Auth] User logging out: " + this.username);

        // 1. Reset the connection identity state
        this.username = null;

        // 2. Reply back with a success confirmation envelope
        sendMessage(gson.toJson(new NetworkMessage("LOGOUT_RESPONSE", "Logged out successfully", true)));
    }

    // --- HELPERS ---
    @Override
    public void onBidUpdated(int auctionId, double newPrice, String highestBidder) {
        try {
            // Package the live update into a clean JSON envelope
            JsonObject liveUpdate = new JsonObject();
            liveUpdate.addProperty("action", "LIVE_BID_UPDATE");
            liveUpdate.addProperty("auctionId", auctionId);
            liveUpdate.addProperty("newPrice", newPrice);
            liveUpdate.addProperty("highestBidder", highestBidder);

            // Push it down the socket to the user's JavaFX screen!
            sendMessage(liveUpdate.toString());

        } catch (Exception e) {
            System.err.println(" Failed to send live update to client: " + this.username);
        }
    }

    @Override
    public void onAuctionClosed(int auctionId, String itemName, String winner, double finalPrice) {
        JsonObject packet = new JsonObject();
        packet.addProperty("action", "GLOBAL_ANNOUNCEMENT_CLOSED");
        packet.addProperty("auctionId", auctionId);
        packet.addProperty("itemName", itemName);
        packet.addProperty("winner", winner);
        packet.addProperty("finalPrice", finalPrice);
        packet.addProperty("timeString", java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));

        sendMessage(packet.toString());
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush();
        }
    }

    private void closeConnection() {
        try {
            AuctionServer.activeClients.remove(this);
            AuctionManager.getInstance().removeObserver(this);
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}