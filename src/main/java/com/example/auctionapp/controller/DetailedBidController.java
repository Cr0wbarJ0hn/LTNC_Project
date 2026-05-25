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

    // 🌟 NEW: Added the FXML binding to match your Scene Builder label component!
    @FXML private Label leadingBidder;

    private javafx.scene.Node previousContent;

    public void setPreviousContent(javafx.scene.Node previousContent) {
        this.previousContent = previousContent;
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

        // Clear the label layout state while loading from server
        leadingBidder.setText("Loading...");
        leadingBidder.setStyle("-fx-text-fill: #888888;");

        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                        Image image = new Image(imagePath, true);
                        DetailImage.setImage(image);
                    } else if (imagePath.length() > 200 || imagePath.startsWith("/9j/")) {
                        byte[] decodedBytes = Base64.getDecoder().decode(imagePath.trim());Image image = new Image(new ByteArrayInputStream(decodedBytes));
                        DetailImage.setImage(image);
                    } else {
                        String formattedUrl = new java.io.File(imagePath).toURI().toString();
                        Image image = new Image(formattedUrl);
                        DetailImage.setImage(image);
                    }
                } catch (Exception e) {
                    System.out.println("Card Image Error: Could not load image from target path: " + imagePath);
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("Card Image Error: Could not load image data.");
        }

        setupPlaceholderChart();

        // 🌟 NEW: Pull absolute up-to-the-second truths (like leading bidder) right as screen loads
        fetchLiveAuctionData(auctionId);
    }

    
    private void fetchLiveAuctionData(int auctionId) {
        new Thread(() -> {
            try {
                Gson gson = new Gson();
                NetworkMessage request = new NetworkMessage("GET_AUCTION_DETAIL", String.valueOf(auctionId), true);

                UserSession.getOut().println(gson.toJson(request));
                UserSession.getOut().flush();

                String serverResponse;
                while ((serverResponse = UserSession.getIn().readLine()) != null) {
                    NetworkMessage response = gson.fromJson(serverResponse, NetworkMessage.class);
                    if ("AUCTION_DETAIL_RESPONSE".equals(response.action)) {

                        JsonObject obj = JsonParser.parseString(response.data).getAsJsonObject();
                        String leader = obj.has("leadingBidder") ? obj.get("leadingBidder").getAsString() : "No bids yet";
                        double livePrice = obj.has("currentPrice") ? obj.get("currentPrice").getAsDouble() : 0.0;

                        Platform.runLater(() -> {
                            // Update the Leading Bidder label dynamically
                            if (leader == null || leader.trim().isEmpty() || leader.equals("No bids yet")) {
                                leadingBidder.setText("No bids yet");
                                leadingBidder.setStyle("-fx-text-fill: #888888;");
                            } else {
                                leadingBidder.setText(leader);
                                leadingBidder.setStyle("-fx-text-fill: #34d399;"); // Vibrant green accent color!
                            }

                            // Keep the price accurate in case a background user bid while switching views
                            if (livePrice > 0.0) {
                                DetailPrice.setText("$" + String.format("%.2f", livePrice));
                            }
                        });
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch live leading bidder statistics:");
                e.printStackTrace();
            }
        }).start();
    }

    private void setupPlaceholderChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Start", 500));
        series.getData().add(new XYChart.Data<>("Day 2", 750));
        series.getData().add(new XYChart.Data<>("Today", 1000));
        priceHistoryChart.getData().add(series);
    }

    @FXML private javafx.scene.control.Button backButton;

    @FXML
    public void goBack() {
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
            System.out.println("Could not go back.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleManualBid() {
        String inputString = manualBidInput.getText().trim();
        if (inputString.isEmpty()) return;

        try {
            double amount = Double.parseDouble(inputString);

            JsonObject payload = new JsonObject();
            payload.addProperty("auctionId", this.currentAuctionId);
            payload.addProperty("bidAmount", amount);

            // 🌟 FIXED: Changed hardcoded "ActiveUser" to match your authentic active UserSession profile data!
            payload.addProperty("username", UserSession.getUsername());

            Gson clientGson = new Gson();
            NetworkMessage messageEnvelope = new NetworkMessage("BID", clientGson.toJson(payload), true);

            new Thread(() -> {
                try {
                    UserSession.getOut().println(clientGson.toJson(messageEnvelope));
                    UserSession.getOut().flush();

                    String responseText;
                    while ((responseText = UserSession.getIn().readLine()) != null) {
                        NetworkMessage serverReply = clientGson.fromJson(responseText, NetworkMessage.class);

                        if ("BID_RESPONSE".equals(serverReply.action)) {
                            Platform.runLater(() -> {
                                if (serverReply.success) {
                                    DetailPrice.setText("$" + String.format("%.2f", amount));

                                    // 🌟 NEW: Instantly declare yourself the leading bidder locally upon confirmation
                                    leadingBidder.setText(UserSession.getUsername());
                                    leadingBidder.setStyle("-fx-text-fill: #34d399;");

                                    manualBidInput.clear();
                                    manualBidInput.setPromptText("Bid Successful!");
                                    manualBidInput.setStyle("-fx-prompt-text-fill: #27ae60; -fx-border-color: #27ae60;");
                                } else {
                                    manualBidInput.clear();
                                    manualBidInput.setPromptText(serverReply.data);
                                    manualBidInput.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
                                }
                            });
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

        } catch (NumberFormatException nfe) {
            manualBidInput.setPromptText("Enter valid number!");
            manualBidInput.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
        }
    }

    @FXML
    public void initialize() {
        autoBidPane.visibleProperty().bind(autoBidSwitch.selectedProperty());
        autoBidPane.managedProperty().bind(autoBidPane.visibleProperty());

        manualBidPane.visibleProperty().bind(autoBidSwitch.selectedProperty().not());
        manualBidPane.managedProperty().bind(manualBidPane.visibleProperty());

        autoBidSwitch.selectedProperty().addListener((obs, wasAuto, isAuto) -> {
            System.out.println("Mode changed: " + (isAuto ? "AUTO" : "MANUAL"));
        });

        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        clip.widthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> DetailImage.getLayoutBounds().getWidth(),
                DetailImage.layoutBoundsProperty()
        ));

        clip.heightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> DetailImage.getLayoutBounds().getHeight(),
                DetailImage.layoutBoundsProperty()
        ));

        DetailImage.setClip(clip);
    }
}