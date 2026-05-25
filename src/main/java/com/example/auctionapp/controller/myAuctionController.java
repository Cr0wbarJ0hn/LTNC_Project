package com.example.auctionapp.controller;

import com.example.auctionapp.model.UserSession;
import com.example.auctionapp.model.NetworkMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class myAuctionController {

    private DashboardController mainDashboard;

    @FXML private Label greetingLabel;
    public static myAuctionController activeMyAuctionsScreen = null;

    @FXML private ScrollPane myAuctionScrollPane;
    @FXML private FlowPane myAuctionFlowPane;
    @FXML private VBox emptyRecentPane;

    public void setMainDashboard(DashboardController mainDashboard) {
        this.mainDashboard = mainDashboard;
    }

    @FXML
    public void initialize() {
        activeMyAuctionsScreen = this;
        // Automatically start fetching the logged-in user's auctions on load
        fetchHostedAuctions();
    }

    public void fetchHostedAuctions() {
        // 1. Resolve the logged-in user's username context
        String currentUsername = UserSession.getUsername();

        if (greetingLabel != null && currentUsername != null) {
            greetingLabel.setText("See all the auctions you held here, " + currentUsername);
        }

        // 2. Set up the UI for loading (Hide empty state, show skeletons)
        if (emptyRecentPane != null) emptyRecentPane.setVisible(false);
        if (myAuctionScrollPane != null) myAuctionScrollPane.setVisible(true);

        myAuctionFlowPane.getChildren().clear();
        for (int i = 0; i < 4; i++) {
            myAuctionFlowPane.getChildren().add(createSkeletonCard());
        }

        // 3. ONLY SEND THE REQUEST. No background threads, no readLine()!
        try {
            Gson gson = new Gson();
            NetworkMessage request = new NetworkMessage("GET_MY_AUCTIONS", currentUsername, true);

            // Send request over the socket stream
            UserSession.getOut().println(gson.toJson(request));
            UserSession.getOut().flush();

            System.out.println("📤 [My Auctions]: Request sent to server. Waiting for Global Router to deliver response...");

        } catch (Exception e) {
            System.err.println("Error sending request:");
            e.printStackTrace();
        }
    }

    // 🌟 THE ROUTER HAND-OFF TARGET




    public void displayAuctionsOnScreen(String serverResponse) {
        myAuctionFlowPane.getChildren().clear();

        System.out.println("DEBUG - My Auctions Raw Server JSON: " + serverResponse);

        if (serverResponse == null || serverResponse.equals("NO_ITEMS") || serverResponse.equals("[]") || serverResponse.isEmpty()) {
            emptyRecentPane.setVisible(true);
            myAuctionScrollPane.setVisible(false);
            return;
        }

        emptyRecentPane.setVisible(false);
        myAuctionScrollPane.setVisible(true);

        try {
            JsonArray auctionItems = JsonParser.parseString(serverResponse).getAsJsonArray();

            for (JsonElement element : auctionItems) {
                JsonObject itemObj = element.getAsJsonObject();

                try {
                    int id = itemObj.get("id").getAsInt();
                    String name = itemObj.get("itemName").getAsString();
                    double startPrice = itemObj.get("startingPrice").getAsDouble();
                    double currentPrice = itemObj.get("currentPrice").getAsDouble();
                    String condition = itemObj.get("itemCondition").getAsString();
                    String base64Image = itemObj.get("imagePath").getAsString();
                    String description = itemObj.get("description").getAsString();
                    String seller = itemObj.get("seller").getAsString();
                    long endTimeMillis = itemObj.get("endTime").getAsLong();
                    double increment = itemObj.get("priceIncrement").getAsDouble();

                    // Load the standard card FXML template instance
                    FXMLLoader cardLoader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/AuctionCard.fxml"));
                    Node cardNode = cardLoader.load();
                    AuctionCardController cardController = cardLoader.getController();

                    // Push attributes directly into the layout controller parameters
                    cardController.setCardData(
                            id,
                            name,
                            startPrice,
                            currentPrice,
                            condition,
                            description,
                            base64Image,
                            seller,
                            endTimeMillis,
                            increment
                    );

                    myAuctionFlowPane.getChildren().add(cardNode);

                } catch (Exception e) {
                    System.out.println("ERROR: Could not process single hosted item block entry!");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: Invalid JSON structure payload handled from server stream!");
            e.printStackTrace();
        }
    }

    private VBox createSkeletonCard() {
        // Base skeleton node sizing matched directly to card template profiles
        VBox skeletonCard = new VBox();
        skeletonCard.setPrefSize(402.0, 551.0);

        Pane container = new Pane();
        container.setPrefSize(402.0, 551.0);
        container.setStyle("-fx-background-color: #20202E; -fx-background-radius: 10px;");

        // Media illustration block mask
        Rectangle ghostImage = new Rectangle(402.0, 357.0);
        ghostImage.setStyle("-fx-fill: #2B2B3C;");
        Rectangle imageClip = new Rectangle(402.0, 357.0);
        imageClip.setArcWidth(40.0);
        imageClip.setArcHeight(40.0);
        ghostImage.setClip(imageClip);

        // Headline Text Fields
        Rectangle ghostTitleLine1 = new Rectangle(180.0, 16.0);
        ghostTitleLine1.setLayoutX(29.0);
        ghostTitleLine1.setLayoutY(376.0);
        ghostTitleLine1.setArcWidth(8.0);
        ghostTitleLine1.setArcHeight(8.0);
        ghostTitleLine1.setStyle("-fx-fill: #2B2B3C;");

        Rectangle ghostTitleLine2 = new Rectangle(120.0, 16.0);
        ghostTitleLine2.setLayoutX(29.0);
        ghostTitleLine2.setLayoutY(402.0);
        ghostTitleLine2.setArcWidth(8.0);
        ghostTitleLine2.setArcHeight(8.0);
        ghostTitleLine2.setStyle("-fx-fill: #2B2B3C;");

        // Quality rating badge shape
        Rectangle ghostCondition = new Rectangle(90.0, 25.0);
        ghostCondition.setLayoutX(289.0);
        ghostCondition.setLayoutY(373.0);
        ghostCondition.setArcWidth(20.0);
        ghostCondition.setArcHeight(20.0);
        ghostCondition.setStyle("-fx-fill: #2B2B3C;");

        // Identity line marker
        Rectangle ghostSeller = new Rectangle(100.0, 14.0);
        ghostSeller.setLayoutX(29.0);
        ghostSeller.setLayoutY(426.0);
        ghostSeller.setArcWidth(8.0);
        ghostSeller.setArcHeight(8.0);
        ghostSeller.setStyle("-fx-fill: #2B2B3C;");

        // Structural grid break rules
        Rectangle ghostSeparator = new Rectangle(349.0, 1.0);
        ghostSeparator.setLayoutX(21.0);
        ghostSeparator.setLayoutY(453.0);
        ghostSeparator.setStyle("-fx-fill: #3E3E53;");

        // Cost detail tags
        Rectangle ghostPriceLabel = new Rectangle(70.0, 12.0);
        ghostPriceLabel.setLayoutX(29.0);
        ghostPriceLabel.setLayoutY(476.0);
        ghostPriceLabel.setStyle("-fx-fill: #2B2B3C;");

        // Main ledger tracking numbers
        Rectangle ghostPrice = new Rectangle(85.0, 24.0);
        ghostPrice.setLayoutX(28.0);
        ghostPrice.setLayoutY(492.0);
        ghostPrice.setArcWidth(8.0);
        ghostPrice.setArcHeight(8.0);
        ghostPrice.setStyle("-fx-fill: #3A3A50;");

        // Interaction button bounding masks
        Rectangle ghostButton = new Rectangle(140.0, 40.0);
        ghostButton.setLayoutX(239.0);
        ghostButton.setLayoutY(484.0);
        ghostButton.setArcWidth(20.0);
        ghostButton.setArcHeight(20.0);
        ghostButton.setStyle("-fx-fill: #2B2B3C;");

        container.getChildren().addAll(
                ghostImage, ghostTitleLine1, ghostTitleLine2, ghostCondition,
                ghostSeller, ghostSeparator, ghostPriceLabel, ghostPrice, ghostButton
        );
        skeletonCard.getChildren().add(container);

        // Bind animations to nodes to simulate smooth asynchronous shimmers
        for (var node : container.getChildren()) {
            if (node != ghostSeparator) {
                FadeTransition pulse = new FadeTransition(Duration.millis(750), node);
                pulse.setFromValue(1.0);
                pulse.setToValue(0.35);
                pulse.setCycleCount(Animation.INDEFINITE);
                pulse.setAutoReverse(true);
                pulse.play();
            }
        }

        return skeletonCard;
    }
}