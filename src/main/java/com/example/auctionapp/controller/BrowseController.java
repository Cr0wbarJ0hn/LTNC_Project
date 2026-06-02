package com.example.auctionapp.controller;

import com.example.auctionapp.model.UserSession;
import com.example.auctionapp.network.NetworkRouter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.ArrayList; // 🌟 ADDED IMPORT
import java.util.List;      // 🌟 ADDED IMPORT
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import com.example.auctionapp.model.NetworkMessage;
import javafx.util.Duration;

public class BrowseController {

    public static BrowseController activeBrowseScreen = null;
    public HashMap<Integer, AuctionCardController> liveCards = new HashMap<>();
    private DashboardController mainDashboard;

    // 🌟 1. MASTER LIST FIELD DECLARATION
    private final List<Node> allAuctionCards = new ArrayList<>();

    @FXML private Label categoryTitleLabel;
    @FXML private FlowPane recentItemsContainer;
    @FXML private TextField searchBar;
    @FXML private VBox emptyRecentPane;
    @FXML private ScrollPane recentScrollPane;

    public void setMainDashboard(DashboardController mainDashboard) {
        this.mainDashboard = mainDashboard;
    }

    public void fetchCategoryAuctions(String targetCategory) {
        if (categoryTitleLabel != null) {
            categoryTitleLabel.setText(targetCategory);
        }
        if (emptyRecentPane != null) emptyRecentPane.setVisible(false);

        recentItemsContainer.getChildren().clear();
        for (int i = 0; i < 4; i++) {
            recentItemsContainer.getChildren().add(createSkeletonCard());
        }

        try {
            Gson gson = new Gson();
            NetworkMessage request = new NetworkMessage("GET_CATEGORY", targetCategory, true);

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
        allAuctionCards.clear(); // 🌟 CLEAR MASTER STORAGE FOR NEW DATA FETCH
        recentItemsContainer.getChildren().clear();

        System.out.println("DEBUG - Raw Server JSON: " + serverResponse);

        if (serverResponse == null || serverResponse.equals("NO_ITEMS") || serverResponse.equals("[]") || serverResponse.isEmpty()) {
            emptyRecentPane.setVisible(true);
            recentScrollPane.setVisible(false);
            return;
        }

        emptyRecentPane.setVisible(false);
        recentScrollPane.setVisible(true);

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

                    Image fxImage = null;
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            if (base64Image.startsWith("http")) {
                                fxImage = new Image(base64Image, true);
                            } else {
                                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                                fxImage = new Image(new ByteArrayInputStream(imageBytes));
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️ Image load failed for: " + name);
                        }
                    }

                    FXMLLoader cardLoader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/AuctionCard.fxml"));
                    Node cardNode = cardLoader.load();
                    AuctionCardController cardController = cardLoader.getController();

                    cardController.setCardData(
                            id, name, startPrice, currentPrice, condition,
                            description, base64Image, seller, endTimeMillis, increment
                    );

                    // 🌟 2. TAG NODE WITH THE ITEM NAME & SAVE TO MASTER STORAGE
                    cardNode.setUserData(name);
                    allAuctionCards.add(cardNode);

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

    private VBox createSkeletonCard() {
        VBox skeletonCard = new VBox();
        skeletonCard.setPrefSize(402.0, 551.0);

        Pane container = new Pane();
        container.setPrefSize(402.0, 551.0);
        container.setStyle("-fx-background-color: #20202E; -fx-background-radius: 10px;");

        Rectangle ghostImage = new Rectangle(402.0, 357.0);
        ghostImage.setStyle("-fx-fill: #2B2B3C;");
        Rectangle imageClip = new Rectangle(402.0, 357.0);
        imageClip.setArcWidth(40.0);
        imageClip.setArcHeight(40.0);
        ghostImage.setClip(imageClip);

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

        Rectangle ghostCondition = new Rectangle(90.0, 25.0);
        ghostCondition.setLayoutX(289.0);
        ghostCondition.setLayoutY(373.0);
        ghostCondition.setArcWidth(20.0);
        ghostCondition.setArcHeight(20.0);
        ghostCondition.setStyle("-fx-fill: #2B2B3C;");

        Rectangle ghostSeller = new Rectangle(100.0, 14.0);
        ghostSeller.setLayoutX(29.0);
        ghostSeller.setLayoutY(426.0);
        ghostSeller.setArcWidth(8.0);
        ghostSeller.setArcHeight(8.0);
        ghostSeller.setStyle("-fx-fill: #2B2B3C;");

        Rectangle ghostSeparator = new Rectangle(349.0, 1.0);
        ghostSeparator.setLayoutX(21.0);
        ghostSeparator.setLayoutY(453.0);
        ghostSeparator.setStyle("-fx-fill: #3E3E53;");

        Rectangle ghostPriceLabel = new Rectangle(70.0, 12.0);
        ghostPriceLabel.setLayoutX(29.0);
        ghostPriceLabel.setLayoutY(476.0);
        ghostPriceLabel.setStyle("-fx-fill: #2B2B3C;");

        Rectangle ghostPrice = new Rectangle(85.0, 24.0);
        ghostPrice.setLayoutX(28.0);
        ghostPrice.setLayoutY(492.0);
        ghostPrice.setArcWidth(8.0);
        ghostPrice.setArcHeight(8.0);
        ghostPrice.setStyle("-fx-fill: #3A3A50;");

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

    private void setupSearchBar() {
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {

            recentItemsContainer.getChildren().clear();

            if (newValue == null || newValue.isEmpty()) {
                recentItemsContainer.getChildren().addAll(allAuctionCards);
                emptyRecentPane.setVisible(allAuctionCards.isEmpty());
                return;
            }

            String lowerCaseFilter = newValue.toLowerCase();
            boolean matchFound = false;

            for (Node cardNode : allAuctionCards) {
                String itemTitle = (String) cardNode.getUserData();

                if (itemTitle != null && itemTitle.toLowerCase().contains(lowerCaseFilter)) {
                    recentItemsContainer.getChildren().add(cardNode);
                    matchFound = true;
                }
            }

            emptyRecentPane.setVisible(!matchFound);
        });
    }

    @FXML
    public void initialize() {
        activeBrowseScreen = this;

        NetworkRouter.startGlobalListener();

        // 🌟 3. INVOKE SEARCH PIPELINE LISTENER AT VIEW STARTUP
        setupSearchBar();

        String categoryToLoad = UserSession.getCurrentCategory();

        if (categoryTitleLabel != null) {
            categoryTitleLabel.setText(categoryToLoad + " Auctions");
        }
    }

    @FXML
    public void BackLink(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/Homeview.fxml"));
            Parent homeScreen = loader.load();

            Pane dashboardCenterPane = (Pane) ((Node) event.getSource()).getScene().lookup("#centerContentArea");

            if (dashboardCenterPane != null) {
                dashboardCenterPane.getChildren().clear();

                Region homeRegion = (Region) homeScreen;
                homeRegion.prefWidthProperty().bind(dashboardCenterPane.widthProperty());
                homeRegion.prefHeightProperty().bind(dashboardCenterPane.heightProperty());

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