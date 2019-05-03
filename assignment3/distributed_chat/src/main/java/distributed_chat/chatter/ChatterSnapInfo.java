package distributed_chat.chatter;

import akka.actor.ActorRef;

public class ChatterSnapInfo {

    private final ActorRef actorRef;
    private final String actorName;
    private final Long lastMsgSentNumber;

    public ChatterSnapInfo(ActorRef actorRef, String actorName, Long lastMsgSentNumber) {
        this.actorRef = actorRef;
        this.actorName = actorName;
        this.lastMsgSentNumber = lastMsgSentNumber;
    }

    public ActorRef getActorRef() {
        return actorRef;
    }

    public String getActorName() {
        return actorName;
    }

    public Long getLastMsgSentNumber() {
        return lastMsgSentNumber;
    }

}
