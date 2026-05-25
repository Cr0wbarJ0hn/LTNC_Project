package com.example.auctionapp.controller;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import javafx.scene.image.Image;

public class AuctionCardController {

    @FXML private Label nameLabel;
    @FXML private Label Seller;
    @FXML private Label conditionLabel;
    @FXML private Label priceLabel;
    @FXML private Pane cardContainer;
    @FXML private ImageView itemImageView;

    // 1. Hidden variables to remember the item's details when clicked
    private String savedName;
    private String savedCondition;
    private String savedImagePath;
    private String savedDescription;


    // --- NEW: Hidden variables for the new database data! ---
    private String savedSeller;
    private double savedStartingBid;
    private double savedCurrentBid;
    private long savedEndTimeMillis;
    private double savedIncrement;
    private int savedId;


    private DashboardController dashboardController;


    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public void setCardData(int id, String name, double startingBid, double currentBid, String condition, String description, String imagePath, String seller, long endTimeMillis, double Increment) {

        this.savedId = id;
        this.savedName = name;
        this.savedStartingBid = startingBid;
        this.savedCurrentBid = currentBid;
        this.savedCondition = condition;
        this.savedDescription = description;
        this.savedImagePath = imagePath; // This now holds the Base64 string
        this.savedEndTimeMillis = endTimeMillis;
        this.savedSeller = seller;
        this.savedIncrement = Increment;

        // Set the visible text on the card itself
        nameLabel.setText(name);
        Seller.setText("Seller: " + seller);
        priceLabel.setText("$" + String.format("%.2f", currentBid));

        String cleanCondition = condition.toUpperCase().trim();
        conditionLabel.setText(cleanCondition);

        // Dynamic Colors for the Condition Tag
        String baseTagStyle = "-fx-padding: 4 12 4 12; -fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold; ";
        switch (cleanCondition) {
            case "NEW":
                conditionLabel.setStyle(baseTagStyle + "-fx-background-color: #e3fbed; -fx-text-fill: #27ae60; -fx-border-color: #27ae60; -fx-border-radius: 12px; -fx-border-width: 0.5px;");
                break;
            case "LIKE NEW":
                conditionLabel.setStyle(baseTagStyle + "-fx-background-color: #fff4e6; -fx-text-fill: #e67e22; -fx-border-color: #e67e22; -fx-border-radius: 12px; -fx-border-width: 0.5px;");
                break;
            case "USED":
                conditionLabel.setStyle(baseTagStyle + "-fx-background-color: #f2f2f7; -fx-text-fill: #8e8e93; -fx-border-color: #c7c7cc; -fx-border-radius: 12px; -fx-border-width: 0.5px;");
                break;
            default:
                conditionLabel.setStyle(baseTagStyle + "-fx-background-color: #f0f0f0; -fx-text-fill: #333333;");
                break;
        }

        // --- FIXED IMAGE LOADING LOGIC ---
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    // 1. New Cloud Storage Setup: Load directly from the Supabase web URL in the background
                    Image image = new Image(imagePath, true);
                    itemImageView.setImage(image);
                } else if (imagePath.length() > 200 || imagePath.startsWith("/9j/")) {
                    // 2. Fallback: Old Base64 logic
                    byte[] decodedBytes = Base64.getDecoder().decode(imagePath.trim());
                    Image image = new Image(new ByteArrayInputStream(decodedBytes));
                    itemImageView.setImage(image);
                } else {
                    // 3. Fallback: Old local file paths
                    String formattedUrl = new java.io.File(imagePath).toURI().toString();
                    Image image = new Image(formattedUrl);
                    itemImageView.setImage(image);
                }
            } catch (Exception e) {
                System.out.println("Card Image Error: Could not load image from target path: " + imagePath);
                e.printStackTrace();
            }
        }
    }
    @FXML
    public void initialize() {
        applyImageRounding();
    }



    public void updateLivePrice(double newPrice) {
        // Change the text to the new price
        priceLabel.setText("$" + newPrice);

        // Optional UI Polish: Make the text flash a different color so the user notices!
        priceLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private void applyImageRounding() {
        // 1. Create a rectangle to act as a "cookie cutter"
        Rectangle clip = new Rectangle();

        // 2. Set the rounding (Match this to your CSS radius!)
        // If your CSS radius is 20px, use 40.0 here (it's diameter-based)
        clip.setArcWidth(40.0);
        clip.setArcHeight(40.0);

        // 3. Bind the rectangle size to the ImageView size
        // This ensures that if the image scales, the rounded corners scale with it
        clip.widthProperty().bind(itemImageView.fitWidthProperty());
        clip.heightProperty().bind(itemImageView.fitHeightProperty());

        // 4. Apply the mask to the image
        itemImageView.setClip(clip);
    }

    @FXML
    public void onCardClicked(MouseEvent event) {
        System.out.println("BOOM! You clicked: " + savedName);

        DashboardController dashboard = DashboardController.getInstance();

        if (dashboard != null) {

            dashboard.showItemPreview(
                    this.savedId,
                    savedName,
                    savedDescription,
                    savedImagePath,
                    savedEndTimeMillis,
                    savedSeller,
                    savedStartingBid,
                    savedCurrentBid,
                    savedCondition,
                    savedIncrement
            );
        } else {
            System.out.println("ERROR: The dashboard shortcut failed!");
        }
    }


    @FXML
    public void onMousePressed(MouseEvent event) {
        applyScaleEffect(0.97, 100);
    }

    @FXML
    public void onMouseReleased(MouseEvent event) {
        applyScaleEffect(1.0, 100);
    }

    @FXML
    public void onMouseEntered(MouseEvent event) {
        // Grow slightly when hovered (102%)
        applyScaleEffect(1.02, 200);
        // You could also add a subtle drop shadow change here via CSS

    }

    @FXML
    public void onMouseExited(MouseEvent event) {
        // Return to normal when mouse leaves
        applyScaleEffect(1.0, 200);
    }


    private void applyScaleEffect(double scale, int duration) {
        ScaleTransition st = new ScaleTransition(Duration.millis(duration), cardContainer);
        st.setToX(scale);
        st.setToY(scale);
        // This makes sure the animation doesn't look "choppy" if the user clicks fast
        st.setCycleCount(1);
        st.setAutoReverse(false);
        st.play();
    }
}