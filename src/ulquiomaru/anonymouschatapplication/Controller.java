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

public class Controller {

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

    public Controller() { }

    @FXML
    private void initialize() {
        menuConnect.setDisable(true);
        menuDisconnect.setDisable(true);
    }

    @FXML
    private void sendMessageClicked() {
        if (txtInput.getText().length() > 0) {
            String message = "UserName: ";
            message += txtInput.getText();

            try {
//                Main.connection.encryptMessage(message, 1);
                txtChat.appendText(message + "\n");
                txtInput.clear();
                txtInput.requestFocus();
            } catch (Exception e) {
                txtChat.appendText("Failed to send message\n");
            }
//            Main.connection.send(message);
        }
    }

    @FXML
    private void clickedGenerateKeys() {
        Main.generateKeys();
        menuConnect.setDisable(false);
    }

    @FXML
    private void connectNetwork() {
        // TODO trigger a pop up that request a Nickname from the user
        // TODO connect to the network
        // TODO consecutively broadcasts the new user’s identity (nickname and public key) to the local subnet.

        menuDisconnect.setDisable(false);
        menuConnect.setDisable(true);
        menuGenerateKeys.setDisable(true);
    }

    @FXML
    private void disconnectNetwork() {
        // TODO trigger a special Quit message broadcast.

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
        btnClosePopup.setOnAction(e -> popupAbout.close());

        VBox layout = new VBox(40);
        layout.getChildren().addAll(lblAbout, btnClosePopup);
        layout.setAlignment(Pos.CENTER);

        Scene scenePopup = new Scene(layout, 400, 400);

        popupAbout.setScene(scenePopup);
        popupAbout.showAndWait();
    }

}
