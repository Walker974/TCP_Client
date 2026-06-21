package org.booking.client.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.booking.client.model.ChatClient;
import org.booking.client.model.ClientConfig;
import org.booking.client.model.ClientListener;

import java.util.List;
import java.util.Optional;

public class ClientApp extends Application implements ClientListener {

    private ChatClient client;
    private boolean readOnly;

    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Circle indicator;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        List<String> args = getParameters().getRaw();
        String hostArg = args.size() > 0 ? args.get(0) : null;
        String portArg = args.size() > 1 ? args.get(1) : null;
        ClientConfig config = new ClientConfig(hostArg, portArg);

        String username = askUsername();
        readOnly = (username == null || username.isBlank());

        buildUi(stage, config);

        client = new ChatClient(config.getHost(), config.getPort(), this);
        client.connect(readOnly ? "" : username);
    }

    private String askUsername() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText("Enter a username to join the chat.\nLeave empty (or Cancel) for READ-ONLY mode.");
        dialog.setContentText("Username:");
        Optional<String> result = dialog.showAndWait();
        return result.map(String::trim).orElse(null);
    }

    private void buildUi(Stage stage, ClientConfig config) {
        Label title = new Label("Group Chat");
        title.getStyleClass().add("title-label");

        indicator = new Circle(7, Color.web("#f38ba8"));
        statusLabel = new Label("Connecting...");
        statusLabel.getStyleClass().add("status-offline");
        HBox statusBox = new HBox(8, indicator, statusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.getStyleClass().add("chat-area");

        messageField = new TextField();
        messageField.setPromptText(readOnly
                ? "READ-ONLY MODE - you cannot send messages"
                : "Type a message, or a command: allUsers, end, bye");
        messageField.getStyleClass().add("message-field");
        messageField.setOnAction(e -> handleSend()); // Enter key sends
        HBox.setHgrow(messageField, Priority.ALWAYS);

        sendButton = new Button("SEND");
        sendButton.getStyleClass().add("send-button");
        sendButton.setOnAction(e -> handleSend());

        messageField.setDisable(readOnly);
        sendButton.setDisable(readOnly);

        HBox inputRow = new HBox(8, messageField, sendButton);
        inputRow.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        grid.getStyleClass().add("root");

        grid.add(title, 0, 0);
        grid.add(statusBox, 0, 1);
        grid.add(chatArea, 0, 2);
        grid.add(inputRow, 0, 3);

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(100);
        grid.getColumnConstraints().add(col);
        GridPane.setVgrow(chatArea, Priority.ALWAYS);

        Scene scene = new Scene(grid, 560, 520);
        scene.getStylesheets().add(getClass().getResource("/client.css").toExternalForm());

        stage.setTitle("TCP Group Chat - Client"
                + (readOnly ? " [READ-ONLY]" : "")
                + " (" + config.getHost() + ":" + config.getPort() + ")");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> client.disconnect());
        stage.show();
    }

    private void handleSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        messageField.clear();

        if (text.equalsIgnoreCase("end") || text.equalsIgnoreCase("bye")) {
            appendLine("You left the chat.");
            client.disconnect();
            return;
        }
        client.send(text);
    }

    private void appendLine(String line) {
        chatArea.appendText(line + "\n");
    }

    @Override
    public void onMessage(String message) {
        Platform.runLater(() -> appendLine(message));
    }

    @Override
    public void onConnected() {
        Platform.runLater(() -> {
            indicator.setFill(Color.web(readOnly ? "#f9e2af" : "#a6e3a1"));
            statusLabel.setText(readOnly ? "Online (READ-ONLY)" : "Online");
            statusLabel.getStyleClass().setAll(readOnly ? "status-readonly" : "status-online");
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            indicator.setFill(Color.web("#f38ba8"));
            statusLabel.setText("Offline");
            statusLabel.getStyleClass().setAll("status-offline");
            messageField.setDisable(true);
            sendButton.setDisable(true);
        });
    }

    @Override
    public void onError(String error) {
        Platform.runLater(() -> appendLine("[ERROR] " + error));
    }

    @Override
    public void stop() {
        if (client != null) {
            client.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
