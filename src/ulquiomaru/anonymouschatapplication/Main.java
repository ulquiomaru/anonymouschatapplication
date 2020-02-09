package ulquiomaru.anonymouschatapplication;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("application.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Chat App");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

    }

//    static void setNickName(String nickname) {
//        nickName = nickname;
//    }

//    private static synchronized HashMap<String, PublicKey> getHashMap() {
//        return hashMap;
//    }

//    @Override
//    public void init() {
////        isServer = getParameters().getRaw().get(0).toLowerCase().equals("server");
//    }

//    @Override
//    public void stop() throws Exception {
//        connection.closeConnection();
//    }


}
