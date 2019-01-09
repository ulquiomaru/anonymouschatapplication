package ulquiomaru.anonymouschatapplication;

import javafx.application.Platform;

import javax.crypto.Cipher;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

class NetworkConnection {

    private Controller controller;
    private ConnectionThread connThread = new ConnectionThread();
    private String nickName;
    private String publicKey;
    private PrivateKey privateKey;
    private boolean isGateway;
    private final static String[] gateways = {"192.168.56.1", "192.168.57.1", "192.168.58.1"};

    NetworkConnection(String nickName, String publicKey, PrivateKey privateKey, boolean isGateway, Controller controller) {
        this.controller = controller;
        this.nickName = nickName;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.isGateway = isGateway;
        connThread.setDaemon(true);
    }

    void startConnection() {
        connThread.start();
    }

    void closeConnection() throws Exception {
        broadcastQuit();
        connThread.socket.close();
    }

    private void forwardPacket(DatagramPacket packet) throws Exception {
        for (String gateway : gateways) { // TODO improve
            DatagramSocket clientSocket = new DatagramSocket();
            packet.setAddress(InetAddress.getByName(gateway));
            clientSocket.send(packet);
            clientSocket.close();
        }
    }

    private void send(String data) throws Exception {
//        Runtime.getRuntime().exec("./sender " + data);

        DatagramSocket clientSocket = new DatagramSocket();
        byte[] sendData = data.getBytes(UTF_8);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("192.168.56.255"), 7777);
        clientSocket.setBroadcast(true);
        clientSocket.send(sendPacket);
        clientSocket.close();
    }

    void encryptMessageBroadcast(String message) throws Exception {
        broadcastMessage(encrypt(privateKey, message));
    }

    void encryptWhisperBroadcast(String target, String message) throws Exception {
        broadcastWhisper(target, encryptWhisper(privateKey, connThread.hashMap.get(target), message));
    }

    private void broadcastWhisper(String target, String message) throws Exception {
        send(String.join("|", "WSP", nickName, target, message));
    }

    private void broadcastMessage(String message) throws Exception {
        send(String.join("|", "MSG", nickName, message));
    }

    private void broadcastIdentity() throws Exception {
        send(String.join("|", "CON", nickName, publicKey));
    }

    private void broadcastQuit() throws Exception {
        send(String.join("|", "BYE", nickName));
    }

    private static String encrypt(PrivateKey privateKey, String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] cipherText = cipher.doFinal(plainText.getBytes(UTF_8));
        return Base64.getMimeEncoder().encodeToString(cipherText);
    }

    private static String encryptWhisper(PrivateKey privateKey, PublicKey publicKey, String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] cipherText = cipher.doFinal(plainText.getBytes(UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] personalText = cipher.doFinal(cipherText);
        return Base64.getMimeEncoder().encodeToString(personalText);
    }

    private static String decrypt(PublicKey publicKey, String cipherText) throws Exception {
        byte[] bytes = Base64.getMimeDecoder().decode(cipherText);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return new String(cipher.doFinal(bytes), UTF_8);
    }

    private static String decryptWhisper(PublicKey publicKey, PrivateKey privateKey, String personalText) throws Exception {
        byte[] bytes = Base64.getMimeDecoder().decode(personalText);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] cipherText = cipher.doFinal(bytes);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return new String(cipher.doFinal(cipherText), UTF_8);
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

    private class ConnectionThread extends Thread {
        private DatagramSocket socket;
        private HashMap<String, PublicKey> hashMap = new HashMap<>();

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(7777)) { // new DatagramSocket(7777, InetAddress.getByName("0.0.0.0"))
                this.socket = socket;
                broadcastIdentity();
                while (true) {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    socket.receive(packet);
                    if (isGateway) forwardPacket(packet);
                    String data = new String(packet.getData(), UTF_8);
                    onMessageReceived(data, hashMap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onMessageReceived(String data, HashMap<String, PublicKey> hashMap) throws Exception {
        String[] split = data.split("[|]");
        String tag = split[0];
        String sender = split[1].trim();
        if (!sender.equals(nickName)) { // Check self-broadcast
            String plainText;
            String message;
            switch (tag) {
                case "MSG":
                    String cipherText = split[2];
                    PublicKey userKey = hashMap.get(sender);
                    plainText = decrypt(userKey, cipherText);
                    message = sender + ": " + plainText;
                    Platform.runLater(() -> controller.appendChat(message));
                    break;
                case "CON":
                    String str_senderPublicKey = split[2];
                    PublicKey senderPublicKey = stringToPublicKey(str_senderPublicKey);
                    if (hashMap.put(sender, senderPublicKey) == null) // Broadcast HELLO back
                        broadcastIdentity();
                    Platform.runLater(() -> controller.updateOnlineUsers(hashMap.keySet()));
                    break;
                case "BYE":
                    hashMap.remove(sender);
                    Platform.runLater(() -> controller.updateOnlineUsers(hashMap.keySet()));
                    break;
                case "WSP":
                    String target = split[2];
                    if (target.equals(nickName)) {
                        String personalText = split[3];
                        PublicKey senderKey = hashMap.get(sender);
                        plainText = decryptWhisper(senderKey, privateKey, personalText);
                        message = sender + " > " + target + ": " +  plainText;
                        Platform.runLater(() -> controller.appendChat(message));
                    }
                    break;
                default:
                    break;
            }
        }
    }


}
