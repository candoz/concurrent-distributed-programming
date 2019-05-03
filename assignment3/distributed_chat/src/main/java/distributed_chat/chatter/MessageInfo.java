package distributed_chat.chatter;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.Objects;

public class MessageInfo implements Serializable {

    private final ActorRef senderRef;
    private final String senderName;
    private final String message;
    private final long senderLocalCounter;

    public MessageInfo(ActorRef senderRef, String senderName, String message, long senderLocalCounter) {
        this.senderRef = senderRef;
        this.senderName = senderName;
        this.senderLocalCounter = senderLocalCounter;
        this.message = message;
    }

    public ActorRef getSenderRef() {
        return senderRef;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public long getLocalCounter() {
        return senderLocalCounter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageInfo that = (MessageInfo) o;
        return senderLocalCounter == that.senderLocalCounter &&
                Objects.equals(senderRef, that.senderRef) &&
                Objects.equals(senderName, that.senderName) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderRef, senderName, message, senderLocalCounter);
    }

}
