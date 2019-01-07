package ulquiomaru.anonymouschatapplication;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.function.Consumer;

public class NetworkConnection {

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

    void send(String data) throws Exception {
        Runtime.getRuntime().exec("./sender " + data);
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
            try (DatagramSocket socket = new DatagramSocket(7777)) { // new DatagramSocket(7777, InetAddress.getByName("0.0.0.0"));
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

//    private class FileConnectionThread extends Thread {
//        private Socket socket;
//        private DataOutputStream out;
//        private final int chunkSize = 1024;
//
//        @Override
//        public void run() {
//            try (ServerSocket server = isServer() ? new ServerSocket(getPort()+1) : null;
//                 Socket socket = isServer() ? server.accept() : new Socket(getIP(), getPort()+1);
//                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                 DataInputStream in = new DataInputStream(socket.getInputStream())) {
//
//                this.socket = socket;
//                this.out = out;
//                socket.setTcpNoDelay(true);
//
//                byte[] data = new byte[chunkSize];
//                int dataSize;
//
//                if (true) {
//                    while (!sendFileCheck); // hold until button clicked
//
//                    onReceiveCallback.accept("File transfer has begun!");
//
//                    Cipher cipher = Cipher.getInstance(FILE_ALGORITHM_AES);
//                    byte[] iV = new byte[cipher.getBlockSize()];
//                    SecureRandom RNG = new SecureRandom();
//                    RNG.nextBytes(iV);
//                    cipher.init(Cipher.ENCRYPT_MODE, getAesKey(), new IvParameterSpec(iV));
//                    CipherInputStream file = new CipherInputStream(new FileInputStream(new File("fileToSend")), cipher);
//
//                    out.write(iV);
//                    out.flush();
//                    while ((dataSize = file.read(data)) > 0) {
//                        out.write(data, 0, dataSize);
//                    }
//                    out.close();
//
//                    onReceiveCallback.accept("File transfer complete.");
//                }
//                else {
//                    Cipher cipher = Cipher.getInstance(FILE_ALGORITHM_AES);
//                    File rFile = new File("fileReceived");
//                    if (rFile.exists()) rFile.delete();
//                    rFile.createNewFile();
//                    byte[] iV = new byte[cipher.getBlockSize()];
//
//                    while (in.available() < iV.length);
//                    in.read(iV, 0, iV.length);
//                    while (in.read(iV) <= 0);
//
//                    onReceiveCallback.accept("File transfer has begun!");
//
//                    cipher.init(Cipher.DECRYPT_MODE, getAesKey(), new IvParameterSpec(iV));
//                    CipherOutputStream file = new CipherOutputStream(new FileOutputStream(rFile, true), cipher);
//
//                    while ((dataSize = in.read(data)) > 0) {
//                        file.write(data, 0, dataSize);
//                    }
//                    file.close();
//                    onReceiveCallback.accept("File transfer complete.");
//                }
//
//            } catch (Exception e) {
//                onReceiveCallback.accept("Connection closed");
//                e.printStackTrace();
//            }
//        }
//    }

//    void encryptMessage(String message, int mode) throws Exception {
//        Cipher cipher = Cipher.getInstance("AES/CFB/PKCS5Padding");
//        byte[] iV = new byte[cipher.getBlockSize()];
//        SecureRandom RNG = new SecureRandom();
//        RNG.nextBytes(iV);
//        cipher.init(Cipher.ENCRYPT_MODE, getAesKey(), new IvParameterSpec(iV));
//
//        byte[] cipherText = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
//
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        output.write(iV);
//        output.write(cipherText);
//        if (mode == 1)
//            send(output.toByteArray());
//    }

//    private byte[] decryptMessage(byte[] data) throws Exception {
//        Cipher cipher = Cipher.getInstance("AES/CFB/PKCS5Padding");
//        byte[] iV = new byte[cipher.getBlockSize()];
//        System.arraycopy(data, 0, iV, 0, iV.length);
//        byte[] cipherText = new byte[data.length - iV.length];
//        System.arraycopy(data, iV.length, cipherText, 0, cipherText.length);
//
//        cipher.init(Cipher.DECRYPT_MODE, getAesKey(), new IvParameterSpec(iV));
//
//        return cipher.doFinal(cipherText);
//    }

}
