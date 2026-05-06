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

        System.out.println("DEBUG - Raw Server String: " + serverResponse);

        if (serverResponse.equals("NO_ITEMS") || serverResponse.isEmpty()) {
            // Show the placeholder, hide the scroll area
            emptyRecentPane.setVisible(true);
            recentScrollPane.setVisible(false);
            return; // Stop here!
        }

        emptyRecentPane.setVisible(false);
        recentScrollPane.setVisible(true);

        String[] auctionItems = serverResponse.split("\\|");

        for (String itemStr : auctionItems) {
            String[] itemDetails = itemStr.split("~");

            System.out.println("DEBUG - Pieces of data found for item: " + itemDetails.length);

            // We now expect at least 8 pieces of data from the server!
            if (itemDetails.length >= 8) {
                try {
                    // 1. Unpack the data in the exact order the server sent it
                    String name = itemDetails[0];
                    double startPrice = Double.parseDouble(itemDetails[1]);
                    double currentPrice = Double.parseDouble(itemDetails[2]);
                    String condition = itemDetails[3];
                    String imagePath = itemDetails[4];
                    String description = itemDetails[5];
                    String seller = itemDetails[6];
                    long endTimeMillis = Long.parseLong(itemDetails[7]);
                    double increment = Double.parseDouble(itemDetails[8]);

                    // 2. Load the Card
                    javafx.fxml.FXMLLoader cardLoader = new javafx.fxml.FXMLLoader();
                    cardLoader.setLocation(getClass().getResource("/com/example/auctionapp/AuctionCard.fxml"));
                    javafx.scene.Node cardNode = cardLoader.load();

                    AuctionCardController cardController = cardLoader.getController();

                    // 3. Pass ALL EIGHT pieces to the card so it can remember them for when it gets clicked!
                    cardController.setCardData(
                            name,
                            startPrice,
                            currentPrice,
                            condition,
                            description,
                            imagePath,
                            seller,
                            endTimeMillis,
                            increment
                    );

                    recentItemsContainer.getChildren().add(cardNode);

                } catch (Exception e) {
                    System.out.println("ERROR: Could not process an item or load the AuctionCard.fxml file!");
                    e.printStackTrace();
                }
            }
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
