package ulquiomaru.anonymouschatapplication;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Main extends Application {

    private static HashMap<String, PublicKey> hashMap = new HashMap<>();
    private static NetworkConnection connection;
    private static TextArea txtChat;
    private static TextArea txtOnlineUsers;
    private static String nickName;
    private static String publicKey;
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
//            Base64.getEncoder().encodeToString(keyPair.getPublic());
            publicKey = keyPair.getPublic().toString();
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

    public static String encrypt(PrivateKey privateKey, String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(UTF_8));

        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decrypt(PublicKey publicKey, String cipherText) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(cipherText);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        return new String(cipher.doFinal(bytes), UTF_8);
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

    static void disconnectFromNetwork() throws Exception {
        connection.closeConnection();
    }

    static void sendMessage(String message) throws Exception {
        connection.broadcastMessage(encrypt(privateKey, message));
    }

    private static void onMessageReceived(String data) {
        // TODO MSG/CON/BYE filtering
        String[] split = data.split("[|]", 1);
        String tag = split[0];
        String sender = split[1];
        switch (tag) {
            case "MSG":
                String cipherText = split[2];
                PublicKey userKey = hashMap.get(sender);
                try {
                    String plainText = decrypt(userKey, cipherText);
                    String message = sender + ": " + plainText + "\n";
                    Platform.runLater(() -> txtChat.appendText(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "CON":
                String str_senderPublicKey = split[2];
                PublicKey senderPublicKey = stringToPublicKey(str_senderPublicKey);
                hashMap.put(sender, senderPublicKey);
                // TODO update online users
                break;
            case "BYE":
                hashMap.remove(sender);
                // TODO update online users
                break;
            default:
                break;
        }

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
