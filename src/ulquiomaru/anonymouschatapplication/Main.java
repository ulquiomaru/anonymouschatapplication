package ulquiomaru.anonymouschatapplication;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Main extends Application {

    private static Controller controller;
    private static HashMap<String, PublicKey> hashMap = new HashMap<>();
    private static NetworkConnection connection;
    private static String nickName;
    private static String publicKey;
    private static PrivateKey privateKey;


    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("application.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

//        txtChat = controller.txtChat;
//        txtOnlineUsers = controller.txtOnlineUsers;

        primaryStage.setTitle("Chat App");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }

//    static void setNickName(String nickname) {
//        nickName = nickname;
//    }

    private static synchronized HashMap<String, PublicKey> getHashMap() {
        return hashMap;
    }

    static void generateKeys() {
        try {
            KeyPair keyPair = buildKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static PublicKey stringToPublicKey(String stringKey) {
        PublicKey receivedPublicKey = null;
        try {
            byte[] keyBytes = Base64.getMimeDecoder().decode(stringKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            receivedPublicKey = keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return receivedPublicKey;
    }

    private static KeyPair buildKeyPair() throws NoSuchAlgorithmException {
        final int keySize = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.genKeyPair();
    }

    private static String encrypt(PrivateKey privateKey, String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(UTF_8));

        return Base64.getEncoder().encodeToString(cipherText);
    }

    private static String decrypt(PublicKey publicKey, String cipherText) throws Exception {
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
        connection.closeConnection();
    }

    static void connectToNetwork(String nickname) {
        nickName = nickname;
        connection = new NetworkConnection(nickname, publicKey, privateKey, Main::onMessageReceived);
        connection.startConnection();
    }

    static void disconnectFromNetwork() throws Exception {
        getHashMap().clear();
        connection.closeConnection();
    }

    static void sendMessage(String message) throws Exception {
        connection.broadcastMessage(encrypt(privateKey, message));
//        connection.broadcastMessage(message);
    }

    private static void onMessageReceived(String data) {
        String[] split = data.split("[|]");
        String tag = split[0];
        String sender = split[1];
        if (!sender.equals(nickName)) { // Check self-broadcast
            switch (tag) {
                case "MSG":
                    String cipherText = split[2];
                    PublicKey userKey = getHashMap().get(sender);
                    try {
                    String plainText = decrypt(userKey, cipherText);
                    String message = sender + ": " + plainText;
//                        String message = sender + ": " + cipherText;
                        controller.appendChat(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "CON":
                    String str_senderPublicKey = split[2];
                    PublicKey senderPublicKey = stringToPublicKey(str_senderPublicKey);
                    controller.appendChat("CON " + sender); // TODO remove - present to debug
                    if (getHashMap().put(sender, senderPublicKey) == null) {
                        try {
                            connection.broadcastIdentity();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    controller.updateOnlineUsers(getHashMap().keySet());
                    break;
                case "BYE":
                    getHashMap().remove(sender);
                    controller.appendChat("BYE " + sender); // TODO remove - present to debug
                    controller.updateOnlineUsers(getHashMap().keySet());
                    break;
                default:
                    controller.appendChat(data); // TODO remove - present to debug
                    break;
            }
        }
    }

}
