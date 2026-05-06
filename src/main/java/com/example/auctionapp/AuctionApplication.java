package com.example.auctionapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;

public class AuctionApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AuctionApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1300, 875);

        // --- NEW: Add the Application Icon ---
        try {
            // We put "/app_icon.png" here. Make sure your image file is named exactly this
            // and is sitting in your src/main/resources folder!
            Image icon = new Image(getClass().getResourceAsStream("/com/example/auctionapp/Gemini_Generated_Image_x5bve4x5bve4x5bv-Photoroom.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            // If the image is STILL missing, this catch block prevents the app from crashing.
            // It will just print this warning and open the app anyway!
        }
        // ------------------------------------

        stage.setTitle("BlockChain Auction House");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}