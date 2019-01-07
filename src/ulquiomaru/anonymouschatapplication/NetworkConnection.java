package ulquiomaru.anonymouschatapplication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.function.Consumer;

class NetworkConnection {

    private ConnectionThread connThread = new ConnectionThread();
    private String nickName;
    private String publicKey;
    private PrivateKey privateKey;
    private Consumer<String> onReceiveCallback;

    NetworkConnection(String nickName, String publicKey, PrivateKey privateKey, Consumer<String> onReceiveCallback) {
        this.onReceiveCallback = onReceiveCallback;
        this.nickName = nickName;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        connThread.setDaemon(true);
    }

    void startConnection() {
        connThread.start();
    }

    void closeConnection() throws Exception {
        broadcastQuit();
        connThread.socket.close();
    }

    private void send(String data) throws Exception {
//        Runtime.getRuntime().exec("./sender " + data);
//        Runtime.getRuntime().exec("./sender " + "DBG|" + data); // DEBUG
        DatagramSocket clientSocket = new DatagramSocket();
        byte[] sendData = data.getBytes(StandardCharsets.UTF_8);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("127.0.0.1"), 7777);
        clientSocket.send(sendPacket);
        clientSocket.close();
    }

    void broadcastMessage(String message) throws Exception {
        send(String.join("|", "MSG", nickName, message));
    }

    void broadcastIdentity() throws Exception {
        send(String.join("|", "CON", nickName, publicKey));
    }

    private void broadcastQuit() throws Exception {
        send(String.join("|", "BYE", nickName));
    }

    private class ConnectionThread extends Thread {
        private DatagramSocket socket;

        @Override
        public void run() {
//            try (DatagramSocket socket = new DatagramSocket(7777)) {
//            try (DatagramSocket socket = new DatagramSocket(7777, InetAddress.getByName("0.0.0.0"))) {
//            try (DatagramSocket socket = new DatagramSocket(7777, InetAddress.getByName("10.0.2.15"))) {
            try (DatagramSocket socket = new DatagramSocket(7777, InetAddress.getByName("127.0.0.1"))) {
                this.socket = socket;
                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                broadcastIdentity();
                while (true) {
                    socket.receive(packet);
                    onReceiveCallback.accept(new String(packet.getData(), StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                onReceiveCallback.accept("Connection closed"); // TODO remove this debug line
                e.printStackTrace();
            }
        }
    }

}
