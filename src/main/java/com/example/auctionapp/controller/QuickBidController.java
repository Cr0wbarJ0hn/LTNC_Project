package com.example.auctionapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class QuickBidController {
    @FXML private Label itemTitleLabel;
    @FXML private TextField bidAmountInput;

    private boolean confirmed = false;
    private double finalBidAmount = 0.0;

    public void setItemTitle(String title) {
        itemTitleLabel.setText("Bidding on: " + title);
    }

    @FXML
    public void handleConfirm() {
        try {
            finalBidAmount = Double.parseDouble(bidAmountInput.getText().trim());
            confirmed = true;
            closeModal();
        } catch (NumberFormatException e) {

            bidAmountInput.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px; -fx-border-radius: 5px;");
            bidAmountInput.clear();
            bidAmountInput.setPromptText("Enter valid numbers only");
        }
    }

    @FXML
    public void handleCancel() {
        confirmed = false;
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) bidAmountInput.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public double getFinalBidAmount() {
        return finalBidAmount;
    }
}

