package com.example.auctionapp.controller;

import com.example.auctionapp.model.UserSession;
import com.google.gson.JsonArray;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.example.auctionapp.model.NetworkMessage;
import javafx.application.Platform;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.util.Base64;

public class DetailedBidController {
    private XYChart.Series<String, Number> priceSeries;

    @FXML
    private Label DetailTimeLeft;

    private static final java.util.Map<Integer, java.util.LinkedHashMap<String, Double>> chartMemory = new java.util.HashMap<>();
    private int currentAuctionId;
    private String currentUsername = UserSession.getUsername();

    @FXML
    private TextField manualBidInput;
    @FXML
    private VBox autoBidSettings;
    @FXML
    private CheckBox autoBidSwitch;
    @FXML
    private TextField autoBidTextField;
    @FXML
    private Pane manualBidPane;
    @FXML
    private Pane autoBidPane;
    @FXML
    private Label DetailIncrement;
    @FXML
    private Hyperlink BackLink;
    @FXML
    private Label ItemName;
    @FXML
    private ImageView DetailImage;
    @FXML
    private Label DetailDescription;
    @FXML
    private Label ConditionLabel;
    @FXML
    private Label Sellerlabel;
    @FXML
    private Label DetailPrice;
    @FXML
    private LineChart<String, Number> priceHistoryChart;
    @FXML
    private Label leadingBidder;

    private javafx.scene.Node previousContent;

    // 🌟 Static tracker so the global mailman can find this screen
    public static DetailedBidController activeDetailBidsScreen = null;

    // 🌟 Getter so the router can check if an incoming live bid belongs to this open item
    public int getCurrentAuctionId() {
        return currentAuctionId;
    }

    public void setPreviousContent(javafx.scene.Node previousContent) {
        this.previousContent = previousContent;
    }

    @FXML
    public void initialize() {

        activeDetailBidsScreen = this; // Register screen

        // 🧼 AUTO-CLEANUP: Clear the active tracker when leaving this screen
        if (BackLink != null) {
            BackLink.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null && activeDetailBidsScreen == this) {
                    activeDetailBidsScreen = null;
                    System.out.println("🧼 Cleaned up Detailed Bid tracker.");
                }
            });
        }
        autoBidPane.visibleProperty().bind(autoBidSwitch.selectedProperty());
        autoBidPane.managedProperty().bind(autoBidPane.visibleProperty());
        manualBidPane.visibleProperty().bind(autoBidSwitch.selectedProperty().not());
        manualBidPane.managedProperty().bind(manualBidPane.visibleProperty());

        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        clip.widthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> DetailImage.getLayoutBounds().getWidth(), DetailImage.layoutBoundsProperty()));
        clip.heightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> DetailImage.getLayoutBounds().getHeight(), DetailImage.layoutBoundsProperty()));
        DetailImage.setClip(clip);
    }

    public void setItemData(int auctionId, String name, String description, String price, String condition, String seller, String imagePath, String increment, String timeLeft) {
        this.currentAuctionId = auctionId;
        ItemName.setText(name);
        DetailDescription.setText(description);
        DetailPrice.setText("$" + price);
        ConditionLabel.setText("Condition: " + condition);
        Sellerlabel.setText("Seller: " + seller);
        DetailIncrement.setText("$" + increment);
        DetailTimeLeft.setText(timeLeft);

        leadingBidder.setText("Loading...");
        leadingBidder.setStyle("-fx-text-fill: #888888;");

        // Load images safely
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    DetailImage.setImage(new Image(imagePath, true));
                } else if (imagePath.length() > 200 || imagePath.startsWith("/9j/")) {
                    byte[] decodedBytes = Base64.getDecoder().decode(imagePath.trim());
                    DetailImage.setImage(new Image(new ByteArrayInputStream(decodedBytes)));
                }
            }
        } catch (Exception e) {
            System.out.println("Image error.");
        }


        fetchLiveAuctionData(auctionId);
    }

    @FXML
    public void handleSetAutoBidButtonClick() {
        String budgetInput = autoBidTextField.getText().trim();

        if (budgetInput.isEmpty()) {
            autoBidTextField.setPromptText("Please enter a maximum budget amount.");
            autoBidTextField.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            double maxBudget = Double.parseDouble(budgetInput);
            int auctionId = this.currentAuctionId; // Uses your existing tracked ID

            // Construct the request packet using Gson
            JsonObject request = new JsonObject();
            request.addProperty("action", "REGISTER_AUTOBID");

            JsonObject dataEnvelope = new JsonObject();
            dataEnvelope.addProperty("auctionId", auctionId);
            dataEnvelope.addProperty("maxBudget", maxBudget);
            request.add("data", dataEnvelope);

            // Drop the packet directly down the socket output stream pipe
            PrintWriter out = UserSession.getOut();
            out.println(new Gson().toJson(request));
            out.flush();


            autoBidTextField.clear();
            autoBidTextField.setPromptText("Sending request to server...");
            autoBidTextField.setStyle("-fx-text-fill: #170C79; -fx-prompt-text-fill: #170C79;");

        } catch (NumberFormatException e) {
            autoBidTextField.clear();
            autoBidTextField.setPromptText("Invalid number format! Use digits only.");
            autoBidTextField.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
        }
    }

    // 🌟 FIX: Only SENDS the data request. Zero line-reading here!
    private void fetchLiveAuctionData(int auctionId) {
        try {
            Gson gson = new Gson();
            NetworkMessage request = new NetworkMessage("GET_AUCTION_DETAIL", String.valueOf(auctionId), true);
            UserSession.getOut().println(gson.toJson(request));
            UserSession.getOut().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🌟 FIX: Only SENDS the bid payload out. Zero line-reading here!
    @FXML
    public void handleManualBid() {
        String inputString = manualBidInput.getText().trim();
        if (inputString.isEmpty()) return;

        try {
            double amount = Double.parseDouble(inputString);
            JsonObject payload = new JsonObject();
            payload.addProperty("auctionId", this.currentAuctionId);
            payload.addProperty("bidAmount", amount);
            payload.addProperty("username", UserSession.getUsername());

            Gson clientGson = new Gson();
            NetworkMessage messageEnvelope = new NetworkMessage("BID", clientGson.toJson(payload), true);

            UserSession.getOut().println(clientGson.toJson(messageEnvelope));
            UserSession.getOut().flush();
            manualBidInput.clear();
            manualBidInput.setPromptText("Sending request to server...");
            manualBidInput.setStyle("-fx-text-fill: #170C79; -fx-prompt-text-fill: #170C79;");
        } catch (NumberFormatException nfe) {
            manualBidInput.clear();
            manualBidInput.setPromptText("Enter valid number!");
            manualBidInput.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
        }
    }

    public void handleAuctionDetailResponse(String jsonPayloadData) {
        try {
            // 1. Parse the incoming JSON data
            JsonObject auctionData = JsonParser.parseString(jsonPayloadData).getAsJsonObject();

            double currentPrice = auctionData.get("currentPrice").getAsDouble();
            int currentId = auctionData.get("id").getAsInt();

            // Update standard labels
            DetailPrice.setText("$" + String.format("%.2f", currentPrice));

            if (auctionData.has("leadingBidder")) {
                String leader = auctionData.get("leadingBidder").getAsString();
                leadingBidder.setText(leader);
                if (leader != null && !leader.equals("No bids yet") && !leader.isEmpty()) {
                    leadingBidder.setStyle("-fx-text-fill: #34d399;");
                } else {
                    leadingBidder.setStyle("-fx-text-fill: #ffffff;");
                }
            }

            // 2. 🌟 THE ANIMATION FIX: Turn off animations completely!
            // Leaving animations ON is the #1 reason JavaFX stamps cluster in the corner.
            priceHistoryChart.setAnimated(false);
            priceHistoryChart.getData().clear();

            // 3. Create a fresh, clean series container
            priceSeries = new XYChart.Series<>();
            priceSeries.setName("Price Timeline ($)");

            // 4. Extract points from the database history payload
            java.util.LinkedHashMap<String, Double> databaseHistoryPoints = new java.util.LinkedHashMap<>();
            if (auctionData.has("priceHistory") && !auctionData.get("priceHistory").isJsonNull()) {
                JsonArray historyArray = auctionData.getAsJsonArray("priceHistory");

                for (com.google.gson.JsonElement element : historyArray) {
                    JsonObject bidRecord = element.getAsJsonObject();
                    String realDatabaseTime = bidRecord.get("time").getAsString();
                    double realDatabaseAmount = bidRecord.get("price").getAsDouble();

                    databaseHistoryPoints.put(realDatabaseTime, realDatabaseAmount);
                }
            }

            // 5. 🌟 THE LAYOUT FIX: Populate the series data points FIRST.
            // Do NOT manually add or clear things from xAxis.getCategories().
            // Manually manipulating categories breaks the automatic spacing layout engine.
            for (java.util.Map.Entry<String, Double> entry : databaseHistoryPoints.entrySet()) {
                priceSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            // 6. 🌟 THE PUNCHLINE: Pin the fully populated series to the chart container last!
            // This forces JavaFX to look at the total number of points and spread them out
            // evenly across the actual width of your UI container.
            priceHistoryChart.getData().add(priceSeries);

            // Save to cache memory bank
            chartMemory.put(currentId, databaseHistoryPoints);
            System.out.println("✅ [CHART]: Rendered " + databaseHistoryPoints.size() + " beautifully spaced points.");

        } catch (Exception e) {
            System.err.println("❌ [CHART ERROR]: Layout positioning exception caught.");
            e.printStackTrace();
        }
    }


    public void handleBidResponse(boolean success, String serverMessage, double attemptedAmount) {
        if (success) {

            // Only handle the input box and success messages here!
            manualBidInput.clear();
            manualBidInput.setPromptText("Bid Successful!");
            manualBidInput.setStyle("-fx-prompt-text-fill: #27ae60; -fx-border-color: #27ae60;");

            // (Note: The LIVE_BID_UPDATE broadcast is already handling the price and leader text!)

        } else {
            manualBidInput.clear();
            manualBidInput.setPromptText(serverMessage);
            manualBidInput.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
        }
    }

    public void handleAutoBidResponse(boolean success, String message) {
        if (success) {
            // Clear input and show success styling in Emerald Green 🟢
            autoBidTextField.clear();
            autoBidTextField.setPromptText("Auto-Bid Configured!");
            autoBidTextField.setStyle("-fx-prompt-text-fill: #27ae60; -fx-border-color: #27ae60;");


        } else {

            autoBidTextField.clear();
            autoBidTextField.setPromptText(message);
            autoBidTextField.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
        }
    }


    // 🌟 NEW METHOD: Called by Global Router when *any* client changes the price in real-time
    public void handleRealtimePriceBroadcast(double price, String highestBidderName) {
        Platform.runLater(() -> {
        DetailPrice.setText("$" + String.format("%.2f", price));
        if (highestBidderName != null && !highestBidderName.isEmpty()) {
            leadingBidder.setText(highestBidderName);
            leadingBidder.setStyle("-fx-text-fill: #34d399;");
            // 3. 🌟 PLOT TO CHART
            if (priceSeries != null) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
                String currentBidTime = java.time.LocalTime.now().format(formatter);

                // Add to the visual screen
                priceSeries.getData().add(new XYChart.Data<>(currentBidTime, price));

                // 🌟 NEW: Save it to the memory bank so it survives screen changes!
                int currentId = getCurrentAuctionId();
                if (chartMemory.containsKey(currentId)) {
                    chartMemory.get(currentId).put(currentBidTime, price);
                }

                System.out.println("📈 [CHART]: Appended new point -> " + currentBidTime + " at $" + price);
            }
        }
        });
    }

    @FXML
    public void goBack() {
        if (activeDetailBidsScreen == this) activeDetailBidsScreen = null;
        try {
            javafx.scene.layout.Pane rightPane = (javafx.scene.layout.Pane) BackLink.getScene().lookup("#rightPane");
            if (rightPane != null) {
                rightPane.setVisible(true);
                rightPane.setManaged(true);
            }
            javafx.scene.layout.Pane dashboardCenter = (javafx.scene.layout.Pane) BackLink.getScene().lookup("#centerContentArea");
            if (dashboardCenter != null && previousContent != null) {
                dashboardCenter.getChildren().clear();
                dashboardCenter.getChildren().add(previousContent);
            } else {
                javafx.scene.control.ToggleButton homeBtn = (javafx.scene.control.ToggleButton) BackLink.getScene().lookup("#HomeButton");
                if (homeBtn != null) homeBtn.fire();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}