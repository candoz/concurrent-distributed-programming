package distributed_chat.messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class MessageFromMyControllerMsg implements Serializable {

    private ActorRef chatterRef;
    private String name, message;

    public MessageFromMyControllerMsg(ActorRef chatterRef, String name, String message) {
        this.chatterRef = chatterRef;
        this.name = name;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public ActorRef getChatterRef() {
        return chatterRef;
    }

}
