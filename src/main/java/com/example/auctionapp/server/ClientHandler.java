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
            Items newItem = new Items(
                    0,
                    request.get("itemType").getAsString(),
                    request.get("itemName").getAsString(),
                    request.get("itemCondition").getAsString(),
                    request.get("description").getAsString(),
                    request.get("imagePath").getAsString()
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