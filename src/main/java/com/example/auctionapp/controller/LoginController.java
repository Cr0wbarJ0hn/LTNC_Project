package com.example.auctionapp.controller;

import com.example.auctionapp.model.NetworkMessage; // Ensure this import matches where you saved NetworkMessage!
import com.example.auctionapp.model.UserSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
        String user = usernameField.getText();
        String pass = passwordField.getText();

        // We MUST capture the window (Stage) on the main JavaFX thread before we go to the background!
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        // Give the user immediate feedback so they know the button worked
        messageLabel.setText("Connecting to server...");
        messageLabel.setStyle("-fx-text-fill: blue;");

        // 1. START A BACKGROUND THREAD (No more frozen UI!)
        new Thread(() -> {
            try {
                // 2. Pick up the phone and call Door 5000
                Socket socket = new Socket("localhost", 5000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                UserSession.setOut(out);
                UserSession.setIn(in);
                UserSession.setUsername(user);

                // 3. Create our JSON Request using Gson
                Gson gson = new Gson();
                JsonObject loginRequest = new JsonObject();
                loginRequest.addProperty("action", "LOGIN");
                loginRequest.addProperty("username", user);
                loginRequest.addProperty("password", pass);

                // Send the JSON to the server
                out.println(gson.toJson(loginRequest));
                out.flush();

                // 4. Wait for the Server's JSON reply
                String serverResponse = in.readLine();
                System.out.println("CLIENT DEBUG: Server says: " + serverResponse);

                // Decode the JSON back into a Java Object
                NetworkMessage response = gson.fromJson(serverResponse, NetworkMessage.class);

                // 5. SAFELY JUMP BACK TO THE JAVAFX UI THREAD TO UPDATE THE SCREEN
                Platform.runLater(() -> {
                    // We check the "success" boolean we set up in our NetworkMessage class
                    if (response.success) {
                        messageLabel.setText("Login Successful! Loading auction...");
                        messageLabel.setStyle("-fx-text-fill: green;");

                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/Dashboard.fxml"));
                            Parent root = loader.load();

                            // Use the stage we captured earlier!
                            Scene scene = new Scene(root, 1870, 1200);
                            stage.setScene(scene);
                            stage.setTitle("UET Auction House - Dashboard");
                            stage.centerOnScreen();
                            stage.show();

                        } catch (IOException e) {
                            System.out.println("Crash! Could not find Dashboard.fxml");
                            e.printStackTrace();
                        }
                    } else {
                        // The server told us the password was wrong
                        messageLabel.setText("Login Failed: " + response.data);
                        messageLabel.setStyle("-fx-text-fill: red;");
                    }
                });

            } catch (Exception e) {
                // If the server is offline, this catches the crash and safely tells the UI
                Platform.runLater(() -> {
                    messageLabel.setText("Error: Could not connect to the server.");
                    messageLabel.setStyle("-fx-text-fill: red;");
                });
                e.printStackTrace();
            }
        }).start(); // Start the background thread!
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