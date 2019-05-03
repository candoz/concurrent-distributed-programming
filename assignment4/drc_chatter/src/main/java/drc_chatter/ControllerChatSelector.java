package drc_chatter;

import com.rabbitmq.client.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.lang.SerializationUtils;

import java.net.URL;
import java.util.*;

import static dictionary.MsgDictionary.*;

public class ControllerChatSelector implements Initializable {
    @FXML
    private TextField nicknameTextField;
    @FXML
    private Button loginButton;
    @FXML
    private VBox chatroomsVBox;
    @FXML
    private TextField createChatroomTextField;
    @FXML
    private Button createChatroomButton;

    private Channel chatroomsListChannel;
    private Channel createChatroomChannel;
    private Channel loginChannel;
    private ToggleGroup toggleGroup;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        toggleGroup = new ToggleGroup();
        setupGuiProperties();
    }

    @FXML
    private void loginPressed(ActionEvent event) {
        try {
            login();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshChatroomsPressed(ActionEvent event) {
        try {
            refreshChatroomsList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void createChatroomPressed(ActionEvent event) {
        try {
            createChatroom();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupGuiProperties() {
        loginButton.disableProperty().bind(Bindings.isEmpty(nicknameTextField.textProperty())
                .or(toggleGroup.selectedToggleProperty().isNull()));

        nicknameTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !nicknameTextField.getText().isEmpty()) {
                event.consume();
                try {
                    login();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        createChatroomButton.disableProperty().bind(Bindings.isEmpty(nicknameTextField.textProperty()));
        createChatroomButton.disableProperty().bind(Bindings.isEmpty(createChatroomTextField.textProperty()));
        createChatroomTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !createChatroomTextField.getText().isEmpty()) {
                event.consume();
                try {
                    createChatroom();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setup() {
        try {
            setupChannels();
            setupHandlers();
            refreshChatroomsList();
        } catch (Exception e) {
            e.printStackTrace();
            closeChannels();
            Chatter.closeConnections();
            System.exit(1);
        }
    }

    private void setupChannels() throws Exception {
        chatroomsListChannel = Chatter.connections.getConnectionToDbService().createChannel();
        createChatroomChannel = Chatter.connections.getConnectionToDbService().createChannel();
        loginChannel = Chatter.connections.getConnectionToLoggerService().createChannel();
    }

    private void setupHandlers() throws Exception {
        setupChatroomListHandler();
        setupCreateChatroomHandler();
        setupLoginHandler();
    }

    private void setupChatroomListHandler() throws Exception {
        chatroomsListChannel.basicConsume(RESPONSE_QUEUE, true, new DefaultConsumer(chatroomsListChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                ArrayList<String> list = (ArrayList<String>) SerializationUtils.deserialize(body);
                boolean first = true;
                Platform.runLater(() -> {
                    chatroomsVBox.getChildren().clear();
                });
                for (String s : list) {
                    RadioButton r = new RadioButton(s);
                    if (first) {
                        r.setSelected(true);
                        first = false;
                    }
                    r.setToggleGroup(toggleGroup);
                    Platform.runLater(() -> {
                        chatroomsVBox.getChildren().add(r);
                    });
                }
            }
        });
    }

    private void setupCreateChatroomHandler() throws Exception {
        createChatroomChannel.basicConsume(RESPONSE_QUEUE, true, new DefaultConsumer(createChatroomChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                if (properties.getHeaders().get(STATE_HEADER).toString().equals(OK_STATE)) {
                    String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                    RadioButton r = new RadioButton(chatroomName);
                    r.setSelected(true);
                    r.setToggleGroup(toggleGroup);
                    Platform.runLater(() -> {
                        createChatroomTextField.clear();
                        chatroomsVBox.getChildren().add(r);
                    });
                } else {
                    Platform.runLater(() -> {
                        createChatroomTextField.clear();
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Cannot create chat!");
                        alert.showAndWait();
                    });
                }
            }
        });
    }

    private void setupLoginHandler() throws Exception {
        loginChannel.basicConsume(RESPONSE_QUEUE, true, new DefaultConsumer(loginChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                String chatterName = properties.getHeaders().get(CHATTER_NAME_HEADER).toString();
                if (properties.getHeaders().get(STATE_HEADER).toString().equals(OK_STATE)) {
                    closeChannels();
                    Platform.runLater(() -> {
                        FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/chat.fxml"));
                        Stage stage = (Stage) loginButton.getScene().getWindow();
                        try {
                            long ticket = Long.parseLong(properties.getHeaders().get(TICKET_HEADER).toString());
                            stage.setScene(new Scene(chatLoader.load()));
                            ((ControllerChat) chatLoader.getController()).setup(chatterName, chatroomName, ticket);
                            stage.setOnCloseRequest(e -> {
                                ((ControllerChat)chatLoader.getController()).logout();
                                ((ControllerChat)chatLoader.getController()).closeChannels();
                                Chatter.closeConnections();
                                Platform.exit();
                                System.exit(0);
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Cannot login as " + chatterName + " in " + chatroomName + " chat!\n" +
                                "Nickname already taken!");
                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);  // doesn't seem to work in linux....
                        alert.showAndWait();
                    });
                }
            }
        });
    }

    private void refreshChatroomsList() throws Exception {
        chatroomsListChannel.basicPublish("", REQUEST_CHATROOMS_QUEUE, new AMQP.BasicProperties.Builder().replyTo(RESPONSE_QUEUE).build(), null);
    }

    private void createChatroom() throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(CHATROOM_NAME_HEADER, createChatroomTextField.getText());
        createChatroomChannel.basicPublish("", REQUEST_CREATE_CHATROOM_QUEUE, new AMQP.BasicProperties.Builder().replyTo(RESPONSE_QUEUE).headers(headers).build(), null);
    }

    private void login() throws Exception {
        String chatroomName = ((RadioButton) toggleGroup.getSelectedToggle()).getText();
        String chatterName = nicknameTextField.getText();
        Map<String, Object> headers = new HashMap<>();
        headers.put(CHATROOM_NAME_HEADER, chatroomName);
        headers.put(CHATTER_NAME_HEADER, chatterName);
        loginChannel.basicPublish("", REQUEST_LOGIN_QUEUE, new AMQP.BasicProperties.Builder().replyTo(RESPONSE_QUEUE).headers(headers).build(), null);
    }

    public void closeChannels() {
        try {
            if (chatroomsListChannel != null && chatroomsListChannel.isOpen()) {
                chatroomsListChannel.close();
            }
            if (createChatroomChannel != null && createChatroomChannel.isOpen()) {
                createChatroomChannel.close();
            }
            if (loginChannel != null && loginChannel.isOpen()) {
                loginChannel.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
