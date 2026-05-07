package auction.view;

import auction.client.BidHistoryClient;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import auction.model.Auction;
import auction.model.BidHistory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * ✅ UPDATED: Fetch data from REST API + WebSocket
 */
public class BidHistoryChartController {

    @FXML
    private VBox chartContainer;

    private LineChart<Number, Number> lineChart;
    private XYChart.Series<Number, Number> dataSeries;
    private volatile Auction currentAuction;
    private Timeline updateTimeline;
    private volatile LocalDateTime auctionStart;
    private final BidHistoryClient bidClient = new BidHistoryClient();

    @FXML
    public void initialize() {
        initializeChart();
        startRealtimeUpdate();
    }

    /**
     * Khởi tạo biểu đồ
     */
    private void initializeChart() {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Thời gian (giây)");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Giá (VNĐ)");

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Lịch sử Bid Realtime");
        lineChart.setStyle("-fx-font-size: 12px;");

        dataSeries = new XYChart.Series<>();
        dataSeries.setName("Giá theo thời gian");
        lineChart.getData().add(dataSeries);

        VBox.setVgrow(lineChart, javafx.scene.layout.Priority.ALWAYS);
        chartContainer.getChildren().add(lineChart);
        chartContainer.setPadding(new Insets(10));
    }

    /**
     * ✅ SET AUCTION (có thể từ local hoặc server)
     */
    public synchronized void setAuction(Auction auction) {
        this.currentAuction = auction;
        this.auctionStart = LocalDateTime.now();

        // Fetch từ server nếu có
        if (auction != null) {
            new Thread(() -> {
                try {
                    List<BidHistoryClient.BidUpdate> serverBids = bidClient.getBidHistory(auction.getId());
                    Platform.runLater(() -> displayBidHistory(serverBids));
                } catch (Exception e) {
                    System.err.println("Error fetching from server: " + e.getMessage());
                    loadHistoricalDataLocal();
                }
            }).start();
        }
    }

    /**
     * Load dữ liệu từ local memory
     */
    private void loadHistoricalDataLocal() {
        Auction auction = currentAuction;
        if (auction == null) return;

        BidHistory history = auction.getBidHistory();
        List<BidHistory.BidRecord> records = history.getAllRecords();
        LocalDateTime start = auctionStart;

        Platform.runLater(() -> {
            dataSeries.getData().clear();

            for (BidHistory.BidRecord record : records) {
                long secondsElapsed = ChronoUnit.SECONDS.between(
                        start, record.getTimestamp()
                );
                dataSeries.getData().add(
                        new XYChart.Data<>(secondsElapsed, record.getPrice())
                );
            }
        });
    }

    /**
     * ✅ Display bid history từ server
     */
    private void displayBidHistory(List<BidHistoryClient.BidUpdate> bids) {
        Platform.runLater(() -> {
            dataSeries.getData().clear();

            for (BidHistoryClient.BidUpdate bid : bids) {
                long secondsElapsed = ChronoUnit.SECONDS.between(
                        auctionStart, LocalDateTime.parse(bid.timestamp)
                );
                dataSeries.getData().add(
                        new XYChart.Data<>(secondsElapsed, bid.price)
                );
            }
        });
    }

    /**
     * Cập nhật realtime từ server
     */
    private void startRealtimeUpdate() {
        updateTimeline = new Timeline(new KeyFrame(
                Duration.seconds(2),
                event -> updateChart()
        ));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }

    /**
     * Update chart data
     */
    private void updateChart() {
        if (currentAuction == null) return;

        Platform.runLater(() -> {
            new Thread(() -> {
                try {
                    List<BidHistoryClient.BidUpdate> bids = bidClient.getBidHistory(currentAuction.getId());
                    Platform.runLater(() -> displayBidHistory(bids));
                } catch (Exception e) {
                    loadHistoricalDataLocal();
                }
            }).start();
        });
    }

    public void stop() {
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
        currentAuction = null;
    }

    public synchronized Auction getCurrentAuction() {
        return currentAuction;
    }
}