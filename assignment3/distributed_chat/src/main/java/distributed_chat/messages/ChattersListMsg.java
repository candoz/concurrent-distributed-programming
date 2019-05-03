package distributed_chat.messages;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.List;

public class ChattersListMsg implements Serializable {

    private final List<ActorRef> chatters;

    public ChattersListMsg(List<ActorRef> chatters) {
        this.chatters = chatters;
    }

    public List<ActorRef> getChatters() {
        return chatters;
    }
}
