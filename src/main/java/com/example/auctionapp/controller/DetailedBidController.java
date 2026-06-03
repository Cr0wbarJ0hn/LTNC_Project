package com.example.auctionapp.controller;

import com.example.auctionapp.model.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.example.auctionapp.model.NetworkMessage;
import com.example.auctionapp.model.UserSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.example.auctionapp.model.NetworkMessage;
import com.example.auctionapp.model.UserSession;
import javafx.application.Platform;
import javafx.application.Platform;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class DetailedBidController {

    // --- FXML IDs from your Scene Builder ---
    @FXML private Label DetailTimeLeft;

    private int currentAuctionId;
    private String currentUsername = UserSession.getUsername();
    @FXML private TextField manualBidInput;
    @FXML private VBox autoBidSettings; // Container for Max Bid, etc.
    @FXML private CheckBox autoBidSwitch;
    @FXML private Pane manualBidPane;
    @FXML private Pane autoBidPane;
    @FXML private Label DetailIncrement;
    @FXML private Hyperlink BackLink;
    @FXML private Label ItemName;
    @FXML private ImageView DetailImage; // Left your spelling exactly as it is in FXML!
    @FXML private Label DetailDescription;
    @FXML private Label ConditionLabel;
    @FXML private Label Sellerlabel;
    @FXML private Label DetailPrice;
    @FXML private TextField autoBidMaxInput;        // Ô nhập trần giá tối đa
    @FXML private TextField autoBidIncrementInput;  // Ô nhập increment tùy chỉnh (không bắt buộc)
    @FXML private TextField autoBidRoundsInput;     // Ô nhập số vòng tối đa (không bắt buộc)
    @FXML private Button    autoBiddingButton;      // Nút "Bắt đầu / Đang chạy"
    @FXML private Button cancelAutoBidButton;    // Nút "Hủy auto-bid"
    @FXML private Label     autoBidStatusLabel;     // Nhãn hiển thị trạng thái
    @FXML private LineChart<String, Number> priceHistoryChart;

    // Cờ theo dõi trạng thái auto-bid của session này
    private boolean autoBidActive = false;

    // This will hold the exact screen the user was looking at before clicking the item
    private javafx.scene.Node previousContent;

    // Method to receive the old screen
    public void setPreviousContent(javafx.scene.Node previousContent) {
        this.previousContent = previousContent;
    }


    // --- Method to catch the data from the Browse screen ---
    public void setItemData(int auctionId, String name, String description, String price, String condition, String seller, String imagePath, String increment, String timeLeft) {
        this.currentAuctionId = auctionId;
        ItemName.setText(name);
        DetailDescription.setText(description);
        DetailPrice.setText("$" + price);
        ConditionLabel.setText("Condition: " + condition);
        Sellerlabel.setText("Seller: " + seller);
        DetailIncrement.setText("$" + increment);
        DetailTimeLeft.setText(timeLeft);

        try {
            // --- FIXED IMAGE LOADING LOGIC ---
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                        // 1. New Cloud Storage Setup: Load directly from the Supabase web URL in the background
                        Image image = new Image(imagePath, true);
                        DetailImage.setImage(image);
                    } else if (imagePath.length() > 200 || imagePath.startsWith("/9j/")) {
                        // 2. Fallback: Old Base64 logic
                        byte[] decodedBytes = Base64.getDecoder().decode(imagePath.trim());
                        Image image = new Image(new ByteArrayInputStream(decodedBytes));
                        DetailImage.setImage(image);
                    } else {
                        // 3. Fallback: Old local file paths
                        String formattedUrl = new java.io.File(imagePath).toURI().toString();
                        Image image = new Image(formattedUrl);
                        DetailImage.setImage(image);
                    }
                } catch (Exception e) {
                    System.out.println("Card Image Error: Could not load image from target path: " + imagePath);
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("Card Image Error: Could not load image data.");
            // e.printStackTrace(); // Uncomment this if it still doesn't work to see the error
        }


        setupPlaceholderChart();
        // Cập nhật UI auto-bid theo trạng thái đang lưu trên server
        refreshAutoBidStatus();
    }

    private void setupPlaceholderChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Start", 500));
        series.getData().add(new XYChart.Data<>("Day 2", 750));
        series.getData().add(new XYChart.Data<>("Today", 1000)); // You can replace 1000 with Double.parseDouble(price) later!
        priceHistoryChart.getData().add(series);
    }


    // 1. Declare the button at the top with your other FXML variables
    @FXML private javafx.scene.control.Button backButton;

    // 2. Add the method to handle the click
    @FXML
    public void goBack() {
        try {
            // 1. Bring the right pane back
            javafx.scene.layout.Pane rightPane = (javafx.scene.layout.Pane) BackLink.getScene().lookup("#rightPane");
            if (rightPane != null) {
                rightPane.setVisible(true);
                rightPane.setManaged(true);
            }

            // 2. Put the EXACT old screen back into the center
            javafx.scene.layout.Pane dashboardCenter = (javafx.scene.layout.Pane) BackLink.getScene().lookup("#centerContentArea");

            if (dashboardCenter != null && previousContent != null) {
                dashboardCenter.getChildren().clear();
                dashboardCenter.getChildren().add(previousContent);
            } else {
                // Failsafe: If it lost the previous screen, click Home to prevent a blank screen
                javafx.scene.control.ToggleButton homeBtn = (javafx.scene.control.ToggleButton) BackLink.getScene().lookup("#HomeButton");
                if (homeBtn != null) homeBtn.fire();
            }

        } catch (Exception e) {
            System.out.println("Could not go back.");
            e.printStackTrace();
        }
    }


    @FXML
    public void handleManualBid() {
        String inputString = manualBidInput.getText().trim();
        if (inputString.isEmpty()) return;

        try {
            double amount = Double.parseDouble(inputString);

            // 1. Pack variables inside a sub-payload layout structure
            JsonObject payload = new JsonObject();
            payload.addProperty("auctionId", this.currentAuctionId); // <-- The tracking variable you fixed earlier!
            payload.addProperty("bidAmount", amount);
            payload.addProperty("username", "ActiveUser"); // Fallback fallback wrapper field

            // 2. Wrap it neatly inside your unified NetworkMessage standard
            Gson clientGson = new Gson();
            NetworkMessage messageEnvelope = new NetworkMessage("BID", clientGson.toJson(payload), true);

            // 3. Dispatch out onto your network infrastructure on a background worker thread
            new Thread(() -> {
                try {
                    UserSession.getOut().println(clientGson.toJson(messageEnvelope));
                    UserSession.getOut().flush();

                    // Listen immediately for response packet from ClientHandler.java
                    String responseText;
                    while ((responseText = UserSession.getIn().readLine()) != null) {
                        NetworkMessage serverReply = clientGson.fromJson(responseText, NetworkMessage.class);

                        if ("BID_RESPONSE".equals(serverReply.action)) {
                            Platform.runLater(() -> {
                                if (serverReply.success) {
                                    // Update price text on display instantly upon affirmation
                                    DetailPrice.setText("$" + String.format("%.2f", amount));
                                    manualBidInput.clear();
                                    manualBidInput.setPromptText("Bid Successful!");
                                    manualBidInput.setStyle("-fx-prompt-text-fill: #27ae60; -fx-border-color: #27ae60;");
                                } else {
                                    // Validation error printed into visual alerts cleanly
                                    manualBidInput.clear();
                                    manualBidInput.setPromptText(serverReply.data); // Holds error description text
                                    manualBidInput.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
                                }
                            });
                            break; // Action handled successfully
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

        } catch (NumberFormatException nfe) {
            manualBidInput.setPromptText("Enter valid number!");
            manualBidInput.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
        }
    }

    @FXML
    public void initialize() {

        autoBidPane.visibleProperty().bind(autoBidSwitch.selectedProperty());
        autoBidPane.managedProperty().bind(autoBidPane.visibleProperty());

        // Manual-bid pane is visible ONLY when switch is NOT selected
        manualBidPane.visibleProperty().bind(autoBidSwitch.selectedProperty().not());
        manualBidPane.managedProperty().bind(manualBidPane.visibleProperty());

        // 2. Add an animation (Optional but smooth)
        autoBidSwitch.selectedProperty().addListener((obs, wasAuto, isAuto) -> {

            System.out.println("Mode changed: " + (isAuto ? "AUTO" : "MANUAL"));
        });

        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);  // How round you want the left/right corners
        clip.setArcHeight(30); // How round you want the top/bottom corners
        clip.widthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> DetailImage.getLayoutBounds().getWidth(),
                DetailImage.layoutBoundsProperty()
        ));

        clip.heightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> DetailImage.getLayoutBounds().getHeight(),
                DetailImage.layoutBoundsProperty()
        ));

        // 4. Apply the cut!
        DetailImage.setClip(clip);

        // Ẩn nút Cancel khi mới vào (chỉ hiện khi auto-bid đang chạy)
        if (cancelAutoBidButton != null) cancelAutoBidButton.setVisible(false);

        // Khởi động thread lắng nghe push event từ server (AUTO_BID_UPDATE, AUTO_BID_STOPPED)
        startServerListener();
    }
    // ════════════════════════════════════════════════════════════════════════
    // XỬ LÝ AUTO-BID — PHÍA CLIENT
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Gọi khi user nhấn nút "Bắt đầu Auto-Bidding".
     * Đọc các ô nhập, gửi lệnh AUTO_BID_SET lên server, xử lý phản hồi.
     */
    @FXML
    public void handleStartAutoBid() {
        // Validate ô nhập trần giá — bắt buộc phải có
        String maxBidStr = autoBidMaxInput.getText().trim();
        if (maxBidStr.isEmpty()) {
            setBidFieldError(autoBidMaxInput, "Please enter your maximum price.");
            return;
        }
        double maxBid;
        try {
            maxBid = Double.parseDouble(maxBidStr);
        } catch (NumberFormatException e) {
            setBidFieldError(autoBidMaxInput, "Please enter a valid number.");
            return;
        }

        // Hai tham số tùy chọn — bỏ trống sẽ dùng giá trị mặc định
        double customIncrement = parseOptionalDouble(autoBidIncrementInput, 0);
        int    maxRounds       = parseOptionalInt(autoBidRoundsInput, 999);

        // Đóng gói payload gửi lên server
        JsonObject payload = new JsonObject();
        payload.addProperty("auctionId",       currentAuctionId);
        payload.addProperty("maxBid",          maxBid);
        payload.addProperty("customIncrement", customIncrement);
        payload.addProperty("maxRounds",       maxRounds);

        Gson clientGson = new Gson();
        NetworkMessage msg = new NetworkMessage("AUTO_BID_SET", clientGson.toJson(payload), true);

        // Hiển thị trạng thái "đang kích hoạt" trong lúc chờ server phản hồi
        autoBiddingButton.setDisable(true);
        autoBiddingButton.setText("Activating...");

        new Thread(() -> {
            try {
                UserSession.getOut().println(clientGson.toJson(msg));
                UserSession.getOut().flush();

                String line;
                while ((line = UserSession.getIn().readLine()) != null) {
                    NetworkMessage reply = clientGson.fromJson(line, NetworkMessage.class);

                    if ("AUTO_BID_RESPONSE".equals(reply.action)) {
                        final boolean ok   = reply.success;
                        final String  data = reply.data;
                        Platform.runLater(() -> {
                            autoBiddingButton.setDisable(false);
                            if (ok) {
                                // Kích hoạt thành công — đổi giao diện sang trạng thái active
                                autoBidActive = true;
                                autoBiddingButton.setText("Auto-Bidding is running ✓");
                                autoBiddingButton.setStyle("-fx-background-color: linear-gradient(to right, #16a34a, #15803d);");
                                if (cancelAutoBidButton != null) cancelAutoBidButton.setVisible(true);
                                setStatusLabel(data, true);
                            } else {
                                // Thất bại — hiện lỗi, giữ nguyên nút
                                autoBidActive = false;
                                autoBiddingButton.setText("Start Auto-Bidding");
                                autoBiddingButton.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #4f46e5);");
                                setStatusLabel("⚠ " + data, false);
                            }
                        });
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    autoBiddingButton.setText("Start Auto-Bidding");
                    autoBiddingButton.setDisable(false);
                    setStatusLabel("Connection error. Please try again.", false);
                });
            }
        }).start();
    }

    /**
     * Gọi khi user nhấn nút "Hủy Auto-Bid".
     * Gửi lệnh AUTO_BID_CANCEL lên server và reset giao diện về mặc định.
     */
    @FXML
    public void handleCancelAutoBid() {
        JsonObject payload = new JsonObject();
        payload.addProperty("auctionId", currentAuctionId);

        Gson clientGson = new Gson();
        NetworkMessage msg = new NetworkMessage("AUTO_BID_CANCEL", clientGson.toJson(payload), true);

        new Thread(() -> {
            try {
                UserSession.getOut().println(clientGson.toJson(msg));
                UserSession.getOut().flush();

                String line;
                while ((line = UserSession.getIn().readLine()) != null) {
                    NetworkMessage reply = clientGson.fromJson(line, NetworkMessage.class);
                    if ("AUTO_BID_CANCEL_RESPONSE".equals(reply.action)) {
                        final boolean ok   = reply.success;
                        final String  data = reply.data;
                        Platform.runLater(() -> {
                            autoBidActive = false;
                            autoBiddingButton.setText("Bắt đầu Auto-Bidding");
                            autoBiddingButton.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #4f46e5);");
                            if (cancelAutoBidButton != null) cancelAutoBidButton.setVisible(false);
                            setStatusLabel(ok ? "Auto-bid cancelled." : data, ok);
                        });
                        break;
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    /**
     * Hỏi server trạng thái auto-bid hiện tại của user này.
     * Được gọi khi: vào màn hình, hoặc khi user bật switch auto-bid.
     */
    private void refreshAutoBidStatus() {
        if (currentAuctionId == 0) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("auctionId", currentAuctionId);

        Gson clientGson = new Gson();
        NetworkMessage msg = new NetworkMessage("AUTO_BID_STATUS", clientGson.toJson(payload), true);

        new Thread(() -> {
            try {
                UserSession.getOut().println(clientGson.toJson(msg));
                UserSession.getOut().flush();

                String line;
                while ((line = UserSession.getIn().readLine()) != null) {
                    NetworkMessage reply = clientGson.fromJson(line, NetworkMessage.class);
                    if ("AUTO_BID_STATUS_RESPONSE".equals(reply.action)) {
                        com.google.gson.JsonObject status =
                                com.google.gson.JsonParser.parseString(reply.data).getAsJsonObject();
                        boolean isActive = status.has("active") && status.get("active").getAsBoolean();
                        Platform.runLater(() -> applyStatusToUI(status, isActive));
                        break;
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    /** Cập nhật toàn bộ UI auto-bid dựa trên object status trả về từ server. */
    private void applyStatusToUI(com.google.gson.JsonObject status, boolean isActive) {
        autoBidActive = isActive;
        if (autoBiddingButton == null) return;

        if (isActive) {
            double maxBid     = status.get("maxBid").getAsDouble();
            int    roundsUsed = status.get("roundsUsed").getAsInt();
            int    maxRounds  = status.get("maxRounds").getAsInt();

            autoBiddingButton.setText("Auto-Bidding đang chạy ✓");
            autoBiddingButton.setStyle("-fx-background-color: linear-gradient(to right, #16a34a, #15803d);");
            if (cancelAutoBidButton != null) cancelAutoBidButton.setVisible(true);
            setStatusLabel("Running · Max $" + String.format("%.2f", maxBid)
                    + " · Round " + roundsUsed + "/" + maxRounds, true);
        } else {
            // Không có rule đang chạy — reset về mặc định
            autoBiddingButton.setText("Start Auto-Bidding");
            autoBiddingButton.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #4f46e5);");
            if (cancelAutoBidButton != null) cancelAutoBidButton.setVisible(false);
            if (autoBidStatusLabel != null) autoBidStatusLabel.setText("");
        }
    }

    /**
     * Background thread lắng nghe push event từ server.
     * Chạy song song — tự động cập nhật giá khi có AUTO_BID_UPDATE.
     */
    private void startServerListener() {
        Thread listener = new Thread(() -> {
            try {
                String line;
                Gson listenerGson = new Gson();
                while ((line = UserSession.getIn().readLine()) != null) {
                    NetworkMessage nm = listenerGson.fromJson(line, NetworkMessage.class);
                    switch (nm.action) {
                        // Server broadcast: có auto-bid mới được đặt
                        case "AUTO_BID_UPDATE"  -> Platform.runLater(() -> handleAutoBidUpdatePush(nm.data));
                        // Server gửi riêng: auto-bid của user này đã dừng
                        case "AUTO_BID_STOPPED" -> Platform.runLater(() -> handleAutoBidStoppedPush(nm.data));
                        // Bỏ qua các action khác — sẽ được xử lý bởi thread tương ứng
                    }
                }
            } catch (Exception e) {
                // Kết nối đóng hoặc user chuyển màn hình — bình thường, không cần xử lý
            }
        }, "DetailBid-Listener");
        listener.setDaemon(true); // Tự tắt khi app tắt
        listener.start();
    }

    /**
     * Xử lý khi nhận được AUTO_BID_UPDATE từ server.
     * Cập nhật nhãn giá ngay lập tức, flash màu xanh để báo hiệu.
     */
    private void handleAutoBidUpdatePush(String data) {
        try {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(data).getAsJsonObject();
            int    auctionId = obj.get("auctionId").getAsInt();
            if (auctionId != this.currentAuctionId) return; // Không phải phiên đang xem

            double amount = obj.get("amount").getAsDouble();
            String bidder = obj.get("bidder").getAsString();

            // Cập nhật giá hiển thị
            DetailPrice.setText("$" + String.format("%.2f", amount));
            DetailPrice.setStyle("-fx-text-fill: #22c55e;"); // Màu xanh flash

            // Nếu là auto-bid của chính user này → làm mới panel trạng thái
            if (bidder.equals(currentUsername)) refreshAutoBidStatus();

            // Trả về màu tím ban đầu sau 1.5 giây
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> DetailPrice.setStyle("-fx-text-fill: #9363fc;"));
            }).start();

        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Xử lý khi nhận được AUTO_BID_STOPPED từ server.
     * Reset giao diện về trạng thái "không chạy" và hiển thị lý do dừng.
     */
    private void handleAutoBidStoppedPush(String message) {
        autoBidActive = false;
        if (autoBiddingButton != null) {
            autoBiddingButton.setText("Bắt đầu Auto-Bidding");
            autoBiddingButton.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #4f46e5);");
        }
        if (cancelAutoBidButton != null) cancelAutoBidButton.setVisible(false);
        setStatusLabel("⚠ " + message, false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPER METHODS — thêm vào cùng với block trên
    // ════════════════════════════════════════════════════════════════════════

    /** Hiển thị thông báo lỗi trực tiếp trên ô nhập. */
    private void setBidFieldError(TextField field, String message) {
        field.clear();
        field.setPromptText(message);
        field.setStyle("-fx-prompt-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
    }

    /** Cập nhật label trạng thái auto-bid với màu phù hợp (xanh = ok, đỏ = lỗi). */
    private void setStatusLabel(String text, boolean ok) {
        if (autoBidStatusLabel == null) return;
        autoBidStatusLabel.setText(text);
        autoBidStatusLabel.setStyle(ok ? "-fx-text-fill: #22c55e;" : "-fx-text-fill: #e74c3c;");
    }

    /** Đọc giá trị số thực từ TextField, trả về fallback nếu trống hoặc không hợp lệ. */
    private double parseOptionalDouble(TextField field, double fallback) {
        if (field == null || field.getText().trim().isEmpty()) return fallback;
        try { return Double.parseDouble(field.getText().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    /** Đọc giá trị số nguyên từ TextField, trả về fallback nếu trống hoặc không hợp lệ. */
    private int parseOptionalInt(TextField field, int fallback) {
        if (field == null || field.getText().trim().isEmpty()) return fallback;
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}