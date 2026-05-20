package com.example.auctionapp.controller;

import com.example.auctionapp.model.NetworkMessage;
import com.example.auctionapp.model.UserSession;
import com.google.gson.Gson;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.IOException;

public class HomeController {
    @FXML private Label welcomeLabel;
    @FXML private StackPane furnitureCard;
    @FXML private StackPane vehiclesCard;
    @FXML private StackPane electronicsCard;
    @FXML private StackPane fashionCard;
    @FXML private StackPane bookCard;
    @FXML private StackPane collectablesCard;
    @FXML private StackPane sportsCard;
    @FXML private StackPane artCard;

    private void addHoverAnimation(StackPane card) {
        // Create the zoom-in animation (takes 0.2 seconds)
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), card);
        scaleUp.setToX(1.03); // Zoom to 103%
        scaleUp.setToY(1.03);

        // Create the zoom-out animation
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), card);
        scaleDown.setToX(1.0); // Back to 100%
        scaleDown.setToY(1.0);

        // Tell the card to play the animations when the mouse enters/exits
        card.setOnMouseEntered(e -> {
            scaleDown.stop(); // Stop zooming out if it's currently doing so
            scaleUp.play();   // Start zooming in
            card.setViewOrder(-1); // Bring the card to the very front so it doesn't overlap neighbors!
        });

        card.setOnMouseExited(e -> {
            scaleUp.stop();
            scaleDown.play();
            card.setViewOrder(0); // Put it back in the normal layer
        });
    }
    public void openBrowseScreen() {
        String currentCategory = UserSession.getCurrentCategory();
        System.out.println("CLIENT DEBUG: Instantly switching to Browse screen for " + currentCategory + "...");

        try {
            // 1. Instantly load the Browse FXML screen on the UI thread
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/Browse.fxml"));
            Parent browseScreen = loader.load();

            // 2. Get the BrowseController instance
            BrowseController controller = loader.getController();

            // 3. Find the Center Content Area and swap the view immediately
            Pane dashboardCenterPane = (Pane) welcomeLabel.getScene().lookup("#centerContentArea");

            if (dashboardCenterPane != null) {
                dashboardCenterPane.getChildren().clear();

                if (browseScreen instanceof Region) {
                    ((Region) browseScreen).prefWidthProperty().bind(dashboardCenterPane.widthProperty());
                    ((Region) browseScreen).prefHeightProperty().bind(dashboardCenterPane.heightProperty());
                }

                dashboardCenterPane.getChildren().add(browseScreen);

                // --- UPDATED CLEAN CODE HERE ---
                // Just tell the controller what category it is. Do NOT touch its labels directly!
                controller.fetchCategoryAuctions(currentCategory);

            } else {
                System.out.println("ERROR: Could not find #centerContentArea. Check your FX:ID in Dashboard.fxml!");
            }

        } catch (Exception e) {
            System.out.println("CRASH: Error switching to Browse screen.");
            e.printStackTrace();
        }
    }

    private void applyRoundedCorners(StackPane card) {
        // 1. Safety check to prevent the crash you just experienced!
        if (card == null) {
            System.err.println("Warning: Tried to apply rounded corners to a null card. Check Scene Builder fx:ids!");
            return;
        }

        // 2. Create the clipping rectangle without hardcoded sizes
        Rectangle clipRect = new Rectangle();

        // 3. Set how round you want the corners
        clipRect.setArcWidth(30.0);
        clipRect.setArcHeight(30.0);

        // 4. BIND the rectangle's size to the card's actual size
        clipRect.widthProperty().bind(card.widthProperty());
        clipRect.heightProperty().bind(card.heightProperty());

        // 5. Apply the clip
        card.setClip(clipRect);
    }


    // 2. This method runs automatically when the screen loads!
    @FXML
    public void initialize() {

        applyRoundedCorners(furnitureCard);
        applyRoundedCorners(vehiclesCard);
        applyRoundedCorners(electronicsCard);
        applyRoundedCorners(fashionCard);
        applyRoundedCorners(bookCard);









        String loggedInUser = UserSession.getUsername();
        welcomeLabel.setText("Welcome back, " + loggedInUser + "!");
        addHoverAnimation(furnitureCard);
        addHoverAnimation(vehiclesCard);
        addHoverAnimation(electronicsCard);
        addHoverAnimation(fashionCard);
        addHoverAnimation(bookCard);

    }
    @FXML
    public void browseFurniture() {
        UserSession.setCurrentCategory("Furniture");
        openBrowseScreen();
    }

    @FXML
    public void browseVehicles() {
        UserSession.setCurrentCategory("Vehicles");
        openBrowseScreen();
    }@FXML
    public void browseElectronics() {
        UserSession.setCurrentCategory("Electronics");
        openBrowseScreen();
    }

    @FXML
    public void browseClothes() {
        UserSession.setCurrentCategory("Fashion");
        openBrowseScreen();
    }
    @FXML
    public void browseBooks() {
        UserSession.setCurrentCategory("Books");
        openBrowseScreen();
    }

    @FXML
    public void browseCollectables() {
        UserSession.setCurrentCategory("Collectables");
        openBrowseScreen();
    }

    @FXML
    public void browseSports() {
        UserSession.setCurrentCategory("Sports");
        openBrowseScreen();
    }

    @FXML
    public void browseArt() {
        UserSession.setCurrentCategory("Art");
        openBrowseScreen();
    }
}
