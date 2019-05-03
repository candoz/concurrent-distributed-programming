package distributed_chat.messages;

import distributed_chat.chatter.MessageInfo;

import java.io.Serializable;

public class MessageFromChatterMsg implements Serializable {

    private final MessageInfo messageInfo;

    public MessageFromChatterMsg(MessageInfo messageInfo) {
        this.messageInfo = messageInfo;
    }

    public MessageInfo getMessageInfo() {
        return messageInfo;
    }

}
