package com.auction.client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainClient extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/client/fxml/login.fxml"));

        primaryStage.setTitle("AuctionHub - Đăng Nhập");
        primaryStage.setScene(new Scene(root));
        primaryStage.centerOnScreen();

        // Phóng to toàn màn hình ngay khi chạy
//        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

