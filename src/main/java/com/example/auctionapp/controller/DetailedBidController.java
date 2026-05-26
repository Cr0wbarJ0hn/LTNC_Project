package com.example.auctionapp.controller;

import com.example.auctionapp.model.UserSession;
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
import java.util.Base64;

public class DetailedBidController {

    @FXML private Label DetailTimeLeft;
    private int currentAuctionId;
    private String currentUsername = UserSession.getUsername();

    @FXML private TextField manualBidInput;
    @FXML private VBox autoBidSettings;
    @FXML private CheckBox autoBidSwitch;
    @FXML private Pane manualBidPane;
    @FXML private Pane autoBidPane;
    @FXML private Label DetailIncrement;
    @FXML private Hyperlink BackLink;
    @FXML private Label ItemName;
    @FXML private ImageView DetailImage;
    @FXML private Label DetailDescription;
    @FXML private Label ConditionLabel;
    @FXML private Label Sellerlabel;
    @FXML private Label DetailPrice;
    @FXML private LineChart<String, Number> priceHistoryChart;
    @FXML private Label leadingBidder;

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

        setupPlaceholderChart();
        fetchLiveAuctionData(auctionId);
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
        } catch (NumberFormatException nfe) {
            manualBidInput.setPromptText("Enter valid number!");
            manualBidInput.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
        }
    }

    // 🌟 NEW METHOD: Called by Global Router to display the initial live stats data
    public void handleAuctionDetailResponse(String jsonData) {
        JsonObject obj = JsonParser.parseString(jsonData).getAsJsonObject();
        String leader = obj.has("leadingBidder") ? obj.get("leadingBidder").getAsString() : "No bids yet";
        double livePrice = obj.has("currentPrice") ? obj.get("currentPrice").getAsDouble() : 0.0;

        if (leader == null || leader.trim().isEmpty() || leader.equals("No bids yet")) {
            leadingBidder.setText("No bids yet");
            leadingBidder.setStyle("-fx-text-fill: #888888;");
        } else {
            leadingBidder.setText(leader);
            leadingBidder.setStyle("-fx-text-fill: #34d399;");
        }

        if (livePrice > 0.0) {
            DetailPrice.setText("$" + String.format("%.2f", livePrice));
        }
    }

    // 🌟 NEW METHOD: Called by Global Router when this specific user bids manually
    // Inside DetailedBidController.java

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

    // 🌟 NEW METHOD: Called by Global Router when *any* client changes the price in real-time
    public void handleRealtimePriceBroadcast(double price, String highestBidderName) {
        DetailPrice.setText("$" + String.format("%.2f", price));
        if (highestBidderName != null && !highestBidderName.isEmpty()) {
            leadingBidder.setText(highestBidderName);
            leadingBidder.setStyle("-fx-text-fill: #34d399;");
        }
    }

    private void setupPlaceholderChart() {
        priceHistoryChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Start", 500));
        series.getData().add(new XYChart.Data<>("Day 2", 750));
        series.getData().add(new XYChart.Data<>("Today", 1000));
        priceHistoryChart.getData().add(series);
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