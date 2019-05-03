package drc_chatter;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Chatter extends Application {
    protected static Connections connections;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat_selector.fxml"));
        Scene scene = new Scene(loader.load());
        setupConnections();
        ((ControllerChatSelector)loader.getController()).setup();
        primaryStage.setTitle("Distributed Reactive Chat");
        primaryStage.setScene(scene);
        primaryStage.setMinHeight(240);
        primaryStage.setMinWidth(360);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            ((ControllerChatSelector)loader.getController()).closeChannels();
            Chatter.closeConnections();
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void setupConnections() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setConnectionTimeout(30000);
        factory.setUri("amqp://pvawzpgd:oZJzaxqO_7uTUkyiH_rLZXm5TSPKMITs@wolverine.rmq.cloudamqp.com/pvawzpgd");
        Connection connectionToDbService = factory.newConnection();
        factory.setUri("amqp://msqhlriu:Jq9QAV3ia2NZr-0LsPIEBPhICJQQ8B1-@wolverine.rmq.cloudamqp.com/msqhlriu");
        Connection connectionToLoggerService = factory.newConnection();
        factory.setUri("amqp://jmrbfcuy:16cLgtQ71ChRr1xxDbPJR1rhpXJ2H4_v@wolverine.rmq.cloudamqp.com/jmrbfcuy");
        Connection connectionToMessagingService = factory.newConnection();
        factory.setUri("amqp://ytkwauqz:k8p8Eza3RvElskjVUY9cf0Y2ESZ5UKV2@wolverine.rmq.cloudamqp.com/ytkwauqz");
        Connection connectionToCsService = factory.newConnection();
        connections = new Connections(connectionToDbService, connectionToLoggerService, connectionToMessagingService, connectionToCsService);
    }

    protected static void closeConnections() {
        try {
            connections.getConnectionToDbService().close();
            connections.getConnectionToLoggerService().close();
            connections.getConnectionToMessagingService().close();
            connections.getConnectionToCsService().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
