package com.example.auctionapp.controller;

import com.example.auctionapp.model.*;
import com.example.auctionapp.network.NetworkRouter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    public void handleSignIn(ActionEvent event) {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            messageLabel.setText("Please enter a username and password.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Capture the active window Stage before stepping out of the JavaFX Application Thread
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        messageLabel.setText("Connecting to server...");
        messageLabel.setStyle("-fx-text-fill: blue;");

        // 1. Fire up the background thread to handle socket overhead
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 5000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 2. Prepare and emit our standard JSON login payload packet
                Gson gson = new Gson();
                JsonObject loginRequest = new JsonObject();
                loginRequest.addProperty("action", "LOGIN");
                loginRequest.addProperty("username", user);
                loginRequest.addProperty("password", pass);

                out.println(gson.toJson(loginRequest));
                out.flush();

                // 3. Receive response line from the server
                String serverResponse = in.readLine();
                System.out.println("CLIENT DEBUG: Server says: " + serverResponse);

                if (serverResponse == null) {
                    Platform.runLater(() -> {
                        messageLabel.setText("Error: Server disconnected unexpectedly.");
                        messageLabel.setStyle("-fx-text-fill: red;");
                    });
                    return; // Stop the thread here so Gson doesn't crash!
                }

                // 🌟 FIX: Parse as a generic JsonObject instead of NetworkMessage to extract dynamic fields
                JsonObject responseJson = JsonParser.parseString(serverResponse).getAsJsonObject();
                boolean isSuccess = responseJson.has("success") && responseJson.get("success").getAsBoolean();

                if (isSuccess) {
                    // 4. Extract out our newly implemented authorization token parameter
                    String assignedRole = responseJson.get("role").getAsString();

                    // 5. Polymorphic Object Creation! Initialize the proper model instance
                    User loggedInUser;
                    if ("ADMIN".equalsIgnoreCase(assignedRole)) {
                        loggedInUser = new Admin("System Admin", "N/A", "admin@uet.edu.vn", user, "");
                    } else {
                        loggedInUser = new Member("Standard User", "N/A", "user@uet.edu.vn", user, "","N/A");
                    }

                    // 6. Bind the initialized session object tracking references globally
                    UserSession.setSession(loggedInUser, out);
                    UserSession.setIn(in); // Save reader pipe to global session tracking just in case

                    // Start up your router background listening loop worker
                    NetworkRouter.startGlobalListener();

                    // 7. SAFELY STEP BACK UNTO JAVAFX MAIN THREAD TO MANIPULATE THE VIEWS
                    Platform.runLater(() -> {
                        messageLabel.setText("Login Successful! Loading workspace...");
                        messageLabel.setStyle("-fx-text-fill: green;");

                        try {
                            // ⚙️ GATES OF ROUTING: Select target layout path based on polymorphic class checks
                            String fxmlPath;
                            String windowTitle;

                            if (UserSession.isAdmin()) {
                                fxmlPath = "/com/example/auctionapp/AdminDashboard.fxml";
                                windowTitle = "UET Auction House - Administrative Control Console";
                            } else {
                                // Points to your primary marketplace interface page
                                fxmlPath = "/com/example/auctionapp/Dashboard.fxml";
                                windowTitle = "UET Auction House - Marketplace";
                            }

                            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                            Parent root = loader.load();

                            Scene scene = new Scene(root, 1900, 1200);
                            stage.setScene(scene);
                            stage.setTitle(windowTitle);
                            stage.centerOnScreen();
                            stage.show();

                        } catch (IOException e) {
                            System.err.println("Critical Failure: Could not load next view FXML file layout page!");
                            e.printStackTrace();
                        }
                    });

                } else {
                    // Login failed response from database manager authentication logic
                    String errorMsg = responseJson.has("message") ? responseJson.get("message").getAsString() : "Incorrect credentials.";
                    Platform.runLater(() -> {
                        messageLabel.setText("Login Failed: " + errorMsg);
                        messageLabel.setStyle("-fx-text-fill: red;");
                    });
                }

            } catch (Exception e) {
                // Handles system offline or broken network loop states gracefully
                Platform.runLater(() -> {
                    messageLabel.setText("Error: Server appears to be offline.");
                    messageLabel.setStyle("-fx-text-fill: red;");
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleSignUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/SignUp.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1300, 875);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.out.println("Crash! Could not find SignUp.fxml");
            e.printStackTrace();
        }
    }
}