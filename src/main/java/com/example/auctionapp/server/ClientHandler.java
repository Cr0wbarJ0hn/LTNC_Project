package com.example.auctionapp.server;

import com.example.auctionapp.model.Items;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.google.gson.Gson;
import com.example.auctionapp.model.NetworkMessage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private Gson gson = new Gson(); // Keep one instance for the whole class

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
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

                    // TODO: Verify your utility class name is 'SupabaseStorageManager'
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

            sendMessage(gson.toJson(new NetworkMessage("SUBMIT_SUCCESS", "Posted!", true)));
        } catch (Exception e) {
            System.err.println("[SERVER ERROR] Error handling SUBMIT_AUCTION request:");
            e.printStackTrace();
            sendMessage(gson.toJson(new NetworkMessage("SUBMIT_ERROR", "Server error.", false)));
        }
    }

    private void handleBid(JsonObject request) {
        synchronized (DatabaseManager.class) {
            // Logic for bidding goes here
            sendMessage(gson.toJson(new NetworkMessage("SYSTEM", "Bid received!", true)));
        }
    }

    private void handleGetCategory(JsonObject request) {
        String cat = request.get("data").getAsString();
        String data = DatabaseManager.fetchAuctionsByCategory(cat);

        String response = (data == null || data.isEmpty()) ? "NO_ITEMS" : data;
        sendMessage(gson.toJson(new NetworkMessage("CATEGORY_RESPONSE", response, true)));
    }

    // --- HELPERS ---

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    private void closeConnection() {
        try {
            AuctionServer.activeClients.remove(this);
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}