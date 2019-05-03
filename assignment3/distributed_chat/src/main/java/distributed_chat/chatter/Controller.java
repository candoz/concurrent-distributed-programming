package distributed_chat.chatter;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import distributed_chat.messages.LoginRequestFromControllerMsg;
import distributed_chat.messages.LogoutRequestFromMyControllerMsg;
import distributed_chat.messages.MessageFromMyControllerMsg;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private TextField nicknameTextField;
    @FXML
    private Button logButton;
    @FXML
    private ListView listView;
    @FXML
    private HBox messageBox;
    @FXML
    private TextArea messageTextArea;
    @FXML
    private Button messageButton;

    private ActorRef chatter;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Config config = ConfigFactory.parseFile(new File("src/main/java/distributed_chat/chatter/chatter.conf"));
        ActorSystem system = ActorSystem.create("MySystem",config);
        chatter = system.actorOf(Props.create(Chatter.class, this));

        logButton.disableProperty().bind(Bindings.isEmpty(nicknameTextField.textProperty()));
        messageButton.disableProperty().bind(Bindings.isEmpty(messageTextArea.textProperty()));
        messageTextArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                if (event.isShiftDown()) {
                    messageTextArea.appendText(System.getProperty("line.separator"));
                } else {
                    if(!messageTextArea.getText().isEmpty()){
                        sendMessage();
                    }
                }
            }
        });
        nicknameTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                if(!nicknameTextField.getText().isEmpty()){
                    login();
                }
            }
        });

    }

    @FXML
    private void sendPressed(ActionEvent event) {
        sendMessage();
        messageTextArea.requestFocus();
    }

    @FXML
    private void logButtonPressed(ActionEvent event) {
        if(nicknameTextField.isDisable()) {
            logout();
        } else {
            login();
        }
    }

    private void login(){
        /*try {
            java.nio.file.Files.write(Paths.get("src/main/java/distributed_chat/"+nicknameTextField.getText()+".txt"),
                    "".getBytes("utf-8"),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        logButton.setText("Logout");
        nicknameTextField.setDisable(true);
        messageBox.setDisable(false);
        chatter.tell((new LoginRequestFromControllerMsg(nicknameTextField.getText())), ActorRef.noSender());
        messageTextArea.requestFocus();
    }

    private void logout(){
        logButton.setText("Login");
        nicknameTextField.setDisable(false);
        messageBox.setDisable(true);
        sendLogoutMessageToMyChatter();
        messageTextArea.clear();
        nicknameTextField.requestFocus();
    }

    private void sendMessage() {
        //for(int i = 0; i < 350; i++)
            chatter.tell(new MessageFromMyControllerMsg(chatter, nicknameTextField.getText(), messageTextArea.getText()),ActorRef.noSender());
        messageTextArea.clear();
    }

    private HBox createBoxedMessage(String name, String message){
        HBox box = new HBox();
        Label l1 = new Label(name + " : ");
        l1.setTextFill(Color.BLUE);
        Label l2 = new Label(message);
        box.getChildren().add(l1);
        box.getChildren().add(l2);
        return box;
    }

    public void visualizeInboxMessage(String senderName, String message) {
        /*try {
            java.nio.file.Files.write(Paths.get("src/main/java/distributed_chat/"+nicknameTextField.getText()+".txt"),
                    (senderName +" : "+ message+"\n").getBytes("utf-8"),
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        Platform.runLater(() -> {
            listView.getItems().add(createBoxedMessage(senderName, message));
        });
    }

    public void visualizeChatterLoggedIn(String name) {
        /*try {
            java.nio.file.Files.write(Paths.get("src/main/java/distributed_chat/"+nicknameTextField.getText()+".txt"),
                    (name +" logged in.\n").getBytes("utf-8"),
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        Platform.runLater(() -> {
            Label l = new Label("-> " + name+" logged in.");
            l.setTextFill(Color.GREEN);
            listView.getItems().add(l);
        });
    }

    public void visualizeChatterLoggedOut(String name) {
        /*try {
            java.nio.file.Files.write(Paths.get("src/main/java/distributed_chat/"+nicknameTextField.getText()+".txt"),
                    (name +" logged out.\n").getBytes("utf-8"),
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        Platform.runLater(() -> {
            Label l = new Label("-> " + name+" logged out.");
            l.setTextFill(Color.RED);
            listView.getItems().add(l);
        });
    }

    public void visualizeEnterCS(String name) {
        Platform.runLater(() -> {
            Label l = new Label("!!! " + name + " entered critical section !!!");
            l.setTextFill(Color.ORANGE);
            listView.getItems().add(l);
        });
    }

    public void visualizeExitCS(String name) {
        Platform.runLater(() -> {
            Label l = new Label("!!! " + name + " exited critical section !!!");
            l.setTextFill(Color.ORANGE);
            listView.getItems().add(l);
        });
    }

    public void sendLogoutMessageToMyChatter(){
        chatter.tell(new LogoutRequestFromMyControllerMsg(nicknameTextField.getText()), ActorRef.noSender());
    }

}
