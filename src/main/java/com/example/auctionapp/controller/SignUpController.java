package com.example.auctionapp.controller;

import com.example.auctionapp.model.NetworkMessage; // Make sure this matches your package
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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

public class SignUpController {
    @FXML private Label messageLabel3;
    @FXML private Label messageLabel2;
    @FXML private Label messageLabel;
    @FXML private TextField signupFullname;
    @FXML private TextField signupPhonenumber;
    @FXML private TextField signupEmail;
    @FXML private TextField signupUserName;
    @FXML private PasswordField signupPassword;

    @FXML
    public void handleRegister(ActionEvent event) {
        String user = signupUserName.getText();
        String email = signupEmail.getText();
        String pass = signupPassword.getText();
        String name = signupFullname.getText();
        String phone = signupPhonenumber.getText();

        // 1. Reset all error labels and red borders before trying again
        messageLabel.setText("");
        messageLabel2.setText("");
        messageLabel3.setText("");
        signupEmail.setStyle("");
        signupUserName.setStyle("");

        // Basic check to make sure they didn't leave boxes blank
        if (user.isEmpty() || email.isEmpty() || pass.isEmpty() || name.isEmpty() || phone.isEmpty()) {
            messageLabel2.setText("Please fill in all fields.");
            messageLabel2.setStyle("-fx-text-fill: red;");
            return; // Stop running the code right here
        }

        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        if (!email.matches(emailPattern)) {
            messageLabel.setText("Invalid email format");
            signupEmail.setStyle("-fx-border-color: red;");
            messageLabel.setStyle("-fx-text-fill: red;");
            return; // STOP HERE! Don't even call the server.
        }

        // Give immediate visual feedback that the app is thinking
        messageLabel2.setText("Connecting to server...");
        messageLabel2.setStyle("-fx-text-fill: blue;");

        // 2. START BACKGROUND THREAD
        new Thread(() -> {
            try {
                // Dial the Server
                Socket socket = new Socket("localhost", 5000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Create JSON Request
                Gson gson = new Gson();
                JsonObject regRequest = new JsonObject();
                regRequest.addProperty("action", "REGISTER");
                regRequest.addProperty("username", user);
                regRequest.addProperty("password", pass);
                regRequest.addProperty("email", email);

                // Send the JSON to the server
                out.println(gson.toJson(regRequest));
                out.flush();

                // Wait for the JSON response
                String serverResponse = in.readLine();
                System.out.println("CLIENT DEBUG: Server says: " + serverResponse);

                // Convert the response back to our NetworkMessage object
                NetworkMessage response = gson.fromJson(serverResponse, NetworkMessage.class);

                // 3. SAFELY JUMP BACK TO UI THREAD TO SHOW RESULTS
                Platform.runLater(() -> {
                    if (response.success) {
                        messageLabel2.setText("Account created! You can now log in.");
                        messageLabel2.setStyle("-fx-text-fill: green;");

                        // Clear the boxes so it looks nice
                        signupFullname.clear();
                        signupPhonenumber.clear();
                        signupUserName.clear();
                        signupEmail.clear();
                        signupPassword.clear();
                    } else {
                        // In the new server code, the exact error (e.g. "Email already taken.") is stored in response.data
                        String errorMessage = response.data;

                        if (errorMessage.contains("Email")) {
                            messageLabel.setText(errorMessage);
                            signupEmail.setStyle("-fx-border-color: red;");
                            messageLabel.setStyle("-fx-text-fill: red;");
                        } else if (errorMessage.contains("Username")) {
                            messageLabel3.setText(errorMessage);
                            signupUserName.setStyle("-fx-border-color: red;");
                            messageLabel3.setStyle("-fx-text-fill: red;");
                        } else {
                            messageLabel2.setText("Error: " + errorMessage);
                            messageLabel2.setStyle("-fx-text-fill: red;");
                        }
                    }
                });

            } catch (Exception e) {
                // Safely update UI if the server is totally offline
                Platform.runLater(() -> {
                    messageLabel2.setText("Error: Could not connect to the server.");
                    messageLabel2.setStyle("-fx-text-fill: red;");
                });
                e.printStackTrace();
            }
        }).start(); // Don't forget to start the thread!
    }

    @FXML
    public void handleSignupBack(ActionEvent event) {
        try {
            // Find your original login screen FXML file
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/hello-view.fxml"));
            Parent root = fxmlLoader.load();

            // Find the current window
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Swap the screen back to the login view
            Scene scene = new Scene(root, 1300, 875);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.out.println("Crash! Could not find the login screen.");
            e.printStackTrace();
        }
    }
}