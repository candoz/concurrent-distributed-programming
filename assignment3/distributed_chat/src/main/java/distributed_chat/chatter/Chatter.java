package distributed_chat.chatter;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import distributed_chat.messages.*;

import java.time.Duration;
import java.util.*;

public class Chatter extends AbstractActorWithStash {

    public final String ENTER_CS_COMMAND = ":enter-cs";
    public final String EXIT_CS_COMMAND = ":exit-cs";
    public final int MAX_CS_MILLIS = 10000;

    private final ActorSelection register = getContext().actorSelection("akka.tcp://MySystem@127.0.0.1:7851/user/register");
    private Controller myController;
    private String myName;
    private long localCounter;

    private List<ActorRef> chatters;

    private Map<MessageInfo,Integer> votesMap;
    private Set<ActorRef> votersSet;
    private MessageInfo myVoteMessageInfo;

    private Set<MessageInfo> messagesAlreadyVisualizedButNotReceivedYet;
    private Set<MessageInfo> messagesAlreadySentButNotVisualizedYet;

    private long votePulse;

    private ActorRef whoIsInCriticalSectionRef;
    private String whoIsInCriticalSectionName;

    /* BEHAVIORS */
    private Receive loggedOutBehavior;
    private Receive defaultBehavior;
    private Receive waitForVotesThenVisualize;
    private Receive inCriticalSection;


    public Chatter(Controller myController) {
        this.myController = myController;

        /* BEHAVIOR: while logged out behavior */
        loggedOutBehavior = receiveBuilder(
            ).match(LoginRequestFromControllerMsg.class, loginRequestFromControllerMsg -> {
                messagesAlreadyVisualizedButNotReceivedYet = new HashSet<>();
                messagesAlreadySentButNotVisualizedYet = new HashSet<>();
                localCounter = 0;
                votePulse = 0;
                myName = loginRequestFromControllerMsg.getName();
                chatters = new ArrayList<>();
                register.tell(new RequestChattersListMsg(), getSelf());
            }).match(ChattersListMsg.class, chattersListMsg -> {
                chatters.addAll(chattersListMsg.getChatters());
                if(chatters.size() == 0){
                    register.tell(new AddMeToChattersMsg(), getSelf());
                    chatters.add(getSelf());
                    myController.visualizeChatterLoggedIn("You");
                    getContext().become(defaultBehavior);
                } else {
                    for (ActorRef chatter: chatters) {
                        chatter.tell(new LoginRequestFromChatterMsg(myName), getSelf());
                    }
                }
            }).match(LoginAcceptedMsg.class, loginAcceptedMsg -> {
                votePulse = loginAcceptedMsg.getPulse(); //prendiamo l'ultimo ma devono essere tutti uguali
                register.tell(new AddMeToChattersMsg(), getSelf());
                chatters.add(getSelf());
                myController.visualizeChatterLoggedIn("You");
                getContext().become(defaultBehavior);
            }).matchAny(otherMsg -> {}).build();

        /* BEHAVIOR: Default behavior */
        defaultBehavior = receiveBuilder(
            ).match(LoginRequestFromChatterMsg.class, msg -> {
                MessageInfo messageInfo = new MessageInfo(getSender(), msg.getName(), ":login",0);
                if (messagesAlreadyVisualizedButNotReceivedYet.contains(messageInfo)) {
                    messagesAlreadyVisualizedButNotReceivedYet.remove(messageInfo);
                } else {
                    voteForThisMsg(messageInfo);
                    getContext().become(waitForVotesThenVisualize, false);
                }
            }).match(MessageFromMyControllerMsg.class, msgContr -> {
                localCounter++;
                MessageInfo messageInfo = new MessageInfo(msgContr.getChatterRef(), msgContr.getName(), msgContr.getMessage(), localCounter);
                messagesAlreadySentButNotVisualizedYet.add(messageInfo);
                for (ActorRef chatter : chatters) {
                    if(!chatter.equals(getSelf())) {
                        chatter.tell(new MessageFromChatterMsg(messageInfo), getSelf());
                    }
                }
                voteForThisMsg(messageInfo);
                getContext().become(waitForVotesThenVisualize, false);
            }).match(MessageFromChatterMsg.class, msgChatter -> {
                MessageInfo messageInfo = msgChatter.getMessageInfo();
                if (messagesAlreadyVisualizedButNotReceivedYet.contains(messageInfo)) {
                    messagesAlreadyVisualizedButNotReceivedYet.remove(messageInfo);
                } else {
                    voteForThisMsg(messageInfo);
                    getContext().become(waitForVotesThenVisualize, false);
                }
            }).match(LogoutRequestFromMyControllerMsg.class, msg -> {
                chatters.remove(getSelf());
                myController.visualizeChatterLoggedOut("You");
                register.tell(new RemoveMeFromChattersMsg(), getSelf());
                for(ActorRef chatter : chatters){
                    chatter.tell(new LogoutNotificationFromAnotherChatterMsg(msg.getName()), getSelf());
                }
                getContext().become(loggedOutBehavior);
            }).match(LogoutNotificationFromAnotherChatterMsg.class, msg -> {
                chatters.remove(getSender());
                myController.visualizeChatterLoggedOut(msg.getName());
            }).match(CSTimeoutExpiredMsg.class, timeoutMsg -> {
                // discard every Critical Section timeout msg
            }).matchAny(otherMsg -> stash()).build();

        /* BEHAVIOR: Wait for votes then visualize the elected one (no one is asking to login) */
        waitForVotesThenVisualize = receiveBuilder(
            ).match(VoteMsg.class, voteMsg -> {
                if (voteMsg.getVotePulse() == votePulse) {
                    if(votersSet.add(getSender())) { //potrebbe tornare utile per implementare il crash model
                        MessageInfo messageInfo = voteMsg.getMessageInfo();
                        int votes = 1;
                        if(votesMap.containsKey(messageInfo)) {
                            votes += votesMap.get(messageInfo);
                        }
                        votesMap.put(messageInfo, votes);
                        if(votesMap.get(voteMsg.getMessageInfo()) > chatters.size()/2) {
                            electMessage(voteMsg.getMessageInfo());
                        } else if (votersSet.size() == chatters.size()) {
                            electMessage(getElectedMessageInfo());
                        }
                    }
                } else if(voteMsg.getVotePulse() > votePulse){
                    stash();  // In case I receive a msg with votePulse = myPulse + 1
                }
            }).match(LogoutNotificationFromAnotherChatterMsg.class, msg -> {
                chatters.remove(getSender());
                myController.visualizeChatterLoggedOut(msg.getName());
                if(!votersSet.contains(getSender())) {
                    for (Map.Entry<MessageInfo, Integer> entry : votesMap.entrySet()) {
                        if (entry.getValue() > chatters.size() / 2) {
                            electMessage(entry.getKey());
                        } else if (votersSet.size() == chatters.size()) {
                            electMessage(getElectedMessageInfo());
                        }
                    }
                }
                unstashAll();
            }).matchAny(otherMsg -> stash()).build();


        /* BEHAVIOR: In critical section */
        inCriticalSection = receiveBuilder(
            ).match(MessageFromMyControllerMsg.class, msgContr -> {
                if (getSelf().equals(whoIsInCriticalSectionRef)) {
                    localCounter++;
                    MessageInfo messageInfo = new MessageInfo(getSelf(), myName, msgContr.getMessage(), localCounter);
                    for (ActorRef chatter : chatters) { // rimbalzo il messaggio a tutti, tra cui me stesso
                        chatter.tell(new MessageFromChatterMsg(messageInfo), getSelf());
                    }
                }  // else discard every message I'm trying to send while in critical section
            }).match(MessageFromChatterMsg.class, msgChatter -> {
                if (getSender().equals(whoIsInCriticalSectionRef)) {
                    String message = msgChatter.getMessageInfo().getMessage();
                    if (message.equals(EXIT_CS_COMMAND)) {
                        myController.visualizeExitCS(whoIsInCriticalSectionName);
                        unstashAll();
                        getContext().become(defaultBehavior);
                    } else {
                        String senderName = msgChatter.getMessageInfo().getSenderName();
                        myController.visualizeInboxMessage(senderName, message);
                    }
                } else {
                    stash();  // stash every message that was sent by someone before entering the critical section...
                              // ... or after he has already exited!
                }
            }).match(CSTimeoutExpiredMsg.class, timeoutMsg -> {  // Simulo il fatto che qualcuno abbia mandato il comando di stop dalla console
                MessageInfo forgedExitMessageInfo = new MessageInfo(  // Forgio un messaggio di uscita dalla CS
                        getSelf(),
                        myName,
                        EXIT_CS_COMMAND,
                        ++localCounter
                );
                for (ActorRef chatter : chatters) {
                    chatter.tell(new MessageFromChatterMsg(forgedExitMessageInfo), getSelf());
                }
            }).match(LogoutRequestFromMyControllerMsg.class, msg -> {
                    myController.visualizeChatterLoggedOut("You");
                    register.tell(new RemoveMeFromChattersMsg(), getSelf());
                    chatters.remove(getSelf());
                    if (getSelf().equals(whoIsInCriticalSectionRef)) {
                        MessageInfo forgedExitMessageInfo = new MessageInfo(
                                getSelf(),
                                myName,
                                EXIT_CS_COMMAND,
                                ++localCounter
                        );
                        for (ActorRef chatter : chatters) {
                            chatter.tell(new MessageFromChatterMsg(forgedExitMessageInfo), getSelf());
                        }
                    }
                    for (ActorRef chatter : chatters) {
                        chatter.tell(new LogoutNotificationFromAnotherChatterMsg(msg.getName()), getSelf());
                    }
                    getContext().become(loggedOutBehavior);
            }).matchAny(otherMsg -> stash()).build();
    }

    @Override
    public Receive createReceive() {
        return loggedOutBehavior;
    }

    /* PRIVATE METHODS */

    private void voteForThisMsg(MessageInfo messageInfo) {
        votesMap = new HashMap<>();
        votersSet = new HashSet<>();
        myVoteMessageInfo = messageInfo;
        for (ActorRef chatter : chatters) {
            chatter.tell(new VoteMsg(messageInfo, votePulse), getSelf());
        }
        unstashAll();
    }

    private void electMessage(MessageInfo electedMessageInfo){
        votePulse++;
        ActorRef senderRef = electedMessageInfo.getSenderRef();
        String senderName = electedMessageInfo.getSenderName();
        String message = electedMessageInfo.getMessage();

        if (message.equals(ENTER_CS_COMMAND)) {  // E' UN MESSAGGIO DI SEZIONE CRITICA
            whoIsInCriticalSectionRef = senderRef;
            whoIsInCriticalSectionName = senderName;
            myController.visualizeEnterCS(senderName);
            ActorSystem system = getContext().getSystem();
            if (getSelf().equals(whoIsInCriticalSectionRef)) {
                system.scheduler().scheduleOnce(
                        Duration.ofMillis(MAX_CS_MILLIS),
                        getSelf(),
                        new CSTimeoutExpiredMsg(),
                        system.dispatcher(),
                        getSelf());
            }

            if(messagesAlreadySentButNotVisualizedYet.contains(electedMessageInfo)) {
                messagesAlreadySentButNotVisualizedYet.remove(electedMessageInfo);
            }
            if (myVoteMessageInfo.equals(electedMessageInfo)) {
                myVoteMessageInfo = null;
            } else {
                messagesAlreadyVisualizedButNotReceivedYet.add(electedMessageInfo);
                voteForThisMsg(myVoteMessageInfo);
            }

            getContext().become(inCriticalSection);

        } else {  // NON E' UN MESSAGGIO DI SEZIONE CRITICA
            if(chatters.contains(electedMessageInfo.getSenderRef())) {
                myController.visualizeInboxMessage(electedMessageInfo.getSenderName(), electedMessageInfo.getMessage());
                if(messagesAlreadySentButNotVisualizedYet.contains(electedMessageInfo)) {
                    messagesAlreadySentButNotVisualizedYet.remove(electedMessageInfo);
                }

            } else {  //eletto un messaggio di login
                myController.visualizeChatterLoggedIn(electedMessageInfo.getSenderName());
                chatters.add(electedMessageInfo.getSenderRef());
                electedMessageInfo.getSenderRef().tell(new LoginAcceptedMsg(votePulse),getSelf());
                for(MessageInfo messageInfo : messagesAlreadySentButNotVisualizedYet){
                    electedMessageInfo.getSenderRef().tell(new MessageFromChatterMsg(messageInfo),getSelf());
                }
            }
            if (myVoteMessageInfo.equals(electedMessageInfo)) {
                myVoteMessageInfo = null;
                getContext().become(defaultBehavior);
            } else {
                messagesAlreadyVisualizedButNotReceivedYet.add(electedMessageInfo);
                voteForThisMsg(myVoteMessageInfo);
            }
            unstashAll();
        }
    }

    private MessageInfo getElectedMessageInfo(){
        int maxVotes = 0;
        MessageInfo electedMessageInfo = null;
        for (Map.Entry<MessageInfo, Integer> messageInfoEntry : votesMap.entrySet()) {
            MessageInfo messageInfo = messageInfoEntry.getKey();
            int votes = messageInfoEntry.getValue();
            if (maxVotes <= votes){
                if(maxVotes == votes){
                    if (chatters.indexOf(electedMessageInfo.getSenderRef()) > chatters.indexOf(messageInfo.getSenderRef())) {
                        electedMessageInfo = messageInfo;
                    }
                } else {
                    maxVotes = votes;
                    electedMessageInfo = messageInfo;
                }
            }
        }
        return electedMessageInfo;
    }

}