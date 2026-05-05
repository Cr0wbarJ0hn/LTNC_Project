package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {


    @FXML private TextField txtUsername;
    @FXML private PasswordField pwPassword;
    @FXML private Button btnLogin;


    @FXML
    void handleLogin(ActionEvent event) {

        String user = txtUsername.getText();
        String pass = pwPassword.getText();

        if (user.equals("admin") && pass.equals("123")) {

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Đăng nhập thành công!");

            alert.showAndWait();

        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Đăng nhập thất bại");
            alert.setContentText("Sai tài khoản hoặc mật khẩu!");
            alert.showAndWait();
        }
    }

    @FXML
    void handleRegisterNavigation(MouseEvent event) {
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/client/fxml/register.fxml"));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(registerRoot));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleForgotPassword(MouseEvent event) {
        System.out.println("Chuyển hướng sang trang khôi phục mật khẩu...");
    }
}