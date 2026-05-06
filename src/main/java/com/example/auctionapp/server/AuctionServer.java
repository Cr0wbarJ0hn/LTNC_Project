package com.example.auctionapp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AuctionServer {
    // The "door" number our server listens on
    private static final int PORT = 5000;

    // Observer Pattern: Keep track of everyone who is currently logged in
    public static List<ClientHandler> activeClients = new ArrayList<>();

    public static void main(String[] args) {
        // 1. Set up the database first
        System.out.println("Booting up system...");
        DatabaseManager.initializeTables();

        // 2. Open the Server Socket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Auction Server is LIVE on port " + PORT + "!");
            System.out.println("Waiting for users to connect...");

            // 3. The Infinite Loop (Stays awake 24/7)
            while (true) {
                // The server pauses here until someone opens the app
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getInetAddress());

                // 4. Concurrency: Give the new user their own dedicated Thread!
                ClientHandler newClient = new ClientHandler(clientSocket);
                activeClients.add(newClient);

                new Thread(newClient).start(); // Start the thread in the background
            }

        } catch (IOException e) {
            System.out.println("Server Error: " + e.getMessage());
        }
    }

    // Observer Pattern: Send a message to EVERY connected screen (e.g., "NEW BID!")
    public static void broadcast(String message) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(message);
        }
    }
}
