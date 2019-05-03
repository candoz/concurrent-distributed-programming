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
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.commons.lang.SerializationUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static dictionary.MsgDictionary.*;

public class ControllerChat implements Initializable {

    @FXML
    private Label nicknameLabel;
    @FXML
    private ListView listView;
    @FXML
    private TextArea messageTextArea;
    @FXML
    private Button messageButton;

    private String nickname;
    private String chatroom;
    private long ticket;
    private HashMap<Long, String[]> stashedMessages = new HashMap<>();
    public final String ENTER_CS_COMMAND = ":enter-cs";
    public final String EXIT_CS_COMMAND = ":exit-cs";

    private Channel messagingChannel;
    private Channel logoutChannel;
    private Channel csChannel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupGuiProperties();
    }

    @FXML
    private void sendPressed(ActionEvent event) {
        try {
            sendMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logoutPressed(ActionEvent event) {
        try {
            logout();

            closeChannels();
            FXMLLoader chatSelectorLoader = new FXMLLoader(getClass().getResource("/chat_selector.fxml"));
            Stage stage = (Stage) messageButton.getScene().getWindow();
            stage.setTitle("Distributed Reactive Chat");
            stage.setScene(new Scene(chatSelectorLoader.load()));
            ((ControllerChatSelector) chatSelectorLoader.getController()).setup();
            stage.setOnCloseRequest(e -> {
                ((ControllerChatSelector) chatSelectorLoader.getController()).closeChannels();
                Chatter.closeConnections();
                Platform.exit();
                System.exit(0);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupGuiProperties() {
        messageButton.disableProperty().bind(Bindings.isEmpty(messageTextArea.textProperty()));
        messageTextArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                if (event.isShiftDown()) {
                    messageTextArea.appendText(System.getProperty("line.separator"));
                } else {
                    messageTextArea.setText(messageTextArea.getText().substring(0, messageTextArea.getText().length()-1));
                    if (!messageTextArea.getText().isEmpty()) {
                        try {
                            sendMessage();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void setup(String nickname, String chatroom, long ticket) {
        this.nickname = nickname;
        this.chatroom = chatroom;
        this.ticket = ticket;
        Platform.runLater(() -> {
            nicknameLabel.setText(nickname);
            Stage stage = (Stage) nicknameLabel.getScene().getWindow();
            stage.setTitle(chatroom);
        });
        try {
            setupChannels();
            setupMessageHandler();
        } catch (Exception e) {
            e.printStackTrace();
            logout();
            closeChannels();
            Chatter.closeConnections();
            System.exit(1);
        }
    }

    private void setupChannels() throws Exception {
        messagingChannel = Chatter.connections.getConnectionToMessagingService().createChannel();
        logoutChannel = Chatter.connections.getConnectionToLoggerService().createChannel();
        csChannel = Chatter.connections.getConnectionToCsService().createChannel();
    }

    private void setupMessageHandler() throws Exception {
        messagingChannel.queueDeclare(nickname + chatroom, false, false, false, null);
        messagingChannel.basicConsume(nickname + chatroom, true, new DefaultConsumer(messagingChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                long messageTicket = Long.parseLong(properties.getHeaders().get(TICKET_HEADER).toString());
                String sender = properties.getHeaders().get(CHATTER_NAME_HEADER).toString();
                String type = properties.getHeaders().get(MESSAGE_TYPE_HEADER).toString();
                String message = (String) SerializationUtils.deserialize(body);
                if (messageTicket == ticket + 1) {
                    checkAndVisualizeMessage(type, sender, message);
                    ticket++;
                    while (stashedMessages.containsKey(ticket)) {  // unstash if there's the next message
                        String messageType = stashedMessages.get(ticket)[0];
                        String messageSender = stashedMessages.get(ticket)[1];
                        String messageContent = stashedMessages.get(ticket)[2];
                        checkAndVisualizeMessage(messageType, messageSender, messageContent);
                        stashedMessages.remove(ticket);
                        ticket++;
                    }
                } else if (messageTicket > ticket + 1) {
                    stashedMessages.put(messageTicket, new String[]{type, sender, message});  // stash
                }
            }
        });
    }

    private void sendMessage() throws Exception {
        String message = messageTextArea.getText();
        Map<String, Object> headers = new HashMap<>();
        headers.put(CHATROOM_NAME_HEADER, chatroom);
        headers.put(CHATTER_NAME_HEADER, nickname);
        if (message.equals(ENTER_CS_COMMAND)) {
            csChannel.basicPublish("", REQUEST_ENTER_CRITICAL_SECTION_QUEUE, new AMQP.BasicProperties.Builder().headers(headers).build(), null);
            messageTextArea.clear();
            messageTextArea.requestFocus();

        } else if (message.equals(EXIT_CS_COMMAND)) {
            csChannel.basicPublish("", REQUEST_EXIT_CRITICAL_SECTION_QUEUE, new AMQP.BasicProperties.Builder().headers(headers).build(), null);
            messageTextArea.clear();
            messageTextArea.requestFocus();
        } else {
            byte[] messageSerialized = SerializationUtils.serialize(message);
            messagingChannel.basicPublish("", REQUEST_MESSAGE_QUEUE, new AMQP.BasicProperties.Builder().headers(headers).build(), messageSerialized);
            messageTextArea.clear();
            messageTextArea.requestFocus();
        }
    }

    private HBox createBoxedMessage(String name, String message) {
        HBox box = new HBox();
        Label l1 = new Label(name + " : ");
        l1.setTextFill(Color.BLUE);
        Label l2 = new Label(message);
        box.getChildren().add(l1);
        box.getChildren().add(l2);
        return box;
    }

    private void checkAndVisualizeMessage(String messageType, String senderName, String message) {
        switch (messageType) {
            case LOGIN_MESSAGE:
                visualizeChatterLoggedIn(senderName);
                break;
            case LOGOUT_MESSAGE:
                visualizeChatterLoggedOut(senderName);
                break;
            case CS_ENTER_MESSAGE:
                visualizeEnterCS(senderName);
                break;
            case CS_EXIT_MESSAGE:
                visualizeExitCS(senderName);
                break;
            default:    //CHATTER_MESSAGE
                visualizeInboxMessage(senderName, message);
                break;
        }
    }

    private void visualizeInboxMessage(String senderName, String message) {
        Platform.runLater(() -> {
            listView.getItems().add(createBoxedMessage(senderName, message));
        });
    }

    private void visualizeChatterLoggedIn(String name) {
        Platform.runLater(() -> {
            Label l = new Label("-> " + name + " logged in.");
            l.setTextFill(Color.GREEN);
            listView.getItems().add(l);
        });
    }

    private void visualizeChatterLoggedOut(String name) {
        Platform.runLater(() -> {
            Label l = new Label("-> " + name + " logged out.");
            l.setTextFill(Color.RED);
            listView.getItems().add(l);
        });
    }

    private void visualizeEnterCS(String name) {
        Platform.runLater(() -> {
            Label l = new Label("!!! " + name + " entered in critical section !!!");
            l.setTextFill(Color.ORANGE);
            listView.getItems().add(l);
        });
    }

    private void visualizeExitCS(String name) {
        Platform.runLater(() -> {
            Label l = new Label("!!! " + name + " left the critical section !!!");
            l.setTextFill(Color.ORANGE);
            listView.getItems().add(l);
        });
    }

    public void logout() {
        try {
            // Send logout message and delete the queue
            Map<String, Object> headers = new HashMap<>();
            headers.put(CHATROOM_NAME_HEADER, chatroom);
            headers.put(CHATTER_NAME_HEADER, nickname);
            logoutChannel.basicPublish("", REQUEST_LOGOUT_QUEUE, new AMQP.BasicProperties.Builder().headers(headers).build(), null);
            messagingChannel.queueDelete(nickname + chatroom);
            // Tell the cs-service to exit critical section (if necessary)
            csChannel.basicPublish("", REQUEST_EXIT_CRITICAL_SECTION_QUEUE, new AMQP.BasicProperties.Builder().headers(headers).build(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeChannels() {
        try {
            if (messagingChannel != null && messagingChannel.isOpen()) {
                messagingChannel.close();
            }
            if (logoutChannel != null && logoutChannel.isOpen()) {
                logoutChannel.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
