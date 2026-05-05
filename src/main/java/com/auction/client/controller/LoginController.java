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

    // @FXML kết nối biến trong Java với các Control có fx:id tương ứng trong file .fxml
    @FXML private TextField txtUsername;    // Ô nhập tên đăng nhập
    @FXML private PasswordField pwPassword; // Ô nhập mật khẩu (ẩn ký tự)
    @FXML private Button btnLogin;          // Nút bấm đăng nhập

    // Phương thức xử lý khi người dùng nhấn nút Đăng nhập (thường gán vào onAction trong FXML)
    @FXML
    void handleLogin(ActionEvent event) {
        // Lấy chuỗi văn bản người dùng đã nhập vào các ô text
        String user = txtUsername.getText();
        String pass = pwPassword.getText();

        // Kiểm tra logic đăng nhập (đang dùng dữ liệu mẫu "admin" / "123")
        if (user.equals("admin") && pass.equals("123")) {

            // Khởi tạo một hộp thoại thông báo loại INFORMATION (thông tin)
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");        // Tiêu đề cửa sổ thông báo
            alert.setHeaderText(null);          // Phần đầu thông báo (để null nếu không cần)
            alert.setContentText("Đăng nhập thành công!"); // Nội dung chính

            // Hiển thị hộp thoại và dừng chương trình cho đến khi người dùng nhấn OK
            alert.showAndWait();

            // LƯU Ý: Thường sau bước này bạn sẽ viết code chuyển sang màn hình chính (Dashboard)
        } else {
            // Khởi tạo một hộp thoại thông báo loại ERROR (lỗi)
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Đăng nhập thất bại");
            alert.setContentText("Sai tài khoản hoặc mật khẩu!");
            alert.showAndWait();
        }
    }

    // Phương thức xử lý chuyển sang trang Đăng ký khi nhấn vào một Label hoặc Hyperlink
    @FXML
    void handleRegisterNavigation(MouseEvent event) {
        try {
            // 1. Dùng FXMLLoader để nạp tệp giao diện đăng ký từ thư mục resources
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/client/fxml/register.fxml"));

            // 2. Xác định Stage (cửa sổ) hiện tại từ đối tượng gây ra sự kiện (event source)
            // Ép kiểu từ Source -> Node -> Scene -> Window (Stage)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Đặt Scene mới (chứa giao diện đăng ký) lên Stage hiện tại
            stage.setScene(new Scene(registerRoot));

            // 4. Hiển thị thay đổi lên màn hình
            stage.show();
        } catch (IOException e) {
            // In ra lỗi nếu không tìm thấy file .fxml hoặc lỗi nạp file
            e.printStackTrace();
        }
    }

    // Phương thức xử lý khi người dùng nhấn "Quên mật khẩu"
    @FXML
    void handleForgotPassword(MouseEvent event) {
        // Hiện tại mới chỉ in ra dòng chữ ở cửa sổ Console để kiểm tra nút có hoạt động không
        System.out.println("Chuyển hướng sang trang khôi phục mật khẩu...");
    }
}