package com.example.auctionapp.network; // Adjust to match your package structure

import com.example.auctionapp.controller.*;
import com.example.auctionapp.model.AdminAuctionRow;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// Import your controllers explicitly here
import com.example.auctionapp.model.UserSession;

public class NetworkRouter {

    private static Thread globalListenerThread;
    private static boolean isListening = false;
    private static final Map<String, Consumer<JsonObject>> routes = new HashMap<>();
    private static boolean isHandlersInitialized = false;

    private static void initializeRoutes() {
        if (isHandlersInitialized) return;

        // Route 1: Live Bid Updates (Updates Browse Screen cards AND Detailed Screen values)
        routes.put("LIVE_BID_UPDATE", packet -> {
            int incomingAuctionId = packet.get("auctionId").getAsInt();
            double newPrice = packet.get("newPrice").getAsDouble();
            String highestBidder = packet.has("highestBidder") ? packet.get("highestBidder").getAsString() : "No bids yet";

            Platform.runLater(() -> {
                // 1. Update browse screen live card cache if it's open
                if (BrowseController.activeBrowseScreen != null &&
                        BrowseController.activeBrowseScreen.liveCards.containsKey(incomingAuctionId)) {
                    BrowseController.activeBrowseScreen.liveCards.get(incomingAuctionId).updateLivePrice(newPrice);
                }

                // 2. Synchronize active detailed view values if looking at this specific item
                if (DetailedBidController.activeDetailBidsScreen != null &&
                        DetailedBidController.activeDetailBidsScreen.getCurrentAuctionId() == incomingAuctionId) {
                    DetailedBidController.activeDetailBidsScreen.handleRealtimePriceBroadcast(newPrice, highestBidder);
                    System.out.println("📢 [ROUTER]: Live UI refresh applied.");
                }
            });
        });

        // Route 2: Category Data
        routes.put("CATEGORY_RESPONSE", packet -> {
            String payloadData = packet.get("data").getAsString();
            Platform.runLater(() -> {
                if (BrowseController.activeBrowseScreen != null) {
                    BrowseController.activeBrowseScreen.displayAuctionsOnScreen(payloadData);
                }
            });
        });

        // Route 3: My Bids Screen Data
        routes.put("MY_BIDS_RESPONSE", packet -> {
            String payloadData = packet.has("data") ?
                    (packet.get("data").isJsonPrimitive() ? packet.get("data").getAsString() : packet.get("data").toString()) : "";
            Platform.runLater(() -> {
                if (myBidController.activeMyBidsScreen != null) {
                    myBidController.activeMyBidsScreen.displayAuctionsOnScreen(payloadData);
                    System.out.println("✅ [ROUTER]: Delivered data to My Bids screen.");
                }
            });
        });

        // Route 4: My Auctions Screen Data
        routes.put("MY_AUCTIONS_RESPONSE", packet -> {
            String payloadData = packet.has("data") ?
                    (packet.get("data").isJsonPrimitive() ? packet.get("data").getAsString() : packet.get("data").toString()) : "";
            Platform.runLater(() -> {
                if (myAuctionController.activeMyAuctionsScreen != null) {
                    myAuctionController.activeMyAuctionsScreen.displayAuctionsOnScreen(payloadData);
                    System.out.println("✅ [ROUTER]: Delivered data to My Auctions screen.");
                }
            });
        });

        // Route 5: Auction Details
        // Route 5: Auction Details inside your Client-Side NetworkRouter.java
        routes.put("AUCTION_DETAIL_RESPONSE", packet -> {
            // Extract the inner string payload containing our combined info & history
            String payloadData = packet.get("data").getAsString();

            Platform.runLater(() -> {
                if (DetailedBidController.activeDetailBidsScreen != null) {
                    // Pass it directly to the UI detail controller method we updated earlier
                    DetailedBidController.activeDetailBidsScreen.handleAuctionDetailResponse(payloadData);
                    System.out.println("✅ [ROUTER]: Initial database stats & history loaded on Detailed Screen.");
                }
            });
        });

        // Route 6: Submit Auction
        routes.put("SUBMIT_AUCTION_RESPONSE", packet -> {
            boolean success = packet.get("success").getAsBoolean();
            String message = packet.has("data") ? packet.get("data").getAsString() : "";
            Platform.runLater(() -> {
                if (SellItemController.activeSellScreen != null) {
                    SellItemController.activeSellScreen.handleSubmitResponse(success, message);
                }
            });
        });

        // Route 7: Manual Bid Response
        routes.put("BID_RESPONSE", packet -> {
            boolean success = packet.get("success").getAsBoolean();
            String message = packet.has("message") ? packet.get("message").getAsString() : (packet.has("data") ? packet.get("data").getAsString() : "");
            double bidAmount = packet.has("bidAmount") ? packet.get("bidAmount").getAsDouble() : 0.0;

            Platform.runLater(() -> {
                if (DetailedBidController.activeDetailBidsScreen != null) {
                    DetailedBidController.activeDetailBidsScreen.handleBidResponse(success, message, bidAmount);
                    System.out.println("✅ [ROUTER]: Bid confirmation delivered.");
                }
            });
        });

        // Route 9: Historical Notification Data Response
        routes.put("NOTIF_HISTORY_RESPONSE", packet -> {
            // Extract the data array containing historical notifications
            com.google.gson.JsonArray historyArray = packet.has("data") && packet.get("data").isJsonArray() ?
                    packet.getAsJsonArray("data") : new com.google.gson.JsonArray();

            Platform.runLater(() -> {
                // Point to your notification screen controller singleton instance
                com.example.auctionapp.controller.NotifController screen =
                        com.example.auctionapp.controller.NotifController.getInstance();

                if (screen != null) {
                    screen.populateHistoryFromNetworkArray(historyArray);
                    System.out.println("✅ [ROUTER]: Historical notification cards rendered on screen.");
                } else {
                    System.out.println("⚠️ [ROUTER]: Notification history arrived, but Notif view is not open.");
                }
            });
        });

        // Route 10: Real-time Live Notification Pushes
        routes.put("GLOBAL_ANNOUNCEMENT_CLOSED", packet -> {
            String type = packet.has("type") ? packet.get("type").getAsString() : "DEFAULT";
            String title = packet.has("title") ? packet.get("title").getAsString() : "Auction Update";
            String message = packet.has("message") ? packet.get("message").getAsString() : "";
            String timeString = packet.has("timeString") ? packet.get("timeString").getAsString() : "Just Now";

            Platform.runLater(() -> {
                com.example.auctionapp.controller.NotifController screen =
                        com.example.auctionapp.controller.NotifController.getInstance();

                if (screen != null) {
                    // Inject directly at index 0 (the absolute top) in real-time
                    screen.receiveLiveServerNotification(type, title, message, timeString);
                    System.out.println("🔔 [ROUTER]: Real-time closing notification injected live!");
                }
            });
        });

        // Route 8: Auto Bid Response
        routes.put("AUTOBID_RESPONSE", packet -> {
            boolean success = packet.get("success").getAsBoolean();
            String message = packet.has("data") ? packet.get("data").getAsString() : "";

            Platform.runLater(() -> {
                if (DetailedBidController.activeDetailBidsScreen != null) {
                    DetailedBidController.activeDetailBidsScreen.handleAutoBidResponse(success, message);
                    System.out.println("📢 [ROUTER]: Auto-bid feedback passed to Detailed UI.");
                }
            });
        });

        // Route 11: Handle Server-Side Error Packets Gracefully
        routes.put("ERROR", packet -> {
            String errorMsg = packet.has("data") ? packet.get("data").getAsString() : "Unknown server-side error.";

            Platform.runLater(() -> {
                System.err.println("🚨 [SERVER ERROR RECEIVED]: " + errorMsg);

                // Optional: If you want to show a popup to the user when things break:
                // javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                // alert.setTitle("Server Error");
                // alert.setHeaderText(null);
                // alert.setContentText(errorMsg);
                // alert.showAndWait();
            });
        });

        routes.put("ADMIN_DELETE_AUCTION_RESPONSE", packet -> {
            boolean success = packet.get("success").getAsBoolean();
            int targetId = packet.get("auctionId").getAsInt();

            if (success) {
                System.out.println("✅ [ROUTER]: Database cascade completed for Auction #" + targetId);
                AdminAuctionManagementController screen = AdminAuctionManagementController.getInstance();
                if (screen != null) {
                    screen.removeAuctionFromUI(targetId);
                }
            } else {
                System.err.println("[ROUTER]: Server reported a cascading delete failure.");
            }
        });

        // Inside your client-side NetworkRouter routes map
        routes.put("ADMIN_FETCH_ALL_AUCTIONS_RESPONSE", packet -> {
            JsonArray array = packet.getAsJsonArray("data");

            AdminAuctionManagementController controller = AdminAuctionManagementController.getInstance();
            if (controller != null) {
                javafx.application.Platform.runLater(() -> {
                    // Access the active data array and repopulate rows
                    for (JsonElement elem : array) {
                        JsonObject obj = elem.getAsJsonObject();
                        int id = obj.get("id").getAsInt();
                        String name = obj.get("itemName").getAsString();
                        double price = obj.get("currentPrice").getAsDouble();
                        String seller = obj.get("seller").getAsString();


                        controller.getAuctionList().add(new AdminAuctionRow(id, name, price, seller));
                    }
                });
            }
        });

        routes.put("ADMIN_DELETE_USER_RESPONSE", packet -> {
            boolean success = packet.get("success").getAsBoolean();
            String message = packet.get("message").getAsString();

            if (success) {
                System.out.println("✅ [SERVER]: Deletion successful.");
                // Re-trigger the fetch command to cleanly update the table layout
                AdminUserManagementController activeScreen = AdminUserManagementController.getInstance();
                if (activeScreen != null) {
                    // Trigger the refresh logic we hooked up earlier!
                    javafx.application.Platform.runLater(() -> activeScreen.initialize());
                }
            } else {
                System.err.println("❌ [SERVER ERROR]: " + message);
            }
        });

        routes.put("SELLER_UPDATE_ITEM_RESPONSE", packet -> {
            boolean success = packet.get("success").getAsBoolean();

            Platform.runLater(() -> {
                if (myAuctionController.activeMyAuctionsScreen != null) {
                    myAuctionController.activeMyAuctionsScreen.handleUpdateResponse(success);
                    System.out.println("✅ [ROUTER]: Item edit confirmation dispatched to My Auctions Screen.");
                }
            });
        });

        routes.put("ADMIN_FETCH_USERS_RESPONSE", packet -> {
            // 1. Extract the JSON array from the response packet
            com.google.gson.JsonArray usersArray = packet.getAsJsonArray("users");
            java.util.List<com.example.auctionapp.model.User> structuralList = new java.util.ArrayList<>();

            // 2. Map JSON elements back into raw clean User models
            for (com.google.gson.JsonElement element : usersArray) {
                com.google.gson.JsonObject uJson = element.getAsJsonObject();

                String email = uJson.has("email") ? uJson.get("email").getAsString() : "";
                String username = uJson.has("username") ? uJson.get("username").getAsString() : "";
                String role = uJson.has("role") ? uJson.get("role").getAsString() : "USER";

                // Create a basic polymorphic placeholder model instance
                structuralList.add(new com.example.auctionapp.model.Member("System Record", "N/A", email, username, "", "N/A"));
            }

            // 3. Locate the active User Controller UI frame and push the data inside it
            AdminUserManagementController activeScreen = AdminUserManagementController.getInstance();
            if (activeScreen != null) {
                activeScreen.populateTable(structuralList);
            }
        });

        isHandlersInitialized = true;
    }

    public static void startGlobalListener() {
        if (globalListenerThread != null && globalListenerThread.isAlive()) {
            return;
        }

        initializeRoutes();
        isListening = true;

        globalListenerThread = new Thread(() -> {
            try {
                BufferedReader in = UserSession.getIn();
                String serverMessage;

                while (isListening && (serverMessage = in.readLine()) != null) {
                    System.out.println("📥 [GLOBAL ROUTER]: Caught -> " + serverMessage);

                    try {
                        JsonObject packet = JsonParser.parseString(serverMessage).getAsJsonObject();
                        String action = packet.get("action").getAsString();

                        Consumer<JsonObject> routeAction = routes.get(action);
                        if (routeAction != null) {
                            routeAction.accept(packet);
                        } else {
                            System.out.println("⚠️ [ROUTER WARNING]: Unknown action: " + action);
                        }

                    } catch (Exception parseError) {
                        System.err.println("❌ [ROUTER ERROR]: Bad packet skipped: " + parseError.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("🔌 [Network] Global Router disconnected.");
            }
        });

        globalListenerThread.setDaemon(true);
        globalListenerThread.start();
    }

    public static void stopGlobalListener() {
        isListening = false;
        if (globalListenerThread != null) {
            globalListenerThread.interrupt();
        }
    }
}