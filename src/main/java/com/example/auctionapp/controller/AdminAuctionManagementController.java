package com.example.auctionapp.controller;

import com.example.auctionapp.model.Admin;
import com.example.auctionapp.model.AdminAuctionRow;
import com.example.auctionapp.model.UserSession;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.PrintWriter;

public class AdminAuctionManagementController {

    private static AdminAuctionManagementController instance;

    @FXML private TableView<AdminAuctionRow> auctionTable;
    @FXML private TextField searchField;
    @FXML private Button deleteAuctionButton;

    private final ObservableList<AdminAuctionRow> auctionList = FXCollections.observableArrayList();

    public static AdminAuctionManagementController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;

        // 1. Configure Layout Columns
        TableColumn<AdminAuctionRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);

        TableColumn<AdminAuctionRow, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        nameCol.setPrefWidth(250);

        TableColumn<AdminAuctionRow, Double> priceCol = new TableColumn<>("Current Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        priceCol.setPrefWidth(120);

        TableColumn<AdminAuctionRow, String> sellerCol = new TableColumn<>("Seller");
        sellerCol.setCellValueFactory(new PropertyValueFactory<>("seller"));
        sellerCol.setPrefWidth(150);

        auctionTable.getColumns().addAll(idCol, nameCol, priceCol, sellerCol);

        // 2. Wire Up Real-time Filtering Search Bar
        FilteredList<AdminAuctionRow> filteredData = new FilteredList<>(auctionList, b -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(row -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String query = newVal.toLowerCase();
                return row.getItemName().toLowerCase().contains(query) ||
                        row.getSeller().toLowerCase().contains(query);
            });
        });
        auctionTable.setItems(filteredData);

        // 3. Setup The Destructive Delete Cascade Action
        deleteAuctionButton.setOnAction(event -> {
            AdminAuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                System.out.println("⚠️ Please highlight an auction row first!");
                return;
            }

            // Optional: Add a confirmation dialog box for extra safety!
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure? This will permanently delete the auction and wipe out all placed bids!",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Execute Cascading Hard Delete?");
            confirm.showAndWait();

            if (confirm.getResult() == ButtonType.YES) {
                if (UserSession.getCurrentUser() instanceof Admin currentAdmin) {
                    currentAdmin.deleteAuctionFromSystem(selected.getId());
                }
            }
        });

        requestAuctionInventory();
    }
    private void requestAuctionInventory() {
        PrintWriter out = UserSession.getOut();
        if (out != null) {
            JsonObject req = new JsonObject();
            req.addProperty("action", "ADMIN_FETCH_ALL_AUCTIONS");
            out.println(req.toString());
            out.flush();
        }
    }

    public void removeAuctionFromUI(int auctionId) {
        Platform.runLater(() -> {
            auctionList.removeIf(row -> row.getId() == auctionId);
        });
    }

    // 🌟 Added Getter Method to resolve NetworkRouter compilation errors
    public ObservableList<AdminAuctionRow> getAuctionList() {
        return this.auctionList;
    }
}