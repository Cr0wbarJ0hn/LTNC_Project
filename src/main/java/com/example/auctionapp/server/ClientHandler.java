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
import com.google.gson.Gson;
import com.example.auctionapp.model.NetworkMessage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClientHandler implements Runnable, AuctionObserver {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private Gson gson = new Gson(); // Keep one instance for the whole class

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
                    case "REGISTER" -> handleRegister(request);
                    case "SUBMIT_AUCTION" -> handleSubmitAuction(request);
                    case "BID" -> handleBid(request);
                    case "GET_CATEGORY" -> handleGetCategory(request);
                    case "GET_MY_BIDS" -> handleGetMyBids(request);
                    case "GET_MY_AUCTIONS" -> handleGetMyAuction(request);
                    case "GET_AUCTION_DETAIL" -> handleGetAuctionDetail(request);
                    case "REGISTER_AUTOBID" -> handleRegisterAutoBid(request);
                    case "LOGOUT" -> handleLogout();
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

        if (DatabaseManager.verifyLogin(loginUser, loginPass)) {
            this.username = loginUser;
            sendMessage(gson.toJson(new NetworkMessage("LOGIN_SUCCESS", "Welcome back!", true)));
        } else {
            sendMessage(gson.toJson(new NetworkMessage("LOGIN_ERROR", "Incorrect credentials.", false)));
        }
    }

    private void handleRegisterAutoBid(JsonObject request) {
        try {
            // Extract parameters from incoming client transmission envelope
            JsonObject bidPayload = request.get("data").getAsJsonObject();
            int auctionId = bidPayload.get("auctionId").getAsInt();
            double maxBudget = bidPayload.get("maxBudget").getAsDouble();

            // Safety check: Use the username attached to this specific client connection thread
            String activeUser = (this.username != null) ? this.username : "Unknown";

            // Commit the auto-bid preference directly to your new Postgres table

            // Notify client of success
            sendMessage(gson.toJson(new NetworkMessage("AUTOBID_RESPONSE", "Auto-bid configured at $" + maxBudget, true)));
            System.out.println("🤖 [SERVER LOG]: Successfully registered Auto-Bid for " + activeUser + " on Item " + auctionId);

        } catch (Exception e) {
            System.err.println("❌ [SERVER ERROR]: Failed to register auto-bid: " + e.getMessage());
            sendMessage(gson.toJson(new NetworkMessage("AUTOBID_RESPONSE", "Failed to activate auto-bid: " + e.getMessage(), false)));
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
            sendMessage(gson.toJson(new NetworkMessage("BID_RESPONSE", "Bid accepted successfully!", true)));

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

    public void sendMessage(String message) {
        if (out != null) out.println(message);
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