package com.auction.client.controller;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtUserName;
    @FXML private TextField txtEmail;
    @FXML private PasswordField pwPassword;
    @FXML private Label lblLogin;
    @FXML private ToggleGroup groupRole; // Nếu bạn dùng ToggleGroup cho Người mua/Người bán

    // Hàm xử lý khi nhấn nút "Tạo tài khoản"
    @FXML
    public void handleRegister(ActionEvent event) {
        String fullName = txtFullName.getText();
        String username = txtUserName.getText();
        String email = txtEmail.getText();
        String password = pwPassword.getText();



        // Sau khi thực hiện logic đăng ký xong:
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chúc mừng");
        alert.setHeaderText(null);
        alert.setContentText("Đăng ký tài khoản thành công!");

        alert.showAndWait(); // Hiện thông báo và đợi người dùng nhấn OK

        // Thường đăng ký xong sẽ tự chuyển về màn hình đăng nhập
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/client/fxml/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loginView));

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Hàm chuyển về màn hình Đăng nhập (Khi click vào Label lblLogin)
    @FXML
    public void handleLoginNavigation(MouseEvent event) throws IOException {
        try {
            // Load file login.fxml
            Parent root = FXMLLoader.load(getClass().getResource("/client/fxml/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            //stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}


