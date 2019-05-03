package distributed_chat.messages;

import distributed_chat.chatter.MessageInfo;

import java.io.Serializable;


public class VoteMsg implements Serializable {

    private final MessageInfo messageInfo;
    private final long votePulse;

    public VoteMsg(MessageInfo messageInfo, long votePulse) {
        this.messageInfo = messageInfo;
        this.votePulse = votePulse;
    }

    public MessageInfo getMessageInfo() {
        return messageInfo;
    }

    public long getVotePulse() {
        return votePulse;
    }

}
