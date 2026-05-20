package com.example.auctionapp.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle; // Make sure to import this!
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.transform.Scale;


import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;


public class DashboardController {
    private static DashboardController instance;
    public DashboardController() {
        instance = this;
    }

    // 3. Allow anyone in the app to grab the Dashboard!
    public static DashboardController getInstance() {
        return instance;
    }
    @FXML private VBox emptyStatePane;
    @FXML private VBox itemDetailsPane;
    @FXML private StackPane centerContentArea;
    @FXML private BorderPane mainContent; // The fx:id you set in Scene Builder
    @FXML private StackPane rootPane;


    @FXML private Label conditionLabel;
    @FXML private Label startingBidLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label previewSeller;
    @FXML
    private Label timeLeftLabel;

    // --- 2. The Elements Inside the Details Panel ---
    @FXML private ImageView previewImage;
    @FXML private Label previewTitle;
    @FXML private Label previewDescription;
    @FXML private Button bidButton;
    @FXML private Button infoButton;
    @FXML private ToggleButton HomeButton;
    @FXML private ToggleButton MyBidButton;
    @FXML private ToggleButton MyAuctionButton;
    @FXML private ToggleButton NotifButton;
    @FXML private StackPane rightPane;

    private double currentItemIncrement;
    private String currentImagePath = "";
    @FXML
    public void sellItem() {
        // This makes the right panel invisible AND removes its reserved space!
        rightPane.setVisible(false);
        rightPane.setManaged(false);
        HomeButton.setSelected(false);
        MyBidButton.setSelected(false);
        MyAuctionButton.setSelected(false);
        NotifButton.setSelected(false);


        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/SellItemView.fxml"));
            Parent sellView = loader.load();

            if (sellView instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) sellView).prefWidthProperty().bind(centerContentArea.widthProperty());
                ((javafx.scene.layout.Region) sellView).prefHeightProperty().bind(centerContentArea.heightProperty());
            }

            centerContentArea.getChildren().clear();
            centerContentArea.getChildren().add(sellView);

        } catch (IOException e) {
            System.out.println("Crash! Could not load SellItemView.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    public void loadMyauction() {
        // Bring the right pane back when returning to Home/Browse!
        rightPane.setVisible(true);
        rightPane.setManaged(true);
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/MyAuctionView.fxml"));
            Parent auction = loader.load();
            rightPane.setVisible(true);
            rightPane.setManaged(true);

            if (auction instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) auction).prefWidthProperty().bind(centerContentArea.widthProperty());
                ((javafx.scene.layout.Region) auction).prefHeightProperty().bind(centerContentArea.heightProperty());
            }


            centerContentArea.getChildren().clear();
            centerContentArea.getChildren().add(auction);

        } catch (IOException e) {
            System.out.println("Crash! Could not load HomeView.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    public void loadNotif() {
        rightPane.setVisible(true);
        rightPane.setManaged(true);
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/Notif.fxml"));
            Parent Notif = loader.load();
            rightPane.setVisible(true);
            rightPane.setManaged(true);

            if (Notif instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) Notif).prefWidthProperty().bind(centerContentArea.widthProperty());
                ((javafx.scene.layout.Region) Notif).prefHeightProperty().bind(centerContentArea.heightProperty());
            }


            centerContentArea.getChildren().clear();
            centerContentArea.getChildren().add(Notif);

        } catch (IOException e) {
            System.out.println("Crash! Could not load HomeView.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    public void loadMyBids() {
        // Bring the right pane back when returning to Home/Browse!
        rightPane.setVisible(true);
        rightPane.setManaged(true);
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/MyBidview.fxml"));
            Parent MyBidview = loader.load();
            rightPane.setVisible(true);
            rightPane.setManaged(true);

            if (MyBidview instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) MyBidview).prefWidthProperty().bind(centerContentArea.widthProperty());
                ((javafx.scene.layout.Region) MyBidview).prefHeightProperty().bind(centerContentArea.heightProperty());
            }


            centerContentArea.getChildren().clear();
            centerContentArea.getChildren().add(MyBidview);

        } catch (IOException e) {
            System.out.println("Crash! Could not load HomeView.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    public void loadHomeView() {

        rightPane.setVisible(true);
        rightPane.setManaged(true);        try {
            // Load the HomeView.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/HomeView.fxml"));
            Parent homeView = loader.load();
            rightPane.setVisible(true);
            rightPane.setManaged(true);

            if (homeView instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) homeView).prefWidthProperty().bind(centerContentArea.widthProperty());
                ((javafx.scene.layout.Region) homeView).prefHeightProperty().bind(centerContentArea.heightProperty());
            }

            // Clear the center area and add the new screen
            centerContentArea.getChildren().clear();
            centerContentArea.getChildren().add(homeView);

        } catch (IOException e) {
            System.out.println("Crash! Could not load HomeView.fxml");
            e.printStackTrace();
        }
    }
    public void showItemDetails(String seller, String title, String description, Image image) {
        emptyStatePane.setVisible(false);
        itemDetailsPane.setVisible(true);

        previewSeller.setText(seller); // Now the seller name updates!
        previewTitle.setText(title);
        previewDescription.setText(description);
        previewImage.setImage(image);
    }

    @FXML
    public void initialize() {

        Platform.runLater(() -> {
            Scene scene = mainContent.getScene();
            if (scene != null) {
                applyScaling(scene);
            }
        });

        emptyStatePane.setVisible(true);
        itemDetailsPane.setVisible(false);

        HomeButton.setSelected(true);
        loadHomeView();


        // 2. The Cookie Cutter for the Image Corners
        // Replace 250 and 200 with the exact Width and Height you typed into Scene Builder!
        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);  // How round you want the left/right corners
        clip.setArcHeight(30); // How round you want the top/bottom corners
        clip.widthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> previewImage.getLayoutBounds().getWidth(),
                previewImage.layoutBoundsProperty()
        ));

        clip.heightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> previewImage.getLayoutBounds().getHeight(),
                previewImage.layoutBoundsProperty()
        ));

        // 4. Apply the cut!
        previewImage.setClip(clip);
    }

    private void applyScaling(Scene scene) {
        // These are your "Design Resolution" numbers from Scene Builder
        final double targetWidth = 1900.0;
        final double targetHeight = 1200.0;

        // Create the Scale object
        Scale scale = new Scale(1, 1, 0, 0);

        // Set the pivot point to the center so it scales inward
        mainContent.getTransforms().add(scale);

        // Listen for window resizing
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            double widthRatio = newVal.doubleValue() / targetWidth;
            double heightRatio = scene.getHeight() / targetHeight;

            // Choose the smaller ratio to ensure the whole app fits on the screen
            double bestRatio = Math.min(widthRatio, heightRatio);

            scale.setX(bestRatio);
            scale.setY(bestRatio);
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            double widthRatio = scene.getWidth() / targetWidth;
            double heightRatio = newVal.doubleValue() / targetHeight;

            double bestRatio = Math.min(widthRatio, heightRatio);

            scale.setX(bestRatio);
            scale.setY(bestRatio);
        });
    }

    public void showItemPreview(String title, String description, String imagePath, long endTimeMillis,
                                String seller, double startingBid, double currentBid, String condition, double increment) {

        // 1. Hide the empty state box and show the item details box
        emptyStatePane.setVisible(false);
        itemDetailsPane.setVisible(true);

        this.currentItemIncrement = increment;
        this.currentImagePath = imagePath;

        // 2. Inject the basic data into your right-panel labels
        previewTitle.setText(title);
        previewDescription.setText(description);
        previewSeller.setText(seller); // 👈 Changed from "Admin" to the real seller!

        // 3. Calculate the time and update the UI label
        String calculatedTime = calculateTimeLeft(endTimeMillis);
        timeLeftLabel.setText(calculatedTime);

        // ---------------------------------------------------------
        // 4. NEW: Update Condition and Pricing Labels!
        // ---------------------------------------------------------
        conditionLabel.setText(condition);
        startingBidLabel.setText("$" + String.format("%.2f", startingBid)); // Formats to 2 decimal places
        currentBidLabel.setText("$" + String.format("%.2f", currentBid));

        // 5. Safely load the image
        // Safely load the image
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                        // 1. New Cloud Storage Setup: Load directly from the Supabase web URL in the background
                        Image image = new Image(imagePath, true);
                        previewImage.setImage(image);
                    } else if (imagePath.length() > 200 || imagePath.startsWith("/9j/")) {
                        // 2. Fallback: Old Base64 logic
                        byte[] decodedBytes = Base64.getDecoder().decode(imagePath.trim());
                        Image image = new Image(new ByteArrayInputStream(decodedBytes));
                        previewImage.setImage(image);
                    } else {
                        // 3. Fallback: Old local file paths
                        String formattedUrl = new java.io.File(imagePath).toURI().toString();
                        Image image = new Image(formattedUrl);
                        previewImage.setImage(image);
                    }
                } catch (Exception e) {
                    System.out.println("Card Image Error: Could not load image from target path: " + imagePath);
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("Card Image Error: Could not load image data.");
            // e.printStackTrace(); // Uncomment this if it still doesn't work to see the error
        }
    }
    private String calculateTimeLeft(long endTimeMillis) {
        long currentTime = System.currentTimeMillis();
        long timeRemaining = endTimeMillis - currentTime;

        // If the time is up!
        if (timeRemaining <= 0) {
            return "Ended";
        }

        // Math to convert milliseconds into days, hours, and minutes
        long days = timeRemaining / (1000 * 60 * 60 * 24);
        long hours = (timeRemaining / (1000 * 60 * 60)) % 24;
        long minutes = (timeRemaining / (1000 * 60)) % 60;

        // Format the output dynamically so it looks clean
        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m left";
        }
    }
    @FXML
    public void loadDetailedBidScreen() {
        try {
            // 1. Load the DetailedBid FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/DetailBid.fxml"));
            javafx.scene.Parent detailedView = loader.load();

            // 2. Get the controller and pass the data from the preview labels!
            com.example.auctionapp.controller.DetailedBidController controller = loader.getController();

            // We grab the text directly from the UI elements in the right pane
            String name = previewTitle.getText();
            String desc = previewDescription.getText();
            String price = currentBidLabel.getText().replace("$", "").replace(",", ""); // Clean formatting
            String condition = conditionLabel.getText();
            String seller = previewSeller.getText();
            String timeLeft = timeLeftLabel.getText();

            // Pass the data (we leave image path blank for a moment, or you can pass your saved URL)
            controller.setItemData(name, desc, price, condition, seller, currentImagePath, String.valueOf(currentItemIncrement), timeLeft);
            if (!centerContentArea.getChildren().isEmpty()) {
                javafx.scene.Node currentView = centerContentArea.getChildren().get(0);
                controller.setPreviousContent(currentView);
            }

            // 3. COLLAPSE THE RIGHT PANE
            // This is the magic! It makes the pane invisible AND tells the BorderPane to reclaim its space
            rightPane.setVisible(false);
            rightPane.setManaged(false);

            // 4. Fill the center pane with the new screen
            centerContentArea.getChildren().clear();

            javafx.scene.layout.Region detailRegion = (javafx.scene.layout.Region) detailedView;
            detailRegion.prefWidthProperty().bind(centerContentArea.widthProperty());
            detailRegion.prefHeightProperty().bind(centerContentArea.heightProperty());

            centerContentArea.getChildren().add(detailedView);

        } catch (Exception e) {
            System.out.println("Crash! Could not load the DetailedBid screen.");
            e.printStackTrace();
        }
    }

}
