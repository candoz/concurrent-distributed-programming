package distributed_chat.chatter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainChatter extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setTitle("Distributed chat");
        primaryStage.setScene(scene);
        primaryStage.setMinHeight(240);
        primaryStage.setMinWidth(360);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            ((Controller)loader.getController()).sendLogoutMessageToMyChatter();
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}
