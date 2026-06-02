package com.example.auctionapp.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class AdminDashboardController {

    @FXML private StackPane centerContentArea;
    @FXML private ToggleButton UserManagementButton;
    @FXML private ToggleButton AuctionsManagementButton;


    @FXML
    public void initialize() {
        // Set default view on load
        loadView("users");

        // Handle Navigation Clicks
        UserManagementButton.setOnAction(e -> loadView("users"));
        AuctionsManagementButton.setOnAction(e -> loadView("auctions"));

    }

    private void loadView(String type) {
        try {
            // 🌟 Determine both the FXML path AND the controller dynamically
            String fxmlPath = switch (type) {
                case "users" -> "/com/example/auctionapp/adminUserView.fxml";
                case "auctions" -> "/com/example/auctionapp/adminAuctionView.fxml";
                default -> null;
            };

            if (fxmlPath == null) return;

            Object controller = switch (type) {
                case "users" -> new AdminUserManagementController();
                case "auctions" -> new AdminAuctionManagementController();
                default -> null;
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setController(controller);
            Parent view = loader.load();

            centerContentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}