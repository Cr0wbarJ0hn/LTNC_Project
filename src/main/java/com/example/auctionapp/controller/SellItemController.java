package com.example.auctionapp.controller;

import com.example.auctionapp.model.NetworkMessage;
import com.example.auctionapp.model.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import java.util.Base64;
import java.nio.file.Files;


import java.time.ZoneId;

import java.io.File;
import java.io.PrintWriter;

import static java.lang.System.out;

public class SellItemController {

    // --- FXML UI Elements ---
    @FXML private Label ErrorTextField;
    @FXML private Label ErrorTextField2;
    @FXML private Label ErrorTextField3;
    @FXML private TextField auctionNameField;
    @FXML private TextField auctionDescriptionField;
    @FXML private TextField initialPriceField;
    @FXML private TextField incrementField;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> conditionComboBox;
    @FXML private ImageView uploadedImageView;
    @FXML private Region regionCamera;


    // --- Memory Variables ---
    // Remembers the file the user chose so it can be sent to the database
    private File selectedImageFile;
    // Remembers the default placeholder image so we can restore it after submitting
    private Image defaultPlaceholderImage;

    @FXML
    public void initialize() {
        conditionComboBox.getItems().addAll(
                "New",
                "Like New",
                "Used"
        );
        categoryComboBox.getItems().addAll(
                "Electronics",
                "Vehicles",
                "Furniture",
                "Fashion",
                "Books"
        );

        // Memorize the placeholder image the moment the screen loads!
        if (uploadedImageView != null) {
            defaultPlaceholderImage = uploadedImageView.getImage();
        }
    }

    private void updatePlaceholderVisibility(boolean hasImage) {
        if (hasImage) {
            // Hide and collapse the space
            regionCamera.setVisible(false);
            regionCamera.setManaged(false);

            // Show the image
            uploadedImageView.setVisible(true);
            uploadedImageView.setManaged(true);
        } else {
            // Show the placeholder and reclaim the space
            regionCamera.setVisible(true);
            regionCamera.setManaged(true);

            // Hide the empty image view
            uploadedImageView.setVisible(false);
            uploadedImageView.setManaged(false);
        }
    }

    @FXML
    public void submitAuction() {
        // 1. Grab all data from the UI (Must be done on the UI thread)
        String itemName = auctionNameField.getText();
        String itemDescription = auctionDescriptionField.getText();
        String category = categoryComboBox.getValue();
        String condition = conditionComboBox.getValue();
        java.time.LocalDate localDate = endDatePicker.getValue();
        String priceText = initialPriceField.getText();
        String incrementText = incrementField.getText();

        // 2. THE BOUNCER: Validation (Stay on UI thread for this)
        if (itemName.isEmpty() || category == null || condition == null || localDate == null || localDate.isBefore(java.time.LocalDate.now())) {
            ErrorTextField.setText("Please fill out all fields and select a valid date!");
            ErrorTextField.setStyle("-fx-text-fill: red;");
            return;
        }

        if (selectedImageFile == null) {
            ErrorTextField2.setText("Please submit a picture of your item");
            ErrorTextField2.setStyle("-fx-text-fill: red;");
            return;
        }

        // 3. START BACKGROUND THREAD (Network work starts here)
        new Thread(() -> {
            try {
                // Parse numbers
                double initialPrice = Double.parseDouble(priceText);
                double priceIncrement = Double.parseDouble(incrementText);

                // Convert date to long timestamp
                long timeInMillis = localDate.atTime(23, 59, 59)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();

                // --- NEW CODE STARTS HERE ---
                // Convert the image file to a Base64 String before sending
                String imageString = "";
                if (selectedImageFile != null) {
                    byte[] fileContent = Files.readAllBytes(selectedImageFile.toPath());
                    imageString = Base64.getEncoder().encodeToString(fileContent);
                }
                // --- NEW CODE ENDS HERE ---

                // 4. CREATE JSON DATA (Clean & Professional)
                Gson gson = new Gson();
                JsonObject auctionReq = new JsonObject();
                auctionReq.addProperty("action", "SUBMIT_AUCTION");
                auctionReq.addProperty("itemName", itemName);
                auctionReq.addProperty("itemType", category);
                auctionReq.addProperty("itemCondition", condition);
                auctionReq.addProperty("description", itemDescription);

                // --- CHANGED THIS LINE ---
                // We now send the giant Base64 string instead of the local computer path
                auctionReq.addProperty("imagePath", imageString);

                auctionReq.addProperty("seller", UserSession.getUsername());
                auctionReq.addProperty("price", initialPrice);
                auctionReq.addProperty("increment", priceIncrement);
                auctionReq.addProperty("endTime", timeInMillis);

                // 5. SEND TO SERVER
                PrintWriter out = UserSession.getOut();
                java.io.BufferedReader in = UserSession.getIn();

                if (out != null && in != null) {
                    out.println(gson.toJson(auctionReq));
                    out.flush();

                    // Wait for JSON response
                    String responseStr = in.readLine();
                    NetworkMessage response = gson.fromJson(responseStr, NetworkMessage.class);

                    // 6. UPDATE UI (Must jump back to UI thread)
                    Platform.runLater(() -> {
                        if (response.success) {
                            ErrorTextField3.setText("Successfully created your auction!");
                            ErrorTextField3.setStyle("-fx-text-fill: green;");
                            clearForm(); // Helper method to wipe fields
                        } else {
                            ErrorTextField.setText("Server Error: " + response.data);
                            ErrorTextField.setStyle("-fx-text-fill: red;");
                        }
                    });
                }

            } catch (NumberFormatException e) {
                Platform.runLater(() -> {
                    ErrorTextField.setText("Please enter valid numbers for the prices!");
                    ErrorTextField.setStyle("-fx-text-fill: red;");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ErrorTextField.setText("Network error: Could not reach server.");
                    ErrorTextField.setStyle("-fx-text-fill: red;");
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Helper method to keep your code clean
    private void clearForm() {
        auctionNameField.clear();
        auctionDescriptionField.clear();
        categoryComboBox.setValue(null);
        conditionComboBox.setValue(null);
        initialPriceField.clear();
        incrementField.clear();
        endDatePicker.setValue(null);
        ErrorTextField.setText("");
        ErrorTextField2.setText("");
        selectedImageFile = null;
        if (uploadedImageView != null) {
            uploadedImageView.setImage(defaultPlaceholderImage);
        }
    }

    @FXML
    public void uploadImage() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an Image for your Auction");

        // 2. Set filters so they can ONLY pick image files
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        // 3. Show the open file dialog
        selectedImageFile = fileChooser.showOpenDialog(null);

        // 4. If they actually picked a file (and didn't click Cancel)
        if (selectedImageFile != null) {
            updatePlaceholderVisibility(true);
            try {
                // Convert the file path into a JavaFX Image
                String imagePath = selectedImageFile.toURI().toString();
                Image newImage = new Image(imagePath);

                // Display it in your ImageView!
                uploadedImageView.setImage(newImage);

            } catch (Exception e) {
                out.println("Error loading image");
                e.printStackTrace();
            }
        } else {
            out.println("User canceled image selection.");
            updatePlaceholderVisibility(false);
        }
    }
}