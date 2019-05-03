package distributed_chat.register;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import distributed_chat.messages.*;

public class Register extends AbstractActorWithStash {
    private ImmutableList<ActorRef> chatters = new ImmutableList.Builder<ActorRef>().build();

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(RequestChattersListMsg.class, msg -> {
            getSender().tell(new ChattersListMsg(chatters), ActorRef.noSender());
                getContext().become(
                    receiveBuilder().match(AddMeToChattersMsg.class, msg1 -> {
                        chatters = new ImmutableList.Builder<ActorRef>()
                                .addAll(chatters)
                                .add(getSender())
                                .build();
                        unstashAll();
                        getContext().unbecome();
                    }).matchAny(msg1 -> stash()).build(), false);

        }).match(RemoveMeFromChattersMsg.class, msg -> {
            chatters = ImmutableList.copyOf(Collections2.filter(chatters, Predicates.not(Predicates.equalTo(getSender()))));
        }).build();
    }
    
}
