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
import java.util.Base64;
import java.io.ByteArrayInputStream;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;

import javafx.scene.layout.VBox;


public class BrowseController {
    private DashboardController mainDashboard;

    @FXML
    private Label categoryTitleLabel;
    @FXML private FlowPane recentItemsContainer;
    @FXML private VBox emptyRecentPane;
    @FXML private ScrollPane recentScrollPane;
    public void setMainDashboard(DashboardController mainDashboard) {
        this.mainDashboard = mainDashboard;
    }

    public void displayAuctionsOnScreen(String serverResponse) {
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
                    // 3. Extract data using the JSON keys we set in DatabaseManager
                    String name = itemObj.get("itemName").getAsString();
                    double startPrice = itemObj.get("startingPrice").getAsDouble();
                    double currentPrice = itemObj.get("currentPrice").getAsDouble();
                    String condition = itemObj.get("itemCondition").getAsString();
                    String base64Image = itemObj.get("imagePath").getAsString();
                    String description = itemObj.get("description").getAsString();
                    String seller = itemObj.get("seller").getAsString();
                    long endTimeMillis = itemObj.get("endTime").getAsLong();
                    double increment = itemObj.get("priceIncrement").getAsDouble();

                    // 4. Convert Base64 String back to a JavaFX Image
                    Image fxImage = null;
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                            fxImage = new Image(new ByteArrayInputStream(imageBytes));
                        } catch (Exception e) {
                            System.out.println("Image decode failed for: " + name);
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
    @FXML
    public void initialize() {
        // 1. Find out what category they clicked
        String categoryToLoad = UserSession.getCurrentCategory();

        // 2. Just update the title text! (No networking needed here anymore)
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
