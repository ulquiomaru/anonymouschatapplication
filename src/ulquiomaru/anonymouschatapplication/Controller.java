package ulquiomaru.anonymouschatapplication;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Set;

public class Controller {

    private String nickName;

    @FXML
    TextArea txtChat;

    @FXML
    TextArea txtOnlineUsers;

    @FXML
    private TextField txtInput;

    @FXML
    MenuItem menuGenerateKeys;

    @FXML
    MenuItem menuConnect;

    @FXML
    MenuItem menuDisconnect;

    @FXML
    MenuItem menuExit;

    @FXML
    MenuItem menuAbout;

    @FXML
    Button btnSendMessage;

    public Controller() { }

    @FXML
    private void initialize() {
        menuConnect.setDisable(true);
        menuDisconnect.setDisable(true);
        txtInput.setDisable(true);
        btnSendMessage.setDisable(true);
    }

    @FXML
    private void sendMessageClicked() {
        if (txtInput.getText().length() > 0) {
            String message = txtInput.getText();
            try {
                Main.sendMessage(message);
                txtChat.appendText(nickName + ": " + message + "\n");
                txtInput.clear();
                txtInput.requestFocus();
            } catch (Exception e) {
                txtChat.appendText("*** ERROR *** : Failed to send message\n");
            }
        }
    }

    @FXML
    private void clickedGenerateKeys() {
        Main.generateKeys();
        menuConnect.setDisable(false);
        txtChat.appendText("*** Generated RSA keys.\n");
    }

    @FXML
    private void connectNetwork() {
        final Stage popupConnect = new Stage();

        popupConnect.initModality(Modality.APPLICATION_MODAL);
        popupConnect.setTitle("Connect");

        final Label label = new Label("Enter your nickname below:\n");
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);

        final TextField textField = new TextField();
        textField.setPrefWidth(150);
        textField.setMaxWidth(150);

        final Button btnClosePopup = new Button("OK");
        btnClosePopup.setDefaultButton(true);
        btnClosePopup.setOnAction(event -> {
            if (textField.getText().length() > 0) {
                nickName = textField.getText();
                txtChat.appendText("*** Connected to the network.\n");
                txtOnlineUsers.setText(nickName + " (me)\n\n");
                Main.connectToNetwork(nickName);
                popupConnect.close();
            }
        });

        VBox layout = new VBox(20);
        layout.getChildren().addAll(label, textField, btnClosePopup);
        layout.setAlignment(Pos.CENTER);

        Scene scenePopup = new Scene(layout, 250, 200);

        popupConnect.setScene(scenePopup);
        popupConnect.showAndWait();

        btnSendMessage.setDisable(false);
        txtInput.setDisable(false);
        txtInput.requestFocus();
        menuDisconnect.setDisable(false);
        menuConnect.setDisable(true);
        menuGenerateKeys.setDisable(true);
    }

    @FXML
    private void disconnectNetwork() {
        try {
            Main.disconnectFromNetwork();
        } catch (Exception e) {
            e.printStackTrace();
        }
        txtChat.appendText("*** Disconnected from the network.\n");
        txtOnlineUsers.clear();
        nickName = null;
        btnSendMessage.setDisable(true);
        txtInput.setDisable(true);
        menuDisconnect.setDisable(true);
        menuConnect.setDisable(false);
        menuGenerateKeys.setDisable(false);
    }

    @FXML
    private void quitApplication() {
        disconnectNetwork();
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void popupAbout() {
        final Stage popupAbout = new Stage();

        popupAbout.initModality(Modality.APPLICATION_MODAL);
        popupAbout.setTitle("About");

        final Label lblAbout = new Label("Developed\nby\n\nBarış Görgülü\n\nfor\n\nCSE 471\nData Communications\nand\nComputer Networks\n\nTerm Project\n\n");
        lblAbout.setTextAlignment(TextAlignment.CENTER);
        lblAbout.setAlignment(Pos.CENTER);

        final Button btnClosePopup = new Button("Close");
        btnClosePopup.setDefaultButton(true);
        btnClosePopup.setOnAction(e -> popupAbout.close());

        VBox layout = new VBox(40);
        layout.getChildren().addAll(lblAbout, btnClosePopup);
        layout.setAlignment(Pos.CENTER);

        Scene scenePopup = new Scene(layout, 400, 400);

        popupAbout.setScene(scenePopup);
        popupAbout.showAndWait();
    }

    void updateOnlineUsers(Set<String> users) {
        txtOnlineUsers.setText(nickName + " (me)\n\n");
        for (String user : users) {
            txtOnlineUsers.appendText(user + "\n");
        }
    }

    void appendChat(String message) {
        txtChat.appendText(message + "\n");
    }

}
