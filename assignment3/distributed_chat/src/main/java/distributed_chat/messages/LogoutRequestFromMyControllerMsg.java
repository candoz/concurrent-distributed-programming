package distributed_chat.messages;

import java.io.Serializable;

public class LogoutRequestFromMyControllerMsg implements Serializable {
    private final String name;

    public LogoutRequestFromMyControllerMsg(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
