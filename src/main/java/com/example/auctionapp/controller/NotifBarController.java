package com.example.auctionapp.controller; // Make sure this matches your project structure!

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class NotifBarController {
    // These MUST match the fx:id attributes in your FXML exactly

    @FXML private ImageView NotifImageview;
    @FXML private Label NotifMainLabel;
    @FXML private Label DetailNotifLabel;
    @FXML private Label timeLabel;

    public void renderNotification(String type, String title, String message, String timestamp) {
        if (timestamp != null && !timestamp.isEmpty()) {
            try {
                // 1. Clean the string trailing fractionals if present so the parser doesn't choke
                if (timestamp.contains(".")) {
                    timestamp = timestamp.split("\\.")[0];
                }
                String cleanIso = timestamp.replace(" ", "T");

                // 2. Parse into a LocalDateTime object
                java.time.LocalDateTime notificationDateTime = java.time.LocalDateTime.parse(cleanIso);

                // 3. Extract just the Dates for comparison
                java.time.LocalDate notificationDate = notificationDateTime.toLocalDate();
                java.time.LocalDate today = java.time.LocalDate.now();

                // 4. Gmail Logic Switch
                if (notificationDate.equals(today)) {
                    // If it happened TODAY, show only the Hour and Minute (e.g., "16:12" or "04:12 PM")
                    java.time.format.DateTimeFormatter timeFormatter =
                            java.time.format.DateTimeFormatter.ofPattern("HH:mm"); // Or "hh:mm a" for AM/PM
                    timestamp = notificationDateTime.format(timeFormatter);
                } else {
                    // If it happened on a PREVIOUS day, show Day and Month (e.g., "29 May" or "May 29")
                    java.time.format.DateTimeFormatter dateFormatter =
                            java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH);
                    timestamp = notificationDateTime.format(dateFormatter);
                }

            } catch (Exception e) {
                System.err.println("Could not parse dynamic Gmail-style time format, using fallback.");
            }
        }

        // 1. Set the standard text placeholders
        NotifMainLabel.setText(title);
        DetailNotifLabel.setText(message);
        timeLabel.setText(timestamp);

        // 2. Reset the main label color to white as a baseline
        NotifMainLabel.setStyle("-fx-text-fill: white;");
        DetailNotifLabel.setStyle("-fx-text-fill: white;");
        timeLabel.setStyle("-fx-text-fill: white;");


        // 3. Customize Colors and Icons based on the notification TYPE!
        switch (type) {
            case "AUCTION_WON":
                setCardIcon("/com/example/auctionapp/icons/trophy_100dp_FFFFFF_FILL0_wght400_GRAD0_opsz48.png");
                break;

            case "AUCTION_LOST":
                setCardIcon("/com/example/auctionapp/icons/money_off_100dp_FFFFFF_FILL0_wght400_GRAD0_opsz48.png");
                break;

            case "ITEM_SOLD":
                // Gold stripe on the left + Gold title text

                setCardIcon("/com/example/auctionapp/icons/attach_money_100dp_FFFFFF_FILL0_wght400_GRAD0_opsz48.png");
                break;

            case "ITEM_EXPIRED":
                setCardIcon("/com/example/auctionapp/icons/hourglass_disabled_100dp_FFFFFF_FILL0_wght400_GRAD0_opsz48.png");
                break;
            default:
                setCardIcon("");
                break;
        }
    }


    private void setCardIcon(String imagePath) {
        try {
            // Note: Make sure the image paths in the switch statement map to actual images in your resources folder!
            Image newIcon = new Image(getClass().getResourceAsStream(imagePath));
            if (newIcon != null && !newIcon.isError()) {
                NotifImageview.setImage(newIcon);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load notification icon: " + imagePath);
        }
    }
}