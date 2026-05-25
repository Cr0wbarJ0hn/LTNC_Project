package com.example.auctionapp.controller;

import com.example.auctionapp.model.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import com.example.auctionapp.model.NetworkMessage;
import javafx.util.Duration;
import com.google.gson.Gson;
import javafx.application.Platform;

import javafx.scene.layout.VBox;


public class BrowseController {

    private static Thread globalListenerThread = null;
    public static BrowseController activeBrowseScreen = null;
    private static volatile boolean isListening = false;
    private HashMap<Integer, AuctionCardController> liveCards = new HashMap<>();
    private DashboardController mainDashboard;


    @FXML
    private Label categoryTitleLabel;
    @FXML private FlowPane recentItemsContainer;
    @FXML private VBox emptyRecentPane;
    @FXML private ScrollPane recentScrollPane;
    public void setMainDashboard(DashboardController mainDashboard) {
        this.mainDashboard = mainDashboard;
    }

    public void fetchCategoryAuctions(String targetCategory) {

        // 1. Set titles and hide empty state
        if (categoryTitleLabel != null) {
            categoryTitleLabel.setText(targetCategory);
        }
        if (emptyRecentPane != null) emptyRecentPane.setVisible(false);

        // 2. Clear old items and show the skeleton loading animation
        recentItemsContainer.getChildren().clear();
        for (int i = 0; i < 4; i++) {
            recentItemsContainer.getChildren().add(createSkeletonCard());
        }

        try {
            Gson gson = new Gson();
            NetworkMessage request = new NetworkMessage("GET_CATEGORY", targetCategory, true);

            // Send request over the socket stream
            UserSession.getOut().println(gson.toJson(request));
            UserSession.getOut().flush();

            System.out.println(" Sent GET_CATEGORY for: " + targetCategory);

        } catch (Exception e) {
            System.err.println(" Error sending category request to server:");
            e.printStackTrace();
        }
    }

    public void displayAuctionsOnScreen(String serverResponse) {
        liveCards.clear();

        recentItemsContainer.getChildren().clear();


        System.out.println("DEBUG - Raw Server JSON: " + serverResponse);

        // 1. Handle Empty or No Items
        if (serverResponse == null || serverResponse.equals("NO_ITEMS") || serverResponse.equals("[]") || serverResponse.isEmpty()) {
            emptyRecentPane.setVisible(true);
            recentScrollPane.setVisible(false);
            return;
        }

        emptyRecentPane.setVisible(false);
        recentScrollPane.setVisible(true);

        try {
            // 2. Parse the JSON string into a JsonArray
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

                    // 4. Smart Image Loader (Handles both URLs and Base64)
                    Image fxImage = null;
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            if (base64Image.startsWith("http")) {
                                // 🌟 IT'S A SUPABASE URL!
                                // The 'true' at the end tells JavaFX to load it in the background so it doesn't freeze your UI
                                fxImage = new Image(base64Image, true);
                            } else {
                                // IT'S BASE64 (Just in case you have older items in the database)
                                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                                fxImage = new Image(new ByteArrayInputStream(imageBytes));
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️ Image load failed for: " + name);
                        }
                    }

                    // 5. Load the FXML Card
                    FXMLLoader cardLoader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/AuctionCard.fxml"));
                    Node cardNode = cardLoader.load();
                    AuctionCardController cardController = cardLoader.getController();

                    // 6. Set the data into the card
                    // Note: Ensure your cardController.setCardData can accept an 'Image' or
                    // just pass the base64Image string if you handle decoding inside the card.
                    cardController.setCardData(
                            id,
                            name,
                            startPrice,
                            currentPrice,
                            condition,
                            description,
                            base64Image, // Passing the string or image as needed
                            seller,
                            endTimeMillis,
                            increment
                    );

                    recentItemsContainer.getChildren().add(cardNode);
                    liveCards.put(id, cardController);



                } catch (Exception e) {
                    System.out.println("ERROR: Could not process a JSON item!");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: Invalid JSON received from server!");
            e.printStackTrace();
        }
    }

    public static void startGlobalListener() {
        if (globalListenerThread != null && globalListenerThread.isAlive()) {
            return; // Already running! Don't duplicate the mailman.
        }

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

                        // Route 1: Live Bid Updates (Pushes new prices directly to live cards)
                        if (action.equals("LIVE_BID_UPDATE")) {
                            int incomingAuctionId = packet.get("auctionId").getAsInt();
                            double newPrice = packet.get("newPrice").getAsDouble();

                            Platform.runLater(() -> {
                                if (activeBrowseScreen != null && activeBrowseScreen.liveCards.containsKey(incomingAuctionId)) {
                                    activeBrowseScreen.liveCards.get(incomingAuctionId).updateLivePrice(newPrice);
                                }
                            });
                        }
                        // Route 2: Category Data (Delivered to Browse Screen)
                        else if (action.equals("CATEGORY_RESPONSE")) {
                            String payloadData = packet.get("data").getAsString();
                            Platform.runLater(() -> {
                                if (activeBrowseScreen != null) {
                                    activeBrowseScreen.displayAuctionsOnScreen(payloadData);
                                }
                            });
                        }
                        // Route 3: My Bids Data (Delivered to MyBids Screen!)
                        else if (action.equals("MY_BIDS_RESPONSE")) {
                            String payloadData = packet.get("data").getAsString();
                            Platform.runLater(() -> {
                                if (myBidController.activeMyBidsScreen != null) {
                                    myBidController.activeMyBidsScreen.displayAuctionsOnScreen(payloadData);
                                    System.out.println("✅ [ROUTER]: Successfully delivered to My Bids screen!");
                                } else {
                                    System.out.println("⚠️ [ROUTER WARNING]: Received bids, but My Bids screen is closed.");
                                }
                            });
                        }
                        else {
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


    private VBox createSkeletonCard() {
        // 1. Base VBox matching your card's FXML dimensions (prefHeight="551", prefWidth="402")
        VBox skeletonCard = new VBox();
        skeletonCard.setPrefSize(402.0, 551.0);

        // 2. Inner Container matching your dark theme background color (#20202E)
        Pane container = new Pane();
        container.setPrefSize(402.0, 551.0); // Bound tightly to match card constraints
        container.setStyle("-fx-background-color: #20202E; -fx-background-radius: 10px;");

        // --- COMPONENT PLACEMENT ---

        // Top Image Block (fitHeight="357", fitWidth="402")
        Rectangle ghostImage = new Rectangle(402.0, 357.0);
        ghostImage.setStyle("-fx-fill: #2B2B3C;");
        // Apply smooth rounding matching your client side mask layout
        Rectangle imageClip = new Rectangle(402.0, 357.0);
        imageClip.setArcWidth(40.0);
        imageClip.setArcHeight(40.0);
        ghostImage.setClip(imageClip);

        // Item Title Lines (layoutX="29", layoutY="371")
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

        // Condition Status Badge (layoutX="289", layoutY="373", width="90", height="25")
        Rectangle ghostCondition = new Rectangle(90.0, 25.0);
        ghostCondition.setLayoutX(289.0);
        ghostCondition.setLayoutY(373.0);
        ghostCondition.setArcWidth(20.0);
        ghostCondition.setArcHeight(20.0);
        ghostCondition.setStyle("-fx-fill: #2B2B3C;");

        // Seller Text Descriptor Block (layoutX="29", layoutY="426")
        Rectangle ghostSeller = new Rectangle(100.0, 14.0);
        ghostSeller.setLayoutX(29.0);
        ghostSeller.setLayoutY(426.0);
        ghostSeller.setArcWidth(8.0);
        ghostSeller.setArcHeight(8.0);
        ghostSeller.setStyle("-fx-fill: #2B2B3C;");

        // Separator line breakpoint element (layoutX="21", layoutY="453", width="349")
        Rectangle ghostSeparator = new Rectangle(349.0, 1.0);
        ghostSeparator.setLayoutX(21.0);
        ghostSeparator.setLayoutY(453.0);
        ghostSeparator.setStyle("-fx-fill: #3E3E53;");

        // Price Descriptor Element label (layoutX="29", layoutY="476")
        Rectangle ghostPriceLabel = new Rectangle(70.0, 12.0);
        ghostPriceLabel.setLayoutX(29.0);
        ghostPriceLabel.setLayoutY(476.0);
        ghostPriceLabel.setStyle("-fx-fill: #2B2B3C;");

        // Actual Large Price Display Block (layoutX="28", layoutY="488")
        Rectangle ghostPrice = new Rectangle(85.0, 24.0);
        ghostPrice.setLayoutX(28.0);
        ghostPrice.setLayoutY(492.0);
        ghostPrice.setArcWidth(8.0);
        ghostPrice.setArcHeight(8.0);
        ghostPrice.setStyle("-fx-fill: #3A3A50;"); // Brighter accent reflection

        // See Item Preview Action Button Frame (layoutX="239", layoutY="484", width="140", height="40")
        Rectangle ghostButton = new Rectangle(140.0, 40.0);
        ghostButton.setLayoutX(239.0);
        ghostButton.setLayoutY(484.0);
        ghostButton.setArcWidth(20.0);
        ghostButton.setArcHeight(20.0);
        ghostButton.setStyle("-fx-fill: #2B2B3C;");

        // Mount structural primitives together inside layouts
        container.getChildren().addAll(
                ghostImage, ghostTitleLine1, ghostTitleLine2, ghostCondition,
                ghostSeller, ghostSeparator, ghostPriceLabel, ghostPrice, ghostButton
        );
        skeletonCard.getChildren().add(container);

        // 3. Shimmer Shading Effects Setup Loop
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

    @FXML
    public void initialize() {
        activeBrowseScreen = this;
        startGlobalListener();

        String categoryToLoad = UserSession.getCurrentCategory();

        // 3. Update the title text
        if (categoryTitleLabel != null) {
            categoryTitleLabel.setText(categoryToLoad + " Auctions");
        }
    }

    @FXML
    public void BackLink(ActionEvent event) {

        try {
            // 1. Load the Home screen back into memory
            // NOTE: Change "Home.fxml" to "HomeView.fxml" if that is what your file is named!
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/Homeview.fxml"));
            Parent homeScreen = loader.load();

            // 2. THE TRICK: Use the button we just clicked (event.getSource()) to find the Scene and the center pane!
            Pane dashboardCenterPane = (Pane) ((Node) event.getSource()).getScene().lookup("#centerContentArea");

            if (dashboardCenterPane != null) {
                // 3. Clear the Browse screen out of the center
                dashboardCenterPane.getChildren().clear();

                // 4. Bind the size just like we did before so it fits perfectly!
                Region homeRegion = (Region) homeScreen;
                homeRegion.prefWidthProperty().bind(dashboardCenterPane.widthProperty());
                homeRegion.prefHeightProperty().bind(dashboardCenterPane.heightProperty());

                // 5. Snap the Home screen perfectly back into the center!
                dashboardCenterPane.getChildren().add(homeRegion);
            } else {
                System.out.println("ERROR: Could not find the Dashboard's centerContentArea!");
            }

        } catch (Exception e) {
            System.out.println("Crash! Could not load Home.fxml");
            e.printStackTrace();
        }
    }

}
