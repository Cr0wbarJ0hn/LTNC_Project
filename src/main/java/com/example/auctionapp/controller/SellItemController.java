package com.example.auctionapp.controller;

import com.example.auctionapp.model.NetworkMessage;
import com.example.auctionapp.model.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    @FXML private Button submitButton;


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
        // 1. Grab all data
        String itemName = auctionNameField.getText();
        String itemDescription = auctionDescriptionField.getText();
        String category = categoryComboBox.getValue();
        String condition = conditionComboBox.getValue();
        java.time.LocalDate localDate = endDatePicker.getValue();
        String priceText = initialPriceField.getText();
        String incrementText = incrementField.getText();

        // 2. THE STRENGHTENED BOUNCER
        // We add checks for description, price, and increment here
        if (itemName.trim().isEmpty() ||
                itemDescription.trim().isEmpty() ||
                priceText.trim().isEmpty() ||
                incrementText.trim().isEmpty() ||
                category == null ||
                condition == null ||
                localDate == null) {

            ErrorTextField.setText("Wait! All fields must be filled out.");
            ErrorTextField.setStyle("-fx-text-fill: red;");
            return; // Stops the method right here
        }

        // Check for past dates
        if (localDate.isBefore(java.time.LocalDate.now())) {
            ErrorTextField.setText("We can't auction things in the past! Pick a new date.");
            ErrorTextField.setStyle("-fx-text-fill: red;");
            return;
        }

        if (selectedImageFile == null) {
            ErrorTextField2.setText("Please submit a picture of your item");
            return;
        }

        submitButton.setDisable(true);
        submitButton.setText("Submitting...");



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


                    Platform.runLater(() -> {

                        submitButton.setDisable(false);
                        submitButton.setText("Submit");

                        if (response.success) {
                            clearForm(); // Helper method to wipe fields
                            ErrorTextField3.setText("Successfully created your auction!");
                            ErrorTextField3.setStyle("-fx-text-fill: green;");

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
        // 1. Clear regular text fields
        auctionNameField.clear();
        auctionDescriptionField.clear();
        initialPriceField.clear();
        incrementField.clear();

        // 2. Clear ComboBoxes and RESTORE the Prompt Text
        categoryComboBox.getSelectionModel().clearSelection();
        categoryComboBox.setValue(null); // This forces the prompt text back

        conditionComboBox.getSelectionModel().clearSelection();
        conditionComboBox.setValue(null);

        // 3. Clear DatePicker text editor AND value
        // (JavaFX requires clearing the editor, otherwise the text sticks around!)
        if (endDatePicker != null) {
            endDatePicker.setValue(null);
            endDatePicker.getEditor().clear();
        }

        // 4. Clear errors and reset image
        ErrorTextField.setText("");
        ErrorTextField2.setText("");
        ErrorTextField3.setText(""); // Added this to clear the success message too!

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