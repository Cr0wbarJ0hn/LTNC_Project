package com.example.auctionapp.controller; // Matches your FXML package path exactly

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import com.google.gson.JsonObject;
import javafx.util.Duration;

import java.util.List;

public class NotifController {

    @FXML private Label greetingLabel;
    @FXML private ScrollPane recentScrollPane;
    @FXML private VBox recentItemsContainer; // Points directly to your scrolling layout target
    @FXML private VBox emptyRecentPane;      // Points to your placeholder empty state container

    // A static singleton instance reference hook allowing our client socket background reader thread
    // to dynamically punch incoming alerts straight onto this screen live.
    private static NotifController instance;

    @FXML
    public void initialize() {
        showLoadingSkeletons(4);
        instance = this;
        greetingLabel.setText("See all updates about your auctions");

        // Construct a notification request packet matching your server architecture
        JsonObject requestPacket = new JsonObject();
        requestPacket.addProperty("action", "FETCH_NOTIF_HISTORY");
        // Make sure you have a way to pull your current username (e.g., from your UserSession or model layer)
        requestPacket.addProperty("username", com.example.auctionapp.model.UserSession.getUsername());

        // Send the packet over your application's out stream writer
        try {
            java.io.PrintWriter out = com.example.auctionapp.model.UserSession.getOut();
            out.println(requestPacket.toString());
            out.flush();
            System.out.println("[UI]: Requested notification history from server router.");
        } catch (Exception e) {
            System.err.println("Failed to transmit history request message.");
        }
    }


    public void receiveLiveServerNotification(String type, String title, String message, String timestamp) {
        // 🚨 SAFETY COMPLIANCE: Wrap in runLater to defend against background Socket exceptions!
        Platform.runLater(() -> {
            try {
                // Ensure layout container components are revealed if this is the first item arriving
                if (!recentScrollPane.isVisible()) {
                    recentScrollPane.setVisible(true);
                    emptyRecentPane.setVisible(false);
                }

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/view/notif_bar.fxml"));
                Pane liveCardRow = loader.load();

                NotifBarController rowController = loader.getController();
                rowController.renderNotification(type, title, message, timestamp);

                // 🌟 Index 0 maps insertion directly to the absolute TOP of the scrolling stack container.
                // Fresh real-time network items click into view on top, pushing older history entries down!
                recentItemsContainer.getChildren().add(0, liveCardRow);

            } catch (Exception e) {
                System.err.println(" Critical failure during live dynamic card assembly execution:");
                e.printStackTrace();
            }
        });
    }
    /**
     * Call this method inside your initialize() hook right before
     * you send out the network request over the socket!
     */
    private void showLoadingSkeletons(int cardCount) {
        // 1. Clear out any previous components and prepare display
        recentItemsContainer.getChildren().clear();
        recentScrollPane.setVisible(true);
        emptyRecentPane.setVisible(false);

        // Color definitions to match your dark theme perfectly
        String cardBgColor = "#20202E";
        String skeletonBlockColor = "#2c2c3c"; // Slightly lighter gray for the pulsing blocks

        // 2. Dynamically construct clean layout shapes matching your FXML configurations
        for (int i = 0; i < cardCount; i++) {

            // Match the FXML root Pane configurations exactly (prefWidth: 1055, prefHeight: 173)
            Pane skeletonCard = new Pane();
            skeletonCard.setPrefWidth(1055.0);
            skeletonCard.setPrefHeight(173.0);
            skeletonCard.setMaxWidth(Pane.USE_PREF_SIZE);
            skeletonCard.setMinWidth(Pane.USE_PREF_SIZE);
            skeletonCard.setMaxHeight(Pane.USE_PREF_SIZE);
            skeletonCard.setMinHeight(Pane.USE_PREF_SIZE);

            // Base card styling matching your modern dark card theme
            skeletonCard.setStyle(
                    "-fx-background-color: " + cardBgColor + "; " +
                            "-fx-background-radius: 12px; " +
                            "-fx-border-color: #2b2b3c; " +
                            "-fx-border-width: 0 0 0 5; " + // Clean left accent border strip
                            "-fx-border-radius: 12px 0px 0px 12px;"
            );

            // 3. Mock Image view asset slot (layoutX="34", layoutY="32", fitHeight="110", fitWidth="111")
            Region mockImage = new Region();
            mockImage.setLayoutX(34.0);
            mockImage.setLayoutY(32.0);
            mockImage.setPrefSize(111.0, 110.0);
            mockImage.setStyle("-fx-background-color: " + skeletonBlockColor + "; -fx-background-radius: 8px;");

            // 4. Mock Main Headline text row slot (layoutX="186", layoutY="39", prefHeight="39", prefWidth="500 [Truncated for look]")
            Region mockTitle = new Region();
            mockTitle.setLayoutX(186.0);
            mockTitle.setLayoutY(46.0); // Slightly adjusted inside the 39px height box for vertical balance
            mockTitle.setPrefSize(420.0, 24.0);
            mockTitle.setStyle("-fx-background-color: " + skeletonBlockColor + "; -fx-background-radius: 4px;");

            // 5. Mock Detail/Description block lines (layoutX="186", layoutY="107", prefHeight="58", prefWidth="874")
            Region mockDescriptionLine1 = new Region();
            mockDescriptionLine1.setLayoutX(186.0);
            mockDescriptionLine1.setLayoutY(110.0);
            mockDescriptionLine1.setPrefSize(780.0, 14.0);
            mockDescriptionLine1.setStyle("-fx-background-color: " + skeletonBlockColor + "; -fx-background-radius: 4px;");

            Region mockDescriptionLine2 = new Region();
            mockDescriptionLine2.setLayoutX(186.0);
            mockDescriptionLine2.setLayoutY(134.0);
            mockDescriptionLine2.setPrefSize(520.0, 14.0);
            mockDescriptionLine2.setStyle("-fx-background-color: " + skeletonBlockColor + "; -fx-background-radius: 4px;");

            // 6. Mock Timestamp block alignment position (layoutX="749", layoutY="39", prefHeight="39", prefWidth="278")
            // We use a label with matching alignment to easily push the placeholder box all the way right
            Label mockTimeContainer = new Label("██████");
            mockTimeContainer.setLayoutX(749.0);
            mockTimeContainer.setLayoutY(39.0);
            mockTimeContainer.setPrefSize(278.0, 39.0);
            mockTimeContainer.setAlignment(Pos.CENTER_RIGHT); // Matches alignment="CENTER_RIGHT"
            mockTimeContainer.setStyle("-fx-text-fill: " + skeletonBlockColor + "; -fx-font-size: 16px;");

            // Assemble the node layers inside the programmatic skeleton card canvas
            skeletonCard.getChildren().addAll(
                    mockImage,
                    mockTitle,
                    mockDescriptionLine1,
                    mockDescriptionLine2,
                    mockTimeContainer
            );

            // 🌟 7. NATIVE JAVAFX PULSING ANIMATION (No CSS file edits needed)
            FadeTransition pulse = new FadeTransition(Duration.millis(800), skeletonCard);
            pulse.setFromValue(1.0);
            pulse.setToValue(0.4);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();

            // Append the compiled card down into the scrolling parent VBox
            recentItemsContainer.getChildren().add(skeletonCard);
        }
    }

    /**
     * HISTORICAL NETWORK LOADER LOGIC
     * Your NetworkRouter calls this when the server responds with the user's notification history.
     */
    public void populateHistoryFromNetworkArray(JsonArray historyList) {
        // Safe clear out of old structural residues
        recentItemsContainer.getChildren().clear();

        // EMPTY CHECK LOGIC: Toggles visibility if user history is empty
        if (historyList == null || historyList.size() == 0) {
            recentScrollPane.setVisible(false);
            emptyRecentPane.setVisible(true);
            return;
        }

        // History found! Hide empty pane, reveal the scroll area
        recentScrollPane.setVisible(true);
        emptyRecentPane.setVisible(false);

        // BROWSE-STYLE HYDRATION LOOP
        for (JsonElement element : historyList) {
            try {
                JsonObject data = element.getAsJsonObject();

                // 1. Inflate an isolated visual instance of your custom card design
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/NotifBar.fxml"));
                Pane cardRow = loader.load();

                // 2. Fetch the newly compiled UI controller bound to that individual row instance
                NotifBarController rowController = loader.getController();

                // 3. Inject structural configurations directly onto the text/image placeholders
                rowController.renderNotification(
                        data.get("type").getAsString(),
                        data.get("title").getAsString(),
                        data.get("message").getAsString(),
                        data.get("time").getAsString()
                );

                // 4. Cleanly append the compiled custom row layout onto the bottom vertical stack
                recentItemsContainer.getChildren().add(cardRow);

            } catch (Exception e) {
                System.err.println("❌ Failed to inflate network notification card row component:");
                e.printStackTrace();
            }
        }
    }

    // Global memory hook accessors
    public static NotifController getInstance() {
        return instance;
    }
}