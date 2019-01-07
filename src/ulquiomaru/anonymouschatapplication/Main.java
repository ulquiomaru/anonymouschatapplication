package ulquiomaru.anonymouschatapplication;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Main extends Application {

    static NetworkConnection connection;
    private static TextArea txtChat;
    private static TextArea txtOnlineUsers;
    private static String nickName;
    private static PublicKey publicKey;
    private static PrivateKey privateKey;


    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("application.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();

        txtChat = controller.txtChat;
        txtOnlineUsers = controller.txtOnlineUsers;

        primaryStage.setTitle("Chat App");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }

    static void setNickName(String nickname) {
        nickName = nickname;
    }

    static void generateKeys() {
        try {
            KeyPair keyPair = buildKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static KeyPair buildKeyPair() throws NoSuchAlgorithmException {
        final int keySize = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.genKeyPair();
    }

    public static byte[] encrypt(PrivateKey privateKey, @NotNull String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        return cipher.doFinal(plainText.getBytes());
    }

    public static byte[] decrypt(PublicKey publicKey, byte [] cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        return cipher.doFinal(cipherText);
    }

//    @Override
//    public void init() {
////        isServer = getParameters().getRaw().get(0).toLowerCase().equals("server");
//    }

    @Override
    public void stop() throws Exception {
        // TODO trigger a special Quit message broadcast.
        connection.closeConnection();
    }

    static void connectToNetwork(String nickName) {
        connection = new NetworkConnection(nickName, publicKey, privateKey, Main::onMessageReceived);
        connection.startConnection();
    }

    private static void onMessageReceived(String data) {
        // TODO MSG/CON/BYE filtering
        String[] split = data.split("|", 1);
        switch (split[0]) {
            case "MSG":
                String sender = split[1];
                String cipherText = split[2];
                PublicKey userKey = null;
                try {
                    String plainText = new String(decrypt(userKey, cipherText.getBytes()), StandardCharsets.UTF_8); // TODO sender's public key ile decrypt etmeli
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case "CON":

                break;

            case "BYE":

                break;

            default:
                break;
        }
        Platform.runLater(() -> txtChat.appendText(data.toString() + "\n"));
    }

    private static PublicKey stringToPublicKey(String stringKey) {
        PublicKey receivedPublicKey = null;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            receivedPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(stringKey)));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return receivedPublicKey;
    }
}
