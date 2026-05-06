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
    private BufferedReader in;  // The "Ears"
    private PrintWriter out;    // The "Mouth"
    private String username;    // To remember who this specific user is

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            Gson gson = new Gson(); // 🛠️ Create our JSON tool

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("SERVER LOG: Received from client: " + clientMessage);

                // 1. Read the JSON Message instead of splitting by ":"
                JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                String command = request.get("action").getAsString();

                switch (command) {
                    case "LOGIN":
                        String loginUser = request.get("username").getAsString();
                        String loginPass = request.get("password").getAsString();

                        if (DatabaseManager.verifyLogin(loginUser, loginPass)) {
                            this.username = loginUser;
                            // Send JSON Response
                            sendMessage(gson.toJson(new NetworkMessage("LOGIN_SUCCESS", "Welcome back!", true)));
                        } else {
                            sendMessage(gson.toJson(new NetworkMessage("LOGIN_ERROR", "Incorrect username or password.", false)));
                        }
                        break;

                    case "REGISTER":
                        String newUsername = request.get("username").getAsString();
                        String newPassword = request.get("password").getAsString();
                        String newEmail = request.get("email").getAsString();

                        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
                        if (!newEmail.matches(emailRegex)) {
                            sendMessage(gson.toJson(new NetworkMessage("REGISTER_ERROR", "Invalid email format.", false)));
                            break;
                        }

                        try {
                            DatabaseManager.registerUser(newUsername, newPassword, newEmail);
                            sendMessage(gson.toJson(new NetworkMessage("REGISTER_SUCCESS", "Welcome!", true)));
                        } catch (Exception e) {
                            String realError = e.getMessage().toLowerCase();
                            if (realError.contains("email")) {
                                sendMessage(gson.toJson(new NetworkMessage("REGISTER_ERROR", "Email already taken.", false)));
                            } else if (realError.contains("username") || realError.contains("primary")) {
                                sendMessage(gson.toJson(new NetworkMessage("REGISTER_ERROR", "Username already taken.", false)));
                            } else {
                                sendMessage(gson.toJson(new NetworkMessage("REGISTER_ERROR", "Registration failed.", false)));
                            }
                        }
                        break;

                    case "SUBMIT_AUCTION":
                        try {
                            // Look how clean this is now! No more parts[0], parts[1] guess-work.
                            String itemName = request.get("itemName").getAsString();
                            String itemType = request.get("itemType").getAsString();
                            String itemCondition = request.get("itemCondition").getAsString();
                            String description = request.get("description").getAsString();
                            String imagePath = request.get("imagePath").getAsString(); // No more [DRIVE] replace hack!
                            String seller = request.get("seller").getAsString();

                            double price = request.get("price").getAsDouble();
                            double increment = request.get("increment").getAsDouble();
                            long receivedTime = request.get("endTime").getAsLong();
                            java.sql.Timestamp endTime = new java.sql.Timestamp(receivedTime);

                            Items newItem = new Items(0, itemType, itemName, itemCondition, description, imagePath);
                            DatabaseManager.insertItemAndAuction(newItem, seller, price, increment, endTime);

                            sendMessage(gson.toJson(new NetworkMessage("SUBMIT_SUCCESS", "Auction posted successfully!", true)));

                        } catch (Exception e) {
                            System.out.println("DEBUG: Server Error -> " + e.getMessage());
                            e.printStackTrace();
                            sendMessage(gson.toJson(new NetworkMessage("SUBMIT_ERROR", "Internal server error.", false)));
                        }
                        break;

                    case "BID":
                        // 🛡️ PROJECT REQUIREMENT: CONCURRENCY / SYNCHRONIZATION 🛡️
                        // We lock the DatabaseManager so only ONE thread can place a bid at a single time!
                        synchronized (DatabaseManager.class) {
                            // TODO: Handle actual database bidding logic here
                            sendMessage(gson.toJson(new NetworkMessage("SYSTEM", "We received your bid safely!", true)));
                        }
                        break ;

                    case "GET_CATEGORY":
                        String requestedCategory = request.get("data").getAsString();
                        String auctionData = DatabaseManager.fetchAuctionsByCategory(requestedCategory);

                        if (auctionData == null || auctionData.isEmpty()) {
                            sendMessage(gson.toJson(new NetworkMessage("CATEGORY_RESPONSE", "NO_ITEMS", true)));
                        } else {
                            sendMessage(gson.toJson(new NetworkMessage("CATEGORY_RESPONSE", auctionData, true)));
                        }
                        break;

                    default:
                        sendMessage(gson.toJson(new NetworkMessage("ERROR", "Command not recognized.", false)));
                }
            }
        } catch (IOException e) {
            System.out.println("A user disconnected abruptly.");
        } catch (Exception e) {
            System.out.println("CRITICAL SERVER CRASH: An unexpected error occurred!");
            e.printStackTrace();
        } finally {
            try {
                AuctionServer.activeClients.remove(this);
                if (clientSocket != null) clientSocket.close();
                System.out.println("Connection closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // This is the method the Server calls to talk BACK to the user
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }

    }
}
