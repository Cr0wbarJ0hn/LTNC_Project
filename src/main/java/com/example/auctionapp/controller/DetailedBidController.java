package com.example.auctionapp.controller;

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

public class DetailedBidController {

    // --- FXML IDs from your Scene Builder ---
    @FXML private Label DetailTimeLeft;
    @FXML private TextField manualBidInput;
    @FXML private VBox autoBidSettings; // Container for Max Bid, etc.
    @FXML private CheckBox autoBidSwitch;
    @FXML private Pane manualBidPane;
    @FXML private Pane autoBidPane;
    @FXML private Label DetailIncrement;
    @FXML private Hyperlink BackLink;
    @FXML private Label ItemName;
    @FXML private ImageView DetailImage; // Left your spelling exactly as it is in FXML!
    @FXML private Label DetailDescription;
    @FXML private Label ConditionLabel;
    @FXML private Label Sellerlabel;
    @FXML private Label DetailPrice;
    @FXML private LineChart<String, Number> priceHistoryChart;

    // This will hold the exact screen the user was looking at before clicking the item
    private javafx.scene.Node previousContent;

    // Method to receive the old screen
    public void setPreviousContent(javafx.scene.Node previousContent) {
        this.previousContent = previousContent;
    }


    // --- Method to catch the data from the Browse screen ---
    public void setItemData(String name, String description, String price, String condition, String seller, String imagePath, String increment, String timeLeft) {

        ItemName.setText(name);
        DetailDescription.setText(description);
        DetailPrice.setText("$" + price);
        ConditionLabel.setText("Condition: " + condition);
        Sellerlabel.setText("Seller: " + seller);
        DetailIncrement.setText("$" + increment);
        DetailTimeLeft.setText(timeLeft);


        // 2. Load the image safely
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                String formattedUrl = new java.io.File(imagePath).toURI().toString();
                javafx.scene.image.Image newImage = new javafx.scene.image.Image(formattedUrl);
                DetailImage.setImage(newImage); // Assuming your ImageView is named itemImageView
            }
        } catch (Exception e) {
            System.out.println("Detailed Screen Error: Could not load image -> " + imagePath);
        }


        setupPlaceholderChart();
    }

    private void setupPlaceholderChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Start", 500));
        series.getData().add(new XYChart.Data<>("Day 2", 750));
        series.getData().add(new XYChart.Data<>("Today", 1000)); // You can replace 1000 with Double.parseDouble(price) later!
        priceHistoryChart.getData().add(series);
    }
    // 1. Declare the button at the top with your other FXML variables
    @FXML private javafx.scene.control.Button backButton;

    // 2. Add the method to handle the click
    @FXML
    public void goBack() {
        try {
            // 1. Bring the right pane back
            javafx.scene.layout.Pane rightPane = (javafx.scene.layout.Pane) BackLink.getScene().lookup("#rightPane");
            if (rightPane != null) {
                rightPane.setVisible(true);
                rightPane.setManaged(true);
            }

            // 2. Put the EXACT old screen back into the center
            javafx.scene.layout.Pane dashboardCenter = (javafx.scene.layout.Pane) BackLink.getScene().lookup("#centerContentArea");

            if (dashboardCenter != null && previousContent != null) {
                dashboardCenter.getChildren().clear();
                dashboardCenter.getChildren().add(previousContent);
            } else {
                // Failsafe: If it lost the previous screen, click Home to prevent a blank screen
                javafx.scene.control.ToggleButton homeBtn = (javafx.scene.control.ToggleButton) BackLink.getScene().lookup("#HomeButton");
                if (homeBtn != null) homeBtn.fire();
            }

        } catch (Exception e) {
            System.out.println("Could not go back.");
            e.printStackTrace();
        }
    }
    @FXML
    public void initialize() {

        autoBidPane.visibleProperty().bind(autoBidSwitch.selectedProperty());
        autoBidPane.managedProperty().bind(autoBidPane.visibleProperty());

        // Manual-bid pane is visible ONLY when switch is NOT selected
        manualBidPane.visibleProperty().bind(autoBidSwitch.selectedProperty().not());
        manualBidPane.managedProperty().bind(manualBidPane.visibleProperty());

        // 2. Add an animation (Optional but smooth)
        autoBidSwitch.selectedProperty().addListener((obs, wasAuto, isAuto) -> {

            System.out.println("Mode changed: " + (isAuto ? "AUTO" : "MANUAL"));
        });

        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);  // How round you want the left/right corners
        clip.setArcHeight(30); // How round you want the top/bottom corners
        clip.widthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> DetailImage.getLayoutBounds().getWidth(),
                DetailImage.layoutBoundsProperty()
        ));

        clip.heightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> DetailImage.getLayoutBounds().getHeight(),
                DetailImage.layoutBoundsProperty()
        ));

        // 4. Apply the cut!
        DetailImage.setClip(clip);
    }
}