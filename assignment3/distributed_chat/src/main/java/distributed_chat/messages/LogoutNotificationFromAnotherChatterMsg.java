package distributed_chat.messages;

import java.io.Serializable;

public class LogoutNotificationFromAnotherChatterMsg implements Serializable {

    private String name;

    public LogoutNotificationFromAnotherChatterMsg(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
