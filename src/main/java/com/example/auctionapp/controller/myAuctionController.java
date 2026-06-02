package com.example.auctionapp.controller;

import com.example.auctionapp.model.Member;
import com.example.auctionapp.model.UserSession;
import com.example.auctionapp.model.NetworkMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class myAuctionController {

    private DashboardController mainDashboard;

    @FXML private Label greetingLabel;
    @FXML private Button EditItemButton;
    public static myAuctionController activeMyAuctionsScreen = null;
    @FXML private ScrollPane myAuctionScrollPane;
    @FXML private FlowPane myAuctionFlowPane;
    @FXML private VBox emptyRecentPane;

    // 📦 Internal data source tracking the items currently shown on screen for the Edit Window
    private final ObservableList<EditableItem> liveUserAuctions = FXCollections.observableArrayList();

    public void setMainDashboard(DashboardController mainDashboard) {
        this.mainDashboard = mainDashboard;
    }

    @FXML
    public void initialize() {
        activeMyAuctionsScreen = this;

        // Wire up the edit button to launch the configuration popup
        EditItemButton.setOnAction(event -> openEditItemWindow());

        // Automatically start fetching the logged-in user's auctions on load
        fetchHostedAuctions();
    }

    public void fetchHostedAuctions() {
        // 1. Resolve the logged-in user's username context
        String currentUsername = UserSession.getUsername();

        if (greetingLabel != null && currentUsername != null) {
            greetingLabel.setText("See all the auctions you held here, " + currentUsername);
        }

        // 2. Set up the UI for loading (Hide empty state, show skeletons)
        if (emptyRecentPane != null) emptyRecentPane.setVisible(false);
        if (myAuctionScrollPane != null) myAuctionScrollPane.setVisible(true);

        myAuctionFlowPane.getChildren().clear();
        for (int i = 0; i < 4; i++) {
            myAuctionFlowPane.getChildren().add(createSkeletonCard());
        }

        // 3. ONLY SEND THE REQUEST. No background threads, no readLine()!
        try {
            Gson gson = new Gson();
            NetworkMessage request = new NetworkMessage("GET_MY_AUCTIONS", currentUsername, true);

            // Send request over the socket stream
            UserSession.getOut().println(gson.toJson(request));
            UserSession.getOut().flush();

            System.out.println("📤 [My Auctions]: Request sent to server. Waiting for Global Router to deliver response...");

        } catch (Exception e) {
            System.err.println("Error sending request:");
            e.printStackTrace();
        }
    }

    // 🌟 THE ROUTER HAND-OFF TARGET
    public void displayAuctionsOnScreen(String serverResponse) {
        Platform.runLater(() -> {
            myAuctionFlowPane.getChildren().clear();
            liveUserAuctions.clear(); // Empty the previous list for the Edit Window

            System.out.println("DEBUG - My Auctions Raw Server JSON: " + serverResponse);

            if (serverResponse == null || serverResponse.equals("NO_ITEMS") || serverResponse.equals("[]") || serverResponse.isEmpty()) {
                emptyRecentPane.setVisible(true);
                myAuctionScrollPane.setVisible(false);
                return;
            }

            emptyRecentPane.setVisible(false);
            myAuctionScrollPane.setVisible(true);

            try {
                JsonArray auctionItems = JsonParser.parseString(serverResponse).getAsJsonArray();

                for (JsonElement element : auctionItems) {
                    JsonObject itemObj = element.getAsJsonObject();

                    try {
                        int id = itemObj.get("id").getAsInt();
                        String name = itemObj.get("itemName").getAsString();
                        double startPrice = itemObj.get("startingPrice").getAsDouble();
                        double currentPrice = itemObj.get("currentPrice").getAsDouble();
                        String condition = itemObj.get("itemCondition").getAsString();
                        String base64Image = itemObj.get("imagePath").getAsString();
                        String description = itemObj.get("description").getAsString();
                        String seller = itemObj.get("seller").getAsString();
                        long endTimeMillis = itemObj.get("endTime").getAsLong();
                        double increment = itemObj.get("priceIncrement").getAsDouble();

                        // Fallback check in case 'itemType' isn't explicitly in this JSON payload yet
                        String type = itemObj.has("itemType") ? itemObj.get("itemType").getAsString() : "";

                        // 🌟 1. Save data into our local list for the Edit Window Dropdown
                        liveUserAuctions.add(new EditableItem(id, name, type, condition, description));

                        // 2. Load the standard card FXML template instance
                        FXMLLoader cardLoader = new FXMLLoader(getClass().getResource("/com/example/auctionapp/AuctionCard.fxml"));
                        Node cardNode = cardLoader.load();
                        AuctionCardController cardController = cardLoader.getController();

                        // Push attributes directly into the layout controller parameters
                        cardController.setCardData(
                                id, name, startPrice, currentPrice, condition,
                                description, base64Image, seller, endTimeMillis, increment
                        );

                        myAuctionFlowPane.getChildren().add(cardNode);

                    } catch (Exception e) {
                        System.out.println("ERROR: Could not process single hosted item block entry!");
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("ERROR: Invalid JSON structure payload handled from server stream!");
                e.printStackTrace();
            }
        });
    }

    // ====================================================================================
    // 🔧 THE EDIT WINDOW COMPONENT
    // ====================================================================================

    private void openEditItemWindow() {
        if (liveUserAuctions.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You don't have any active auctions to edit!", ButtonType.OK);
            alert.setTitle("No Content Available");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        Stage editStage = new Stage();
        editStage.initModality(Modality.APPLICATION_MODAL); // Freeze background UI
        editStage.setTitle("Modify Your Listing");

        VBox rootLayout = new VBox(15);
        rootLayout.setPadding(new Insets(20));
        rootLayout.setStyle("-fx-background-color: #16161f;");

        Label selectLabel = new Label("Select Item to Update:");
        selectLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        ComboBox<EditableItem> itemComboBox = new ComboBox<>();
        itemComboBox.setPrefWidth(320);
        itemComboBox.setItems(liveUserAuctions);
        itemComboBox.setStyle("-fx-background-color: #20202E; -fx-text-fill: white; -fx-border-color: #2b2b3c;");

        // Convert the object references into readable item names in the dropdown
        itemComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(EditableItem row) { return row != null ? row.name : ""; }
            @Override public EditableItem fromString(String string) { return null; }
        });

        // 🌟 Inputs: Text Fields & Area
        TextField nameField = new TextField();
        TextArea descField = new TextArea();
        descField.setPrefRowCount(3);

        // 🌟 Inputs: New Dropdown ComboBoxes
        ComboBox<String> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll("Electronics", "Vehicles", "Furniture", "Books", "Fashion");
        typeComboBox.setPrefWidth(320);

        ComboBox<String> conditionComboBox = new ComboBox<>();
        conditionComboBox.getItems().addAll("NEW", "USED", "LIKE NEW");
        conditionComboBox.setPrefWidth(320);

        // Styling configurations
        String inputStyles = "-fx-background-color: #20202E; -fx-text-fill: white; -fx-border-color: #2b2b3c; -fx-border-radius: 6;";
        nameField.setStyle(inputStyles);
        typeComboBox.setStyle(inputStyles);
        conditionComboBox.setStyle(inputStyles);
        descField.setStyle(
                "-fx-control-inner-background: #20202E; " +  // This targets the inner text viewport
                        "-fx-text-fill: white; " +
                        "-fx-background-color: #20202E; " +
                        "-fx-border-color: #2b2b3c; " +
                        "-fx-border-radius: 6;"
        );

        // 🌟 CRITICAL FIX: Custom cell factory styling to crush the default Modena grey selection blocks
        String cellStyles = "-fx-background-color: #20202E; -fx-text-fill: white; -fx-padding: 8px;";

        javafx.util.Callback<ListView<String>, ListCell<String>> customCellFactory = listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #20202E;");
                } else {
                    setText(item);
                    setStyle(cellStyles);
                }
            }
        };

        // Apply the cell designs to both the dropdown list cells AND the main selected window button preview
        typeComboBox.setCellFactory(customCellFactory);
        typeComboBox.setButtonCell(customCellFactory.call(null));

        conditionComboBox.setCellFactory(customCellFactory);
        conditionComboBox.setButtonCell(customCellFactory.call(null));
        itemComboBox.setStyle(itemComboBox.getStyle() + " -fx-control-inner-background: #20202E;");

        // Pre-fill input layouts instantly when an item from the main dropdown is selected
        itemComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                nameField.setText(newSel.name);
                descField.setText(newSel.description);

                // Smart matching logic for Type selection
                if (newSel.type != null && !newSel.type.isEmpty()) {
                    typeComboBox.getSelectionModel().select(newSel.type);
                } else {
                    typeComboBox.getSelectionModel().clearSelection();
                }

                // Smart matching logic for Condition selection
                if (newSel.condition != null && !newSel.condition.isEmpty()) {
                    // normalize inputs (e.g. "New" -> "NEW") to match the static choices perfectly
                    conditionComboBox.getSelectionModel().select(newSel.condition.toUpperCase());
                } else {
                    conditionComboBox.getSelectionModel().clearSelection();
                }
            }
        });

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(12);

        formGrid.add(new Label("Item Name:") {{ setStyle("-fx-text-fill: #ffffff;"); }}, 0, 0);
        formGrid.add(nameField, 1, 0);
        formGrid.add(new Label("Item Type:") {{ setStyle("-fx-text-fill: #ffffff;"); }}, 0, 1);
        formGrid.add(typeComboBox, 1, 1);
        formGrid.add(new Label("Condition:") {{ setStyle("-fx-text-fill: #ffffff;"); }}, 0, 2);
        formGrid.add(conditionComboBox, 1, 2);
        formGrid.add(new Label("Description:") {{ setStyle("-fx-text-fill: #ffffff;"); }}, 0, 3);
        formGrid.add(descField, 1, 3);

        Button saveButton = new Button("Commit Changes");
        saveButton.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #7c3aed, #4f46e5); -fx-text-fill: white; -fx-font-weight: bold;");
        saveButton.setPrefWidth(160);

        saveButton.setOnAction(e -> {
            EditableItem targetItem = itemComboBox.getSelectionModel().getSelectedItem();
            if (targetItem == null) return;

            // Extract selected choice values safely, falling back to empty string if none selected
            String selectedType = typeComboBox.getSelectionModel().getSelectedItem();
            String selectedCondition = conditionComboBox.getSelectionModel().getSelectedItem();

            if (UserSession.getCurrentUser() instanceof Member currentMember) {
                // Fires your domain model method with the clean dropdown values!
                currentMember.editItemListing(
                        targetItem.id,
                        nameField.getText(),
                        selectedType != null ? selectedType : "",
                        selectedCondition != null ? selectedCondition : "",
                        descField.getText()
                );
                editStage.close();
            }
        });

        rootLayout.getChildren().addAll(selectLabel, itemComboBox, new Separator(), formGrid, saveButton);
        editStage.setScene(new Scene(rootLayout, 800, 600));
        editStage.showAndWait();
    }

    public void handleUpdateResponse(boolean success) {
        if (success) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Your auction item was modified successfully!", ButtonType.OK);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.showAndWait();

            // Instant Visual Refresh! Re-fetch data from the server so the cards update on screen
            fetchHostedAuctions();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update item details. Please check your database permissions.", ButtonType.OK);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    // ====================================================================================
    // SKELETON ANIMATIONS & HELPERS
    // ====================================================================================

    private VBox createSkeletonCard() {
        // Base skeleton node sizing matched directly to card template profiles
        VBox skeletonCard = new VBox();
        skeletonCard.setPrefSize(402.0, 551.0);

        Pane container = new Pane();
        container.setPrefSize(402.0, 551.0);
        container.setStyle("-fx-background-color: #20202E; -fx-background-radius: 10px;");

        // Media illustration block mask
        Rectangle ghostImage = new Rectangle(402.0, 357.0);
        ghostImage.setStyle("-fx-fill: #2B2B3C;");
        Rectangle imageClip = new Rectangle(402.0, 357.0);
        imageClip.setArcWidth(40.0);
        imageClip.setArcHeight(40.0);
        ghostImage.setClip(imageClip);

        // Headline Text Fields
        Rectangle ghostTitleLine1 = new Rectangle(180.0, 16.0);
        ghostTitleLine1.setLayoutX(29.0);
        ghostTitleLine1.setLayoutY(376.0);
        ghostTitleLine1.setArcWidth(8.0);
        ghostTitleLine1.setArcHeight(8.0);
        ghostTitleLine1.setStyle("-fx-fill: #2B2B3C;");

        Rectangle ghostTitleLine2 = new Rectangle(120.0, 16.0);
        ghostTitleLine2.setLayoutX(29.0);
        ghostTitleLine2.setLayoutY(402.0);
        ghostTitleLine2.setArcWidth(8.0);
        ghostTitleLine2.setArcHeight(8.0);
        ghostTitleLine2.setStyle("-fx-fill: #2B2B3C;");

        // Quality rating badge shape
        Rectangle ghostCondition = new Rectangle(90.0, 25.0);
        ghostCondition.setLayoutX(289.0);
        ghostCondition.setLayoutY(373.0);
        ghostCondition.setArcWidth(20.0);
        ghostCondition.setArcHeight(20.0);
        ghostCondition.setStyle("-fx-fill: #2B2B3C;");

        // Identity line marker
        Rectangle ghostSeller = new Rectangle(100.0, 14.0);
        ghostSeller.setLayoutX(29.0);
        ghostSeller.setLayoutY(426.0);
        ghostSeller.setArcWidth(8.0);
        ghostSeller.setArcHeight(8.0);
        ghostSeller.setStyle("-fx-fill: #2B2B3C;");

        // Structural grid break rules
        Rectangle ghostSeparator = new Rectangle(349.0, 1.0);
        ghostSeparator.setLayoutX(21.0);
        ghostSeparator.setLayoutY(453.0);
        ghostSeparator.setStyle("-fx-fill: #3E3E53;");

        // Cost detail tags
        Rectangle ghostPriceLabel = new Rectangle(70.0, 12.0);
        ghostPriceLabel.setLayoutX(29.0);
        ghostPriceLabel.setLayoutY(476.0);
        ghostPriceLabel.setStyle("-fx-fill: #2B2B3C;");

        // Main ledger tracking numbers
        Rectangle ghostPrice = new Rectangle(85.0, 24.0);
        ghostPrice.setLayoutX(28.0);
        ghostPrice.setLayoutY(492.0);
        ghostPrice.setArcWidth(8.0);
        ghostPrice.setArcHeight(8.0);
        ghostPrice.setStyle("-fx-fill: #3A3A50;");

        // Interaction button bounding masks
        Rectangle ghostButton = new Rectangle(140.0, 40.0);
        ghostButton.setLayoutX(239.0);
        ghostButton.setLayoutY(484.0);
        ghostButton.setArcWidth(20.0);
        ghostButton.setArcHeight(20.0);
        ghostButton.setStyle("-fx-fill: #2B2B3C;");

        container.getChildren().addAll(
                ghostImage, ghostTitleLine1, ghostTitleLine2, ghostCondition,
                ghostSeller, ghostSeparator, ghostPriceLabel, ghostPrice, ghostButton
        );
        skeletonCard.getChildren().add(container);

        // Bind animations to nodes to simulate smooth asynchronous shimmers
        for (var node : container.getChildren()) {
            if (node != ghostSeparator) {
                FadeTransition pulse = new FadeTransition(Duration.millis(750), node);
                pulse.setFromValue(1.0);
                pulse.setToValue(0.35);
                pulse.setCycleCount(Animation.INDEFINITE);
                pulse.setAutoReverse(true);
                pulse.play();
            }
        }

        return skeletonCard;
    }

    /**
     * A lightweight internal data class used exclusively to bridge the gap between
     * the incoming JSON layout parameters and the Dropdown ComboBox selection menu.
     */
    public static class EditableItem {
        public int id;
        public String name;
        public String type;
        public String condition;
        public String description;

        public EditableItem(int id, String name, String type, String condition, String description) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.condition = condition;
            this.description = description;
        }
    }
}