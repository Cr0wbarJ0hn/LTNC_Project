package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

    // --- Các thành phần giao diện (fx:id) ---
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private HBox filterBar;
    @FXML private TitledPane auctionGrid; // Trong FXML của bạn là TitledPane
    @FXML private VBox upcomingBox;
    @FXML private Label liveCountLabel;
    @FXML private Label statLive;
    @FXML private Label statSoon;
    @FXML private Label statTotal;
    @FXML private Label statBids;
    @FXML private Label tickerLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userAvatarLabel;
    @FXML private Label userRoleLabel;
    @FXML private ScrollPane mainScroll;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        System.out.println("Auction List Controller đã sẵn sàng!");

        if (sortCombo != null) {
            sortCombo.getItems().addAll("Mới nhất", "Giá thấp đến cao", "Giá cao đến thấp");
        }
    }


    @FXML
    private void filterAll(ActionEvent event) {
        System.out.println("Đang lọc: Tất cả phiên");
    }

    @FXML
    private void filterElectronics(ActionEvent event) {
        System.out.println("Đang lọc: Điện tử");
    }

    @FXML
    private void filterArt(ActionEvent event) {
        System.out.println("Đang lọc: Nghệ thuật");
    }

    @FXML
    private void filterVehicle(ActionEvent event) {
        System.out.println("Đang lọc: Xe cộ");
    }

    @FXML
    private void filterLive(ActionEvent event) {
        System.out.println("Đang lọc: Đang diễn ra");
    }

    @FXML
    private void filterEnded(ActionEvent event) {
        System.out.println("Đang lọc: Đã kết thúc");
    }
}

