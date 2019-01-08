package ulquiomaru.anonymouschatapplication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

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

        byte[] sendData = data.getBytes(UTF_8);
//        byte[] sendData = Base64.getEncoder().encode(data.getBytes());
//        byte[] sendData = data.getBytes();
//        byte[] sendData = Base64.getDecoder().decode(data);

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("127.0.0.1"), 7777);
        clientSocket.send(sendPacket);
        clientSocket.close();
    }

    void broadcastMessage(String message) throws Exception {
        send(String.join("|", "MSG", nickName, message));
    }

    void broadcastIdentity() throws Exception {
        send(String.join("|", "CON", nickName, publicKey));
//        sendHello(String.join("|", "CON", nickName, ""));
    }

    private void broadcastQuit() throws Exception {
        send(String.join("|", "BYE", nickName));
    }

    private class ConnectionThread extends Thread {
        private DatagramSocket socket;

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(7777)) {
//            try (DatagramSocket socket = new DatagramSocket(7777, InetAddress.getByName("0.0.0.0"))) {
//            try (DatagramSocket socket = new DatagramSocket(7777, InetAddress.getByName("10.0.2.15"))) {0
//            try (DatagramSocket socket = new DatagramSocket(7777, InetAddress.getByName("127.0.0.1"))) {
                this.socket = socket;
                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                broadcastIdentity();
                while (true) {
                    socket.receive(packet);
                    byte[] packetData = packet.getData();

//                    String data = new String(packetData);
//                    String data = Base64.getEncoder().encodeToString(packetData);
                    String data = new String(packetData, UTF_8);

                    onReceiveCallback.accept(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
