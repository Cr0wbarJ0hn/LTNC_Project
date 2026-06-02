package com.example.auctionapp.controller;

import com.example.auctionapp.model.Admin;
import com.example.auctionapp.model.AdminAuctionRow;
import com.example.auctionapp.model.User;
import com.example.auctionapp.model.UserSession;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.PrintWriter;
import java.util.List;

public class AdminUserManagementController {

    private static AdminUserManagementController instance;

    @FXML private TableView<User> dataTable;
    @FXML private TextField searchField;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;


    private final ObservableList<User> userList = FXCollections.observableArrayList();

    public static AdminUserManagementController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;

        // 1. Setup Columns
        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        dataTable.getColumns().addAll(emailCol, userCol, roleCol);


        FilteredList<User> filteredData = new FilteredList<>(userList, b -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                // If search text is empty, display all users
                if (newValue == null || newValue.isEmpty()) return true;

                String lowerCaseFilter = newValue.toLowerCase();

                // Check if username or email matches the search box
                if (user.getUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                else if (user.getEmail().toLowerCase().contains(lowerCaseFilter)) return true;
                else return false; // Does not match
            });
        });

        // Wrap the FilteredList in a SortedList to allow column clicking to sort A-Z
        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(dataTable.comparatorProperty());
        dataTable.setItems(sortedData);

        // 🌟 3. SETUP THE REFRESH BUTTON
        refreshButton.setOnAction(event -> {
            System.out.println("🔄 Refreshing data...");
            userList.clear(); // Empty the table
            loadUserData();   // Ask the server for fresh data
        });

        deleteButton.setOnAction(event -> {
            User selectedUser = dataTable.getSelectionModel().getSelectedItem();

            if (selectedUser == null) {
                System.out.println("⚠️ No user selected to delete!");
                return;
            }

            if (selectedUser.getUsername().equals(UserSession.getCurrentUser().getUsername())) {
                System.out.println("❌ You cannot delete your own active admin account!");
                return;
            }
            if (UserSession.getCurrentUser() instanceof Admin) {
                // Downcast from generic 'User' to specific 'Admin' subclass
                Admin currentAdmin = (Admin) UserSession.getCurrentUser();

                // Let the object do the heavy lifting!
                currentAdmin.deleteUserAccount(selectedUser.getUsername());
            } else {
                System.out.println("🚨 System Refusal: Non-admin instance detected in session cache.");
            }
        });
        // 5. Initial Data Load
        loadUserData();
    }

    private void loadUserData() {
        PrintWriter out = UserSession.getOut();
        if (out != null) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "ADMIN_FETCH_USERS");
            out.println(request.toString());
            out.flush();
        }
    }

    public void populateTable(List<User> freshUsers) {
        Platform.runLater(() -> {
            userList.setAll(freshUsers);
        });
    }
}